package com.persee.workstation.uv_inexus.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@Slf4j
@Service("DeviceMessageUtils")
public class DeviceMessageUtils {
    private volatile static String DEFAULT_IP = "169.254.110.254";

    private volatile static Integer DEFAULT_PORT = 5000;

    public void connectDevice() {
        /*
        1，广播
        2，返回获取ip地址
        3，将ip赋给静态变量
        4，建立连接
        5，连接成功
         */
        try {
            for (byte[] bytes : ByteUse.SearchMachineSend) {
                InetAddress ip = InetAddress.getByName(DEFAULT_IP);
                byte[][] receive1 = send(ip, bytes, DEFAULT_PORT);
                dealData(receive1, 1);
            }

            for (byte[] bytes : ByteUse.LinkMachineSend) {
                InetAddress ip = InetAddress.getByName(DEFAULT_IP);
                byte[][] receive1 = send(ip, bytes, DEFAULT_PORT);
                dealData(receive1, 2);
            }

            for (byte[] bytes : ByteUse.ReadSerialnumberSend) {
                InetAddress ip = InetAddress.getByName(DEFAULT_IP);
                byte[][] receive1 = send(ip, bytes, DEFAULT_PORT);
                dealData(receive1, 3);
            }


        }catch(Exception e) {
            log.info(e.getMessage());
        }


    }

    private String dealData(byte[][] data, Integer dealType){
        String result = null;
        switch (dealType) {
            case 1: {
                log.info(String.valueOf(data[0][11]));
                log.info(String.valueOf((byte) 0xB9));
                if ((data[0][11]& 0xFF) == 0xB9 && (data[1][41] & 0xFF) == 0xB3 ) {
                    result = "true";
                } else {
                    result = "false";
                }

                // 继续处理数据
                int l = data.length;
                StringBuilder s = new StringBuilder();
                byte[] a = data[1];
                for (int i = 0; i < 4; i++) {
                    s.append((a[i + 15] & 0xFF)).append(".");
                }
                // 去掉最后一个点
                DEFAULT_IP = s.substring(0, s.length() - 1);
//                log.info(DEFAULT_IP);

                return result;
            }
            case 2:
            {
//                byte [][] def2 = ByteUse.LinkMachineReceive;
                if (data[0][11] == (byte) 0xBA && data[1][11] == (byte) 0xA4 ) {
//                    System.out.println("---和数据一致");
                    result =  "true";
                }
                else
                    result =  "false";

                return result;
            }
            default:
                return "";
        }

    }

    public byte[][] send(InetAddress ip, byte[] data, int port){
        try{
            DatagramSocket socket = new DatagramSocket();
            int length = data.length;
            byte[] cmd = new byte[length -1];
            //提取data指令
//            System.arraycopy(data, 0, cmd, 0, length);
            for (int i = 0; i < length - 1; i++) {
                cmd[i]= data[i];
            }

            InetAddress localBindIp = InetAddress.getByName("169.254.110.67");
            socket = new DatagramSocket(6000, localBindIp); // 绑定到指定 IP
            //发送udp请求
            DatagramPacket packet = new DatagramPacket(cmd, cmd.length, ip, port);

            socket.send(packet);

            int num = data[length-1];

            int i = 0;
            //创建对象

            //返回结果类
            byte[][] result = new byte[num][4096];


            while (i != num) {
                byte[] receive = result[i];

                DatagramPacket dp = new DatagramPacket(receive,receive.length);
                socket.receive(dp);
                //解析数据
                receive = dp.getData();
                i++;
            }
            return result;
        } catch (Exception e) {
            log.info("出现异常：{}", e.getMessage());
            return null;
        }
    }


}


