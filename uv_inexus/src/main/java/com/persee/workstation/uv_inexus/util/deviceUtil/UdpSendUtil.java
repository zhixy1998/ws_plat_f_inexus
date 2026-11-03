/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
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

    @Autowired
    SamplePoolConfigService samplePoolConfigService;

    private final static ConnectUtil connectUtil = ConnectUtil.getInstance();

    private final static DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

    private final static UdpReceiveThread udpReceiveThread = UdpReceiveThread.getInstance();

    //时间扫描线程测量是否结束，true-结束，false-未结束
    public final static ConcurrentMap<String, Boolean> timeMeasureIsStop = new ConcurrentHashMap<>();

    // ================= ARP 心跳保活 =================
    //心跳发送间隔（毫秒），需短于下位机 W5500 的 ARP 缓存过期时间
    private static final long HEARTBEAT_INTERVAL_MILLIS = 4000L;

    //心跳报文内容（1 字节占位包，仅用于保持链路活跃，下位机无需回应；如协议有专用心跳指令可替换）
    private static final byte[] HEARTBEAT_DATA = new byte[]{0x00};

    //每个 deviceId 对应一个心跳任务，防止重复创建
    private final ConcurrentMap<String, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();

    //所有心跳共用一个守护线程调度器
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "udp-arp-heartbeat");
        t.setDaemon(true);
        return t;
    });

    private static UnknowCountMessageQueue unknowCountMessageQueue = UnknowCountMessageQueue.getInstance();

    private final AtomicInteger scanResultIndex = new AtomicInteger(0);

    private static long currentTime;
    @Autowired
    MessageOnTimeDeal messageOnTimeDeal;

    @Autowired
    private SpectrumResultCalculator calculator;

    /**
     * 以太网查找设备
     */
    public R<List<SearchInfo>> searchDevice(String sendIp, String cmdId, byte[] sendBytes, int timeout, int connectType, Integer sendPort) {

        DatagramSocket socket;
        int receivePort;

        try {
            socket = connectUtil.getSocket(connectType, ethernetReceivePort);

            if (socket == null) {
                return R.fail("0020A023", DeviceConnectTypeEnum.getEnum(connectType).getDesc());
            }
            receivePort = connectUtil.getSocketPort(connectType);

            udpReceiveThread.networkReceptionStart(receivePort, connectType);
            MessageStoreManager store = MessageStoreManager.getInstance();

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), sendPort);

            socket = sendWithNicRecover(socket, packet, connectType);

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

                            OperationResultEntity parse =UdpResponseDeal.parse(cmdId, Collections.singletonList(message), sendBytes);
                            searchInfo.setDeviceSerialNumber(parse.getDeviceSerialNumber());

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
            return R.fail("0020A024");
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
                return R.fail("0020A023", DeviceConnectTypeEnum.getEnum(connectType).getDesc());
            }
            receivePort = connectUtil.getSocketPort(connectType);

            DeviceConnectInfo device = deviceManager.getDevice(deviceId);
            String xmlKey = device.getXmlKey();
            DevXmlEnums anEnum = DevXmlEnums.getEnum(xmlKey);

            MessageStoreManager store = MessageStoreManager.getInstance();

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName("255.255.255.255"), anEnum.getPort());
            store.clearDeviceMessages(deviceId);

            String newDeviceId = deviceManager.getOrCreateDeviceId(newIp, receivePort, connectType);

            socket = sendWithNicRecover(socket, packet, connectType);
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

                if (bytes == null) {
                    continue;
                }

                String receiveIp = deviceManager.getIpByDeviceId(deviceId);

                messageGetLog(deviceId, receiveIp, bytes);

                if (isNewIpStartMessage(bytes, deviceId)) {
                    log.info("07回应报文，开始接收");
                    start = !start;

                    if (!start) {
                        log.info("多收07报文！退出");
                        return R.fail("0020A025");
                    }
                    byte[] message = store.getMessage(deviceId);
                    responses.add(message);
                    i += message.length;
                    store.transferQueueMerge(deviceId, newDeviceId);
                    deviceId = newDeviceId;
                    deviceManager.updateDeviceInfo(deviceId, Constants.CONNECT_DEVICE_NO, device.getMacInfo(), anEnum.getDevType());
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
        System.out.println(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) + " 发送指令 [" + cmdId + "] 报文：" + bytesToHex(bytes) + "  到" + ip);
    }

    private void messageGetLog(String deviceId, String ip, byte[] bytes){
        log.info("收到来自deviceId：{}, deviceIp: {} 的数据：{}", deviceId, ip, bytesToHex(bytes));
    }

    /**
     * 判断异常是否为物理连接层面的故障（网卡变动、socket失效、链路断开）。
     * 实时回显等调用方据此返回unCountResponse，走统一的"网线断开/下位机异常"判断逻辑。
     */
    private boolean isNicInvalidError(Exception e) {
        if (isSocketInvalidError(e)) {
            return true;
        }
        String msg = e.getMessage();
        return msg != null && (msg.contains("Datagram send failed")
                || msg.contains("sendto failed")
                || msg.contains("Network is unreachable")
                || msg.contains("No route to host")
                || msg.contains("Host is unreachable"));
    }

    /**
     * 判断异常是否为socket本身失效（可通过重建/复用socket恢复发送）：
     * 1. 网卡变动导致绑定地址失效（BindException / Cannot assign requested address），
     *    如USB转网口拔出重插后Windows生成新网络接口，旧socket绑定的本地地址不再属于任何网卡；
     * 2. socket被其他线程关闭（Socket is closed）——心跳线程等并发触发socket重建时，
     *    正在发送的业务线程仍持有旧socket引用，send时报此错。
     * 注意：拔掉网线（网卡还在）只报Network is unreachable，不属于socket失效，
     * 重建socket无济于事，还会误伤正在使用共享socket的业务线程。
     */
    private boolean isSocketInvalidError(Exception e) {
        if (e instanceof BindException) {
            return true;
        }
        String msg = e.getMessage();
        return msg != null && (msg.contains("Cannot assign requested address")
                || msg.contains("Socket is closed"));
    }

    /**
     * 重建socket：停止旧socket上的接收线程，关闭失效socket，
     * 重新枚举网卡创建新socket（USB网口重插后的新网卡会被重新找到并绑定），并重启接收线程
     * @return 新socket，重建失败返回null
     */
    private DatagramSocket rebuildSocket(Integer connectType) {
        try {
            if (DeviceConnectTypeEnum.WIFI.getConnectType().equals(connectType)) {
                udpReceiveThread.stopWifiReceive();
                connectUtil.closeWifiSocket();
            } else {
                udpReceiveThread.stopEthernetReceive();
                connectUtil.closeEthernetSocket();
            }

            DatagramSocket socket = connectUtil.getSocket(connectType, ethernetReceivePort);
            if (socket == null) {
                return null;
            }

            Integer receivePort = connectUtil.getSocketPort(connectType);
            if (!udpReceiveThread.isRunning(connectType)) {
                udpReceiveThread.networkReceptionStart(receivePort, connectType);
            }
            return socket;
        } catch (Exception e) {
            log.error("重建socket失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 带socket失效恢复的发送：
     * - socket被其他线程关闭（并发重建）时，直接复用ConnectUtil当前socket重试，无需重建；
     * - 网卡变动导致绑定地址失效时，等待网卡就绪后重建socket再重试。
     * 链路断开（Network is unreachable等）不属于socket失效，直接抛出由调用方按无响应处理。
     * @return 实际发送使用的socket（可能为复用/重建后的新socket），重试仍失败抛出原异常
     */
    private DatagramSocket sendWithNicRecover(DatagramSocket socket, DatagramPacket packet, Integer connectType) throws Exception {
        int retry = 0;
        while (true) {
            try {
                socket.send(packet);
                return socket;
            } catch (Exception e) {
                if (!isSocketInvalidError(e) || retry >= 2) {
                    throw e;
                }
                retry++;
                boolean closedByOther = socket.isClosed();
                log.warn("UDP发送失败（{}），{}，第{}次", e.getMessage(),
                        closedByOther ? "socket被并发重建关闭，复用当前socket重发" : "疑似网卡变动，重建socket后重发", retry);
                if (!closedByOther) {
                    // 网卡变动：给系统时间完成网卡注册
                    try {
                        Thread.sleep(500L * retry);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
                socket = recoverSocket(connectType, closedByOther);
                if (socket == null) {
                    throw e;
                }
            }
        }
    }

    /**
     * 恢复socket
     * @param closedByOther true=旧socket已被其他线程（如心跳）关闭并重建完毕，直接取当前socket复用；
     *                      false=绑定地址失效（网卡变动），需停接收线程、关旧socket、重新枚举网卡重建
     * @return 可用的socket，恢复失败返回null
     */
    private DatagramSocket recoverSocket(Integer connectType, boolean closedByOther) {
        try {
            if (closedByOther) {
                DatagramSocket current = connectUtil.getSocket(connectType, ethernetReceivePort);
                if (current == null) {
                    return null;
                }
                if (!udpReceiveThread.isRunning(connectType)) {
                    udpReceiveThread.networkReceptionStart(connectUtil.getSocketPort(connectType), connectType);
                }
                return current;
            }
            return rebuildSocket(connectType);
        } catch (Exception e) {
            log.error("恢复socket失败：{}", e.getMessage());
            return null;
        }
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
            return R.fail("0020A026");
        }

        DatagramSocket socket;
        int receivePort;
        boolean isReceiveRunning;

        try {

            socket = connectUtil.getSocket(connectType, ethernetReceivePort);

            if (socket == null) {
                return R.fail("0020A023", DeviceConnectTypeEnum.getEnum(connectType).getDesc());
            }
            receivePort = connectUtil.getSocketPort(connectType);
            isReceiveRunning = udpReceiveThread.isRunning(connectType);


            if (!isReceiveRunning) {
                udpReceiveThread.networkReceptionStart(receivePort, connectType);
            }

            DeviceConnectInfo device = deviceManager.getDevice(deviceId);
            String sendIp = device.getConnectIdentity();

            MessageStoreManager store = MessageStoreManager.getInstance();

            String xmlKey = device.getXmlKey();
            DevXmlEnums anEnum = DevXmlEnums.getEnum(xmlKey);

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), anEnum.getPort());
            store.clearDeviceMessages(deviceId);

            socket = sendWithNicRecover(socket, packet, connectType);

            sendMessageLog(sendIp, cmdId, sendBytes);
            R<List<byte[]>> r = new R<>();


            if (!Objects.equals(cmdId, "Stop")) {
                for (int i = 0; i < 3; i++) {

                    r = messageDeal(deviceId, timeout, replyNumber, null, headReceiveBytes);
                    if (r.getSuccess()) {
                        break;
                    } else {
                        log.info("报文接收不足！重试第{}次", i+1);
                        store.clearDeviceMessages(deviceId);
                        socket = sendWithNicRecover(socket, packet, connectType);

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
                return R.fail("0020A027");
            }

            if (parse.getCurrentMeasure() != null){
                ChangeParam changeParam = parse.getCurrentMeasure();
                System.out.println("测量值：" + changeParam);
            }

            return R.ok(parse);


        } catch (Exception e) {

            log.info("仪器信息出错：{}", e.getMessage());
            if (isNicInvalidError(e)) {
                // 网卡变动（如USB网口重插）、socket被并发重建关闭且恢复失败、或链路断开（拔网线），
                // 均等同于设备无响应，返回unCountResponse走统一的断线判断逻辑
                return R.fail(DeviceConstants.unCountResponse);
            }
            return R.fail("0020A028");
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
//                return R.fail("0020A028");
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
                return R.fail("0020A023", DeviceConnectTypeEnum.getEnum(connectType).getDesc());
            }
            Integer receivePort = connectUtil.getSocketPort(connectType);
            isReceiveRunning = udpReceiveThread.isRunning(connectType);

            if (!isReceiveRunning) {
                udpReceiveThread.networkReceptionStart(receivePort, connectType);
            }

            String sendIp = device.getConnectIdentity();


            String xmlKey = device.getXmlKey();
            DevXmlEnums anEnum = DevXmlEnums.getEnum(xmlKey);

            DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length,
                    InetAddress.getByName(sendIp), anEnum.getPort());
            store.clearDeviceMessages(sendIp);

            socket = sendWithNicRecover(socket, packet, connectType);

            sendMessageLog(sendIp, cmdId, sendBytes);

            if (isDealOnTime) {
                ScanSessionManager.startScan(deviceId);
                messageOnTimeDeal.parse(cmdId, deviceId, deviceSerialNumber, sendBytes, timeout,headReceiveBytes);
                return R.ok();
            }

            R<List<byte[]>> r = new R<>();
            for (int i = 0; i < 3; i++) {
                r = messageDeal(deviceId, timeout, Integer.MAX_VALUE, endBytes, headReceiveBytes);
                if (r.getSuccess()) {
                    break;
                } else {
                    log.info("报文接收不足！重试第{}次", i+1);
                    store.clearDeviceMessages(sendIp);
                    socket = sendWithNicRecover(socket, packet, connectType);
                    sendMessageLog(sendIp, cmdId, sendBytes);
                }
            }

            if (!r.getSuccess()) {
                return R.fail(DeviceConstants.unCountResponse);
            }
            List<byte[]> responses = r.getData();

            OperationResultEntity parse = UdpResponseDeal.parse(cmdId, responses, sendBytes);
            if (!parse.getIsSuccess()){
                if (parse.getErrorCode() != null) {
                    return R.failCode(parse.getErrorCode(), parse.getErrorArgs());
                }
                return R.fail(parse.getErrorMsg());
            }
            return R.ok(parse);

        } catch (Exception e) {
            log.error("连接仪器出错：{}", e.getMessage());
            if (isNicInvalidError(e)) {
                // 网卡变动、socket失效或链路断开，等同于设备无响应，走统一的断线判断逻辑
                return R.fail(DeviceConstants.unCountResponse);
            }
        }
        return R.ok();

    }

    public R<Object> TimeMeasure(String deviceId, String cmdId, byte[] sendBytes, int replyNumber, int timeout, Integer connectType, Double intervalTime, Double totalTime, String deviceSerialNumber, List<String> headReceiveBytes) {

        if (replyNumber < 1) {
            return R.fail("0020A026");
        }

        try {

            DatagramSocket socket = connectUtil.getSocket(connectType, ethernetReceivePort);

            if (socket == null) {
                return R.fail("0020A023", DeviceConnectTypeEnum.getEnum(connectType).getDesc());
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
                return R.fail("0020A029");
            }

            /* ================= 4. 立即返回 ================= */

            return R.ok("TimeMeasure 启动成功");


        } catch (Exception e) {

            log.info("仪器信息出错：{}", e.getMessage());
            return R.fail("0020A028");
        }
    }

    //测试
    private R<List<byte[]>> messageDeal(String deviceId, Integer timeout, Integer totalByteNumber, byte[] endBytes, List<String> headReceiveBytes) {

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
            int timeoutMs = Constants.HANDSHAKE_TIMEOUT;

            while (i < totalByteNumber && System.currentTimeMillis() - lastReceiveTime < timeoutMs) {
                if (store.getQueueSize(deviceId) == 0) {
                    Thread.sleep(10);
                    continue;
                }

                byte[] bytes = store.peekMessage(deviceId);

                messageGetLog(deviceId, sendIp, bytes);

                if (isStartMessage(bytes, deviceId)) {
                    log.info("07回应报文");
                    start = !start;
                    if (!start) {
                        log.info("多收07报文！退出");
                        return R.fail("0020A025");
                    }
                    timeoutMs = timeout;
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
                    for (byte aByte : bytes) {
                        s.append(0XFF & aByte).append(":");
                    }
                    String substring = s.substring(0, s.length() - 1);
                    boolean isHead = false;
                    for (String receiveByte : headReceiveBytes) {
                        if (substring.startsWith(receiveByte)) {
                            isHead = true;
                            break;
                        }
                    }

                    if (!isHead) {
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
                return R.fail("0020A025");
            }
        } catch (Exception e) {
            log.info("处理消息出错：{}", e.getMessage());
            return R.fail("0020A02A");
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

    private boolean isNewIpStartMessage(byte[] firstBytes, String deviceId) {

        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        DevXmlEnums anEnum = DevXmlEnums.getEnum(xmlKey);
        byte[] newIpStartMessage = anEnum.getNewIpStartMessage();


        // 验证字节数组长度
        if (firstBytes == null || newIpStartMessage == null || firstBytes.length < 12 || newIpStartMessage.length < 12) {
            return false;
        }

        // 比较两个字节数组的前12个字节
        for (int i = 0; i < 12; i++) {
            if (firstBytes[i] != newIpStartMessage[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isStartMessage(byte[] message, String deviceId) {

        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        DevXmlEnums anEnum = DevXmlEnums.getEnum(xmlKey);
        byte[] startMessage = anEnum.getStartMessage();

        // 验证字节数组长度
        if (message == null || startMessage == null || message.length < 12 || startMessage.length < 12) {
            return false;
        }

        // 比较两个字节数组的前12个字节
        for (int i = 0; i < 12; i++) {
            if (message[i] != startMessage[i]) {
                return false;
            }
        }
        return true;
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

        unknowCountMessageQueue.clearCommand(deviceSerialNumber, cmdId);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        long startTime = System.currentTimeMillis();
        long intervalMillis = (long) (intervalTime * 1000);
        long totalMillis = (long) (totalTime * 1000);

        MessageStoreManager store = MessageStoreManager.getInstance();

        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        DevXmlEnums anEnum = DevXmlEnums.getEnum(xmlKey);

        DatagramPacket packet;
        try {
            packet = new DatagramPacket(
                    sendBytes,
                    sendBytes.length,
                    InetAddress.getByName(sendIp),
                    anEnum.getPort()
            );
            timeMeasureIsStop.put(deviceId, false);
        } catch (Exception e) {
            log.error("创建 DatagramPacket 失败", e);
            return scheduler;
        }

        // ====== 修改点 1：引入原子计数器，记录当前是第几次采样 ======
        AtomicInteger scheduleCount = new AtomicInteger(0);

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
                    scheduler.shutdownNow();
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
                    scheduler.shutdownNow();
                    return;
                }

                // ====== 新增防线：防滞后跳点逻辑 ======
                int currentCount = scheduleCount.getAndIncrement();
                long absoluteEndTime = startTime + (long)(currentCount + 1) * intervalMillis;

                // 如果当前时间已经超过了本次应有的绝对截止时间，说明发生了严重滞后
                if (System.currentTimeMillis() > absoluteEndTime) {
                    log.warn("调度发生严重滞后，跳过第 {} 次采样点以对齐时间轴", currentCount + 1);
                    return; // 直接返回，不发包，不接收，等待下一个周期
                }

                // ===== 正常一次采样 =====
                store.clearDeviceMessages(deviceId);
                socket.send(packet);
                sendMessageLog(sendIp, cmdId, packet.getData());

                long firstTimeMillis = System.currentTimeMillis();

                // ====== 修改点 2：计算本次采样的绝对截止时间 ======
                // 基于 startTime 往后推 (count + 1) 个周期，作为绝对对齐的时间线

                // 传入绝对截止时间
                receiveAndDeal(deviceId, firstTimeMillis, absoluteEndTime, replyNumber, cmdId, deviceSerialNumber, headReceiveBytes);

            } catch (Exception e) {
                log.error("时间扫描定时任务异常，立即停止", e);
                unknowCountMessageQueue.putMessage(
                        deviceSerialNumber,
                        cmdId,
                        EndMarker.unNormal(deviceSerialNumber, cmdId)
                );

                infoService.newReleaseDevice(deviceSerialNumber);
                scheduler.shutdownNow();
            }

        }, 0, intervalMillis, TimeUnit.MILLISECONDS);

        return scheduler;
    }

    // ====== 修改点 3：修改 receiveAndDeal 的入参，使用 absoluteEndTime ======
    private void receiveAndDeal(String deviceId, long firstTimeMillis, long absoluteEndTime, Integer replyNumber, String cmdId, String deviceSerialNumber, List<String> headReceiveBytes) {

        log.info("处理时间扫描报文，起始时间：{}，当前时间：{}，绝对截止时间：{}", firstTimeMillis, System.currentTimeMillis(), absoluteEndTime);

        MessageStoreManager store = MessageStoreManager.getInstance();
        List<byte[]> responses = new ArrayList<>();
        boolean start = false;
        int i = 0;

        // ====== 修改点 4：while 循环判断使用绝对截止时间 ======
        while (System.currentTimeMillis() <= absoluteEndTime) {
            byte[] bytes = store.peekMessage(deviceId);
            if (bytes == null || bytes.length == 0) {
                try {
                    Thread.sleep(1); // 让出CPU，大幅减少无效耗时
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }
            log.info("开始接收，起始时间：{}，当前时间：{}", firstTimeMillis, System.currentTimeMillis());
            if (isStartMessage(bytes, deviceId)) {
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

        OperationResultEntity parse = UdpResponseDeal.parse(cmdId, responses, null);
        if (!parse.getIsSuccess()) {
            log.info("时间扫描数据处理失败");
            timeMeasureIsStop.put(deviceId, true);
            return;
        }
        log.info("时间扫描数据处理成功");
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);

        String xmlKey = device.getXmlKey();

        String currentWavelength = device.getCurrentWavelength() == null ? "" : device.getCurrentWavelength();
        ChangeParam measureResult = parse.getMeasureResult();
        ChangeParam zeroRsd = deviceManager.getZeroResult(deviceId, currentWavelength);
        double wl;
        try {
            wl = Double.parseDouble(currentWavelength);
        } catch (Exception e) {
            log.warn("波长格式非法: {}", currentWavelength);
            return;
        }
        if (zeroRsd == null) {
            log.warn("未找到 zeroRsd, wl={}", currentWavelength);
        }

        if (measureResult == null) {
            log.warn("measureRsd 为空");
            return;
        }

        SpectrumPoint point = new SpectrumPoint(wl);
        point.setZeroRsd(zeroRsd);
        point.setMeasureRsd(measureResult);
        point.setTimestamp(System.currentTimeMillis());

        int abs = 1;
        int tpercent = 2;
        int es = 3;
        int er = 4;
        int r = 2;

        if (xmlKey != null && xmlKey.equals(DevXmlEnums.T800.getDevType())) {
            abs = 5;
            tpercent = 6;
            es = 7;
            er = 8;
            r = 6;
        }
        String poolValue = null;

        Integer currentPoolNum = deviceManager.getCurrentPoolNum(deviceId);
        FSamplePoolStatus poolStatus = samplePoolConfigService.getPoolCorrectionData(deviceSerialNumber, currentPoolNum);
        String luminosityMode = deviceManager.getLuminosityMode(deviceId);
        if (poolStatus != null) {

            if (poolStatus.getLuminosity() != null && poolStatus.getLuminosity().equals(luminosityMode)) {
                Map<String, String> poolData = poolStatus.getPoolData();
                poolValue = poolData.get(currentWavelength);
            }
        }

        SpectrumCalcResult absResult =
                calculator.calculate(abs, point, SpectrumSourceType.SINGLE, deviceSerialNumber);

        SpectrumCalcResult tResult =
                calculator.calculate(tpercent, point, SpectrumSourceType.SINGLE, deviceSerialNumber);

        SpectrumCalcResult esResult =
                calculator.calculate(es, point, SpectrumSourceType.SINGLE, deviceSerialNumber);

        SpectrumCalcResult erResult =
                calculator.calculate(er, point, SpectrumSourceType.SINGLE, deviceSerialNumber);
        SpectrumCalcResult rResult =
                calculator.calculate(r, point, SpectrumSourceType.SINGLE, deviceSerialNumber);
        rResult.setRPercent(rResult.getTPercent());

        if (luminosityMode.equals(WaveLengthEnum.abs.getCode())) {
            if (poolValue != null) {
                String s = samplePoolConfigService.applyPoolCorrection(absResult.getAbs(), poolValue, luminosityMode);
                absResult.setAbs(s);
            }
        } else if (luminosityMode.equals(WaveLengthEnum.tPercent.getCode())) {
            if (poolValue != null) {
                String s = samplePoolConfigService.applyPoolCorrection(tResult.getTPercent(), poolValue, luminosityMode);
                tResult.setTPercent(s);
            }
        } else if (luminosityMode.equals(WaveLengthEnum.rPercent.getCode())) {
            if (poolValue != null) {
                String s = samplePoolConfigService.applyPoolCorrection(rResult.getRPercent(), poolValue, luminosityMode);
                rResult.setRPercent(s);
            }
        }
        
        point.setAbsResult(absResult);
        point.setTResult(tResult);
        point.setEsResult(esResult);
        point.setErResult(erResult);
        point.setRResult(rResult);
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

    // ================= ARP 心跳保活：启动/停止 =================

    /**
     * 启动指定设备的心跳线程（幂等，重复调用不会重复创建）
     * 在连接并初始化成功之后调用
     *
     * @param deviceId 设备ID
     */
    public void startHeartbeat(String deviceId) {
        if (deviceId == null) {
            return;
        }
        heartbeatTasks.compute(deviceId, (id, existing) -> {
            // 已存在且未结束的任务，直接复用，不重复创建
            if (existing != null && !existing.isDone()) {
                log.info("心跳线程已存在，跳过创建，deviceId={}", id);
                return existing;
            }
            try {
                DeviceConnectInfo device = deviceManager.getDevice(id);
                if (device == null) {
                    log.warn("启动心跳失败，设备不存在，deviceId={}", id);
                    return null;
                }
                String sendIp = device.getConnectIdentity();
                DevXmlEnums anEnum = DevXmlEnums.getEnum(device.getXmlKey());

                DatagramPacket packet = new DatagramPacket(
                        HEARTBEAT_DATA,
                        HEARTBEAT_DATA.length,
                        InetAddress.getByName(sendIp),
                        anEnum.getPort()
                );

                ScheduledFuture<?> future = heartbeatScheduler.scheduleAtFixedRate(() -> {
                    try {
                        DatagramSocket socket = connectUtil.getSocket(device.getConnectType(), ethernetReceivePort);
                        if (socket == null) {
                            log.warn("心跳发送失败，socket不可用，deviceId={}", id);
                            return;
                        }
                        socket = sendWithNicRecover(socket, packet, device.getConnectType());
                    } catch (Exception e) {
                        //单次发送失败不能让定时任务终止（scheduleAtFixedRate 遇异常会停止），只记录日志
                        log.warn("心跳发送异常，deviceId={}，原因：{}", id, e.getMessage());
                    }
                }, 0, HEARTBEAT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

                log.info("心跳线程启动成功，deviceId={}，目标={}:{}，间隔={}ms",
                        id, sendIp, anEnum.getPort(), HEARTBEAT_INTERVAL_MILLIS);
                return future;
            } catch (Exception e) {
                log.error("启动心跳线程失败，deviceId={}", id, e);
                return null;
            }
        });
    }

    /**
     * 停止指定设备的心跳线程
     * 在移除仪器之后调用
     *
     * @param deviceId 设备ID
     */
    public void stopHeartbeat(String deviceId) {
        if (deviceId == null) {
            return;
        }
        ScheduledFuture<?> future = heartbeatTasks.remove(deviceId);
        if (future != null) {
            future.cancel(false);
            log.info("心跳线程已停止，deviceId={}", deviceId);
        }
    }

    /**
     * 停止所有心跳线程（可选，用于程序退出或全部断开时清理）
     */
    public void stopAllHeartbeat() {
        int count = heartbeatTasks.size();
        heartbeatTasks.forEach((id, future) -> future.cancel(false));
        heartbeatTasks.clear();
        log.info("所有心跳线程已停止，共清理 {} 个", count);
    }

}
