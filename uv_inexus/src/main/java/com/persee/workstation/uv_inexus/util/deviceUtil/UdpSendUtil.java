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
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Autowired
    DeviceInfoService infoService;

    private final static ConnectUtil connectUtil = ConnectUtil.getInstance();

    private final static DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

    private final static UdpReceiveThread udpReceiveThread = UdpReceiveThread.getInstance();

    //时间扫描线程测量是否结束，true-结束，false-未结束
    public final static ConcurrentMap<String, Boolean> timeMeasureIsStop = new ConcurrentHashMap<>();

    private static UnknowCountMessageQueue unknowCountMessageQueue = UnknowCountMessageQueue.getInstance();

    private final AtomicInteger scanResultIndex = new AtomicInteger(0);

    private static long currentTime;
    @Autowired
    MessageOnTimeDeal messageOnTimeDeal;

    /**
     * 以太网查找设备
     */
    public R<List<SearchInfo>> searchDevice(String sendIp, String cmdId, byte[] sendBytes, int timeout, int connectType) {

        DatagramSocket socket;
        int receivePort;

        try {
            socket = connectUtil.getSocket(connectType, ethernetReceivePort);

            if (socket == null) {
                return R.fail("请检查"+ DeviceConnectTypeEnum.getEnum(connectType).getDesc()+"连接！");
            }
            receivePort = connectUtil.getSocketPort(connectType);

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
                        byte[] message = store.takeMessageBlocking(deviceId, timeout);

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
                            break;
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
    public R<String> deviceNewIp(String deviceId, String cmdId, byte[] sendBytes, int replyNumber, int timeout, int connectType, String newIp, List<String> headReceiveBytes) {

        DatagramSocket socket;
        int receivePort;
        try {

            socket = connectUtil.getSocket(connectType, ethernetReceivePort);

            if (socket == null) {
                return R.fail("请检查"+ DeviceConnectTypeEnum.getEnum(connectType).getDesc()+"连接！");
            }
            receivePort = connectUtil.getSocketPort(connectType);


            MessageStoreManager store = MessageStoreManager.getInstance();

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName("255.255.255.255"), sendPort);
            store.clearDeviceMessages(deviceId);

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
                byte[] bytes = store.peekMessage(deviceId);

                if (bytes == null) continue;

                String receiveIp = deviceManager.getIpByDeviceId(deviceId);

                messageGetLog(deviceId, receiveIp, bytes);

                if (isNewIpStartMessage(bytes)) {
                    log.info("07回应报文，开始接收");
                    start = !start;

                    if (!start) {
                        log.info("多收07报文！退出");
                        return R.fail("报文不足!");
                    }
                    byte[] message = store.getMessage(deviceId);
                    responses.add(message);
                    i += message.length;
                    store.transferQueueMerge(deviceId, newDeviceId);
                    deviceId = newDeviceId;
                    continue;
                }  else {
                    if (!start) {
                        log.info("该报文丢弃！");
                        store.getMessage(deviceId);
                        continue;
                    }

                    StringBuilder s = new StringBuilder();
                    for (int j = 0; j < 4; j++) {
                        s.append(0XFF & bytes[j]).append(":");
                    }
                    String substring = s.substring(0, s.length() - 1);
                    if (!headReceiveBytes.contains(substring)) {
                        log.info("不是需要的报文头！");
                        store.getMessage(deviceId);
                        continue;
                    }

                    byte[] message = store.getMessage(deviceId);
                    responses.add(message);
                    i += message.length;
                }

                lastReceiveTime = System.currentTimeMillis();
            }

            if (i >= replyNumber) {
                OperationResultEntity parse = UdpResponseDeal.parse(cmdId, responses, sendBytes);
                if (!parse.getIsSuccess()) {
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
    public R<Object> knowTotalReceiveSend(String deviceId, String cmdId, byte[] sendBytes, int replyNumber, int timeout, Integer connectType, List<String> headReceiveBytes) {

        if (replyNumber < 1) {
            return R.fail("回应报文字节数大于 1 ！");
        }

        DatagramSocket socket;
        int receivePort;
        boolean isReceiveRunning;

        try {

            socket = connectUtil.getSocket(connectType, ethernetReceivePort);

            if (socket == null) {
                return R.fail("请检查"+ DeviceConnectTypeEnum.getEnum(connectType).getDesc()+"连接！");
            }
            receivePort = connectUtil.getSocketPort(connectType);
            isReceiveRunning = udpReceiveThread.isRunning(connectType);


            if (!isReceiveRunning) {
                udpReceiveThread.networkReceptionStart(receivePort, connectType);
            }

            DeviceConnectInfo device = deviceManager.getDevice(deviceId);
            String sendIp = device.getConnectIdentity();

            MessageStoreManager store = MessageStoreManager.getInstance();

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), sendPort);
            store.clearDeviceMessages(deviceId);

            socket.send(packet);

            sendMessageLog(sendIp, cmdId, sendBytes);
            R<List<byte[]>> r = new R<>();

            if (!Objects.equals(cmdId, "Stop")) {
                for (int i = 0; i < 3; i++) {

                    r = messageDeal1(deviceId, timeout, replyNumber, null, headReceiveBytes);
                    if (r.getSuccess()) {
                        break;
                    } else {
                        log.info("报文接收不足！重试第{}次", i+1);
                        store.clearDeviceMessages(deviceId);
                        socket.send(packet);

                        sendMessageLog(sendIp, cmdId, sendBytes);
                    }
                }
            }

            if (!r.getSuccess()) {
                return R.fail(DeviceConstants.unCountResponse);
            }
            List<byte[]> responses = r.getData();

            OperationResultEntity parse = UdpResponseDeal.parse(cmdId, responses, sendBytes);

            if (!parse.getIsSuccess()) {
                return R.fail("解析失败！");
            }

            if (parse.getCurrentMeasure() != null){
                ChangeParam changeParam = parse.getCurrentMeasure();
                System.out.println("测量值：" + changeParam);
            }

            return R.ok(parse);


        } catch (Exception e) {

            log.info("仪器信息出错：{}", e.getMessage());
            return R.fail("报文接收不足！");
        }
    }


    //测试
    public R<Object> knowTotalReceiveSend1(String deviceId, String cmdId, byte[] sendBytes, int replyNumber, int timeout, Integer connectType, List<String> headReceiveBytes) {

        if (replyNumber < 1) {
            return R.fail("回应报文字节数大于 1 ！");
        }

        DatagramSocket socket;
        int receivePort;
        boolean isReceiveRunning;

        try {

            socket = connectUtil.getSocket(connectType, ethernetReceivePort);

            if (socket == null) {
                return R.fail("请检查"+ DeviceConnectTypeEnum.getEnum(connectType).getDesc()+"连接！");
            }
            receivePort = connectUtil.getSocketPort(connectType);
            isReceiveRunning = udpReceiveThread.isRunning(connectType);


            if (!isReceiveRunning) {
                udpReceiveThread.networkReceptionStart(receivePort, connectType);
            }

            DeviceConnectInfo device = deviceManager.getDevice(deviceId);
            String sendIp = device.getConnectIdentity();

            MessageStoreManager store = MessageStoreManager.getInstance();

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), sendPort);
            store.clearDeviceMessages(deviceId);

            socket.send(packet);

            sendMessageLog(sendIp, cmdId, sendBytes);
            R<List<byte[]>> r = new R<>();

            if (!Objects.equals(cmdId, "Stop")) {
                for (int i = 0; i < 3; i++) {

                    r = messageDeal1(deviceId, timeout, replyNumber, null, headReceiveBytes);
                    if (r.getSuccess()) {
                        break;
                    } else {
                        log.info("报文接收不足！重试第{}次", i+1);
                        store.clearDeviceMessages(deviceId);
                        socket.send(packet);

                        sendMessageLog(sendIp, cmdId, sendBytes);
                    }
                }
            }

            if (!r.getSuccess()) {
                return R.fail(DeviceConstants.unCountResponse);
            }
            List<byte[]> responses = r.getData();

            OperationResultEntity parse = UdpResponseDeal.parse(cmdId, responses, sendBytes);

            if (!parse.getIsSuccess()) {
                return R.fail("解析失败！");
            }

            if (parse.getCurrentMeasure() != null){
                ChangeParam changeParam = parse.getCurrentMeasure();
                System.out.println("测量值：" + changeParam);
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

//    public R<Object> unknownReceive(String deviceId, String cmdId, byte[] sendBytes, int timeout, Boolean isDealOnTime, String deviceSerialNumber, byte[] endBytes) {
//
//        DatagramSocket socket;
//        try {
//
//            MessageStoreManager store = MessageStoreManager.getInstance();
//
//            DeviceConnectInfo device = deviceManager.getDevice(deviceId);
//            Integer connectType = device.getConnectType();
//
//            if (connectType.equals(DeviceConnectTypeEnum.WIFI.getConnectType())) {
//                socket = connectUtil.getWifiSocket(wifiReceivePort);
//            } else {
//                socket = connectUtil.getEtherSocket(ethernetReceivePort);
//            }
//
//            String sendIp = device.getConnectIdentity();
//
//            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
//                    InetAddress.getByName(sendIp), sendPort);
//            store.clearDeviceMessages(sendIp);
//            store.clearDeviceMessages(deviceId);
//
//            socket.send(packet);
//
//            sendMessageLog(sendIp, cmdId, sendBytes);
//
//            if (isDealOnTime) {
//                long startTime = System.currentTimeMillis();
//                long currentTimeout = timeout;   // 第一条使用原 timeout
//                int recvCount = 0;
//
//                while (System.currentTimeMillis() - startTime < currentTimeout) {
//
//                    if (store.getQueueSize(deviceId) == 0) {
//                        Thread.sleep(5);
//                        continue;
//                    }
//
//                    log.info(
//                            "[DEBUG] queueSizeBeforeGet={}",
//                            store.getQueueSize(deviceId)
//                    );
//
//
//                    byte[] message = store.getMessage(deviceId);
//                    if (message == null) {
//                        continue;
//                    }
//                    recvCount++;
//
//                    // 第二条开始，启用 5s 滑动超时
//                    if (recvCount >= 2) {
//                        currentTimeout = 30000L;
//                    }
//                    startTime = System.currentTimeMillis();
//
//                    OperationResultEntity parsed =
//                            UdpResponseDeal.parseScan(
//                                    Collections.singletonList(message),
//                                    cmdId,
//                                    deviceSerialNumber
//                            );
//
//                    if (parsed != null
//                            && parsed.getCurrentMeasure() != null
//                            && parsed.getCurrentMeasure().getData() != null) {
//
//                        ChangeParam data = parsed.getCurrentMeasure().getData();
//
//                        log.info(
//                                "[SCAN-RECV] device={}, cmd={}, wl={}, s={}, r={}, d={}",
//                                deviceSerialNumber,
//                                cmdId,
//                                data.getWl(),
//                                data.getS(),
//                                data.getR(),
//                                data.getD()
//                        );
//
//                        unknowCountMessageQueue.putMessage(
//                                deviceSerialNumber,
//                                cmdId,
//                                data
//                        );
//                    }
//                }
//
//                return R.ok();
//            }
//
//            R<List<byte[]>> r = messageDeal(deviceId, timeout, Integer.MAX_VALUE, endBytes);
//            if (!r.getSuccess()) {
//                return R.fail("报文接收不足！");
//            }
//            List<byte[]> responses = r.getData();
//
//            Object parse = UdpResponseDeal.parse(cmdId, responses, sendBytes, deviceSerialNumber);
//            return R.ok(parse);
//
//        } catch (Exception e) {
//            log.error("连接仪器出错：{}", e.getMessage());
//        }
//        return R.ok();
//
//    }

    public R<Object> unknownReceive(String deviceId, String cmdId, byte[] sendBytes, int timeout, Boolean isDealOnTime, String deviceSerialNumber, byte[] endBytes, List<String> headReceiveBytes) {

        DatagramSocket socket;
        try {
            boolean isReceiveRunning;
            MessageStoreManager store = MessageStoreManager.getInstance();

            DeviceConnectInfo device = deviceManager.getDevice(deviceId);
            Integer connectType = device.getConnectType();

            socket = connectUtil.getSocket(connectType, ethernetReceivePort);

            if (socket == null) {
                return R.fail("请检查"+ DeviceConnectTypeEnum.getEnum(connectType).getDesc()+"连接！");
            }
            Integer receivePort = connectUtil.getSocketPort(connectType);
            isReceiveRunning = udpReceiveThread.isRunning(connectType);

            if (!isReceiveRunning) {
                udpReceiveThread.networkReceptionStart(receivePort, connectType);
            }

            String sendIp = device.getConnectIdentity();

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), sendPort);
            store.clearDeviceMessages(sendIp);

            socket.send(packet);

            sendMessageLog(sendIp, cmdId, sendBytes);

            if (isDealOnTime) {
                ScanSessionManager.startScan(deviceId);
                messageOnTimeDeal.parse(cmdId, deviceId, deviceSerialNumber, sendBytes, timeout,headReceiveBytes);
                return R.ok();
            }

            R<List<byte[]>> r = new R<>();
            for (int i = 0; i < 3; i++) {
                r = messageDeal1(deviceId, timeout, Integer.MAX_VALUE, endBytes, headReceiveBytes);
                if (r.getSuccess()) {
                    break;
                } else {
                    log.info("报文接收不足！重试第{}次", i+1);
                    store.clearDeviceMessages(sendIp);
                    socket.send(packet);
                    sendMessageLog(sendIp, cmdId, sendBytes);
                }
            }

            if (!r.getSuccess()) {
                return R.fail(DeviceConstants.unCountResponse);
            }
            List<byte[]> responses = r.getData();

//            Object parse = UdpResponseDeal.parse(cmdId, responses, sendBytes);
            OperationResultEntity parse = UdpResponseDeal.parse(cmdId, responses, sendBytes);
            if (!parse.getIsSuccess()){
                return R.fail(parse.getErrorMsg());
            }
            return R.ok(parse);

        } catch (Exception e) {
            log.error("连接仪器出错：{}", e.getMessage());
        }
        return R.ok();

    }

    public R<Object> TimeMeasure(String deviceId, String cmdId, byte[] sendBytes, int replyNumber, int timeout, Integer connectType, Double intervalTime, Double totalTime, String deviceSerialNumber, List<String> headReceiveBytes) {

        if (replyNumber < 1) {
            return R.fail("回应报文字节数大于 1 ！");
        }

        try {

            DatagramSocket socket = connectUtil.getSocket(connectType, ethernetReceivePort);

            if (socket == null) {
                return R.fail("请检查"+ DeviceConnectTypeEnum.getEnum(connectType).getDesc()+"连接！");
            }
            int receivePort = connectUtil.getSocketPort(connectType);
            boolean isReceiveRunning = udpReceiveThread.isRunning(connectType);


            if (!isReceiveRunning) {
                udpReceiveThread.networkReceptionStart(receivePort, connectType);
            }

            DeviceConnectInfo device = deviceManager.getDevice(deviceId);
            String sendIp = device.getConnectIdentity();

            ScheduledExecutorService scheduler;

            if (intervalTime != null && totalTime != null) {
                scheduler = startTimeMeasureSender(
                        socket,
                        sendBytes,
                        sendIp,
                        cmdId,
                        intervalTime,
                        totalTime,
                        deviceId,
                        replyNumber,
                        deviceSerialNumber,
                        headReceiveBytes
                );
            } else {
                scheduler = null;
                return R.fail("请选择测量间隔时间和测量总时间！");
            }

            /* ================= 4. 立即返回 ================= */

            return R.ok("TimeMeasure 启动成功");


        } catch (Exception e) {

            log.info("仪器信息出错：{}", e.getMessage());
            return R.fail("报文接收不足！");
        }
    }

    //测试
    private R<List<byte[]>> messageDeal1(String deviceId, Integer timeout, Integer totalByteNumber, byte[] endBytes,List<String> headReceiveBytes) {

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

                    StringBuilder s = new StringBuilder();
                    for (int j = 0; j < 4; j++) {
                        s.append(0XFF & bytes[j]).append(":");
                    }
                    String substring = s.substring(0, s.length() - 1);
                    if (!headReceiveBytes.contains(substring)) {
                        log.info("不是需要的报文头！");
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
            if (i < totalByteNumber && totalByteNumber != Integer.MAX_VALUE) {
                log.info("报文不足！");
                return R.fail("报文不足!");
            }
        } catch (Exception e) {
            log.info("处理消息出错：{}", e.getMessage());
            return R.fail("处理消息出错！");
        }
        return R.ok(responses);
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
            if (i < totalByteNumber && totalByteNumber != Integer.MAX_VALUE) {
                log.info("报文不足！");
                return R.fail("报文不足!");
            }
        } catch (Exception e) {
            log.info("处理消息出错：{}", e.getMessage());
            return R.fail("处理消息出错！");
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

    private boolean isNewIpStartMessage(byte[] firstBytes) {



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

    //定时器工具
    private ScheduledExecutorService startTimeMeasureSender(
            DatagramSocket socket,
            byte[] sendBytes,
            String sendIp,
            String cmdId,
            Double intervalTime,
            Double totalTime,
            String deviceId,
            Integer replyNumber,
            String deviceSerialNumber,
            List<String> headReceiveBytes
    ) {

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        long startTime = System.currentTimeMillis();
        long intervalMillis = (long) (intervalTime * 1000);
        long totalMillis = (long) (totalTime * 1000);

        MessageStoreManager store = MessageStoreManager.getInstance();

        DatagramPacket packet;
        try {
            packet = new DatagramPacket(
                    sendBytes,
                    sendBytes.length,
                    InetAddress.getByName(sendIp),
                    sendPort
            );
            timeMeasureIsStop.put(deviceId, false);
        } catch (Exception e) {
            log.error("创建 DatagramPacket 失败", e);
            return scheduler;
        }

        scheduler.scheduleAtFixedRate(() -> {
            try {
                currentTime  = System.currentTimeMillis();

                if (timeMeasureIsStop.get(deviceId)==null || timeMeasureIsStop.get(deviceId)) {
                    log.info("时间扫描停止");
                    timeMeasureIsStop.remove(deviceId);
                    unknowCountMessageQueue.putMessage(
                            deviceSerialNumber,
                            cmdId,
                            EndMarker.timeStop(deviceSerialNumber, cmdId)
                    );

                    infoService.newReleaseDevice(deviceSerialNumber);
                    scheduler.shutdown();
                    return;
                }

                // ===== 总时长到，结束 =====
                if (currentTime  - startTime >= totalMillis) {
                    log.info("时间扫描完成，总时长 {} 秒", totalTime);

                    unknowCountMessageQueue.putMessage(
                            deviceSerialNumber,
                            cmdId,
                            EndMarker.Normal(deviceSerialNumber, cmdId)
                    );

                    infoService.newReleaseDevice(deviceSerialNumber);
                    scheduler.shutdown();
                    return;
                }

                // ===== 正常一次采样 =====
                store.clearDeviceMessages(deviceId);
                socket.send(packet);
                sendMessageLog(sendIp, cmdId, packet.getData());

                long firstTimeMillis = System.currentTimeMillis();
                receiveAndDeal(deviceId, firstTimeMillis, intervalMillis, replyNumber, cmdId, deviceSerialNumber,headReceiveBytes);

            } catch (Exception e) {
                log.error("时间扫描定时任务异常，立即停止", e);
                unknowCountMessageQueue.putMessage(
                        deviceSerialNumber,
                        cmdId,
                        EndMarker.unNormal(deviceSerialNumber, cmdId)
                );

                infoService.newReleaseDevice(deviceSerialNumber);
                scheduler.shutdown();
            }

        }, 0, intervalMillis, TimeUnit.MILLISECONDS);

        return scheduler;
    }

    private void receiveAndDeal(String deviceId, long firstTimeMillis, long intervalTime, Integer replyNumber, String cmdId, String deviceSerialNumber, List<String> headReceiveBytes) {

        log.info("处理时间扫描报文，起始时间：{}，当前时间：{}", firstTimeMillis, System.currentTimeMillis());


        MessageStoreManager store = MessageStoreManager.getInstance();
        List<byte[]> responses = new ArrayList<>();
        boolean start = false;
        int i = 0;
        while ((System.currentTimeMillis() - firstTimeMillis) < intervalTime && i < replyNumber) {
            byte[] bytes = store.peekMessage(deviceId);
            if (bytes == null || bytes.length == 0) {
                continue;
            }
            log.info("开始接收，起始时间：{}，当前时间：{}", firstTimeMillis, System.currentTimeMillis());
            if (isStartMessage(bytes)) {
                log.info("07回应报文");
                start = !start;
                if (!start) {
                    log.info("多收07报文！退出");
                    return;
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

                StringBuilder s = new StringBuilder();
                for (int j = 0; j < 4; j++) {
                    s.append(0XFF & bytes[j]).append(":");
                }
                String substring = s.substring(0, s.length() - 1);
                if (!headReceiveBytes.contains(substring)) {
                    log.info("不是需要的报文头！");
                    store.getMessage(deviceId);
                    continue;
                }

                byte[] message = store.getMessage(deviceId);
                responses.add(message);
                i += message.length;
            }
        }
        log.info("接收完成，起始时间：{}，当前时间：{}", firstTimeMillis, System.currentTimeMillis());

        if (i < replyNumber) {
            log.info("接收超时");
            timeMeasureIsStop.put(deviceId, true);
            return;
        }
        OperationResultEntity parse = UdpResponseDeal.parse(cmdId, responses, null);
        if (!parse.getIsSuccess()) {
            log.info("时间扫描数据处理失败");
            timeMeasureIsStop.put(deviceId, true);
            return;
        }
        log.info("时间扫描数据处理成功");
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String currentWavelength = device.getCurrentWavelength() == null ? "" : device.getCurrentWavelength();
        Map<String, ChangeParam> zeroResult = device.getZeroResult();
        ChangeParam measureResult = parse.getMeasureResult();

        double wl;
        try {
            wl = Double.parseDouble(currentWavelength);
        } catch (Exception e) {
            log.warn("波长格式非法: {}", currentWavelength);
            return;
        }

        ChangeParam zeroRsd = zeroResult.get(currentWavelength);
        if (zeroRsd == null) {
            log.warn("未找到 zeroRsd, wl={}", currentWavelength);
//            return;
        }

        if (measureResult == null) {
            log.warn("measureRsd 为空");
            return;
        }

        SpectrumPoint point = new SpectrumPoint(wl);
        point.setZeroRsd(zeroRsd);
        point.setMeasureRsd(measureResult);
        point.setTimestamp(System.currentTimeMillis());

        SpectrumCalcResult absResult =
                SpectrumResultCalculator.calculate(1, point, SpectrumSourceType.SINGLE);

        SpectrumCalcResult tResult =
                SpectrumResultCalculator.calculate(2, point, SpectrumSourceType.SINGLE);

        SpectrumCalcResult esResult =
                SpectrumResultCalculator.calculate(3, point, SpectrumSourceType.SINGLE);

        SpectrumCalcResult erResult =
                SpectrumResultCalculator.calculate(4, point, SpectrumSourceType.SINGLE);

        point.setAbsResult(absResult);
        point.setTResult(tResult);
        point.setEsResult(esResult);
        point.setErResult(erResult);
        point.setTimestamp(currentTime);

        log.info("[时间扫描计算结果] wl={}, Abs={}, T%={}, Es={}, Er={},time={},index={}",
                wl,
                absResult != null ? absResult.getAbs() : "-",
                tResult   != null ? tResult.getTPercent() : "-",
                esResult  != null ? esResult.getEs() : "-",
                erResult  != null ? erResult.getEr() : "-",
                point.getTimestamp(),
                scanResultIndex.incrementAndGet()
        );

        unknowCountMessageQueue.putMessage(deviceSerialNumber, cmdId, point);

        log.info("时间扫描单次结束");
        log.info("执行完成，起始时间：{}，当前时间：{}", firstTimeMillis, System.currentTimeMillis());
    }

}
