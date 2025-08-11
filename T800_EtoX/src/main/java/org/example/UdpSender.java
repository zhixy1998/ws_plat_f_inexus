package org.example;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


import java.net.*;
import java.util.Map;

public class UdpSender {
    private static final String LOCAL_IP = "192.168.51.36";  // 上位机 IP
    private static final int LOCAL_PORT = 6000;  // 上位机端口
    private static final int REMOTE_PORT = 5000;  // 下位机端口

    public static void main(String[] args) throws Exception {
        // 加载协议文件
        Map<String, ProtocolItem> map = ProtocolItem.loadFormXml("D:\\project\\java\\T800.xml");

        try (DynamicCommandService service = new DynamicCommandService(LOCAL_IP, LOCAL_PORT, REMOTE_PORT, "D:\\project\\java\\T800.xml")) {

            // 定义多个命令及其参数
            String[] commandIds = {"80", "81", "InitSlit2"};  // 假设要发送的指令ID
            Object[][] paramsArray = {new Object[]{700}, new Object[]{800}, new Object[]{0}};  // 每个命令对应的参数

            // 遍历命令并发送
            for (int i = 0; i < commandIds.length; i++) {
                String cmdId = commandIds[i];
                Object[] params = paramsArray[i];

                System.out.println("发送指令 " + cmdId + " 参数: " + params[0]);

                // 调用 executeCommand 发送命令并接收响应
                List<byte[]> responses = service.executeCommand(cmdId, params);

                // 打印收到的回应
                System.out.println("收到的回应：");
                for (byte[] response : responses) {
                    for (byte b : response) {
                        System.out.printf("%02X ", b);  // 输出回应的十六进制数据
                    }
                    System.out.println();
                }
            }
        }catch (Exception e) {
            e.printStackTrace();  // 捕获并打印异常
        }
    }
//
//        // 使用 DatagramSocket 发送数据
//        DatagramSocket socket = new DatagramSocket(LOCAL_PORT);
//
//        // 创建发送报文的目标地址（下位机 IP 和端口）
//        InetAddress remoteAddress = InetAddress.getByName("169.254.110.68");  // 假设下位机 IP 是 169.254.110.68
//        DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length, remoteAddress, REMOTE_PORT);
//
//        // 发送报文
//        socket.send(packet);
//        System.out.println("发送指令报文: ");
//        for (byte b : sendBytes) {
//            System.out.printf("%02X ", b);  // 输出十六进制的报文
//        }
//        System.out.println();
//
//
//        // 关闭 socket
//        socket.close();
}

//try (CommandExecutor executor = new CommandExecutor(LOCAL_IP, LOCAL_PORT, REMOTE_PORT)){
////调用sendAndReceive方法并传入指令ID和报文数据
//List<byte[]> responses = executor.sendAndReceive("Init", sendBytes);
//
////打印
//           System.out.println("收到的回应：");
//           for (byte[] response : responses) {
//        for (byte b : response) {
//        System.out.printf("%02X ", b);  // 输出回应的十六进制数据
//               }
//                       System.out.println();
//           }
//                   }