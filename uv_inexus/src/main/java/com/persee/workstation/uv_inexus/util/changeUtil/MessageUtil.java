/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.changeUtil;

import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.model.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

@Service("MessageUtil")
public class MessageUtil {

    @Value("${device.default.sendPort}")
    Integer remotePort;

    @Value("${device.default.receivePort}")
    Integer localPort;

    public static DatagramSocket socket;
    private static String localIp="";


    public R<DatagramSocket> getSocket() {
        if (!StringUtils.hasText(localIp)){
            localIp = getEthernetIPv4();
        }
        if (!StringUtils.hasText(localIp)) {
            System.out.println("获取当前ip有问题");
            return R.fail("请检查网线连接！");
        }

        try {
            if (socket == null) {
                socket = new DatagramSocket(localPort, InetAddress.getByName(localIp));
                return R.ok(socket);
            }
            InetAddress address = socket.getLocalAddress();

            if (address != null && !address.getHostAddress().equals(localIp)) {
                socket = new DatagramSocket(localPort, InetAddress.getByName(localIp));
                return R.ok(socket);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return R.fail("请检查网线连接！");
        }
        return R.ok(socket);
    }



    /**
     * 光谱扫描调用
     *
     * @param scanSocket 获取socket
     * @param sendIp     机器ip
     * @param cmdId      指令
     * @param sendBytes  发送报文
     */
    public void scanSendAndDeal(DatagramSocket scanSocket, String sendIp, String cmdId, byte[] sendBytes, int replyNumber, int timeout) {

        try {
            List<byte[]> responses = new ArrayList<>();
            // 发送主命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            scanSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

            scanSocket.setSoTimeout(timeout);

            long lastReceiveTime = System.currentTimeMillis();
            Thread sendMeasure = null;
            if (cmdId.equals("ScanFast")){
                sendMeasure = new Thread(() -> {
                    scanReceiveAndDealScanFast(scanSocket);
                });
            } else {
                sendMeasure = new Thread(() -> {
                    scanReceiveAndDeal(scanSocket);
                });
            }
            Constants.sendMeasureThread = sendMeasure;
            sendMeasure.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 光谱扫描基线调用
     *
     * @param scanSocket 获取socket
     * @param sendIp     机器ip
     * @param cmdId      指令
     * @param sendBytes  发送报文
     */
    public void scanSendAndDealBaseline(DatagramSocket scanSocket, String sendIp, String cmdId, byte[] sendBytes, int replyNumber, int timeout) {

        try {
            List<byte[]> responses = new ArrayList<>();
            // 发送主命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            scanSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

            scanSocket.setSoTimeout(timeout);

            long lastReceiveTime = System.currentTimeMillis();

            Thread sendMeasure = null;
            if (cmdId.equals("BaseLineFast")){
                sendMeasure = new Thread(() -> {
                    scanReceiveAndDealFastBaseline(scanSocket);
                });
            } else {
                sendMeasure = new Thread(() -> {
                    scanReceiveAndDealBaseline(scanSocket);
                });
            }

            Constants.sendMeasureThread = sendMeasure;
            sendMeasure.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 非光谱扫描指令使用的发送
     *
     * @param currentSocket
     * @param sendIp
     * @param cmdId
     * @param sendBytes
     * @return
     */
    public List<byte[]> singleSendAndReceive(DatagramSocket currentSocket,
                                             String sendIp,
                                             String cmdId,
                                             byte[] sendBytes,
                                             int replyNumber,
                                             int timeout) {

        try {
            List<byte[]> responses = new ArrayList<>();

            // 发送命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            currentSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

            if (replyNumber == -1) {
                // 一次性等待 timeout 毫秒
                currentSocket.setSoTimeout(timeout);

                long lastReceiveTime = System.currentTimeMillis();
                int count = 0;

                while (true) {
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                        currentSocket.receive(receivePacket);
                        count++;

                        byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                        responses.add(reply);

                        System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                        System.out.print("十六进制回应：");
                        for (byte b : reply) System.out.printf("%02X ", b);
                        System.out.println();

                        //收到第二包后把等待时间缩短到 100ms
                        lastReceiveTime = System.currentTimeMillis();
                        if (count > 1 ) {
                            currentSocket.setSoTimeout(100);
                            if (cmdId.equals( "BaseLine" ) || cmdId.equals("BaseLineFast") || cmdId.equals("Scan") || cmdId.equals("ScanFast") || cmdId.equals("CheckWLamp") || cmdId.equals("CheckDLamp") || cmdId.equals("CheckWl") || cmdId.equals("CorrectWl") || cmdId.equals("SetWl")){
                                currentSocket.setSoTimeout(10000);
                            }
                        }

                    } catch (SocketTimeoutException e) {
                        // 收到 timeout 就结束
                        if (System.currentTimeMillis() - lastReceiveTime >= currentSocket.getSoTimeout()) {
                            break;
                        }
                    }
                }

                return responses;
            }

            long startTime = System.currentTimeMillis();
            currentSocket.setSoTimeout(timeout);   // 等待所有包的最长时间

            while (System.currentTimeMillis() - startTime < timeout) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                    currentSocket.receive(receivePacket);

                    byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                    responses.add(reply);

                    System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                    System.out.print("十六进制回应：");
                    for (byte b : reply) System.out.printf("%02X ", b);
                    System.out.println();

                    //replyNumber 个直接结束
                    if (responses.size() >= replyNumber) {
                        break;
                    }

                } catch (SocketTimeoutException e) {
                    //超时 → 跳出
                    break;
                }
            }
            if (responses.size() == 1&&!cmdId.equals("Stop")) {
                responses.clear();
                responses = singleSendAndReceive(currentSocket, sendIp, cmdId, sendBytes, replyNumber, timeout);
            }

            return responses;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<byte[]> calibrationSendAndReceive(DatagramSocket currentSocket, String sendIp, String cmdId, byte[] sendBytes) {
        try {

            List<byte[]> responses = new ArrayList<>();

            // 发送命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            currentSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();


            int timeoutMs = 18000 ;
            currentSocket.setSoTimeout(timeoutMs);

            long lastReceiveTime = System.currentTimeMillis();

            int count = 0;

            while (Constants.isRunning) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                    currentSocket.receive(receivePacket);
                    count++;

                    byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                    responses.add(reply);

                    System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());

                    System.out.print("十六进制回应：");
                    for (byte b : reply) System.out.printf("%02X ", b);
                    System.out.println();


                    lastReceiveTime = System.currentTimeMillis();
                    if(cmdId.equals("CorrectWl") && (reply[2] & 0xFF) == 0 && (reply[3] & 0xFF) == 15){
                        System.out.println("CorrectWl 命令结束");
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    if (System.currentTimeMillis() - lastReceiveTime >= timeoutMs) break;
                }
            }


            return responses;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 仪器监测使用的发送和接收
     */

    public List<byte[]> detectionSendAndReceive(DatagramSocket currentSocket, String sendIp, String cmdId, byte[] sendBytes, Integer responseNum, Integer maxRetryNum) {
        try {

            List<byte[]> responses = new ArrayList<>();
            // 发送命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            currentSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

            int timeoutMs = 3000;
            currentSocket.setSoTimeout(timeoutMs);

            int count = 0;

            while (count < responseNum) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                    currentSocket.receive(receivePacket);
                    count++;

                    byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                    responses.add(reply);

                    System.out.println("实时监测收到回应来自 " + receivePacket.getAddress().getHostAddress());
                    System.out.print("实时监测十六进制回应：");
                    for (byte b : reply) System.out.printf("%02X ", b);
                    System.out.println();

                } catch (SocketTimeoutException e) {
                    System.out.println("超时");

                    // 若已收到部分数据但不足，并且还有重试机会，则继续重试
                    if (responses.size() < responseNum && maxRetryNum > 0) {
                        responses.clear();
                        System.out.println("回应不足，重试");
                        return detectionSendAndReceive(currentSocket, sendIp, cmdId, sendBytes, responseNum, --maxRetryNum);
                    } else if (responses.size() < responseNum) {
                        // 达到最大重试次数仍未完成任务，放弃并返回null
                        return null;
                    }
                }
            }

            return responses;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


    /**
     * 非光谱扫描指令使用的发送2(用于时间扫描)
     *
     * @param currentSocket
     * @param sendIp
     * @param cmdId
     * @param sendBytes
     * @return
     */
    public List<byte[]> singleSendAndReceive2(DatagramSocket currentSocket,
                                              String sendIp,
                                              String cmdId,
                                              byte[] sendBytes,
                                              int replyNumber,
                                              int timeout) {

        try {
            List<byte[]> responses = new ArrayList<>();

            // 发送命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            currentSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

            if (replyNumber == -1) {
                // 一次性等待 timeout 毫秒
                currentSocket.setSoTimeout(timeout);

                long lastReceiveTime = System.currentTimeMillis();
                int count = 0;

                while (true) {
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                        currentSocket.receive(receivePacket);
                        count++;

                        byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                        responses.add(reply);

                        System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                        System.out.print("十六进制回应：");
                        for (byte b : reply) System.out.printf("%02X ", b);
                        System.out.println();

                        //收到第二包后把等待时间缩短到 100ms
                        lastReceiveTime = System.currentTimeMillis();
                        if (count > 1 ) {
                            currentSocket.setSoTimeout(100);
                            if (cmdId.equals( "BaseLine" ) || cmdId.equals("BaseLineFast") || cmdId.equals("Scan") || cmdId.equals("ScanFast") || cmdId.equals("CheckWLamp") || cmdId.equals("CheckDLamp") || cmdId.equals("CheckWl") || cmdId.equals("CorrectWl") || cmdId.equals("SetWl")){
                                currentSocket.setSoTimeout(4000);
                            }
                        }

                    } catch (SocketTimeoutException e) {
                        // 收到 timeout 就结束
                        if (System.currentTimeMillis() - lastReceiveTime >= currentSocket.getSoTimeout()) {
                            break;
                        }
                    }
                }

                return responses;
            }

            long startTime = System.currentTimeMillis();
            currentSocket.setSoTimeout(timeout);   // 等待所有包的最长时间

            while (System.currentTimeMillis() - startTime < timeout) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                    currentSocket.receive(receivePacket);

                    byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                    responses.add(reply);

                    System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                    System.out.print("十六进制回应：");
                    for (byte b : reply) System.out.printf("%02X ", b);
                    System.out.println();

                    //replyNumber 个直接结束
                    if (responses.size() >= replyNumber) {
                        break;
                    }

                } catch (SocketTimeoutException e) {
                    //超时 → 跳出
                    break;
                }
            }

            return responses;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 非光谱扫描指令使用的发送(用于时间扫描)
     *
     * @param currentSocket
     * @param sendIp
     * @param cmdId
     * @param sendBytes
     * @return
     */
    public void singleSend(DatagramSocket currentSocket, String sendIp, String cmdId, byte[] sendBytes) {
        try {

            // 发送命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            currentSocket.send(packet);

            System.err.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }

    }

    /**
     * 非光谱扫描指令使用的接收(用于时间扫描)
     *
     * @param currentSocket
     * @param sendIp
     * @param cmdId
     * @return
     */
    public List<byte[]> singleReceive(DatagramSocket currentSocket, String sendIp, String cmdId) {
        try {


            List<byte[]> responses = new ArrayList<>();

            // 是否为慢命令（用于接收等待时间判断）
            boolean isSlowCommand = cmdId.equalsIgnoreCase("BaseLine")
                    || cmdId.equalsIgnoreCase("Scan")
                    || cmdId.equalsIgnoreCase("ScanFast")
                    || cmdId.equalsIgnoreCase("InitSlit1Motor")
                    || cmdId.equalsIgnoreCase("SetSlit1")
                    || cmdId.equalsIgnoreCase("InitWlMotor")
                    || cmdId.equalsIgnoreCase("InitLampMotor")
                    || cmdId.equalsIgnoreCase("CheckWLamp")
                    || cmdId.equalsIgnoreCase("CheckDLamp")
                    || cmdId.equalsIgnoreCase("CheckWl")
                    || cmdId.equalsIgnoreCase("BaseLineFast");

            int timeoutMs = isSlowCommand ? 18000 : 4500;
            currentSocket.setSoTimeout(timeoutMs);

            long lastReceiveTime = System.currentTimeMillis();

            int count = 0;
            while (Constants.isRunning) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                    currentSocket.receive(receivePacket);
                    count++;

                    byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                    responses.add(reply);

                    System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                    System.out.print("十六进制回应：");
                    for (byte b : reply) System.out.printf("%02X ", b);
                    System.out.println();

                    lastReceiveTime = System.currentTimeMillis();
                    if (count > 1 ) {
                        currentSocket.setSoTimeout(100);
                        timeoutMs = 100;
                        if (cmdId.equals( "BaseLine" ) || cmdId.equals("BaseLineFast") || cmdId.equals("Scan") || cmdId.equals("ScanFast") || cmdId.equals("CheckWLamp") || cmdId.equals("CheckDLamp") || cmdId.equals("CheckWl") || cmdId.equals("CorrectWl") || cmdId.equals("SetWl")){
                            currentSocket.setSoTimeout(4000);
                        }
                    }
                    System.out.println("[[[[[[[[[[[[[[[[[[[[[[[[[[[["+System.currentTimeMillis());
                } catch (SocketTimeoutException e) {
                    if (System.currentTimeMillis() - lastReceiveTime >= timeoutMs) break;
                }
            }

            return responses;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 定量测量微分调用
     *
     * @param scanSocket 获取socket
     * @param sendIp     机器ip
     * @param cmdId      指令
     * @param sendBytes  发送报文
     */
    public List<ChangeParam> RationDifferentialCall(DatagramSocket scanSocket, String sendIp, String cmdId, byte[] sendBytes, int replyNumber, int timeout) {

        try {
            List<byte[]> responses = new ArrayList<>();
            // 发送主命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            scanSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

            scanSocket.setSoTimeout(timeout);

            List<ChangeParam> changeParams = parseScanFast(scanSocket);
            if (changeParams == null) {
                return null;
            }
            return changeParams;


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

//    public List<byte[]> quickSendAndReceive(DatagramSocket currentSocket, String sendIp, String cmdId, byte[] sendBytes, int Number, int time) {
//        try {
//
//            List<byte[]> responses = new ArrayList<>();
//
//            // 发送命令
//            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
//                    InetAddress.getByName(sendIp), remotePort);
//            currentSocket.send(packet);
//
//            System.out.print("发送指令 [" + cmdId + "] 报文：");
//            for (byte b : sendBytes) System.out.printf("%02X ", b);
//            System.out.println();
//
//
//            // 是否为慢命令（用于接收等待时间判断）
//            boolean isSlowCommand = cmdId.equalsIgnoreCase("BaseLine")
//                    || cmdId.equalsIgnoreCase("Scan")
//                    || cmdId.equalsIgnoreCase("ScanFast")
//                    || cmdId.equalsIgnoreCase("InitSlit1Motor")
//                    || cmdId.equalsIgnoreCase("SetSlit1")
//                    || cmdId.equalsIgnoreCase("InitWlMotor")
//                    || cmdId.equalsIgnoreCase("InitLampMotor")
//                    || cmdId.equalsIgnoreCase("CheckWLamp")
//                    || cmdId.equalsIgnoreCase("CheckDLamp")
//                    || cmdId.equalsIgnoreCase("CheckWl")
//                    || cmdId.equalsIgnoreCase("SetWl")
//                    || cmdId.equalsIgnoreCase("BaseLineFast");
//            int timeoutMs = isSlowCommand ? 15000 : 4500;
//            currentSocket.setSoTimeout(timeoutMs);
//
//            long lastReceiveTime = System.currentTimeMillis();
//
//            int count = 0;
//            while (Constants.isRunning) {
//                try {
//                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
//                    currentSocket.receive(receivePacket);
//                    count++;
//
//                    byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
//                    responses.add(reply);
//
//                    System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
//                    System.out.print("十六进制回应：");
//                    for (byte b : reply) System.out.printf("%02X ", b);
//                    System.out.println();
//
//                    lastReceiveTime = System.currentTimeMillis();
//                    if (count > 1) {
//                        currentSocket.setSoTimeout(100);
//                        timeoutMs = 100;
//                    }
//
//
//                } catch (SocketTimeoutException e) {
//                    if (System.currentTimeMillis() - lastReceiveTime >= timeoutMs) break;
//                }
//            }
//
//            return responses;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//
//    }

    public List<byte[]> quickSendAndReceive(
            DatagramSocket currentSocket,
            String sendIp,
            String cmdId,
            byte[] sendBytes,
            int replyNumber,
            int timeout) {

        try {
            List<byte[]> responses = new ArrayList<>();

            // 发送命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            currentSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

            if (replyNumber == -1) {
                // 一次性等待 timeout 毫秒
                currentSocket.setSoTimeout(timeout);

                long lastReceiveTime = System.currentTimeMillis();
                int count = 0;

                while (true) {
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                        currentSocket.receive(receivePacket);
                        count++;

                        byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                        responses.add(reply);

                        System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                        System.out.print("十六进制回应：");
                        for (byte b : reply) System.out.printf("%02X ", b);
                        System.out.println();

                        //收到第二包后把等待时间缩短到 100ms
                        lastReceiveTime = System.currentTimeMillis();
                        if (count > 1 ) {
                            currentSocket.setSoTimeout(100);
                            if (cmdId.equals( "BaseLine" ) || cmdId.equals("BaseLineFast") || cmdId.equals("Scan") || cmdId.equals("ScanFast") || cmdId.equals("CheckWLamp") || cmdId.equals("CheckDLamp") || cmdId.equals("CheckWl") || cmdId.equals("SetWl")){
                                currentSocket.setSoTimeout(4000);
                            } else if (cmdId.equals("CorrectWl")) {
                                currentSocket.setSoTimeout(8000);
                            }
                        }

                        if(cmdId.equals("CorrectWl") && (reply[2] & 0xFF) == 0 && (reply[3] & 0xFF) == 15){
                            System.out.println("CorrectWl 命令结束");
                            break;
                        }

                    } catch (SocketTimeoutException e) {
                        // 收到 timeout 就结束
                        if (System.currentTimeMillis() - lastReceiveTime >= currentSocket.getSoTimeout()) {
                            break;
                        }
                    }
                }

                return responses;
            }

            long startTime = System.currentTimeMillis();
            currentSocket.setSoTimeout(timeout);   // 等待所有包的最长时间

            while (System.currentTimeMillis() - startTime < timeout) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                    currentSocket.receive(receivePacket);

                    byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                    responses.add(reply);

                    System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                    System.out.print("十六进制回应：");
                    for (byte b : reply) System.out.printf("%02X ", b);
                    System.out.println();

                    //replyNumber 个直接结束
                    if (responses.size() >= replyNumber) {
                        break;
                    }

                } catch (SocketTimeoutException e) {
                    //超时 → 跳出
                    break;
                }
            }

            return responses;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    /**
     * 扫描接收并处理
     *
     * @param scanSocket
     */
    private void scanReceiveAndDeal(DatagramSocket scanSocket) {
        int i = 0;
        ChangeParam changeParam = null;
        int count = 0;
        while (Constants.isRunning) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                scanSocket.receive(receivePacket);
                System.out.println("收到当前消息的时间为: "+ System.currentTimeMillis());
                count++;
                if (count>1){
                    //修改时间
                    scanSocket.setSoTimeout(10000);
                }
                byte[] data = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

                System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                System.out.print("十六进制回应：");
                for (byte b : data) System.out.printf("%02X ", b);
                System.out.println();
                if (data.length >= 12 && (data[3] & 0xFF) != 0x07) {
                    if (changeParam == null) {
                        changeParam = new ChangeParam();
                    }
                }

                if (data[3] == 0x01 && changeParam != null) {
                    int wl = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int s = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    changeParam.setS(String.valueOf(s));
                    changeParam.setWl(String.valueOf(wl));
                    System.out.println("s = " + s);
                } else if (data[3] == 0x02 && changeParam != null) {
                    int d = ((data[4] & 0xFF) << 16) | ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int r = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    changeParam.setD(String.valueOf(d));
                    changeParam.setR(String.valueOf(r));
                    System.out.println("d = " + d);
                    System.out.println("r = " + r);
                }

                // 检查是否完整：如果有值 wl, s, d, r 都已经填充，认为对象已完成
                if (changeParam != null && changeParam.getWl() != null && changeParam.getS() != null && changeParam.getD() != null && changeParam.getR() != null) {
                    changeParam.setStep(changeParam.getWl());
                    Constants.measurd_data.add(changeParam);  // 完成的对象添加到公共列表
                    System.out.println("已完成一个完整的对象：" + changeParam);
                    Constants.instrumentData.add(changeParam.toString());
                    changeParam = null;  // 清空临时对象，准备接收下一个
                }

            } catch (Exception e) {
                Constants.instrumentData.add(Constants.EndData);
                Constants.endTime = LocalDateTime.now();
                return;
            }
        }
    }

    /**
     * 快速扫描接收并处理
     *
     * @param scanSocket
     */
    private void scanReceiveAndDealScanFast(DatagramSocket scanSocket) {
        int i = 0;
        ChangeParam changeParam = null;
        int count = 0;
        while (Constants.isRunning) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                scanSocket.receive(receivePacket);
                System.out.println("当前收到消息的时间是: "+ System.currentTimeMillis());
                count++;
                if (count>1){
                    scanSocket.setSoTimeout(4000);
                }
                byte[] data = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

                System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                System.out.print("十六进制回应：");
                for (byte b : data) System.out.printf("%02X ", b);
                System.out.println();
                if (data.length >= 12 && (data[3] & 0xFF) != 0x07) {
                    if (changeParam == null) {
                        changeParam = new ChangeParam();
                    }
                }

                if (data[3] == 0x0B && changeParam != null) {
                    int wl = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int s = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    changeParam.setS(String.valueOf(s));
                    changeParam.setWl(String.valueOf(wl));
                    System.out.println("s = " + s);
                } else if (data[3] == 0x0C && changeParam != null) {
                    int d = ((data[4] & 0xFF) << 16) | ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int r = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    changeParam.setD(String.valueOf(d));
                    changeParam.setR(String.valueOf(r));
                    System.out.println("d = " + d);
                    System.out.println("r = " + r);
                }

                // 检查是否完整：如果有值 wl, s, d, r 都已经填充，认为对象已完成
                if (changeParam != null && changeParam.getWl() != null && changeParam.getS() != null && changeParam.getD() != null && changeParam.getR() != null) {
                    changeParam.setStep(changeParam.getWl());
                    Constants.measurd_data.add(changeParam);  // 完成的对象添加到公共列表
                    System.out.println("已完成一个完整的对象：" + changeParam);
                    Constants.instrumentData.add(changeParam.toString());
                    changeParam = null;  // 清空临时对象，准备接收下一个
                }

            } catch (Exception e) {
                Constants.instrumentData.add(Constants.EndData);
                Constants.endTime = LocalDateTime.now();
                return;
            }
        }
    }

