package com.persee.workstation.uv_inexus.util;

import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoIt.TestUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandDTO;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandRequestDTO;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.ScanParamInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    @Autowired
    private DeviceManageService deviceManageService;
    @Autowired
    private DeviceMessageUtils deviceMessageUtils;

    // 有效报文计数
    private static int validPacketCount = 0;
    private static boolean receiving = true;
    public void scanStart(String deviceSerialNumber, ScanParamInfo scanParamInfo) {
        // 根据设备序列号获取目标IP地址
        TARGET_IP=deviceInfoService.getIp(deviceSerialNumber);

        CommandRequestDTO commandRequestDTO = new CommandRequestDTO();
        List<CommandDTO> commands = new ArrayList<>();

        // 光度模式
        CommandDTO com1 = new CommandDTO();
        com1.setCmdId(SetParamEnum.SetPhotoMode.getComId());
        com1.setParams(new Object[]{scanParamInfo.getLuminosity()});
        commands.add(com1);
        // 开始波长
        CommandDTO com2 = new CommandDTO();
        com2.setCmdId(SetParamEnum.SetStartWl.getComId());
        com2.setParams(new Object[]{scanParamInfo.getStartWaveLength()});
        commands.add(com2);
        // 结束波长
        CommandDTO com3 = new CommandDTO();
        com3.setCmdId(SetParamEnum.SetEndWl.getComId());
        com3.setParams(new Object[]{scanParamInfo.getEndWaveLength()});
        commands.add(com3);
        // 波长间隔
        CommandDTO com4 = new CommandDTO();
        com4.setCmdId(SetParamEnum.SetScanWlInterval.getComId());
        com4.setParams(new Object[]{scanParamInfo.getWaveLengthInterval()});
        commands.add(com4);
        // 响应时间
        CommandDTO com5 = new CommandDTO();
        com5.setCmdId(SetParamEnum.SetResponseTime.getComId());
        com5.setParams(new Object[]{scanParamInfo.getResponseTime()});
        commands.add(com5);
        // 换灯波长
        CommandDTO com6 = new CommandDTO();
        com6.setCmdId(SetParamEnum.SetChangeLampWl.getComId());
        com6.setParams(new Object[]{scanParamInfo.getChangeLampWaveLength()});
        commands.add(com6);
        // 能量灯
        CommandDTO com7 = new CommandDTO();
        com7.setCmdId(SetParamEnum.SetLampPos.getComId());
        com7.setParams(new Object[]{scanParamInfo.getEnergyLamp()});
        commands.add(com7);
        // 增益
        CommandDTO com8 = new CommandDTO();
        com8.setCmdId(SetParamEnum.SetGain.getComId());
        com8.setParams(new Object[]{scanParamInfo.getGain()});
        commands.add(com8);

        commandRequestDTO.setCommands(commands);

        TestUtil.connect(commandRequestDTO);



        // 设置扫描参数
        deviceMessageUtils.setScanParam(TARGET_IP,TARGET_PORT,ByteUse.SpectrumScanAllConfig);
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

            // 开始扫描报文
            byte[] scanCmd = new byte[]{
                    // 极快扫描
                    (byte) 0xB5, (byte) 0xFF, (byte) 0x13, (byte) 0x64,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2B
                    // 慢扫
                    //  (byte) 0xB5, (byte) 0xFF, (byte) 0x13, (byte) 0x01,
                    //  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    //  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC8
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
            Thread.sleep(100);

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

                    int index = 2;
                    // 将field1和field2转换为bigdecimal类型 根据index的值保留小数的位数
                    field1 = BigDecimal.valueOf(field1).setScale(index, RoundingMode.HALF_UP).doubleValue();
                    field2 = BigDecimal.valueOf(field2).setScale(index, RoundingMode.HALF_UP).doubleValue();

                    // 存储解析出的字段值.2
                    Constants.field1Values.add(field1+","+field2);
                    Constants.field2Values.add(field1Raw+","+field2Raw);



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
            Constants.field1Values.add(Constants.EndData);
        }
        Constants.endTime = LocalDateTime.now();
    }
}
