package com.persee.workstation.uv_inexus.util;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class SendMessageUtils {

    public static void main(String[] args) {

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(); // 创建UDP发送端 socket

            // 设置目标IP（下位机 IP）和端口
            InetAddress targetIP = InetAddress.getByName("169.254.110.68");
            int targetPort = 5000;
            //byte[] cmd = {(byte)0xB5,(byte) 0xFF,(byte) 0xF0,(byte) 0x01,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x0B,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0xB0};
            InetAddress localBindIp = InetAddress.getByName("169.254.110.67");
            socket = new DatagramSocket(6000, localBindIp); // 绑定到指定 IP
            byte[] cmd = new byte[12];


            // 填写固定头和命令字节
            cmd[0] =  (byte)0xB5;
            cmd[1] = (byte) 0xFE;
            cmd[2] = (byte) 0xF1;
            cmd[3] = (byte) 0x03;
            cmd[4] = (byte) 0x01;

            // MAC地址字节（可根据实际修改）
            cmd[5] = (byte) 0x00;
            cmd[6] = (byte) 0x00;
            cmd[7] = (byte) 0x00;
            cmd[8] = (byte) 0x00;
            cmd[9] = (byte) 0x00;
            cmd[10] = (byte) 0x00;

            // 计算校验和（前11字节求和，取低8位）
            byte checksum = 0;
            for (int i = 0; i <= 10; i++) {
                checksum += cmd[i];
            }
            cmd[11] = checksum;
            for (byte b : cmd) {
                System.out.println("*****"+b);
            }
            // 无限循环，每秒发送一次
            while (true) {

                // 发送数据包
                DatagramPacket packet = new DatagramPacket(cmd, cmd.length, targetIP, targetPort);
                socket.send(packet);

                System.out.println("UDP 报文已发送，校验和 = 0x" + String.format("%02X", checksum));

                // 每秒发送一次
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.err.println("发送异常：" + e.getMessage());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }



}
