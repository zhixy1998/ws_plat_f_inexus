/*
    发送和接收报文
* */
package com.persee.workstation.uv_inexus.util.nettyUtil.SeRePa;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandExecutor implements AutoCloseable {
    private final String localIp;
    private final int localPort;
    private final int remotePort;
    private DatagramSocket socket;

    public CommandExecutor(String localIp, int localPort, int remotePort) throws Exception {
        this.localIp = localIp;
        this.localPort = localPort;
        this.remotePort = remotePort;

        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(localIp, localPort));
        socket.setBroadcast(true);
        socket.setSoTimeout(3000);
    }


    public List<byte[]> sendAndReceive(String cmdId, byte[] sendBytes) throws Exception {
        List<byte[]> responses = new ArrayList<>();

        // 发送主命令
        DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                InetAddress.getByName("255.255.255.255"), remotePort);
        socket.send(packet);

        System.out.print("发送指令 [" + cmdId + "] 报文：");
        for (byte b : sendBytes) System.out.printf("%02X ", b);
        System.out.println();

        // 是否为慢命令（用于接收等待时间判断）
        boolean isSlowCommand = cmdId.equalsIgnoreCase("BaseLine")
                || cmdId.equalsIgnoreCase("Scan")
                || cmdId.equalsIgnoreCase("ScanFast")
                || cmdId.equalsIgnoreCase("BaseLineFast");
        int timeoutMs = isSlowCommand ? 5000 : 4500;
        socket.setSoTimeout(timeoutMs);

        long lastReceiveTime = System.currentTimeMillis();

        while (true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                socket.receive(receivePacket);

                byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                responses.add(reply);

                System.out.println("收到回应来自 " + receivePacket.getAddress().getHostAddress());
                System.out.print("十六进制回应：");
                for (byte b : reply) System.out.printf("%02X ", b);
                System.out.println();

                lastReceiveTime = System.currentTimeMillis();
                socket.setSoTimeout(4500);

            } catch (SocketTimeoutException e) {
                if (System.currentTimeMillis() - lastReceiveTime >= timeoutMs) break;
            }
        }

        return responses;
    }



    public void close() {
        if (socket != null && !socket.isClosed()) socket.close();
    }
}
