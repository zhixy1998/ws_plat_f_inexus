/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.changeUtil.T800;

import com.persee.workstation.uv_inexus.model.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service("T800MessageUtil")
public class T800MessageUtil{

    @Value("${device.default.sendPort}")
    private Integer remotePort;

    @Value("${device.default.receivePort}")
    private Integer localPort;

    @Value("${device.default.T800xmlPath}")
    private String T800xmlPath;

    private DatagramSocket socket;

    private volatile boolean running = true;

    private boolean started = false;

    private final BlockingDeque<SendReceivePair> receiveQueue = new LinkedBlockingDeque<>();
    private final BlockingDeque<String> sendOrderQueue = new LinkedBlockingDeque<>();
    private final Map<String,  T800Transition> commandMap = new HashMap<>();
    private final Map<String, byte[]> sentMap = new ConcurrentHashMap<>();
    // 每个 cmdId 对应一个结果队列
    static Map<String, BlockingDeque<List<Object>>> resultMap = new ConcurrentHashMap<>();


    private final AtomicLong recvCount = new AtomicLong();

    /**
     * 获取Socket（单例模式）
     */
//    public R<DatagramSocket> getSocket() {
//        String localIp = getEthernetIPv4();
//        if (localIp == null) {
//            return R.fail("未找到可用的IP");
//        }
//
//        try {
//            if (socket == null) {
//                socket = new DatagramSocket(localPort, InetAddress.getByName(localIp));
//                socket.setBroadcast(true);
//                socket.setSoTimeout(5500);
//
//                // 启动接收+解析线程（只启动一次）
//                if (!started) {
//                    new Thread(this::receiveLoop, "UDP-Receiver").start();
//                    new Thread(this::parseLoop, "UDP-Parser").start();
//                    started = true;
//                }
//
//                System.out.println("UDP Socket 初始化完成，本地IP=" + localIp + " 端口=" + localPort);
//            }
//            return R.ok(socket);
//        } catch (Exception e) {
//            return R.fail(e.getMessage());
//        }
//    }

//    public synchronized R<DatagramSocket> getSocket() {
//        if (socket != null && !socket.isClosed()) {
//            // 已经存在可用 socket，直接返回
//            return R.ok(socket);
//        }
//
//        String localIp = getEthernetIPv4();
//        if (localIp == null) return R.fail("未找到可用的IP");
//
//        try {
//            socket = new DatagramSocket(localPort, InetAddress.getByName(localIp));
//            socket.setBroadcast(true);
//            socket.setSoTimeout(5500);
//
//            // 启动接收+解析线程（只启动一次）
//            if (!started) {
//                new Thread(this::receiveLoop, "UDP-Receiver").start();
//                new Thread(this::parseLoop, "UDP-Parser").start();
//                started = true;
//            }
//
//            System.out.println("UDP Socket 初始化完成，本地IP=" + localIp + " 端口=" + localPort);
//            return R.ok(socket);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return R.fail("创建 DatagramSocket 失败: " + e.getMessage());
//        }
//    }

    public synchronized R<DatagramSocket> getSocket() {
        if (socket != null && !socket.isClosed()) {
            // 已经存在可用 socket，直接返回
            return R.ok(socket);
        }

        String localIp = getEthernetIPv4();
        if (localIp == null) return R.fail("未找到可用的IP");

        int attemptPort = localPort;
        final int MAX_ATTEMPTS = 10; // 最多尝试10个端口

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            try {
                socket = new DatagramSocket(attemptPort, InetAddress.getByName(localIp));
                socket.setBroadcast(true);
                socket.setSoTimeout(6000);

                if (!started) {
                    new Thread(this::receiveLoop, "UDP-Receiver").start();
                    new Thread(this::parseLoop, "UDP-Parser").start();
                    started = true;
                }

                System.out.println("UDP Socket 初始化完成，本地IP=" + localIp + " 端口=" + attemptPort);
                // 保存实际绑定的端口
                localPort = attemptPort;
                return R.ok(socket);

            } catch (BindException e) {
                System.out.println("端口 " + attemptPort + " 已被占用，尝试下一个端口...");
                attemptPort += 1000; // 尝试下一个端口
            } catch (Exception e) {
                e.printStackTrace();
                return R.fail("创建 DatagramSocket 失败: " + e.getMessage());
            }
        }

