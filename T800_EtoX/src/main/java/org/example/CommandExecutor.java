package org.example;

import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

public class CommandExecutor implements AutoCloseable {

    private final String localIp;
    private final int localPort;
    private final int remotePort;
    private final DatagramSocket socket;

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
        socket.setSoTimeout(3000);

        // 加载 XML 协议文件
        Map<String, ProtocolItem> loadedMap = ProtocolItem.loadFormXml(xmlPath);
        this.commandMap.putAll(loadedMap);

        // 启动接收和解析线程
        new Thread(this::receiveLoop, "UDP-Receiver").start();
        new Thread(this::parseLoop, "UDP-Parser").start();
    }

    /** 接收线程 */
    private AtomicLong recvCount = new AtomicLong();

//    private void receiveLoop() {
//        try {
//            socket.setSoTimeout(3000); // 2秒超时，避免一直卡住
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
//        while (running) {
//            try {
//                DatagramPacket receivePacket = new DatagramPacket(new byte[4096], 4096);
//                socket.receive(receivePacket);
//
//                long count = recvCount.incrementAndGet();
//
//                byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
//                String deviceIp = receivePacket.getAddress().getHostAddress();
//
//                System.out.println("收到第 " + count + " 条来自设备 " + deviceIp + " 的报文，长度=" + reply.length);
//
//                receiveQueue.offer(new SendReceivePair(null, null, reply));
//
//            } catch (SocketTimeoutException e) {
//                System.out.println("接收超时，等待中...");
//            } catch (Exception e) {
//                if (running) e.printStackTrace();
//            }
//        }
//    }


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
    private void parseLoop() {
        while (running) {
            try {
                SendReceivePair pair = receiveQueue.take();
                byte[] recvBytes = pair.receiveBytes;
                byte[] sendBytes = pair.sendBytes;

                System.out.print("开始解析报文：");
                for (byte b : recvBytes) System.out.printf("%02X ", b);
                System.out.println();

                List<Object> result;
                if (sendBytes != null) {
                    result = ResponseParser.parseMultipleResponses(
                            Collections.singletonList(recvBytes), sendBytes);
                } else {
                    result = ResponseParser.parsePassiveReport(recvBytes);
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
    private static class SendReceivePair {
        final String cmdId;
        final byte[] sendBytes;
        final byte[] receiveBytes;

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
