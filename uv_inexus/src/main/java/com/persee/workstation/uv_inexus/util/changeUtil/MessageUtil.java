package com.persee.workstation.uv_inexus.util.changeUtil;

import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.model.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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


    public R<DatagramSocket> getSocket() {
        String localIp = getEthernetIPv4();
        if (localIp == null) {
            System.out.println("获取当前ip有问题");
            return R.fail("未找到可用的IP");
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
    public void scanSendAndDeal(DatagramSocket scanSocket, String sendIp, String cmdId, byte[] sendBytes) {

        try {
            List<byte[]> responses = new ArrayList<>();
            // 发送主命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            scanSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

            // 是否为慢命令（用于接收等待时间判断）
            boolean isSlowCommand = cmdId.equalsIgnoreCase("BaseLine")
                    || cmdId.equalsIgnoreCase("Scan")
                    || cmdId.equalsIgnoreCase("ScanFast")
                    || cmdId.equalsIgnoreCase("BaseLineFast");
            int timeoutMs = isSlowCommand ? 5000 : 4500;
            scanSocket.setSoTimeout(timeoutMs);

            long lastReceiveTime = System.currentTimeMillis();

            new Thread(() -> {
                scanReceiveAndDeal(scanSocket);
            }).start();
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
    public void scanSendAndDealBaseline(DatagramSocket scanSocket, String sendIp, String cmdId, byte[] sendBytes) {

        try {
            List<byte[]> responses = new ArrayList<>();
            // 发送主命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            scanSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

            // 是否为慢命令（用于接收等待时间判断）
            boolean isSlowCommand = cmdId.equalsIgnoreCase("BaseLine")
                    || cmdId.equalsIgnoreCase("Scan")
                    || cmdId.equalsIgnoreCase("ScanFast")
                    || cmdId.equalsIgnoreCase("BaseLineFast");
            int timeoutMs = isSlowCommand ? 5000 : 4500;
            scanSocket.setSoTimeout(timeoutMs);

            long lastReceiveTime = System.currentTimeMillis();

            new Thread(() -> {
                scanReceiveAndDealBaseline(scanSocket);
            }).start();
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
    public List<byte[]> singleSendAndReceive(DatagramSocket currentSocket, String sendIp, String cmdId, byte[] sendBytes) {
        try {

            List<byte[]> responses = new ArrayList<>();

            // 发送命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            currentSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

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
            int timeoutMs = isSlowCommand ? 10000 : 4500;
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
                    if (count > 1 && cmdId != "BaseLine" && cmdId != "BaseLineFast") {
                        currentSocket.setSoTimeout(1300);
                        timeoutMs = 1300;
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
     * 非光谱扫描指令使用的发送2(用于时间扫描)
     *
     * @param currentSocket
     * @param sendIp
     * @param cmdId
     * @param sendBytes
     * @return
     */
    public List<byte[]> singleSendAndReceive2(DatagramSocket currentSocket, String sendIp, String cmdId, byte[] sendBytes) {
        try {
            System.err.println("////////////////////////////"+System.currentTimeMillis());
            List<byte[]> responses = new ArrayList<>();

            // 发送命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            currentSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

            // 是否为慢命令（用于接收等待时间判断）
            boolean isSlowCommand = cmdId.equalsIgnoreCase("Scan");
            int timeoutMs = isSlowCommand ? 10000 : 4500;
            currentSocket.setSoTimeout(timeoutMs);

            int count = 0;
            while (Constants.isRunning) {
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                currentSocket.receive(receivePacket);
                count++;

                byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                responses.add(reply);

                System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                System.out.print("十六进制回应：");
                for (byte b : reply) System.out.printf("%02X ", b);
                System.out.println();

                if (count==3){
                    break;
                }
            }
            System.err.println("[[[[[[[[[[[[[[[[[[[[[[[[[[[["+System.currentTimeMillis());
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
    public List<ChangeParam> RationDifferentialCall(DatagramSocket scanSocket, String sendIp, String cmdId, byte[] sendBytes) {

        try {
            List<byte[]> responses = new ArrayList<>();
            // 发送主命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            scanSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

            // 是否为慢命令（用于接收等待时间判断）
            boolean isSlowCommand = cmdId.equalsIgnoreCase("BaseLine")
                    || cmdId.equalsIgnoreCase("Scan")
                    || cmdId.equalsIgnoreCase("ScanFast")
                    || cmdId.equalsIgnoreCase("BaseLineFast");
            int timeoutMs = isSlowCommand ? 5000 : 4500;
            scanSocket.setSoTimeout(timeoutMs);

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


//    public List<byte[]> singleSendAndReceive(DatagramSocket currentSocket, String sendIp, String cmdId, byte[] sendBytes) {
//        try {
//            List<byte[]> responses = new ArrayList<>();
//
//            // 记录发送报文的第 3、4、5 位
//            byte sentThirdByte = sendBytes[2];  // 第3位
//            byte sentFourthByte = sendBytes[3]; // 第4位
//            byte sentFifthByte = sendBytes[4];  // 第5位
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
//            // 是否为特定指令，结束条件是根据接收到多条回应
//            Set<String> specialCommands = new HashSet<>();
//            specialCommands.add("CheckWLamp");
//            specialCommands.add("CheckDLamp");
//            specialCommands.add("CheckWl");
//            specialCommands.add("Measure");
//            specialCommands.add("BaseLine");
//            specialCommands.add("Scan");
//            specialCommands.add("Zero");
//            specialCommands.add("BaseLineFast");
//            specialCommands.add("ScanFast");
//            specialCommands.add("SearchInSameZoom");
//            specialCommands.add("SearchAround");
//            specialCommands.add("NewIP");
//            specialCommands.add("GetSn");
//
//            boolean isSpecialCommand = specialCommands.contains(cmdId);
//
//            int timeoutMs = isSpecialCommand ? 20000 : 4500;
//            currentSocket.setSoTimeout(timeoutMs);
//
//            long lastReceiveTime = System.currentTimeMillis();
//            int count = 0;
//
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
//
//                    if (isSpecialCommand) {
//                        // 对于特殊指令，接收到超过 1 条回应后结束接收
//                        if (count > 1) {
//                            break; // 结束接收
//                        }
//                    } else {
//                        // 对于其他指令，检查发送的第 3、4、5 位与接收到的最后一条报文的 3、4、5 位是否匹配
//                        if (reply.length > 4 && reply[2] == sentThirdByte && reply[3] == sentFourthByte && reply[4] == sentFifthByte) {
//                            break; // 如果匹配，终止接收
//                        }
//                    }
//
//                    // 设置更短的超时时间
//                    if (count > 1) {
//                        currentSocket.setSoTimeout(1000);
//                        timeoutMs = 1000;
//                    }
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
//    }


    public List<byte[]> quickSendAndReceive(DatagramSocket currentSocket, String sendIp, String cmdId, byte[] sendBytes) {
        try {

            List<byte[]> responses = new ArrayList<>();

            // 发送命令
            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), remotePort);
            currentSocket.send(packet);

            System.out.print("发送指令 [" + cmdId + "] 报文：");
            for (byte b : sendBytes) System.out.printf("%02X ", b);
            System.out.println();

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
                    || cmdId.equalsIgnoreCase("SetWl")
                    || cmdId.equalsIgnoreCase("BaseLineFast");
            int timeoutMs = isSlowCommand ? 15000 : 4500;
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
                    if (count > 1) {
                        currentSocket.setSoTimeout(500);
                        timeoutMs = 500;
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
     * 扫描接收并处理
     *
     * @param scanSocket
     */
    private void scanReceiveAndDeal(DatagramSocket scanSocket) {
        int i = 0;
        ChangeParam changeParam = null;
        while (Constants.isRunning) {
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
                    int wl = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int s = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
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
        while (Constants.isRunning) {
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

                if (data[3] == 0x05 && changeParam != null) {
                    int wl = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int s = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    changeParam.setS(String.valueOf(s));
                    changeParam.setWl(String.valueOf(wl / 50.0f));
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


}