    /**
     * 基线计算
     *
     * @param scanSocket
     */
    private void scanReceiveAndDealBaseline(DatagramSocket scanSocket) {
        int i = 0;
        ChangeParam changeParam = null;
        int count = 0;
        while (Constants.isRunning) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                scanSocket.receive(receivePacket);
                count++;
                if (count > 1) {
                    //修改了光谱扫描的超时时间
                    scanSocket.setSoTimeout(10000);
                }

                byte[] data = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

                System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                System.out.print("十六进制回应：");
                for (byte b : data) System.out.printf("%02X ", b);
                System.out.println();
                if (data.length >= 12 && (data[3] & 0xFF) != 0x07) {
                    if (changeParam == null) {
                        changeParam = new ChangeParam();
                    }
                }

                if (data[3] == 0x05 && changeParam != null) {
                    int wl = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int s = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    changeParam.setS(String.valueOf(s));
                    changeParam.setWl(String.valueOf(wl));
                    System.out.println("s = " + s);
                } else if (data[3] == 0x06 && changeParam != null) {
                    int d = ((data[4] & 0xFF) << 16) | ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int r = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    changeParam.setD(String.valueOf(d));
                    changeParam.setR(String.valueOf(r));
                    System.out.println("d = " + d);
                    System.out.println("r = " + r);
                }

                // 检查是否完整：如果有值 wl, s, d, r 都已经填充，认为对象已完成
                if (changeParam != null && changeParam.getWl() != null && changeParam.getS() != null && changeParam.getD() != null && changeParam.getR() != null) {
                    Constants.measurd_data.add(changeParam);  // 完成的对象添加到公共列表
                    System.out.println("已完成一个完整的对象：" + changeParam);
                    Constants.instrumentData.add(changeParam.toString());
                    changeParam = null;  // 清空临时对象，准备接收下一个
                }

            } catch (Exception e) {
                Constants.instrumentData.add(Constants.EndData);
                Constants.endTime = LocalDateTime.now();
                return;
            }
        }
    }

    /**
     * 极快基线计算
     *
     * @param scanSocket
     */
    private void scanReceiveAndDealFastBaseline(DatagramSocket scanSocket) {
        int i = 0;
        ChangeParam changeParam = null;
        int cont=0;
        while (Constants.isRunning) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                scanSocket.receive(receivePacket);
                cont++;
                if (cont>1){
                    scanSocket.setSoTimeout(5000);
                }
                byte[] data = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

                System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                System.out.print("十六进制回应：");
                for (byte b : data) System.out.printf("%02X ", b);
                System.out.println();
                if (data.length >= 12 && (data[3] & 0xFF) != 0x07) {
                    if (changeParam == null) {
                        changeParam = new ChangeParam();
                    }
                }

                if (data[3] == 0x09 && changeParam != null) {
                    int wl = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int s = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    changeParam.setS(String.valueOf(s));
                    changeParam.setWl(String.valueOf(wl));
                    System.out.println("s = " + s);
                } else if (data[3] == 0x30 && changeParam != null) {
                    int d = ((data[4] & 0xFF) << 16) | ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int r = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    changeParam.setD(String.valueOf(d));
                    changeParam.setR(String.valueOf(r));
                    System.out.println("d = " + d);
                    System.out.println("r = " + r);
                }

                // 检查是否完整：如果有值 wl, s, d, r 都已经填充，认为对象已完成
                if (changeParam != null && changeParam.getWl() != null && changeParam.getS() != null && changeParam.getD() != null && changeParam.getR() != null) {
                    Constants.measurd_data.add(changeParam);  // 完成的对象添加到公共列表
                    System.out.println("已完成一个完整的对象：" + changeParam);
                    Constants.instrumentData.add(changeParam.toString());
                    changeParam = null;  // 清空临时对象，准备接收下一个
                }

            } catch (Exception e) {
                Constants.instrumentData.add(Constants.EndData);
                Constants.endTime = LocalDateTime.now();
                return;
            }
        }
    }

    /**
     * 定量测量:(微分测量)扫描接受并处理
     */

    private static List<ChangeParam> parseScanFast(DatagramSocket scanSocket) {
        int i = 0;
        List<ChangeParam> changeParamList = new ArrayList<>();
        ChangeParam changeParam = null;
        Boolean isRunning = true;
        while (isRunning) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                scanSocket.receive(receivePacket);

                byte[] data = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

                System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                System.out.print("十六进制回应：");
                for (byte b : data) System.out.printf("%02X ", b);
                System.out.println();
                if (data.length >= 12 && (data[3] & 0xFF) != 0x07) {
                    if (changeParam == null) {
                        changeParam = new ChangeParam();
                    }
                }

                if (data[3] == 0x01 && changeParam != null) {
                    int s = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    int wl = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    changeParam.setS(String.valueOf(s));
                    changeParam.setWl(String.valueOf(wl / 50.0f));
                    System.out.println("s = " + s);
                } else if (data[3] == 0x02 && changeParam != null) {
                    int d = ((data[4] & 0xFF) << 16) | ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int r = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    changeParam.setD(String.valueOf(d));
                    changeParam.setR(String.valueOf(r));
                    System.out.println("d = " + d);
                    System.out.println("r = " + r);
                }

                // 检查是否完整：如果有值 wl, s, d, r 都已经填充，认为对象已完成
                if (changeParam != null && changeParam.getWl() != null && changeParam.getS() != null && changeParam.getD() != null && changeParam.getR() != null) {
                    System.out.println("已完成一个完整的对象：" + changeParam);
                    changeParamList.add(changeParam);
                    changeParam = null;  // 清空临时对象，准备接收下一个
                }
            } catch (Exception e) {
                System.out.println("扫描结束");
                return changeParamList;
            }
        }
        return changeParamList;
    }


    /**
     * 获取本机以太网IPv4地址
     *
     * @return
     */
    private String getEthernetIPv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();

                if (!ni.isUp() || ni.isLoopback()) continue;

                String name = ni.getName().toLowerCase();
                String displayName = ni.getDisplayName().toLowerCase();
                //保留有线以太网接口（常见关键词）
                if (!(name.contains("eth") || displayName.contains("ethernet") || displayName.contains("以太网")))
                    continue;


                //排除 VMware、VirtualBox、Wi-Fi 等虚拟或无线网卡
                if (name.contains("vmnet") || displayName.contains("vmware")) continue;
                if (name.contains("vbox") || displayName.contains("virtualbox")) continue;
                if (name.contains("wlan") || displayName.contains("wi-fi") || displayName.contains("wireless"))
                    continue;
                if (displayName.contains("virtual ethernet") || displayName.contains("以太网")) continue;


