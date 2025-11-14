package com.persee.workstation.uv_inexus.websocket.liveDetection;

/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceConnectMessageEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.*;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.net.DatagramSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@Slf4j
public class LiveDetectionWebSocket {

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Autowired
    DeviceInfoService infoService; // 你现有的 service

    @Autowired
    private MessageUtil messageUtil;

    @Autowired
    ProtocolItemUtil protocolItemUtil;

    @Autowired
    DeviceInfoDAO deviceInfoDAO;

    @Autowired
    FormulaUtil formulaUtil;

    @Autowired
    DevMessageUtil devMessageUtil;

//    @Autowired
//    private ExperimentTestService experimentService;

    @Value("${device.default.T600xmlPath}")
    String T600xmlPath;

    @Value("${device.default.T800xmlPath}")
    String T800xmlPath;

    @Value("/topic/detection")
    String measureTopic;

    @Value("/topic/deviceStatus")
    String deviceStatusTopic;

    private static volatile boolean status = true;

    private static volatile String serialNumber = null;

    // 用于跟踪活跃的订阅者数量
    private final AtomicInteger activeSubscribers = new AtomicInteger(0);
    // 添加会话和订阅映射
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    @MessageMapping("/detection/start")
    public void startMeasure(String deviceSerialNumber) {
        if (deviceSerialNumber == null || deviceSerialNumber.isEmpty()) {
            messagingTemplate.convertAndSend(measureTopic,
                    "缺少 deviceSerialNumber 参数");
            return;
        }
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);

        if (info == null || info.getIp() == null || !info.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS)) {
            messagingTemplate.convertAndSend(measureTopic,
                    "设备未连接: " + deviceSerialNumber);
            return;
        }
        String ip = info.getIp();

        if (info.getIsActivated() == null || info.getIsActivated() == 0) {
            messagingTemplate.convertAndSend(measureTopic,
                    "设备未激活: " + deviceSerialNumber);
            return;
        }

        if (status) {
            // 启动发送接收线程
            serialNumber = deviceSerialNumber;
            this.startMeasureLoop(deviceSerialNumber);
            this.getDeviceStatus(deviceSerialNumber);
            status = false;
        }

        // 主动推送通知给前端
        messagingTemplate.convertAndSend(measureTopic,
                "开始测量: " + deviceSerialNumber + ", ip=" + ip);
    }


    public R<Boolean> startMeasureLoop(String deviceSerialNumber) {
        String deviceIp = infoService.getIp(deviceSerialNumber);
        DeviceInfoPO po = infoService.getInfo(deviceSerialNumber);
        if (po != null && po.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS)) {
            Constants.devConnectInfo.put(deviceSerialNumber, true);
        }
