/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
