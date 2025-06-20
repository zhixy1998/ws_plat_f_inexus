package com.persee.workstation.uv_inexus.util;

import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;



@Service("SpectrumScan")
public class SpectrumScan {

    // 本地IP地址
    private static final String LOCAL_IP = "169.254.110.67";
    // 本地端口号
    private static final int LOCAL_PORT = 6000;
    // 目标IP地址
    private static volatile String TARGET_IP = "169.254.110.254";
    // 目标端口号
    private static final int TARGET_PORT = 5000;

    // 存储接收到的消息
    private static volatile  List<byte[]> receivedMessages = new CopyOnWriteArrayList<>();
    @Autowired
    private DeviceInfoService deviceInfoService;
    // 存储解析出的数据
//    private static volatile List<String> field1Values = new CopyOnWriteArrayList<>();
//    // 存储原始的数据
//    private static volatile List<String> field2Values = new CopyOnWriteArrayList<>();

    // 有效报文计数
    private static int validPacketCount = 0;
    private static boolean receiving = true;

    // 定义终止报文
//    private static final byte[] END_FRAME = new byte[]{
//            (byte) 0xFF, (byte) 0xB5, (byte) 0x13, (byte) 0x0C,
//            (byte) 0x00, (byte) 0x07, (byte) 0x0D, (byte) 0x00,
//            (byte) 0x00, (byte) 0x02, (byte) 0xE2, (byte) 0xCB
//    };
    public void scanStart(String deviceSerialNumber) {
        TARGET_IP=deviceInfoService.getIp(deviceSerialNumber);
        try {
            validPacketCount = 0;
            // 获取本地IP地址
            InetAddress localAddr = InetAddress.getByName(LOCAL_IP);
            // 创建UDP套接字，绑定本地IP地址和端口号
            DatagramSocket socket = new DatagramSocket(LOCAL_PORT, localAddr);
            // 设置接收超时时间为2秒
            socket.setSoTimeout(1000);
            System.out.println("[主线程] Socket 创建成功，绑定 IP：" + LOCAL_IP + "，端口：" + LOCAL_PORT);
            Constants.startTime = LocalDateTime.now();
            // 启动接收线程
            new Thread(() -> receiveLoop(socket)).start();
            // 启动发送线程
            new Thread(() -> sendLoop(socket)).start();


            // 延迟打印解析值
//            new Thread(() -> {
//                try {
//                    Thread.sleep(10000);
//                    printParsedValues();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }).start();
        } catch (Exception e) {
            System.err.println("Socket 初始化失败: " + e.getMessage());
        }

    }

    // ========== 发送配置和开始扫描报文 ==========
    private static void sendLoop(DatagramSocket socket) {
        try {
            // 获取目标IP地址
            InetAddress targetAddr = InetAddress.getByName(TARGET_IP);

            // 配置报文
            byte[] configCmd = new byte[]{
                    (byte) 0xFF, (byte) 0xB5, (byte) 0x13, (byte) 0x04,
                    (byte) 0x00, (byte) 0x07, (byte) 0x0D, (byte) 0x00,
                    (byte) 0x00, (byte) 0x2A, (byte) 0x9D, (byte) 0xA6
            };
            System.out.println("[发送线程] 即将发送第一条配置报文...");
            socket.send(new DatagramPacket(configCmd, configCmd.length, targetAddr, TARGET_PORT));
            System.out.println("[发送线程] 第一条配置报文已发送");

            // 等待4秒
//            Thread.sleep(4000);

            // 开始扫描报文
            byte[] scanCmd = new byte[]{
                    // 极快扫描
                    (byte) 0xB5, (byte) 0xFF, (byte) 0x13, (byte) 0x64,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2B
                    // 慢扫
//                    (byte) 0xB5, (byte) 0xFF, (byte) 0x13, (byte) 0x01,
//                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
//                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC8
            };
            System.out.println("[发送线程] 即将发送第二条开始扫描报文...");
            socket.send(new DatagramPacket(scanCmd, scanCmd.length, targetAddr, TARGET_PORT));
            System.out.println("[发送线程] 开始扫描报文已发送");

        } catch (Exception e) {
            System.err.println("发送线程异常: " + e.getMessage());
        }
    }

    // ========== 接收并按条件处理UDP数据 ==========
    private static void receiveLoop(DatagramSocket socket) {
        try {
            // 创建缓冲区
            byte[] buffer = new byte[1024];
            System.out.println("[接收线程] 启动成功，监听中...");

            while (receiving) {
                // 创建数据报文
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                // 接收数据报文
                socket.receive(packet);

                // 获取数据
                byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                // 存储数据
                receivedMessages.add(data);

                System.out.println("\n[接收] 来自 " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " 的回应:");
                System.out.print("十六进制数据内容：");
                for (byte b : data) {
                    System.out.printf("%02X ", b);
                }
                System.out.println();

//                if (Arrays.equals(data, END_FRAME)) {
//                    System.out.println("[终止] 收到终止指令报文，停止接收。");
//                    receiving = false;
//                }

                if (data.length < 11) {
                    System.out.println("[跳过] 数据长度不足，忽略本报文。");
                    continue;
                }

                validPacketCount++;

                // 每两个有效报文解析一次
                if (validPacketCount % 2 == 0) {
                    // 解析字段1
                    Double field1Raw = (double) (((data[5] & 0xFF) << 8) | (data[6] & 0xFF));
                    Double field1 = field1Raw / 50.0f;

                    // 解析字段2
                    Double field2Raw = (double) (((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF));
                    Double field2 = (field2Raw / 8388607) * 100;

                    // 存储解析出的字段值.2
//                    synchronized (Constants.field1Values){
                        Constants.field1Values.add(field1+","+field2);
                        Constants.field2Values.add(field1Raw+","+field2Raw);
//                    }



                    System.out.printf("[解析] 字段1 = %.2f，字段2 = %.2f\n", field1, field2);
                } else {
                    System.out.println("[等待] 已接收一个有效报文，等待下一个再解析。");
                }

                System.out.println("---------------------------");
            }
            socket.close();

        } catch (Exception e) {
            System.err.println("接收线程异常: " + e.getMessage());
            socket.close();
            e.printStackTrace();
            // 存储解析出的字段值.2
//            synchronized (Constants.field1Values){
                Constants.field1Values.add(Constants.EndData);
//            }
        }
        Constants.endTime = LocalDateTime.now();
    }

    // ========== 打印解析出的字段数组 ==========
//    public static void printParsedValues() {
//        System.out.println("\n【字段解析结果汇总】");
//        for (int i = 0; i < field1Values.size(); i++) {
//            System.out.printf("[%d] 字段1 = %.2f, 字段2 = %.2f\n", i + 1, field1Values.get(i), field2Values.get(i));
//        }
//    }
}