//        R<DatagramSocket> re = messageUtil.getSocket();
//        if (!re.getSuccess()) {
//            log.print("绑定 socket 失败: {}");
//            return R.fail("绑定 socket 失败，请检查网线接口");
//        }
        R<Boolean> r = infoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_ECHO);
        if (!r.getSuccess()) {
            log.info(r.getMessage());
            stopAllMeasurements();
            return R.fail(r.getMessage());
        }
        DetectionConstants.isDeviceIdle = true;

        Thread measureThread = new Thread(() -> {
            try {
                // 获取 socket
                // 循环发送
                int error = 0;

                while (Constants.devConnectInfo.get(deviceSerialNumber) != null && Constants.devConnectInfo.get(deviceSerialNumber)) {

                    while (!DetectionConstants.isDeviceIdle) {
                        Thread.sleep(1000);
                        JSONObject ignore = new JSONObject();
                        ignore.put("ignore", "设备在执行其他操作，等待空闲！");
                        messagingTemplate.convertAndSend(measureTopic, ignore.toJSONString());
                    }
                    int timeout = 0;

                    while (activeSubscribers.get() <= 0 && sessionSubscriptions.isEmpty()){
                         Thread.sleep(1000);
                         timeout++;

                         if (timeout > 20) {
                             log.info("没有订阅者，断开仪器连接！");
                             devMessageUtil.disConnect(deviceIp);
                             stopAllMeasurements();
                             DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
                             info.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
                             infoService.saveOrUpdateInfo(info);
                             return;
                         }
                    }

                    DetectionConstants.isDeviceIdle = false;
                    R<DatagramSocket> news = messageUtil.getSocket();
                    if (!news.getSuccess()) {
                        log.info("绑定 socket 失败: {}");

                        JSONObject errorMsg = new JSONObject();
                        errorMsg.put("error", "请检查网口连接！");
                        messagingTemplate.convertAndSend(measureTopic, errorMsg);
                        throw new RuntimeException("绑定 socket 失败，请检查网线接口");
                    }
                    DatagramSocket socket = news.getData();

                    try {
                        Map<String, Transition> xml = protocolItemUtil.loadFromXml(T600xmlPath);
                        DetectionResponseInfoVO vo = new DetectionResponseInfoVO();

                        DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfo(deviceSerialNumber);
                        vo.setAttachmentName(attachmentInfo.getAttachmentName());
                        vo.setAttachmentKind(attachmentInfo.getAttachmentKind());
                        vo.setTotalPoolNum(attachmentInfo.getSamplePoolNum());

                        int sum = 1;
                        while (sum <= LiveDetectionCmdEnum.getLength()) {
                            if (Constants.devConnectInfo.get(deviceSerialNumber) == null || !Constants.devConnectInfo.get(deviceSerialNumber)) {
                                break;
                            }

                            LiveDetectionCmdEnum anEnum = LiveDetectionCmdEnum.getEnum(sum);

                            String comId = anEnum.getComId();

                            Object[] objects =anEnum.getParam() == null ? new Object[]{} : new Object[] {anEnum.getParam()};

                            Integer count = anEnum.getCount();

                            byte[] sendBytes = protocolItemUtil.calcSendBytes(objects, xml.get(comId));

                            List<byte[]> responses = messageUtil.detectionSendAndReceive(socket, deviceIp, comId, sendBytes, count);
                            if (responses == null || responses.isEmpty()) {
                                log.info("仪器无响应");
                                error ++;
                                throw new RuntimeException("仪器无响应");
                            }
                            Object parseResult = ResponseDeal.parse(comId, responses, sendBytes);
                            if (comId.equals(LiveDetectionCmdEnum.Connect.getComId())) {
                                String ans = parseResult.toString();
                                if (ans.equals("true")) {
                                    vo.setConnectStatus(DeviceConnectMessageEnum.CONNECT_SUCCESS.getName());
                                    continue;
                                } else {
                                    //重连
                                }
                            } else if (comId.equals(LiveDetectionCmdEnum.Measure.getComId())) {
                                ChangeParam measureParam = (ChangeParam) parseResult;
                                if (!Objects.isNull(measureParam)) {
                                    String luminosity = DetectionConstants.luminosity;

                                    Double wl = formulaUtil.calculateWl(Double.parseDouble(measureParam.getWl()), deviceSerialNumber);
                                    vo.setX(wl + "nm");
                                    measureParam.setWl(String.valueOf(wl));

                                    if (luminosity.equals(WaveLengthEnum.abs.getCode())) {
                                        ChangeParam zeroParam = DetectionConstants.zeroResult.get(Double.valueOf(measureParam.getWl()));
                                        String s;
                                        if (!Objects.isNull(zeroParam)) {
                                             s = DataCountUtil.countTPercent(zeroParam, measureParam, 1.0);
                                        } else {
                                            s = DataCountUtil.countTPercent(null, measureParam, 1.0);
                                        }
                                        String abs = DataCountUtil.changeTPercentOrAbs(null, s);
                                        vo.setY(DataCountUtil.formatValueWithBigDecimal(Constants.DEFAULT_DECIMAL_NUM, abs));
                                    } else if(luminosity.equals(WaveLengthEnum.es.getCode()) || luminosity.equals(WaveLengthEnum.er.getCode())) {
                                        String s = DataCountUtil.countErOrEs(measureParam, luminosity);
                                        vo.setY(DataCountUtil.formatValueWithBigDecimal(Constants.DEFAULT_DECIMAL_NUM, s));
                                    } else if (luminosity.equals(WaveLengthEnum.tPercent.getCode())) {
                                        ChangeParam zeroParam = DetectionConstants.zeroResult.get(Double.valueOf(measureParam.getWl()));
                                        String s;
                                        if (!Objects.isNull(zeroParam)) {
                                            s = DataCountUtil.countTPercent(zeroParam, measureParam, 1.0);
                                        } else {
                                            s = DataCountUtil.countTPercent(null, measureParam, 1.0);
                                        }
                                        vo.setY(DataCountUtil.formatValueWithBigDecimal(Constants.DEFAULT_DECIMAL_NUM, s));
                                    }
                                }

                            } else if (comId.equals(LiveDetectionCmdEnum.GetLampPos.getComId())) {
                                List<DetectionLampStatus> dataList = getDetectionLampStatuses((int) parseResult);
                                vo.setDataList(dataList);
                            } else if (comId.equals(LiveDetectionCmdEnum.GetPhotoMode.getComId())) {
                                vo.setLuminosity(WaveLengthEnum.getName(DetectionConstants.luminosity));
                            } else if (comId.equals(LiveDetectionCmdEnum.GetChangeLampWl.getComId())) {
                                Double v = formulaUtil.calculateWl(Double.parseDouble(parseResult.toString()), deviceSerialNumber);
                                vo.setChangeLampWl(String.valueOf(v));
                            }

                            sum ++ ;
                        }

                        vo.setY(vo.getY() + WaveLengthEnum.getName(DetectionConstants.luminosity));
                        if (!StringUtils.hasText(vo.getBandwidth())) {
                            vo.setBandwidth("2.0");
                        }
                        Map<String, Object> map = vo.toMap();
                        ConcurrentHashMap<String, Object> detectionInfo = DetectionConstants.detectionInfo;
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.putAll(map);
                        jsonObject.putAll(detectionInfo);

                        messagingTemplate.convertAndSend(measureTopic, jsonObject);
                        error = 0;
                        DetectionConstants.isDeviceIdle = true;

                        // 休眠2秒
                        Thread.sleep(DetectionConstants.interval);

                    } catch (Exception e) {
                        log.info("监测线程异常: {}", e.getMessage());

                        if (error > 3) {
                            stopAllMeasurements();
                            if (e.getMessage().equals("仪器无响应")) {
                                DeviceInfoPO infoPO = infoService.getInfo(deviceSerialNumber);
                                infoPO.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
                                infoService.saveOrUpdateInfo(infoPO);

                                JSONObject errorMsg = new JSONObject();
                                errorMsg.put("error", "仪器无响应！");
                                messagingTemplate.convertAndSend(measureTopic, errorMsg);

                            }
                        }
                        DetectionConstants.isDeviceIdle = true;
                        try {
                            Thread.sleep(DetectionConstants.interval); // 出错时延时重试
                        } catch (InterruptedException ignored) {
                            DetectionConstants.isDeviceIdle = true;
                        }
                    }
                }
                status = true;
                DetectionConstants.isDeviceIdle = true;

            } catch (Exception e) {
                log.info("监测线程启动出错：{}", e.getMessage());
                JSONObject errorMsg = new JSONObject();
                errorMsg.put("error", "监测线程启动失败！");
                messagingTemplate.convertAndSend(measureTopic, errorMsg);
                DetectionConstants.isDeviceIdle = true;
            }  finally {
                status = true;
                DetectionConstants.isDeviceIdle = true;
            }
        }, "MeasureLoopThread");

        // 添加线程结束监听器
        measureThread.setUncaughtExceptionHandler((thread, exception) -> {
            log.info("测量线程异常终止: {}", exception.getMessage());
            status = true;
            DetectionConstants.isDeviceIdle = true;
        });

        measureThread.start();

        return R.ok();
    }

    public R<Boolean> getDeviceStatus(String deviceSerialNumber) {

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null) {
            log.info("实时监测：未找到该设备");
            return R.fail("请先连接仪器");
        }

        new Thread(() -> {

            int connectStatus = info.getConnectStatus() == null? Constants.CONNECT_DEVICE_NO : info.getConnectStatus();
            while (connectStatus == Constants.CONNECT_DEVICE_SUCCESS && Constants.devConnectInfo.get(deviceSerialNumber) != null
                    && Constants.devConnectInfo.get(deviceSerialNumber)) {
                try {
                    Thread.sleep(1000);

                    DeviceInfoPO infoPO = infoService.getInfo(deviceSerialNumber);
                    if (infoPO != null && infoPO.getDeviceCurrentStatus() != null) {
                        JSONObject status = new JSONObject();
                        status.put("devCurrentStatus", infoPO.getDeviceCurrentStatus());
                        messagingTemplate.convertAndSend(deviceStatusTopic, status);

                    }
                    if (infoPO == null) {
                        connectStatus = Constants.CONNECT_DEVICE_NO;
                    } else {
                        connectStatus = infoPO.getConnectStatus() == null? Constants.CONNECT_DEVICE_NO : info.getConnectStatus();
                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        return R.ok();
    }

    @MessageMapping("/detection/stop")
    public void stopMeasure() {
        // 减少活跃订阅者计数
        activeSubscribers.decrementAndGet();
        Constants.Running = false;
        status = true;
        messagingTemplate.convertAndSend(measureTopic, "停止测量");
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        log.info("WebSocket连接建立: {}", sessionId);


        // 从嵌套消息中提取 deviceSerialNumber
        String deviceSerialNumber = extractDeviceSerialNumber(headerAccessor);

        if (StringUtils.hasText(deviceSerialNumber) && status && Constants.devConnectInfo.containsKey(deviceSerialNumber)) {
            // 启动发送接收线程
            serialNumber = deviceSerialNumber;
            Constants.devConnectInfo.put(deviceSerialNumber, true);
            startMeasureLoop(deviceSerialNumber);
            status = false;
        }

    }

    private String extractDeviceSerialNumber(StompHeaderAccessor headerAccessor) {
        // 获取 simpConnectMessage 对象
        Object simpConnectMessage = headerAccessor.getHeader("simpConnectMessage");

        if (simpConnectMessage instanceof org.springframework.messaging.Message) {
            org.springframework.messaging.Message<?> connectMessage = (org.springframework.messaging.Message<?>) simpConnectMessage;

            // 从 connectMessage 的 nativeHeaders 中获取 deviceSerialNumber
            Object nativeHeadersObj = connectMessage.getHeaders().get("nativeHeaders");

            if (nativeHeadersObj instanceof Map) {
                Map<String, Object> nativeHeaders = (Map<String, Object>) nativeHeadersObj;
                Object deviceSerialNumberObj = nativeHeaders.get("deviceSerialNumber");

                if (deviceSerialNumberObj instanceof List) {
                    List<?> deviceSerialNumbers = (List<?>) deviceSerialNumberObj;
                    if (!deviceSerialNumbers.isEmpty()) {
                        return deviceSerialNumbers.get(0).toString();
                    }
                }
            }
        }

        return "";
    }

    // 监听STOMP订阅事件
    @EventListener
    public void handleStompSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        log.info("WebSocket订阅: {}", sessionId);


        if (measureTopic.equals(destination)) {
            // 使用 Set 避免同一会话重复订阅同一主题
            sessionSubscriptions.computeIfAbsent(sessionId, k -> {
                activeSubscribers.incrementAndGet();
                log.info("会话 {} 首次订阅主题: {}", sessionId, destination);
                return ConcurrentHashMap.newKeySet();
            });

            if (sessionSubscriptions.get(sessionId).add(destination)) {
                log.info("会话 {} 订阅了主题: {}", sessionId, destination);
            }
        }


        if (deviceStatusTopic.equals(destination) && StringUtils.hasText(serialNumber)){
            getDeviceStatus(serialNumber);
        }

    }

    @EventListener
    public void handleStompUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();


        log.info("WebSocket取消订阅: {}", sessionId);


        if (sessionSubscriptions.containsKey(sessionId)) {
            Set<String> subscriptions = sessionSubscriptions.get(sessionId);
            if (subscriptions.remove(measureTopic) && subscriptions.isEmpty()) {
                sessionSubscriptions.remove(sessionId);
                activeSubscribers.decrementAndGet();
                log.info("会话 {} 完全取消订阅", sessionId);
            }
        }

        if (activeSubscribers.get() <= 0 && sessionSubscriptions.isEmpty()) {
            status = true;
            DetectionConstants.isDeviceIdle = true;
        }
    }

    // 监听WebSocket断开事件
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        // 获取客户端IP地址
        log.info("WebSocket连接断开: {}", sessionId);


        // 清理该会话的所有订阅
        Set<String> subscriptions = sessionSubscriptions.remove(sessionId);
        if (subscriptions != null && !subscriptions.isEmpty()) {
            activeSubscribers.decrementAndGet();
            log.info("清理会话 {} 的所有订阅", sessionId);
        }
        if (activeSubscribers.get() <= 0 && sessionSubscriptions.isEmpty()) {
            status = true;
            DetectionConstants.isDeviceIdle = true;
        }

    }

    private void stopAllMeasurements() {

        Constants.devConnectInfo.clear();
        status = true;
        DetectionConstants.isDeviceIdle = true;
    }

    private static List<DetectionLampStatus> getDetectionLampStatuses(int parseResult) {
        List<DetectionLampStatus> dataList = new ArrayList<>();
        DetectionLampStatus status = new DetectionLampStatus();
        status.setLabel("氘灯");
        status.setValue(parseResult == 1 ? "亮" : "不亮");
        dataList.add(status);

        DetectionLampStatus status1 = new DetectionLampStatus();
        status1.setLabel("钨灯");
        status1.setValue(parseResult == 0 ? "亮" : "不亮");
        dataList.add(status1);
        return dataList;
    }

}
