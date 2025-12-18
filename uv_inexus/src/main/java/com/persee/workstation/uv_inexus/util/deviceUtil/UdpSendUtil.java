/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 报文发送工具类，负责发送报文，然后从队列取报文并解析后返给操作工具类
 * 该工具类只负责发送报文和报文处理
 */
@Slf4j
@Service("UdpSendUtil")
public class UdpSendUtil {

    @Value("${device.default.sendPort}")
    private Integer sendPort;

    @Value("${device.default.receivePort}")
    private Integer ethernetReceivePort;

    @Value("${device.default.receivePort}")
    private Integer wifiReceivePort;

    private final static ConnectUtil connectUtil = ConnectUtil.getInstance();

    private final static DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

    /**
     * 以太网查找设备
     */
    public R<List<SearchInfo>> searchDevice(String sendIp, String cmdId, byte[] sendBytes, int timeout, int connectType) {

        DatagramSocket socket;
        int receivePort;
        if (connectType == DeviceConnectTypeEnum.WIFI.getConnectType()) {
            socket = connectUtil.getWifiSocket(wifiReceivePort);
            wifiReceivePort = connectUtil.getWifiPort();
            receivePort = wifiReceivePort;
        } else {
            socket = connectUtil.getEtherSocket(ethernetReceivePort);
            ethernetReceivePort = connectUtil.getEthernetPort();
            receivePort = ethernetReceivePort;
        }
        try {

            UdpReceiveThread udpReceiveThread = new UdpReceiveThread();
            udpReceiveThread.networkReceptionStart(receivePort, connectType);
            MessageStoreManager store = MessageStoreManager.getInstance();

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), sendPort);

            socket.send(packet);

            sendMessageLog(sendIp, cmdId, sendBytes);

            long lastReceiveTime = System.currentTimeMillis();

            List<SearchInfo> ans = new ArrayList<>();

            while (System.currentTimeMillis() - lastReceiveTime < timeout) {
                try {
                    //等待查找仪器
                    Thread.sleep(2000);
                    List<String> allTempDevice = deviceManager.getAllTempDeviceIds();
                    log.info("正在查找仪器，已查找到{}台！", allTempDevice.size());

                } catch (Exception e) {
                    // 收到 timeout 就结束
                    if (System.currentTimeMillis() - lastReceiveTime >= socket.getSoTimeout()) {
                        break;
                    }
                }
            }

            List<String> allTempDevice = deviceManager.getAllTempDeviceIds();
//            for (String deviceId : allTempDevice) {
//                List<byte[]> bytes = store.drainAllMessages(deviceId);
//                Object parse = UdpResponseDeal.parse(cmdId, bytes, sendBytes);
//                SearchInfo searchInfo = (SearchInfo) parse;
//                searchInfo.setConnectType(connectType);
//                searchInfo.setDeviceId(deviceId);
//                ans.add(searchInfo);
//            }

