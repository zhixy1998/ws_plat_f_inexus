package com.persee.workstation.uv_inexus.util.changeUtil.T800;

import com.persee.workstation.uv_inexus.constant.Constants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.persee.workstation.uv_inexus.util.changeUtil.T800.ResponseParser.inContinuous;
import static com.persee.workstation.uv_inexus.util.changeUtil.T800.T800MessageUtil.resultMap;

public class CommandExecutor implements AutoCloseable {

    private final String localIp;
    private final int localPort;
    private final int remotePort;
    private final DatagramSocket socket;

    private static final long CONTINUOUS_TIMEOUT = 2000; // 连续报文超时 2 秒
    private long lastContinuousTime = 0;

    // 队列 + 状态
    private final BlockingDeque<SendReceivePair> receiveQueue = new LinkedBlockingDeque<>();
    private volatile boolean running = true;

    // 指令映射表
    public final Map<String, ProtocolItem> commandMap = new HashMap<>();

    // 发送报文缓存（可选）
    private final Map<String, byte[]> sentMap = new HashMap<>();

    // 构造函数
    public CommandExecutor(String localIp, int localPort, int remotePort, String xmlPath) throws Exception {
        this.localIp = localIp;
        this.localPort = localPort;
        this.remotePort = remotePort;

        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(localIp, localPort));
        socket.setBroadcast(true);
        socket.setSoTimeout(5500);

        // 加载 XML 协议文件
//        Map<String, T800Transition> loadedMap = protocolItem.loadFormXml(xmlPath);
//        this.commandMap.putAll(loadedMap);

        // 启动接收和解析线程
        new Thread(this::receiveLoop, "UDP-Receiver").start();
        new Thread(this::parseLoop, "UDP-Parser").start();
    }

    /** 接收线程 */
    private AtomicLong recvCount = new AtomicLong();

    /**
     * 接收数据包的主循环函数
     *
     * 该函数在独立线程中运行，持续监听并处理来自网络的数据包。
     * 对于每个接收到的数据包，函数会解析出设备IP地址，匹配对应的命令ID，
     * 并将发送和接收的数据对放入处理队列中。
     *
     * 函数会一直运行直到running标志被设置为false。
     */
    private void receiveLoop() {
        while (running) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(new byte[4096], 4096);
                socket.receive(receivePacket);

                long count = recvCount.incrementAndGet();
                byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                String deviceIp = receivePacket.getAddress().getHostAddress();

                System.out.println("收到第 " + count + " 条来自设备 " + deviceIp + " 的报文，长度=" + reply.length);

                String cmdId = findCmdIdForReply(reply);   // 根据 reply 匹配发送报文的 cmdId
                byte[] sendBytes = sentMap.get(cmdId);


                // 放入队列
                receiveQueue.offer(new SendReceivePair(cmdId, sendBytes, reply));

            } catch (Exception e) {
                if (running) e.printStackTrace();
            }
        }
    }


    /** 解析线程 */
//    private void parseLoop() {
//        while (running) {
//            try {
//                SendReceivePair pair = receiveQueue.take();
//                byte[] recvBytes = pair.receiveBytes;
//                byte[] sendBytes = pair.sendBytes;
//                String cmdId = pair.cmdId;
//
//                System.out.println("开始解析报文：" + Arrays.toString(recvBytes));
//
//                Object result;
//
//                if (sendBytes == null && recvBytes[5] == 0x66) {
//                    result = ResponseParser.parsePassiveReport(recvBytes);
//                } else if (sendBytes == null) {
//                    result = ResponseParser.parseNullReport(recvBytes);
//                    ResponseParser.parseNullReport(recvBytes);
//                } else {
//                    result = ResponseParser.parseMultipleResponses(
//                            Collections.singletonList(recvBytes), sendBytes);
//                }
//
//                if (cmdId != null) {
//                    BlockingDeque<List<Object>> queue = resultMap.computeIfAbsent(cmdId, k -> new LinkedBlockingDeque<>());
//                    queue.offer(Collections.singletonList(result));
//                }
//
//                System.out.println("解析结果：" + result); // result 是 Map 或 List，可用自定义打印更友好
//            } catch (Exception e) {
//                System.err.println("【解析线程出错】" + e.getMessage());
//                e.printStackTrace();
//            }
//        }
//    }

    private void parseLoop() {
        while (running) {
            try {
                SendReceivePair pair = receiveQueue.poll(200, TimeUnit.MILLISECONDS); // 200ms超时
                long now = System.currentTimeMillis();

                if (pair == null) {
                    // 如果正在连续模式，并且超过超时时间，认为结束
                    if (inContinuous && (now - lastContinuousTime > CONTINUOUS_TIMEOUT)) {
                        Constants.instrumentData.add(Constants.EndData);
                        System.out.println("连续报文超时结束，插入 EndData");
                        inContinuous = false;
                    }
                    continue;
                }

                byte[] recvBytes = pair.receiveBytes;
                byte[] sendBytes = pair.sendBytes;
                String cmdId = pair.cmdId;

                if (recvBytes == null || recvBytes.length < 6) {
                    System.err.println("收到报文过短，丢弃：" + (recvBytes == null ? "null" : recvBytes.length));
                    continue;
                }

                System.out.println("开始解析报文：" + Arrays.toString(recvBytes));

                Object result = null;

                if (sendBytes == null) {
                    if (recvBytes.length > 5 && recvBytes[5] == 0x66) {
                        result = ResponseParser.parsePassiveReport(recvBytes);
                    } else {
                        result = ResponseParser.parseNullReport(recvBytes);
                    }
                } else {
                    result = ResponseParser.parseMultipleResponses(
                            Collections.singletonList(recvBytes), sendBytes);
                }

                // 如果进入连续模式，更新时间戳
                if (inContinuous) {
                    lastContinuousTime = now;
                }

                if (cmdId != null) {
                    BlockingDeque<List<Object>> queue =
                            resultMap.computeIfAbsent(cmdId, k -> new LinkedBlockingDeque<>());
                    queue.offer(Collections.singletonList(result));
                }

                System.out.println("解析结果：" + result);
            } catch (Exception e) {
                System.err.println("【解析线程出错】" + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    /** 发送命令 */
    private final BlockingDeque<String> sendOrderQueue = new LinkedBlockingDeque<>();

    public void sendCommand(String cmdId, byte[] sendBytes, String sendIp) throws Exception {
        DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                InetAddress.getByName(sendIp), remotePort);
        socket.send(packet);

        // 保存发送报文（可选，用于解析匹配）
        sendOrderQueue.add(cmdId);
        sentMap.put(cmdId, sendBytes);

        System.out.print("发送指令 [" + cmdId + "] 报文：");
        for (byte b : sendBytes) System.out.printf("%02X ", b);
        System.out.println();
    }

    /** 队列中发送+接收报文的配对对象 */

    public class SendReceivePair {
        public final String cmdId;
        public final byte[] sendBytes;
        public final byte[] receiveBytes;

        public SendReceivePair(String cmdId, byte[] sendBytes, byte[] receiveBytes) {
            this.cmdId = cmdId;
            this.sendBytes = sendBytes;
            this.receiveBytes = receiveBytes;
        }
    }

    private String findCmdIdForReply(byte[] reply) {
        // FIFO，先发送的指令对应先收到的报文
        return sendOrderQueue.poll(); // 返回并移除队列头
    }


    @Override
    public void close() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
