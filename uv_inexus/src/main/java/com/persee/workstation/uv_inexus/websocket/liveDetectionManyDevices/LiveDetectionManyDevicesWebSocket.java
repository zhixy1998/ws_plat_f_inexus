/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices;

import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@Slf4j
public class LiveDetectionManyDevicesWebSocket {

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Autowired
    DeviceInfoService infoService; // 你现有的 service

    @Autowired
    DevOperationUtilService operationUtilService;


    private static final DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();


    @Value("/topic/detection")
    String measureTopic;

    @Value("/topic/deviceStatus")
    String deviceStatusTopic;

    private static volatile Thread measureThread;

    private volatile Map<String, Thread> liveThreads = new ConcurrentHashMap<>();


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

//        String newTopic = measureTopic + "/" + deviceSerialNumber;
        String newTopic = measureTopic;

        Thread thread = liveThreads.get(deviceSerialNumber);
        if (thread != null && thread.isAlive()) {
            messagingTemplate.convertAndSend(measureTopic,
                    "开始测量: " + deviceSerialNumber);
            return;
        } else {

            thread = new Thread(() -> {
                startMeasureLoop(deviceSerialNumber, newTopic);
            });
            liveThreads.put(deviceSerialNumber, thread);
            thread.start();

            // 添加线程结束监听器
            thread.setUncaughtExceptionHandler((Thread, exception) -> {
                log.info("测量线程异常终止: {}", exception.getMessage());
            });
        }
        // 主动推送通知给前端
        messagingTemplate.convertAndSend(measureTopic,
                "开始测量: " + deviceSerialNumber);
    }

    private  R<Boolean> startMeasureLoop(String deviceSerialNumber, String topic) {

            try {

                DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
                String deviceId = info.getDeviceId();

                DeviceConnectInfo device = deviceManager.getDevice(deviceId);
                if (device == null) {
                    return R.fail("设备未连接");
                }

                Integer isInited = info.getIsInited();

                while (device != null && device.getConnectStatus() && isInited.equals(Constants.INIT_STATUS_SUCCESS)) {
                    log.info("循环进入, 仪器状态{}", device.getIsIdle());
                    int timeout = 0;
//                    while (activeSubscribers.get() <= 0 && sessionSubscriptions.isEmpty()) {
//                        Thread.sleep(1000);
//
//                        if (timeout == 0) {
//                            deviceManager.setDeviceIdleStatus(deviceId, true);
//
//                        }
//                        device = deviceManager.getDevice(deviceId);
//                        timeout++;
//                        if (!device.getIsIdle()) {
//                            log.info("仪器被重新占用，等待下次判断");
//                            return R.ok();
//                        }
//                    }

                    while (!device.getIsIdle()) {
                        Thread.sleep(1000);
                        JSONObject ignore = new JSONObject();
                        ignore.put("ignore", "设备在执行其他操作，等待空闲！");
                        messagingTemplate.convertAndSend(topic, ignore.toJSONString());
                    }
                    DevOperationInputParam inputParam = new DevOperationInputParam();
                    inputParam.setDeviceSerialNumber(deviceSerialNumber);
                    inputParam.setOperationType(OperationTypeEnums.LiveDetection.getType());

                    R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
                    if (!operation.getSuccess()) {
                        messagingTemplate.convertAndSend(topic,
                                "实时回显失败：" + deviceSerialNumber);
                        log.info("仪器未回应");

                        DeviceInfoPO infoPO = infoService.getInfo(deviceSerialNumber);
                        infoPO.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
                        infoService.saveOrUpdateInfo(infoPO);

                        JSONObject errorMsg = new JSONObject();
                        errorMsg.put("error", "仪器无响应！");
                        errorMsg.put("disconnect", "仪器无响应，请检查仪器状态和网线连接后，重新查找并联接仪器！");
                        messagingTemplate.convertAndSend(topic, errorMsg);

                        liveThreads.remove(deviceSerialNumber);

                        return R.fail(operation.getMessage());

                    }

                    OperationResultEntity data = operation.getData();
                    DetectionManyDevicesResponseInfoVO vo = new DetectionManyDevicesResponseInfoVO();

                    BeanUtils.copyProperties(data, vo);

                    device = deviceManager.getDevice(deviceId);
                    BeanUtils.copyProperties(device, vo);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.putAll(vo.toMap());

                    messagingTemplate.convertAndSend(topic, jsonObject);

                    // 休眠2秒
                    Thread.sleep(DetectionManyDevicesConstants.interval);

                    device = deviceManager.getDevice(deviceId);
                    info = infoService.getInfo(deviceSerialNumber);
                    isInited = info.getIsInited();
                }

            } catch (Exception e) {
                log.info("监测线程启动出错：{}", e.getMessage());
                JSONObject errorMsg = new JSONObject();
                errorMsg.put("error", "监测线程启动失败！");
                messagingTemplate.convertAndSend(topic, errorMsg);

            }

        return R.ok();
    }

    private volatile Thread statusThread;

    public R<Boolean> getDeviceStatus(String deviceSerialNumber) {
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null) {
            log.info("实时监测：未找到该设备");
            return R.fail("请先连接仪器");
        }
        if (info.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS)) {
            Constants.devConnectInfo.put(deviceSerialNumber, true);
        }

        // 只有当线程尚未启动或者已经终止时才重新启动
        if (statusThread == null || !statusThread.isAlive()) {
            synchronized (this) {
                if (statusThread == null || !statusThread.isAlive()) {
                    statusThread = new Thread(() -> {
                        int connectStatus = info.getConnectStatus() == null ?
                                Constants.CONNECT_DEVICE_NO : info.getConnectStatus();
                        while (connectStatus == Constants.CONNECT_DEVICE_SUCCESS &&
                                Constants.devConnectInfo.get(deviceSerialNumber) != null &&
                                Constants.devConnectInfo.get(deviceSerialNumber)) {
                            try {
                                Thread.sleep(500);
                                int timeout = 0;
//                                while (activeSubscribers.get() <= 0 && sessionSubscriptions.isEmpty()){
//
//                                    if (timeout == 0) {
//                                        DetectionConstants.isDeviceIdle = true;
//                                    }
//                                    Thread.sleep(1000);
//                                    timeout++;
//                                    if (!DetectionConstants.isDeviceIdle) {
//                                        log.info("仪器被重新占用，等待下次判断");
//                                        return;
//                                    }
//                                }


                                DeviceInfoPO infoPO = infoService.getInfo(deviceSerialNumber);
                                if (infoPO != null && infoPO.getDeviceCurrentStatus() != null) {
                                    JSONObject status = new JSONObject();
                                    status.put("devCurrentStatus", infoPO.getDeviceCurrentStatus());
                                    log.info("仪器状态：{}", status);
                                    messagingTemplate.convertAndSend(deviceStatusTopic, status);

                                    if (infoPO.getDeviceCurrentStatus().equals(Constants.DEVICE_STATUS_ZERO) && StringUtils.hasText(DetectionManyDevicesConstants.zeroWavelength)) {
                                        log.info("校零中，回显当前波长");

                                        DetectionManyDevicesResponseInfoVO vo = new DetectionManyDevicesResponseInfoVO();
                                        vo.setX(DetectionManyDevicesConstants.zeroWavelength + "nm");

                                        Map<String, Object> map = vo.toMap();
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.putAll(map);

                                        messagingTemplate.convertAndSend(measureTopic, jsonObject);
                                    }
                                }

                                if (infoPO == null) {
                                    connectStatus = Constants.CONNECT_DEVICE_NO;
                                } else {
                                    connectStatus = infoPO.getConnectStatus() == null ?
                                            Constants.CONNECT_DEVICE_NO : infoPO.getConnectStatus();
                                }

                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt(); // 恢复中断状态
                                break;
                            }
                        }
                    }, "StatusThread");

                    statusThread.start();
                }
            }
        }

        return R.ok();
    }