            for (String deviceId : allTempDevice) {
                DeviceConnectInfo device = deviceManager.getDevice(deviceId);

                int size = store.getQueueSize(deviceId);
                if (size > 0) {
                    for (int i = 0; i < size; i++) {
                        byte[] message = store.getMessage(deviceId);

                        messageGetLog(deviceId, device.getConnectIdentity(), message);

                        if (isSearchDeviceMessage(message)){
                            SearchInfo searchInfo = new SearchInfo();
                            searchInfo.setDeviceIp(device.getConnectIdentity());
                            searchInfo.setConnectPort(device.getPort().toString());
                            searchInfo.setConnectType(connectType);
                            searchInfo.setDeviceId(deviceId);

                            StringBuilder deviceMac = new StringBuilder();

                            for (int m = 9; m <= 14; m++) {
                                deviceMac.append(message[m] & 0xFF); // 转换为无符号十进制数
                                deviceMac.append(":");
                            }
                            searchInfo.setDeviceMac(deviceMac.deleteCharAt(deviceMac.length()-1).toString());
                            ans.add(searchInfo);
                        }
                    }
                }
            }
            return R.ok(ans);
        } catch (Exception e) {
            log.error("查找仪器出错：{}", e.getMessage(), e);
            return R.fail("请检查仪器！");
        }
    }

    /**
     * 单个设备连接
     *
     * @param deviceId    设备ID
     * @param cmdId       指令ID
     * @param sendBytes   发送报文
     * @param replyNumber 回应报文字节数，包含 07 报文
     * @param timeout     超时时间
     * @param connectType 连接类型
     */
    public R<String> deviceNewIp(String deviceId, String cmdId, byte[] sendBytes, int replyNumber, int timeout, int connectType, String newIp) {

        DatagramSocket socket;
        int receivePort;
        if (connectType == DeviceConnectTypeEnum.WIFI.getConnectType()) {
            socket = connectUtil.getWifiSocket(ethernetReceivePort);
            wifiReceivePort = connectUtil.getWifiPort();
            receivePort = wifiReceivePort;
        } else {
            socket = connectUtil.getEtherSocket(ethernetReceivePort);
            ethernetReceivePort = connectUtil.getEthernetPort();
            receivePort = ethernetReceivePort;
        }
        try {

            MessageStoreManager store = MessageStoreManager.getInstance();

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName("255.255.255.255"), sendPort);
            store.clearDevice(deviceId);

            String newDeviceId = deviceManager.getOrCreateDeviceId(newIp, receivePort, connectType);

            socket.send(packet);
            sendMessageLog("255.255.255.255", cmdId, sendBytes);

            int i = 0;
            long lastReceiveTime = System.currentTimeMillis();
            boolean start = false;

            List<byte[]> responses = new ArrayList<>();

            while (i < replyNumber && System.currentTimeMillis() - lastReceiveTime < timeout) {
                if (store.getQueueSize(deviceId) == 0) {
                    Thread.sleep(10);
                    continue;
                }
                byte[] message = store.getMessage(deviceId);

                if (message == null) continue;
                i += message.length;
                responses.add(message);

                String receiveIp = deviceManager.getIpByDeviceId(deviceId);

                messageGetLog(deviceId, receiveIp, message);

                if (i < 12) {
                    continue;
                }

                if (isStartMessage(responses)) {
                    log.info("07回应报文，开始接收");
                    start = true;
                    responses.add(message);
                    store.transferQueueMerge(deviceId, newDeviceId);
                    deviceId = newDeviceId;
                    continue;
                }  else {
                    if (!start) {
                        log.info("该报文丢弃！");
                        i = 0;
                        responses.clear();
                        continue;
                    }
                }

                lastReceiveTime = System.currentTimeMillis();
            }

            if (i == replyNumber) {
                Object parse = UdpResponseDeal.parse(cmdId, responses, sendBytes);
                if (parse.equals("false")) {
                    return R.fail();
                }
                return R.ok(newDeviceId);
            }
            return R.fail();
        } catch (Exception e) {

            log.info("连接仪器出错：{}", e.getMessage());
            return R.fail();
        }
    }

    private void sendMessageLog(String ip, String cmdId, byte[] bytes) {

        log.info("发送指令 [{}] 报文：{}  到{}", cmdId, bytesToHex(bytes), ip);

    }

    private void messageGetLog(String deviceId, String ip, byte[] bytes){
        log.info("收到来自deviceId：{}, deviceIp: {} 的数据：{}", deviceId, ip, bytesToHex(bytes));
    }





    /**
     * 知道接收总数且全部接收后处理
     * @param deviceId  发送的机器ip
     * @param cmdId  发送和解析的指令
     * @param sendBytes  发送的具体报文
     * @param replyNumber  回应报文字节数量--包含07报文
     * @param timeout  报文最大超时时间

     */
    public R<Object> knowTotalReceiveSend(String deviceId, String cmdId, byte[] sendBytes, int replyNumber, int timeout, Integer connectType) {

        if (replyNumber < 1) {
            return R.fail("回应报文字节数大于 1 ！");
        }

        DatagramSocket socket;
        if (connectType.equals(DeviceConnectTypeEnum.WIFI.getConnectType())) {
            socket = connectUtil.getWifiSocket(ethernetReceivePort);
        } else {
            socket = connectUtil.getEtherSocket(ethernetReceivePort);
        }

        try {

            DeviceConnectInfo device = deviceManager.getDevice(deviceId);
            String sendIp = device.getConnectIdentity();

            MessageStoreManager store = MessageStoreManager.getInstance();

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), sendPort);
            store.clearDevice(deviceId);
            socket.send(packet);

            sendMessageLog(sendIp, cmdId, sendBytes);
            R<List<byte[]>> r = messageDeal(deviceId, timeout, replyNumber, null);
            if (!r.getSuccess()) {
                return R.fail("报文接收不足！");
            }
            List<byte[]> responses = r.getData();

            Object parse = UdpResponseDeal.parse(cmdId, responses, sendBytes);

            if (parse.equals("false")) {
                return R.fail("解析失败！");
            }

            return R.ok(parse);


        } catch (Exception e) {

            log.info("仪器信息出错：{}", e.getMessage());
            return R.fail("报文接收不足！");
        }
    }



    /**
     * 未知数量的报文
     * @param deviceId 操作的仪器唯一
     * @param cmdId  发送和解析的指令
     * @param sendBytes  发送的具体报文
     * @param timeout  报文最大超时时间
     * @param isDealOnTime  是否需要实时回显
     * @param deviceSerialNumber  仪器序列号--用于数据存储
     *
     */
    public R<Object> unknownReceive(String deviceId, String cmdId, byte[] sendBytes, int timeout, Boolean isDealOnTime, String deviceSerialNumber, byte[] endBytes) {
        DatagramSocket socket = connectUtil.getEtherSocket(ethernetReceivePort);
        try {

            MessageStoreManager store = MessageStoreManager.getInstance();

            DeviceConnectInfo device = deviceManager.getDevice(deviceId);
            String sendIp = device.getConnectIdentity();

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), sendPort);
            store.clearDevice(sendIp);
            socket.send(packet);

            sendMessageLog(sendIp, cmdId, sendBytes);

            if (isDealOnTime) {
                //String cmdId, String deviceId, String deviceSerialNumber, byte[] sendBytes, String xmlKey
                MessageOnTimeDeal.parse(cmdId, deviceId, deviceSerialNumber, sendBytes);
                return R.ok();
            }

            R<List<byte[]>> r = messageDeal(deviceId, timeout, Integer.MAX_VALUE, endBytes);
            List<byte[]> responses = r.getData();

            Object parse = UdpResponseDeal.parse(cmdId, responses, sendBytes);
            return R.ok(parse);

        } catch (Exception e) {
            log.error("连接仪器出错：{}", e.getMessage());
        }
        return R.ok();

    }


    private R<List<byte[]>> messageDeal(String deviceId, Integer timeout, Integer totalByteNumber, byte[] endBytes) {

        MessageStoreManager store = MessageStoreManager.getInstance();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String sendIp = device.getConnectIdentity();
        List<byte[]> responses = new ArrayList<>();
        boolean start = false;


        if (totalByteNumber == null || totalByteNumber < 12) {
            totalByteNumber = 12;
        }

        long lastReceiveTime = System.currentTimeMillis();
        int i = 0;
        try {
            while (i < totalByteNumber && System.currentTimeMillis() - lastReceiveTime < timeout) {
                if (store.getQueueSize(deviceId) == 0) {
                    Thread.sleep(10);
                    continue;
                }

                byte[] bytes = store.peekMessage(deviceId);

                messageGetLog(deviceId, sendIp, bytes);

                if (isStartMessage(bytes)) {
                    log.info("07回应报文");
                    start = !start;
                    if (!start) {
                        log.info("多收07报文！退出");
                        return R.fail("报文不足!");
                    }
                    byte[] message = store.getMessage(deviceId);
                    responses.add(message);
                    i += message.length;
                    continue;
                } else {
                    if (!start) {
                        log.info("错误报文丢弃！");
                        store.getMessage(deviceId);
                        continue;
                    }

                    byte[] message = store.getMessage(deviceId);
                    responses.add(message);
                    i += message.length;

                    if (endBytes != null && endBytes.length >= 4) {
                        boolean isEnd = true;
                        for (int j = 0; j < 4; j++) {
                            if (message[j] != endBytes[j]) {
                                isEnd = false;
                                break;
                            }
                        }
                        if (isEnd) {
                            log.info("报文结束！");
                            break;
                        }
                    }
                }

                lastReceiveTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            log.info("处理消息出错：{}", e.getMessage());
        }
        return R.ok(responses);


    }


    // 添加辅助方法用于将字节数组转换为十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private static final byte SEARCH_DEVICE_HEADER1 = (byte) 0xF1;
    private static final byte SEARCH_DEVICE_HEADER2 = (byte) 0x03;

    /**
     * 判断是否为设备搜索消息（协议头匹配）
     * @param reply 原始响应字节数组（可为null）
     * @return true: 是设备搜索消息, false: 否
     */
    private boolean isSearchDeviceMessage(byte[] reply) {
        // 防御性检查
        if (reply == null || reply.length < 13) {
            return false;
        }
        return reply[2] == SEARCH_DEVICE_HEADER1 &&
                reply[3] == SEARCH_DEVICE_HEADER2;
    }

    private boolean isStartMessage(List<byte[]> responses) {

        // 2. 从前往后收集前12字节（从第一条响应开始）
        int needed = 12;
        byte[] firstBytes = new byte[12];
        int index = 0; // 从firstBytes开头填充

        // 从第一条响应开始向前遍历
        for (int i = 0; i < responses.size() && needed > 0; i++) {
            byte[] response = responses.get(i);
            int len = response.length;

            // 当前响应长度足够
            if (len >= needed) {
                // 从当前响应开头取所需字节
                System.arraycopy(response, 0, firstBytes, index, needed);
                break;
            }
            // 当前响应长度不足
            else {
                // 复制整个响应
                System.arraycopy(response, 0, firstBytes, index, len);
                needed -= len;
                index += len;
            }
        }

        // 3. 检查是否收集到12字节
        if (needed > 0) {
            return false; // 不足12字节
        }

        // 4. 验证报文开头（关键修正：从前往后匹配）
        return firstBytes[0] == (byte) 0xFE &&
                firstBytes[1] == (byte) 0xB5 &&
                firstBytes[2] == (byte) 0xFF &&
                firstBytes[3] == (byte) 0x07 &&
                firstBytes[4] == (byte) 0x00 &&
                firstBytes[5] == (byte) 0x00 &&
                firstBytes[6] == (byte) 0x00 &&
                firstBytes[7] == (byte) 0x00 &&
                firstBytes[8] == (byte) 0x00 &&
                firstBytes[9] == (byte) 0x00 &&
                firstBytes[10] == (byte) 0x00 &&
                firstBytes[11] == (byte) 0xB9;
    }

    private boolean isStartMessage(byte[] message) {
//        FF B5 FF 07 00 00 00 00 00 00 00 BA

        return message[0] == (byte) 0xFF && message[1] == (byte) 0xB5
                && message[2] == (byte) 0xFF && message[3] == (byte) 0x07 && message[4] == (byte) 0x00
                && message[5] == (byte) 0x00 && message[6] == (byte) 0x00 && message[7] == (byte) 0x00
                && message[8] == (byte) 0x00 && message[9] == (byte) 0x00 && message[10] == (byte) 0x00 && message[11] == (byte) 0xBA;
    }
}
