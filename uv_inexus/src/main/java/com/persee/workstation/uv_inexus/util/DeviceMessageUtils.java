package com.persee.workstation.uv_inexus.util;

import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.instrumentVo.FindDeviceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@Service("DeviceMessageUtils")
public class DeviceMessageUtils {
    private volatile static String DEFAULT_IP = "255.255.255.255";
//    private volatile static String DEFAULT_IP = "169.254.110.254";

    @Value("${device.default.machineIP}")
    private String MACHINE_IP;

    @Value("${device.default.sendPort}")
    private Integer SEND_PORT;

    @Value("${device.default.receivePort}")
    private Integer RECEIVE_PORT;

    private volatile static Integer DEFAULT_PORT = 5000;

    private final String check = "校验数据";

    private final Integer timeOut = 15000; //10s


    public FindDeviceVO searchDevice(){
        /*
        1,连接设备---返回ip地址
        2，获取序列号--返回序列号
        3，获取初始化状态--返回状态值
         */
        FindDeviceVO vo = new FindDeviceVO();

        try {

            //广播消息，获取机器回复
            for (byte[] bytes : ByteUse.SearchMachineSend) {
                InetAddress ip = InetAddress.getByName(DEFAULT_IP);
                byte[][] receive1 = send(ip, bytes, DEFAULT_PORT);
                String s = dealData(receive1, 1);
                vo.setIp(s);
            }

            //连接机器
            for (byte[] bytes : ByteUse.LinkMachineSend) {
                InetAddress ip = InetAddress.getByName(DEFAULT_IP);
                byte[][] receive1 = send(ip, bytes, DEFAULT_PORT);
                String s = dealData(receive1, 2);

            }

            //获取序列号
            for (byte[] bytes : ByteUse.ReadSerialnumberSend) {
                InetAddress ip = InetAddress.getByName(DEFAULT_IP);
                byte[][] receive1 = send(ip, bytes, DEFAULT_PORT);
                String ds = dealData(receive1, 3);
                vo.setDeviceSerialNumber(ds);
            }

            //获取初始化状态值
            for (byte[] bytes : ByteUse.InitStatusSend) {
                InetAddress ip = InetAddress.getByName(DEFAULT_IP);
                byte[][] receive1 = send(ip, bytes, DEFAULT_PORT);
                String ds = dealData(receive1, 5);
                if (ds.equals("true")) {
                    vo.setIsInited(1);
                } else  {
                    vo.setIsInited(2);
                }
            }
            return vo;

        } catch (Exception e) {
            log.info(e.getMessage());
            return null;
        }
    }

    public R<String> initUtil(String deviceIp, Integer port, byte[][] data, Integer dealType) {

        String s = "";

        try{
            InetAddress ip = InetAddress.getByName(deviceIp);
            if(dealType == 13 || dealType == 14 || dealType == 15) {
                for (byte[] bytes : data) {
                    log.info("钨灯,氘灯和波长检查");
                    log.info("连接设备");
                    byte[][] receive = testSend(ip, bytes, port,dealType);
                    log.info(check);
                    s = dealData(receive, dealType);
                }
            } else {
                for (byte[] bytes : data) {
                    log.info("连接设备");
                    byte[][] receive = send(ip, bytes, port);
                    log.info(check);
                    s = dealData(receive, dealType);
                }
            }

            if (s.equals("false")){
                log.info("接收数据有问题");
                return R.fail(false, "处理接收数据出错");
            }
            return R.ok(s);

        } catch (Exception e) {
            log.info("出现异常");
            log.info(e.getMessage());
            return R.fail(false, "出现异常");
        }

    }

    public void setScanParam(String deviceIp, Integer port, byte[][] data){
        String s = "";

        try{
            InetAddress ip = InetAddress.getByName(deviceIp);

            for (byte[] bytes : data) {
                log.info("连接设备");
                int length = bytes.length;
                int dealType = bytes[length - 1];
//                bytes[length - 1] = 0;
                byte[] newData = Arrays.copyOf(bytes, length - 1);
                byte[][] receive = send(ip, newData, port);
                log.info(check);
                s = scanDealData(receive, dealType);
            }
            if (s.equals("false")){
                log.info("接收的数据有问题");

            }
        } catch (Exception e) {
            log.info("出现异常");
            log.info(e.getMessage());

        }
    }

    private String scanDealData(byte[][] data, Integer dealType) {
        String result = null;
        switch (dealType) {
            case 1:{//设置扫描间隔
                if ((data[0][11] & 0xff) == 0xBA && (data[1][4] & 0xff) == 0x19){
                    result = "true";
                }else
                    result = "false";
                return result;
            }

            case 2:{//设置起始波长
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xCE){
                    result = "true";
                }else
                    result = "false";
                return result;
            }

            case 3:{//设置结束波长
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0x11){
                    result = "true";
                }else
                    result = "false";
                return result;
            }

