package org.example;

import java.net.*;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CommandExecutor implements AutoCloseable{
    private final String localIp;
    private final int localPort;
    private final int remotePort;
    private DatagramSocket socket;


    public CommandExecutor(String localIp, int localPort, int remotePort) throws SocketException {
        this.localIp = localIp;
        this.localPort = localPort;
        this.remotePort = remotePort;

        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);       //允许多个DatagramSocket实例在同一端口
        socket.bind(new InetSocketAddress(localIp, localPort));
        socket.setBroadcast(true);
        socket.setSoTimeout(3000);
    }


    public List<byte[]> sendAndReceive(String cmdId, byte[] sendBytes, String sendIp) throws Exception{
        List<byte[]> responses = new ArrayList<>();

        //发送主命令
        DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length, InetAddress.getByName(sendIp), remotePort);
        socket.send(packet);

        System.out.print("发送指令 [" + cmdId + "] 报文：");
        for (byte b : sendBytes) System.out.printf("%02X ", b);
        System.out.println();

        // 是否为慢命令（用于接收等待时间判断）
//        boolean isSlowCommand = cmdId.equalsIgnoreCase("BaseLine")
//                || cmdId.equalsIgnoreCase("Scan")
//                || cmdId.equalsIgnoreCase("ScanFast")
//                || cmdId.equalsIgnoreCase("BaseLineFast");
//        int timeoutMs = isSlowCommand ? 5000 : 4500;
//        socket.setSoTimeout(timeoutMs);

        long lastReceiveTime = System.currentTimeMillis();

        while (true){
            try{
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024],1024);
                socket.receive(receivePacket);

                byte[] reply = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                responses.add(reply);

                String deviceIp = receivePacket.getAddress().getHostAddress();
                System.out.println("收到来自回应： " + deviceIp);
                System.out.println("十六进制回应： ");
                for (byte b : reply) System.out.printf("%02X ", b);
                System.out.println();

                //  每收一条就解析
                List<byte[]> oneResponseList = new ArrayList<>();
                oneResponseList.add(reply);

                List<Object> result = ResponseParser.parseMultipleResponses(oneResponseList, sendBytes);
                System.out.println("解析结果：" + result);

                lastReceiveTime = System.currentTimeMillis();

            }catch (SocketTimeoutException e){
                if (System.currentTimeMillis() - lastReceiveTime >= 1000) break;
            }
        }
        return responses;
    }


    @Override
    public void close() throws Exception {
        if (socket != null && !socket.isClosed()) socket.close();
    }
}