//                if (name.contains("eth") && displayName.contains("vEthernet") || displayName.contains("以太网")) continue;

                // 遍历地址，返回第一个 IPv4 地址
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
//                        System.out.println(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    //获取wifi连接的ip地址
//    private String getEthernetIPv4() {
//        try {
//            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//            while (interfaces.hasMoreElements()) {
//                NetworkInterface ni = interfaces.nextElement();
//
//                if (!ni.isUp() || ni.isLoopback()) continue;
//
//                String name = ni.getName().toLowerCase();
//                String displayName = ni.getDisplayName().toLowerCase();
//
//                // 保留有线以太网接口和WiFi接口（常见关键词）
//                boolean isEthernetOrWiFi = name.contains("eth") ||
//                        displayName.contains("ethernet") ||
//                        displayName.contains("以太网") ||
//                        name.contains("wlan") ||
//                        displayName.contains("wi-fi") ||
//                        displayName.contains("wireless");
//
//                if (!isEthernetOrWiFi) continue;
//
//                // 排除 VMware、VirtualBox 等虚拟网卡
//                if (name.contains("vmnet") || displayName.contains("vmware")) continue;
//                if (name.contains("vbox") || displayName.contains("virtualbox")) continue;
//                if (displayName.contains("virtual ethernet")) continue;
//
//                // 遍历地址，返回第一个 IPv4 地址
//                Enumeration<InetAddress> addresses = ni.getInetAddresses();
//                while (addresses.hasMoreElements()) {
//                    InetAddress addr = addresses.nextElement();
//                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
//                        return addr.getHostAddress();
//                    }
//                }
//            }
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }


}