            case 4:{//设置扫描速度
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xC7){
                    result = "true";
                }else
                    result = "false";
                return result;
            }

            case 5:{//设置光度模式
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xC2){
                    result = "true";
                }else
                    result = "false";
                return result;
            }

            case 6:{//设置灯的位置
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xBF){
                    result = "true";
                }else
                    result = "false";
                return result;
            }

            case 7:{//设置增益
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xC4){
                    result = "true";
                }else
                    result = "false";
                return result;
            }

            case 8:{//位置设置
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xC4){
                    result = "true";
                }else
                    result = "false";
                return result;
            }
            default:
                return "false";
        }
    }

    private static String dealData(byte[][] data, Integer dealType) {
        String result = null;
        switch (dealType) {
            case 1: {

                if ((data[0][11] & 0xff) == 0xB9 && (data[1][3] & 0xff) ==  0x03) {
                    // 继续处理数据
                    int l = data.length;
                    StringBuilder s = new StringBuilder();
                    byte[] a = data[1];
                    for (int i = 0; i < 4; i++) {
                        s.append((a[i + 15] & 0xFF)).append(".");
                    }
                    // 去掉最后一个点
                    DEFAULT_IP = s.substring(0, s.length() - 1);
                    log.info(DEFAULT_IP);
                    result = DEFAULT_IP;
                } else
                    result = "false";
                return result;
            }

            case 2: {

                if ((data[0][11] & 0xff) == 0xBA && (data[1][4] & 0xff) ==  0x00) {
                    result = "true";
                } else if((data[0][11] & 0xff) == 0xBA && (data[1][4] & 0xff) ==  0x03)
                    result = "true";
                else
                    result = "false";

                return result;
            }

            case 3:
            {
                if ((data[0][3] & 0xff) == 0x07 && (data[1][3] & 0xff) == 0xFD) {
                    byte[] res = new byte[15];

                    for (int i = 4; i < 11; i++) {
                        res[i - 4] = data[1][i];
                        res[i + 3] = data[2][i];
                    }

                    res[14] = data[3][4];
                    String asciiStr = new String(res, StandardCharsets.US_ASCII);
                    StringBuilder a = new StringBuilder(asciiStr);
                    a.insert(2, "-");

                    result = a.toString();
                } else
                    result = "false";
                return result;
            }

            case 4:
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xE0) {
                    result = "true";
                } else
                    result = "false";
                return result;
            }

            case 5://初始化状态
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][3] & 0xff) == 0x0B) {
                    if ((data[1][4] & 0xff) == 0x01){
                        result = "true";
                    } else {
                        result = "false";
                    }
                } else
                    result = "false";
                return result;
            }

            case 6://初始化样品池
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xC1) {
                    result = "true";
                } else
                    result = "false";
                return result;
            }

            case 7://样品池定位
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xBD) {
                    result = "true";
                } else
                    result = "false";
                return result;
            }

            case 8://初始化滤光片
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xB7) {
                    result = "true";
                } else
                    result = "false";
                return result;
            }

            case 9://狭缝点击初始化
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xB8) {
                    result = "true";
                } else
                    result = "false";
                return result;
            }

            case 10://设置狭缝宽度
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][3] & 0xff) == 0x02) {
                    result = "true";
                } else
                    result = "false";
                return result;
            }

            case 11://初始化波长
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xB9) {
                    result = "true";
                } else
                    result = "false";
                return result;
            }

            case 12://灯电机初始化
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][11] & 0xff) == 0xBA) {
                    result = "true";
                } else
                    result = "false";
                return result;
            }

            case 13://检查钨灯能量
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][3] & 0xff) == 0x0b && (data[1][4] & 0xff) == 0x00)  {
                    result = "true";
                } else
                    result = "false";
                return result;
            }

            case 14://检查氘灯能量
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][3] & 0xff) == 0x0C && (data[1][4] & 0xff) == 0x00)  {
                    result = "true";
                } else
                    result = "false";
                return result;
            }
            case 15://检查氘灯能量
            {
                if ((data[0][11] & 0xff) == 0xBA && (data[1][3] & 0xff) == 0x0A && (data[1][4] & 0xff) == 0x00)  {
                    result = "true";
                } else
                    result = "false";
                return result;
            }
            default:
                return "false";
        }
    }



    public String spectrumMeasurement() {
        /*
        1，发送配置
        2，发送数据
        3，返回数据
        4，处理数据
         */
        try {
//            for (byte[] bytes : ByteUse.configurationSend) {
//                InetAddress ip = InetAddress.getByName(DEFAULT_IP);
//                byte[][] receive1 = send(ip, bytes, DEFAULT_PORT);
//                measurementDealData(receive1, 1);
//            }

        }catch (Exception e) {
            log.info(e.getMessage());
        }
        return DEFAULT_IP;
    }

    private String measurementDealData(byte[][] data, Integer dealType){
        switch (dealType){
            case 1: {

                return "";
            }
            case 2: {

                return "";
            }
            case 3: {

                return "";
            }
            case 4: {

                return "";
            }
            default:
                return "";
        }
    }

    public byte[][] send(InetAddress ip, byte[] data, int port) {
        DatagramSocket socket = null;
        try {

            int length = data.length;
            byte[] cmd = new byte[length - 1];
            //提取data指令
//            System.arraycopy(data, 0, cmd, 0, length);
            for (int i = 0; i < length - 1; i++) {
                cmd[i] = data[i];
            }

            InetAddress localBindIp = InetAddress.getByName(MACHINE_IP);
            socket = new DatagramSocket(RECEIVE_PORT, localBindIp); // 绑定到指定 IP
            socket.setSoTimeout(timeOut);
            //发送udp请求
            DatagramPacket packet = new DatagramPacket(cmd, cmd.length, ip, port);

            socket.send(packet);

            int num = data[length - 1];

            int i = 0;

            //返回结果类
            byte[][] result = new byte[num][50];


            while (i != num) {
                byte[] receive = result[i];

                DatagramPacket dp = new DatagramPacket(receive, receive.length);
                socket.receive(dp);
                //解析数据
                receive = Arrays.copyOf(dp.getData(), dp.getLength());
                i++;
            }
            socket.close();
            return result;
        } catch (Exception e) {
            log.info("出现异常：{}", e.getMessage());
            assert socket != null;
            socket.close();
            return null;
        }
    }

    public byte[][] testSend(InetAddress ip, byte[] data, int port, Integer b) {
        DatagramSocket socket = null;

        try {
            int length = data.length;
            byte[] cmd = Arrays.copyOf(data, length - 1);  // 去掉最后一字节

            InetAddress localBindIp = InetAddress.getByName(MACHINE_IP);
            socket = new DatagramSocket(RECEIVE_PORT, localBindIp);
            socket.setSoTimeout(timeOut);

            // 构造发送的UDP数据包
            DatagramPacket packet = new DatagramPacket(cmd, cmd.length, ip, port);
            socket.send(packet); // 发送请求

            byte[] firstPacket = null;
            byte[] lastPacket = null;

            boolean firstReceived = false;

            while (true) {
                byte[] buffer = new byte[20];
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(dp);  // 接收数据
                    byte[] receivedData = Arrays.copyOf(dp.getData(), dp.getLength());

                    if (!firstReceived) {
                        firstPacket = receivedData;  // 保存第一条
                        firstReceived = true;
                    }

                    lastPacket = receivedData;  // 一直更新，最后一次即为最后一条

                    //只用前4字节判断是否为结束帧
                    if (isEndFrame(lastPacket, b)) {
                        break;
                    }

                } catch (Exception e) {
                    // 超时未收到更多数据，退出
                    log.info("异常");
                    break;
                }
            }

            socket.close();

            // 构造返回的二维数组，只保存第一条和最后一条报文
            if (firstPacket != null && lastPacket != null) {
                byte[][] result = new byte[2][];
                result[0] = firstPacket;
                result[1] = lastPacket;
                return result;
            } else {
                return null; // 如果一条都没收到
            }

        } catch (Exception e) {
            if (socket != null) socket.close();
            e.printStackTrace();
            return null;
        }
    }

    //只比较前4字节是否匹配结束帧标志
    private boolean isEndFrame(byte[] data, Integer b) {

        if (data.length < 4) return false;
        log.info(b.toString());
        if (b == 13) {
            log.info("钨灯");
            return (data[0] == (byte) 0xFF &&
                    data[1] == (byte) 0xB5 &&
                    data[2] == (byte) 0x00 &&
                    data[3] == (byte) 0x0b);
        }else if (b == 14){
            log.info("氘灯");
            return  (data[0] == (byte) 0xFF &&
                    data[1] == (byte) 0xB5 &&
                    data[2] == (byte) 0x00 &&
                    data[3] == (byte) 0x0C);
        } else if (b == 15) {
            log.info("波长");
            return (data[0] == (byte) 0xFF &&
                    data[1] == (byte) 0xB5 &&
                    data[2] == (byte) 0x00 &&
                    data[3] == (byte) 0x0A);
        }

        return false;
    }
}