        return R.fail("无法绑定任何可用端口，从 " + localPort + " 起连续尝试 " + MAX_ATTEMPTS + " 个端口失败");
    }



    /**
     * 接收线程：持续监听UDP报文
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

                String cmdId = findCmdIdForReply(reply);
//                byte[] sendBytes = sentMap.get(cmdId);
                byte[] sendBytes = cmdId == null ? null : sentMap.get(cmdId);

                receiveQueue.offer(new SendReceivePair(cmdId, sendBytes, reply));
            } catch (Exception e) {
                if (running) e.printStackTrace();
            }
        }
    }

    /**
     * 解析线程：解析接收队列中的报文
     */
    private void parseLoop() {
        while (running) {
            try {
                SendReceivePair pair = receiveQueue.take();
                byte[] recvBytes = pair.receiveBytes;
                byte[] sendBytes = pair.sendBytes;
                String cmdId = pair.cmdId;

                System.out.print("开始解析报文：");
                for (byte b : recvBytes) System.out.printf("%02X ", b);
                System.out.println();

                Object result;

                if (sendBytes == null && recvBytes[5] == 0x66) {
                    result = ResponseParser.parsePassiveReport(recvBytes);
                    System.out.println("解析结果：" + result);
                    continue;
                } else if (sendBytes == null) {
                    result = ResponseParser.parseNullReport(recvBytes);
                    System.out.println("解析结果：" + result);
                    continue;
                }else{
                    result = ResponseParser.parseMultipleResponses(
                            Collections.singletonList(recvBytes), sendBytes);
                    System.out.println("解析结果：" + result);
                }

                // 如果是 List<Object>，合并成字符串
                String resultStr;
                if (result instanceof List) {
                    resultStr = ((List<?>) result).stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(",")); // 用逗号分隔，可改成其他分隔符
                } else {
                    resultStr = result.toString();
                }

                System.out.println("解析结果：" + resultStr);

                // 存入对应 cmdId 的结果队列
                if (cmdId != null && resultMap.containsKey(cmdId)) {
                    // 将 resultStr 包装成 List<Object> 再存入队列
                    resultMap.get(cmdId).offer(Collections.singletonList(resultStr));
                }

            } catch (Exception e) {
                System.err.println("【解析线程出错】" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送命令
     */
    public void sendCommand(String cmdId, byte[] sendBytes, String sendIp) throws Exception {
        DatagramSocket s = socket;
        if (s == null || s.isClosed()) {
            R<DatagramSocket> ans = getSocket();
            if (!ans.getSuccess()) {
                throw new RuntimeException("Socket 初始化失败: " + ans.getMessage());
            }
            s = ans.getData();
        }

        synchronized (s) {
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            s.send(packet);
        }

        sendOrderQueue.add(cmdId);
        sentMap.put(cmdId, sendBytes);

        resultMap.put(cmdId, new LinkedBlockingDeque<>());

        System.out.print("发送指令 [" + cmdId + "] 报文：");
        for (byte b : sendBytes) System.out.printf("%02X ", b);
        System.out.println();
    }

    /**
     * 获取某个 cmdId 的解析结果（阻塞等待，带超时）
     */
    public List<Object> getResult(String cmdId, long timeoutMillis) throws InterruptedException {
        BlockingDeque<List<Object>> queue = resultMap.get(cmdId);
        if (queue == null) return null;
        return queue.poll(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 获取某个 cmdId 的最新解析结果（阻塞等待，带超时）
     * 只返回列表中的第一个元素（通常是干净的字符串）
     */
    public String getResultSingle(String cmdId, long timeoutMillis) throws InterruptedException {
        BlockingDeque<List<Object>> queue = resultMap.get(cmdId);
        if (queue == null) return null;

        List<Object> resultList = queue.poll(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (resultList == null || resultList.isEmpty()) return null;

        return resultList.get(0).toString(); // 返回列表第一个元素的干净值
    }

    /**
     * 配对对象：存放发送报文和接收报文
     */
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
        return sendOrderQueue.poll();
    }

    /**
     * Bean销毁时关闭Socket
     */
    @PreDestroy
    public void close() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("UDP Socket 已关闭");
    }

    /**
     * 获取本机以太网IPv4地址
     */
    private String getEthernetIPv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();

                if (!ni.isUp() || ni.isLoopback()) continue;

                String name = ni.getName().toLowerCase();
                String displayName = ni.getDisplayName().toLowerCase();

                if (!(name.contains("eth") || displayName.contains("ethernet") || displayName.contains("以太网")))
                    continue;
                if (name.contains("vmnet") || displayName.contains("vmware")) continue;
                if (name.contains("vbox") || displayName.contains("virtualbox")) continue;
                if (name.contains("wlan") || displayName.contains("wi-fi") || displayName.contains("wireless"))
                    continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
}


