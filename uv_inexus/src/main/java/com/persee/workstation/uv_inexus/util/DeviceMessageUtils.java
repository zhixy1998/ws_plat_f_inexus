package com.persee.workstation.uv_inexus.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Service("DeviceMessageUtils")
public class DeviceMessageUtils {

    private volatile static String DEFAULT_IP = "255.255.255.255";

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

        }catch(Exception e) {
            log.info(e.getMessage());
        }


    }

    private void dealData(byte[][] data, Integer dealType){
        switch (dealType) {
            case 1:
            {
                byte[][] def = ByteUse.SearchMachineReceive;
                if (data == def){
                    System.out.println("---和数据一致");
                }
                int l = data.length;
                StringBuilder s = new StringBuilder();
                byte[] a = data[1];
                for(int i = 0; i < 4; i++) {
                    s = s.append(a[i+15] + ".");
                }
                DEFAULT_IP = s.toString();
                log.info(DEFAULT_IP);
            }
                break;
            case 2:
            {
                log.info("继续处理");
            }
            break;
            default:
        }
    }

    public byte[][] send(InetAddress ip, byte[] data, int port){
        try{
            DatagramSocket socket = new DatagramSocket();
            int length = data.length;
            byte[] cmd = new byte[length -1];
            //提取data指令
//            System.arraycopy(data, 0, cmd, 0, length);
            for (int i = 0; i < length; i++) {
                cmd[i]= data[i];
            }

            //发送udp请求
            DatagramPacket packet = new DatagramPacket(cmd, cmd.length, ip, port);
            socket.send(packet);

            int num = data[length-1];

            int i = 0;
            //创建对象
            DatagramSocket ds = new DatagramSocket(6000);
            //返回结果类
            byte[][] result = new byte[num][1024];


            while (i != num) {
                byte[] receive = result[i];

                DatagramPacket dp = new DatagramPacket(receive,receive.length);
                ds.receive(dp);
                //解析数据
                byte[] getData = dp.getData();

                System.out.println(new String(data, 0, dp.getLength()));
                result[i] = getData;
                i++;
            }
            return result;
        } catch (Exception e) {
            log.info("出现异常：{}", e.getMessage());
            return null;
        }
    }



}
