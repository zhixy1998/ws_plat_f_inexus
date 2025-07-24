package com.persee.workstation.uv_inexus.util;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ReceiveDeviceMsgUtil {

    public static void main(String[] args) {

        try{

            //创建对象
            DatagramSocket ds = new DatagramSocket(6000);
            //创建数据包
            byte[] bytes = new byte[1024];
            DatagramPacket dp = new DatagramPacket(bytes,bytes.length);
            //阻塞等待接收数据，填充到dp的缓冲区
            while(true) {
                ds.receive(dp);
                //解析数据
                byte[] data = dp.getData();
                int length = dp.getLength();
                System.out.println(new String(data, 0, length));
                if(dp.equals("bye")) break;
            }
            //关闭接收端
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
