package com.persee.workstation.uv_inexus.util.changeUtil;

import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.model.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.*;
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
     * @param scanSocket 获取socket
     * @param sendIp  机器ip
     * @param cmdId  指令
     * @param sendBytes 发送报文
     * @param machineList  机器报文数据存储数组
     * @param sumData  机器初步计算后的存储数组
     */
    public void scanSendAndDeal(DatagramSocket scanSocket, String sendIp, String cmdId, byte[] sendBytes, List<String> machineList, List<String> sumData) {

        try {
            List<byte[]> responses = new ArrayList<>();

            // 特殊命令预处理
            if (cmdId.equalsIgnoreCase("Scan") || cmdId.equalsIgnoreCase("ScanFast")) {
                byte[] firstCmd = new byte[]{
                        (byte) 0xB5, (byte) 0xFF, (byte) 0xF0, (byte) 0x01,
                        (byte) 0x00, (byte) 0x40, (byte) 0xC2, (byte) 0xBA,
                        (byte) 0xA9, (byte) 0x08, (byte) 0x64, (byte) 0x76
                };

                byte[] configCmd = new byte[]{
                        (byte) 0xFF, (byte) 0xB5, (byte) 0x13, (byte) 0x04,
                        (byte) 0x00, (byte) 0x07, (byte) 0x0D, (byte) 0x00,
                        (byte) 0x00, (byte) 0x2A, (byte) 0x9D, (byte) 0xA6
                };

                // 发送 firstCmd
                DatagramPacket packet1 = new DatagramPacket(firstCmd, firstCmd.length,
                        InetAddress.getByName(sendIp), remotePort);
                scanSocket.send(packet1);
                System.out.print("预发送报文 firstCmd：");
                for (byte b : firstCmd) System.out.printf("%02X ", b);
                System.out.println();

                // 等待10秒
                Thread.sleep(100);

                // 发送 configCmd
                DatagramPacket packet2 = new DatagramPacket(configCmd, configCmd.length,
                        InetAddress.getByName(sendIp), remotePort);
                scanSocket.send(packet2);
                System.out.print("预发送报文 configCmd：");
                for (byte b : configCmd) System.out.printf("%02X ", b);
                System.out.println();

                // 再等待10秒
                Thread.sleep(100);
            }

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

            new Thread(() -> {scanReceiveAndDeal(scanSocket, machineList, sumData);}).start();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 非光谱扫描指令使用的发送
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
                    || cmdId.equalsIgnoreCase("BaseLineFast");
            int timeoutMs = isSlowCommand ? 5000 : 4500;
            currentSocket.setSoTimeout(timeoutMs);

            long lastReceiveTime = System.currentTimeMillis();


            while (Constants.isRunning) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                    currentSocket.receive(receivePacket);

                    byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                    responses.add(reply);

                    System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                    System.out.print("十六进制回应：");
                    for (byte b : reply) System.out.printf("%02X ", b);
                    System.out.println();

                    lastReceiveTime = System.currentTimeMillis();
                    currentSocket.setSoTimeout(4500);

                } catch (SocketTimeoutException e) {
                    if (System.currentTimeMillis() - lastReceiveTime >= timeoutMs) break;
                }
            }

            return responses;
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


    /**
     * 扫描接收并处理
     * @param scanSocket
     * @param machineList
     * @param sumData
     */
    private void scanReceiveAndDeal(DatagramSocket scanSocket, List<String> machineList, List<String> sumData) {
        while (Constants.isRunning) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                scanSocket.receive(receivePacket);

                byte[] data = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

                System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                System.out.print("十六进制回应：");
                for (byte b : data) System.out.printf("%02X ", b);
                System.out.println();

                if (data.length >= 12 && (data[4] & 0xFF) != 0) {
                    int x = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int y = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    double result_x = x / 50.0;
                    double result_y = y / 8388607.0 * 100;

                    sumData.add(result_x + "," + result_y);
                    machineList.add(x + "," + y);
                }

            } catch (Exception e) {
                Constants.field1Values.add(Constants.EndData);
                return;
            }
        }
    }


    /**
     * 获取本机以太网IPv4地址
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
                if (!(name.contains("eth") || displayName.contains("ethernet") || displayName.contains("以太网"))) continue;



                //排除 VMware、VirtualBox、Wi-Fi 等虚拟或无线网卡
                if (name.contains("vmnet") || displayName.contains("vmware")) continue;
                if (name.contains("vbox") || displayName.contains("virtualbox")) continue;
                if (name.contains("wlan") || displayName.contains("wi-fi") || displayName.contains("wireless")) continue;
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