//    @PreDestroy
//    public void end(){
//        log.info("关闭前执行");
//        DeviceInfoPO info = infoService.getInfo(serialNumber);
//        if (info != null && info.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS)) {
//            String ip = info.getIp();
//            devMessageUtil.disConnect(ip);
//            info.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
//            infoService.saveOrUpdateInfo(info);
//        }
//    }

    @MessageMapping("/detection/stop")
    public void stopMeasure(String deviceSerialNumber) {
        // 减少活跃订阅者计数


    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        log.info("WebSocket连接建立: {}", sessionId);


        // 从嵌套消息中提取 deviceSerialNumber
        String deviceSerialNumber = extractDeviceSerialNumber(headerAccessor);
        if (StringUtils.hasText(deviceSerialNumber)) {
            Thread thread = liveThreads.get(deviceSerialNumber);
            if (thread != null && thread.isAlive()) {
                log.info("设备 {} 正在运行，请勿重复启动", deviceSerialNumber);
                return;
            }

//            String topic = measureTopic + "/" + deviceSerialNumber;
            String topic = measureTopic;

            Thread liveThread = new Thread(() -> {
                startMeasureLoop(deviceSerialNumber, topic);
            });
            liveThreads.put(deviceSerialNumber, liveThread);
            getDeviceStatus(deviceSerialNumber);
            liveThread.start();
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

        log.info("WebSocket{}订阅: {}", sessionId, destination);

        if (measureTopic.equals(destination) || deviceStatusTopic.equals(destination)) {
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
    }

}
