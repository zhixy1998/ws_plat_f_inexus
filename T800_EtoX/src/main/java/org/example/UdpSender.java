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
            String[] commandIds = {"Search", "Set", "41"};  // 假设要发送的指令ID
            Object[][] paramsArray = {new Object[]{1}, new Object[]{800,600}, new Object[]{1}};  // 每个命令对应的参数

            // 遍历命令并发送
            for (int i = 0; i < commandIds.length; i++) {
                String cmdId = commandIds[i];
                Object[] params = paramsArray[i];

                System.out.println("发送指令 " + cmdId + " 参数: " + params[0]);

                // 调用 executeCommand 发送命令并接收响应
                List<byte[]> responses = service.executeCommand(cmdId, params);

            }
        }catch (Exception e) {
            e.printStackTrace();  // 捕获并打印异常
        }
    }
}