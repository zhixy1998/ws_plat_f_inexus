/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.enums.measurementEnums.DataIdentityEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTableEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.websocket.guide.GuidePhotometricWebSocket;
import com.persee.workstation.uv_inexus.websocket.measure.PhotometricNewWebSocket;
import com.persee.workstation.uv_inexus.websocket.measure.PhotometricWebsocketParam;
import com.persee.workstation.uv_inexus.websocket.measure.TimeWebNewSocket;
import com.persee.workstation.uv_inexus.websocket.measure.ZeroCalibrationNewWebSocket;
import com.persee.workstation.uv_inexus.websocket.measure.concise.ConciseBaseLineNewWebSocket;
import com.persee.workstation.uv_inexus.websocket.measure.concise.ConciseSpectrumNewWebSocket;
import com.persee.workstation.uv_inexus.websocket.measure.concise.ConciseTimeNewWebSocket;
import com.persee.workstation.uv_inexus.websocket.measure.concise.ConciseZeroCalibrationNewWebSocket;
import com.persee.workstation.uv_inexus.websocket.measure.param.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Controller
@Slf4j
public class LiveDetectionDevicesWebSocket {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private DeviceInfoService infoService;

    @Autowired
    private DevOperationUtilService operationUtilService;

    @Autowired
    WebSocketSendUtil webSocketSendUtil;

    @Autowired
    PhotometricNewWebSocket photometricNewWebSocket;

    @Autowired
    ZeroCalibrationNewWebSocket zeroCalibrationNewWebSocket;

    @Autowired
    ConciseZeroCalibrationNewWebSocket conciseZeroCalibrationNewWebSocket;

    @Autowired
    ConciseBaseLineNewWebSocket conciseBaseLineNewWebSocket;

    @Autowired
    TimeWebNewSocket timeWebNewSocket;

    @Autowired
    ConciseTimeNewWebSocket conciseTimeNewWebSocket;

    @Autowired
    private ConciseSpectrumNewWebSocket conciseSpectrumNewWebSocket;

    @Autowired
    GuidePhotometricWebSocket guidePhotometricWebSocket;

    private static final DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

    // ==================== 配置参数 ====================
//    @Value("${websocket.topic.device:/topic/device}")
//    private String deviceTopic;

    @Value("/topic/device")
    private String deviceTopic;


    @Value("${websocket.measurement.interval:2000}")
    private long measurementInterval;

    @Value("${websocket.status.interval:500}")
    private long statusInterval;

    @Value("${websocket.max.retry.count:3}")
    private int maxRetryCount;

    // ==================== 核心数据结构 ====================
    // 测量任务管理（按设备序列号）
    private final Map<String, MeasureTaskInfo> measureTasks = new ConcurrentHashMap<>();

    // 状态任务管理（按设备序列号）
    private final Map<String, StatusTaskInfo> statusTasks = new ConcurrentHashMap<>();

    // 会话订阅关系（会话ID -> 设备序列号集合）
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    // 设备订阅关系（设备序列号 -> 订阅的会话集合）
    private final Map<String, Set<String>> deviceSessionSubscriptions = new ConcurrentHashMap<>();

    // ==================== 线程池配置 ====================
    private ScheduledExecutorService taskScheduler;
    private ExecutorService measurementExecutor;

    @PostConstruct
    public void init() {
        log.info("初始化WebSocket实时回显控制器...");

        taskScheduler = Executors.newScheduledThreadPool(5);
        measurementExecutor = Executors.newFixedThreadPool(10);

        // 启动健康检查
        taskScheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndRecoverTasks();
            } catch (Exception e) {
                log.error("健康检查异常", e);
            }
        }, 30, 30, TimeUnit.SECONDS);

        log.info("WebSocket实时回显控制器初始化完成");
    }

    // ==================== WebSocket消息处理 ====================


    /**
     * 前端发送操作指令
     */
    @MessageMapping("/device/option")
    public void handleMeasurementOperation(@Payload String payload) {
        try {
            log.info("收到原始消息: {}", payload);

            // 处理双重引号问题
            String jsonString = payload;
            if (payload.startsWith("\"") && payload.endsWith("\"")) {
                // 去掉首尾的引号
                jsonString = payload.substring(1, payload.length() - 1);
                log.info("处理后的JSON字符串: {}", jsonString);
            }

            WebSocketInput input = JSON.parseObject(payload, WebSocketInput.class);

            log.info("接收到实时回显操作指令：{}", input);

            String deviceSerialNumber = input.getDeviceSerialNumber();
            String operation = input.getOperation();


            if (!StringUtils.hasText(deviceSerialNumber) || !StringUtils.hasText(operation)) {
                webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.LiveDetection.getOperation(), "缺少设备序列号或操作类型");
                return;
            }

            WebSocketOperationEnum anEnum = WebSocketOperationEnum.getEnum(operation);
            if (anEnum == null) {
                webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.LiveDetection.getOperation(), "未知操作类型: " + operation);
                return;
            }
            JSONObject data = input.getData();
            switch (anEnum) {
                case LiveDetection:{
                    startMeasurement(deviceSerialNumber);
                    break;
                }
                case DeviceStatus:{
                    deviceStatusStart(deviceSerialNumber);
                    break;
                }
                case ConciseZeroCalibration:{
                    FZeroCalibrationWebSocketParam param = data.toJavaObject(FZeroCalibrationWebSocketParam.class);
                    conciseZeroCalibrationNewWebSocket.measureTask(param);
                    break;
                }
                case ConciseBaseLine:{
                    FBaseLineWebSocketParam param = data.toJavaObject(FBaseLineWebSocketParam.class);
                    conciseBaseLineNewWebSocket.measureTask(param);
                    break;
                }
                case ConcisePhotoMetric:{
                    PhotometricWebsocketParam param = data.toJavaObject(PhotometricWebsocketParam.class);
                    photometricNewWebSocket.measureTask(param);
                    break;
                }
                case ConciseRationMeasure:{
                    Measure(deviceSerialNumber, WebSocketOperationEnum.ConciseRationMeasure.getOperation());
                    break;
                }
                case ConciseSpectrum:{
                    FSpectrumNewWebSocketParam param = data.toJavaObject(FSpectrumNewWebSocketParam.class);
                    conciseSpectrumNewWebSocket.measureTask(param);
                    break;
                }
                case ConciseTimeMeasure:{
                    FTimeWebNewSocketParam param = data.toJavaObject(FTimeWebNewSocketParam.class);
                    conciseTimeNewWebSocket.measureTask(param);
                    break;
                }
                case ZeroCalibration:{
                    ZeroCalibrationWebSocketParam param = data.toJavaObject(ZeroCalibrationWebSocketParam.class);
                    zeroCalibrationNewWebSocket.measureTask(param);
                    break;
                }
                case BaseLine:{
                    Measure(deviceSerialNumber, WebSocketOperationEnum.BaseLine.getOperation());
                    break;
                }
                case PhotoMetric:{
                    JSONObject photometricdata = input.getData();
                    PhotometricWebsocketParam photometricparam = photometricdata.toJavaObject(PhotometricWebsocketParam.class);
                    guidePhotometricWebSocket.measureTask(photometricparam);
//                    Measure(deviceSerialNumber, WebSocketOperationEnum.PhotoMetric.getOperation());
                    break;
                }
                case RationMeasure:{
                    Measure(deviceSerialNumber, WebSocketOperationEnum.RationMeasure.getOperation());
                    break;
                }
                case Spectrum:{
                    Measure(deviceSerialNumber, WebSocketOperationEnum.Spectrum.getOperation());
                    break;
                }
                case TimeMeasure:{
                    TimeWebNewSocketParam param = new TimeWebNewSocketParam();
                    timeWebNewSocket.measureTask(param);
                    break;
                }
                default:{
                    webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.LiveDetection.getOperation(), "未知操作类型: " + operation);
                    return;
            }
            }


        } catch (Exception e) {
            log.error("测量操作请求解析失败", e);
        }
    }

    /**
     * 假测试数据，前端测试用，后续测量调试好之后，相应类型做替换
     */
    private void Measure(String deviceSerialNumber, String operation) {
        JSONObject jsonObject = new JSONObject();
        WebSocketResponseVO vo = new WebSocketResponseVO();
        vo.setOperation(operation);
        for (int i = 0; i < 10; i++) {

            jsonObject.put("sampleId", i);
            jsonObject.put("key", "samplingTime");
            jsonObject.put("value", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            jsonObject.put("currentPoolNum", i<5?i+1:i-4);
            vo.setData(jsonObject);
            webSocketSendUtil.sendMessage(deviceSerialNumber, vo);

            jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + i);
            jsonObject.put("value",  "sample-" + (i + 1));
            vo.setData(jsonObject);
            webSocketSendUtil.sendMessage(deviceSerialNumber, vo);

            jsonObject.put("key", "abs-1");
            jsonObject.put("identity", DataIdentityEnums.avg.getIdentity() + "-" + (1+i));
            jsonObject.put("value", i*i);

            vo.setData(jsonObject);
            webSocketSendUtil.sendMessage(deviceSerialNumber, vo);

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("线程休眠异常", e);
            }

        }
        jsonObject.clear();

        jsonObject.put("success", "测量完成，请更换样品继续测量。");
        jsonObject.put("result", "finish");
        vo.setData(jsonObject);

        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }



    // ==================== 核心业务逻辑（减少方法跳转） ====================

    /**
     * 处理订阅（直接业务逻辑，减少跳转）
     */
    private void handleSubscribe(String deviceSerialNumber) {
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null) {
            webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.LiveDetection.getOperation(), "设备不存在");
            return;
        }

        int subscriptionCount = getDeviceSubscriptionCount(deviceSerialNumber);
        log.info("设备订阅数更新: 设备={}, 当前订阅会话数={}", deviceSerialNumber, subscriptionCount);
        if (subscriptionCount > 0 && info.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS)) {
            deviceStatusStart(deviceSerialNumber);

            if (info.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                startMeasurement(deviceSerialNumber);
            }

        }


        log.info("设备订阅成功: {}", deviceSerialNumber);
    }

    private void deviceStatusStart(String deviceSerialNumber) {
        // 启动状态推送（只需要设备连接）
        StatusTaskInfo statusTask = statusTasks.get(deviceSerialNumber);
        if (statusTask == null) {
            statusTask = new StatusTaskInfo();
            statusTask.deviceSerialNumber = deviceSerialNumber;
            statusTask.isRunning = true;

            // 启动状态推送任务（0.5秒一次）
            StatusTaskInfo finalStatusTask = statusTask;
            statusTask.scheduledFuture = taskScheduler.scheduleAtFixedRate(() -> {
                if (!finalStatusTask.isRunning || !hasSubscribers(deviceSerialNumber)) return;

                try {
                    DeviceInfoPO deviceInfo = infoService.getInfo(deviceSerialNumber);
                    if (deviceInfo == null) return;

                    if (!Constants.CONNECT_DEVICE_SUCCESS.equals(deviceInfo.getConnectStatus())) {
                        log.info("设备已断开，停止状态监控: {}", deviceSerialNumber);
                        finalStatusTask.isRunning = false;
                        if (finalStatusTask.scheduledFuture != null) finalStatusTask.scheduledFuture.cancel(false);
                        statusTasks.remove(deviceSerialNumber);
                        return;
                    }

                    // 推送设备状态

                    DetectionManyDevicesResponseInfoVO statusInfo = new DetectionManyDevicesResponseInfoVO();
                    statusInfo.setDevCurrentStatus(deviceInfo.getDeviceCurrentStatus());

                    String deviceId = deviceInfo.getDeviceId();
                    DeviceConnectInfo device = deviceManager.getDevice(deviceId);
                    if (device != null) {

                        // 校零中推送校零波长
                        if (Constants.DEVICE_STATUS_ZERO.equals(deviceInfo.getDeviceCurrentStatus())
                                && StringUtils.hasText(device.getZeroWavelength())) {
                            statusInfo.setX(device.getZeroWavelength() + "nm");
                        }
                    }

                    WebSocketResponseVO webSocketResponseVO = new WebSocketResponseVO();
                    webSocketResponseVO.setData(statusInfo);
                    webSocketResponseVO.setOperation(WebSocketOperationEnum.DeviceStatus.getOperation());
                    webSocketSendUtil.sendMessage(deviceSerialNumber, webSocketResponseVO);

                } catch (Exception e) {
                    log.error("状态推送异常: {}", deviceSerialNumber, e);
                }
            }, 0, statusInterval, TimeUnit.MILLISECONDS);

            statusTasks.put(deviceSerialNumber, statusTask);
            log.info("创建状态监控任务: {}", deviceSerialNumber);
        } else if (!statusTask.isRunning) {
            statusTask.isRunning = true;
            log.info("恢复状态监控任务: {}", deviceSerialNumber);
        }
    }

    /**
     * 处理取消订阅（直接业务逻辑，减少跳转）
     */
    private void handleUnsubscribe(String deviceSerialNumber) {

        // 获取设备的实际订阅会话数
        int subscriptionCount = getDeviceSubscriptionCount(deviceSerialNumber);
        log.info("设备订阅数更新: 设备={}, 当前订阅会话数={}", deviceSerialNumber, subscriptionCount);

        // 如果没有订阅者了，停止所有任务
        if (subscriptionCount <= 0) {
            stopMeasurement(deviceSerialNumber);
            pauseStatusTask(deviceSerialNumber);
            log.info("设备无订阅者，停止所有任务: {}", deviceSerialNumber);
        }

        log.info("设备取消订阅: {}", deviceSerialNumber);
    }

    /**
     * 获取设备的实际订阅会话数
     */
    private int getDeviceSubscriptionCount(String deviceSerialNumber) {
        Set<String> sessions = deviceSessionSubscriptions.get(deviceSerialNumber);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * 启动实时回显测量（直接业务逻辑，减少跳转）
     */
    private void startMeasurement(String deviceSerialNumber) {
        try {
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info == null) {
                webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.LiveDetection.getOperation(), "设备不存在");
                return;
            }

            if (!Constants.CONNECT_DEVICE_SUCCESS.equals(info.getConnectStatus())) {
                webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.LiveDetection.getOperation(), "设备未连接");
                return;
            }

            if (!Constants.INIT_STATUS_SUCCESS.equals(info.getIsInited())) {
                webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.LiveDetection.getOperation(), "设备未初始化");
                return;
            }

            // 创建或恢复测量任务（减少方法调用链）
            MeasureTaskInfo taskInfo = measureTasks.get(deviceSerialNumber);

            if (taskInfo == null) {
                // 创建新的测量任务
                taskInfo = new MeasureTaskInfo();
                taskInfo.deviceSerialNumber = deviceSerialNumber;
                taskInfo.isRunning = true;
                taskInfo.lastActiveTime = System.currentTimeMillis();
                taskInfo.consecutiveFailures = 0;

                // 直接调度第一次测量（减少方法调用）
                scheduleMeasurementExecution(taskInfo, 0);

                measureTasks.put(deviceSerialNumber, taskInfo);
                log.info("创建测量任务: {}", deviceSerialNumber);

            } else if (!taskInfo.isRunning) {
                // 恢复已存在的任务
                taskInfo.isRunning = true;
                taskInfo.lastActiveTime = System.currentTimeMillis();

                if (taskInfo.scheduledFuture == null || taskInfo.scheduledFuture.isDone()) {
                    scheduleMeasurementExecution(taskInfo, 0);
                }

                log.info("恢复测量任务: {}", deviceSerialNumber);
            } else {
                WebSocketResponseVO webSocketResponseVO = new WebSocketResponseVO();
                webSocketResponseVO.setOperation(WebSocketOperationEnum.LiveDetection.getOperation());
                webSocketResponseVO.setData("测量任务已在运行");
                webSocketSendUtil.sendMessage(deviceSerialNumber, webSocketResponseVO);
                return;
            }
            WebSocketResponseVO webSocketResponseVO = new WebSocketResponseVO();
            webSocketResponseVO.setOperation(WebSocketOperationEnum.LiveDetection.getOperation());
            webSocketResponseVO.setData("实时回显已启动");
            webSocketSendUtil.sendMessage(deviceSerialNumber, webSocketResponseVO);
            log.info("启动实时回显测量: {}", deviceSerialNumber);

        } catch (Exception e) {
            log.error("启动测量失败: {}", deviceSerialNumber, e);
            webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.LiveDetection.getOperation(), "启动测量失败: " + e.getMessage());
        }
    }

    /**
     * 调度测量执行（减少方法跳转，将多个方法合并）
     */
    private void scheduleMeasurementExecution(MeasureTaskInfo taskInfo, long delayMillis) {
        if (!taskInfo.isRunning || !hasSubscribers(taskInfo.deviceSerialNumber)) {
            taskInfo.isRunning = false;
            return;
        }

        taskInfo.scheduledFuture = taskScheduler.schedule(() -> {
            // 直接执行测量循环（减少方法调用链）
            String deviceSerialNumber = taskInfo.deviceSerialNumber;

            if (!taskInfo.isRunning || !hasSubscribers(deviceSerialNumber)) return;

            long startTime = System.currentTimeMillis();

            measurementExecutor.execute(() -> {
                try {
                    // 1. 检查设备状态
                    DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
                    if (info == null) {
                        log.warn("设备信息不存在: {}", deviceSerialNumber);
                        taskInfo.isRunning = false;
                        return;
                    }

                    String deviceId = info.getDeviceId();
                    DeviceConnectInfo device = deviceManager.getDevice(deviceId);

                    if (!Constants.CONNECT_DEVICE_SUCCESS.equals(info.getConnectStatus())) {
                        log.info("设备已断开，暂停测量: {}", deviceSerialNumber);
                        taskInfo.isRunning = false;

                        webSocketSendUtil.sendDeviceDisConnectMessage(deviceSerialNumber);
                        if (device != null) {
                            deviceManager.removeDevice(deviceId);
                        }
                        return;
                    }

                    if (!Constants.INIT_STATUS_SUCCESS.equals(info.getIsInited())) {
                        log.warn("设备未初始化，跳过本次测量: {}", deviceSerialNumber);
                        taskInfo.consecutiveFailures++;
                        return;
                    }

                    // 2. 检查设备是否空闲

                    if (device != null && Boolean.FALSE.equals(device.getIsIdle())) {
                        log.debug("设备正忙，跳过本次测量: {}", deviceSerialNumber);
                        return;
                    }

                    // 3. 执行测量操作
                    DevOperationInputParam inputParam = new DevOperationInputParam();
                    inputParam.setDeviceSerialNumber(deviceSerialNumber);
                    inputParam.setOperationType(OperationTypeEnums.LiveDetection.getType());

                    R<OperationResultEntity> operation = operationUtilService.operation(inputParam);

                    if (!operation.getSuccess()) {
                        log.warn("实时回显测量失败: {}, 错误: {}", deviceSerialNumber, operation.getMessage());
                        taskInfo.consecutiveFailures++;

                        taskInfo.isRunning = false;

                        DeviceInfoPO infoPO = infoService.getInfo(deviceSerialNumber);
                        infoPO.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
                        infoService.saveOrUpdateInfo(infoPO);

                        webSocketSendUtil.sendDeviceDisConnectMessage(deviceSerialNumber);

                        return;

                    } else {
                        // 测量成功
                        taskInfo.consecutiveFailures = 0;

                        // 4. 处理测量结果
                        OperationResultEntity data = operation.getData();
                        DetectionManyDevicesResponseInfoVO vo = new DetectionManyDevicesResponseInfoVO();
                        BeanUtils.copyProperties(data, vo);

                        if (device != null) {
                            BeanUtils.copyProperties(device, vo);
                        }

                        // 5. 推送测量结果
                        WebSocketResponseVO webSocketResponseVO = new WebSocketResponseVO();
                        webSocketResponseVO.setData(vo);
                        webSocketResponseVO.setOperation(WebSocketOperationEnum.LiveDetection.getOperation());
                        webSocketSendUtil.sendMessage(deviceSerialNumber, webSocketResponseVO);
                    }

                    // 6. 更新最后活跃时间
                    taskInfo.lastActiveTime = System.currentTimeMillis();

                } catch (Exception e) {
                    log.error("测量执行异常: {}", deviceSerialNumber, e);
                    taskInfo.consecutiveFailures++;

                    if (taskInfo.consecutiveFailures >= maxRetryCount) {
                        log.error("测量异常次数过多，暂停任务: {}", deviceSerialNumber);
                        taskInfo.isRunning = false;
                    }
                } finally {
                    // 7. 调度下一次测量（测量完成后等待2.5秒）
                    long executionTime = System.currentTimeMillis() - startTime;
                    long waitTime = Math.max(0, measurementInterval);

                    log.info("测量完成，耗时{}ms，等待{}ms后执行下一次", executionTime, waitTime);

                    // 直接调度下一次（减少方法调用）
                    if (taskInfo.isRunning && hasSubscribers(deviceSerialNumber)) {
                        taskScheduler.schedule(() -> {
                            scheduleMeasurementExecution(taskInfo, 0);
                        }, waitTime, TimeUnit.MILLISECONDS);
                    }
                }
            });

        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止测量（直接业务逻辑，减少跳转）
     */
    private void stopMeasurement(String deviceSerialNumber) {
        MeasureTaskInfo taskInfo = measureTasks.get(deviceSerialNumber);
        if (taskInfo != null) {
            taskInfo.isRunning = false;

            if (taskInfo.scheduledFuture != null && !taskInfo.scheduledFuture.isDone()) {
                taskInfo.scheduledFuture.cancel(false);
            }

            measureTasks.remove(deviceSerialNumber);
            log.info("停止测量任务: {}", deviceSerialNumber);
            WebSocketResponseVO webSocketResponseVO = new WebSocketResponseVO();
            webSocketResponseVO.setOperation(WebSocketOperationEnum.LiveDetection.getOperation());
            webSocketResponseVO.setData("测量任务已停止");
            webSocketSendUtil.sendMessage(deviceSerialNumber, webSocketResponseVO);
        } else {
            webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.LiveDetection.getOperation(), "测量任务不存在");
        }
    }

    /**
     * 暂停测量任务
     */
    private void pauseMeasurementTask(String deviceSerialNumber) {
        MeasureTaskInfo taskInfo = measureTasks.get(deviceSerialNumber);
        if (taskInfo != null && taskInfo.isRunning) {
            taskInfo.isRunning = false;

            if (taskInfo.scheduledFuture != null && !taskInfo.scheduledFuture.isDone()) {
                taskInfo.scheduledFuture.cancel(false);
            }

            log.info("暂停测量任务: {}", deviceSerialNumber);
        }
    }

    /**
     * 暂停状态任务
     */
    private void pauseStatusTask(String deviceSerialNumber) {
        StatusTaskInfo taskInfo = statusTasks.get(deviceSerialNumber);
        if (taskInfo != null && taskInfo.isRunning) {
            taskInfo.isRunning = false;
            log.info("暂停状态监控: {}", deviceSerialNumber);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查是否有订阅者
     */
    private boolean hasSubscribers(String deviceSerialNumber) {
        return getDeviceSubscriptionCount(deviceSerialNumber) > 0;
    }

    // ==================== WebSocket事件监听 ====================

    @EventListener
    public void handleStompSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        log.info("WebSocket{}订阅: {}", sessionId, destination);

        if (StringUtils.hasText(destination) && destination.startsWith(deviceTopic)) {
            String[] split = destination.split("/");
            String deviceSerialNumber = split[split.length - 1];

            // 检查此会话是否已经订阅了此设备
            boolean isNewSubscription = addSubscription(sessionId, deviceSerialNumber);

            if (isNewSubscription) {
                log.info("新增订阅: 会话={} -> 设备={}", sessionId, deviceSerialNumber);
                handleSubscribe(deviceSerialNumber);
            } else {
                log.debug("重复订阅忽略: 会话={} -> 设备={}", sessionId, deviceSerialNumber);
            }
        }
    }

    /**
     * 添加订阅关系，返回是否是新订阅
     */
    private synchronized boolean addSubscription(String sessionId, String deviceSerialNumber) {
        // 检查会话是否已订阅此设备
        Set<String> devices = sessionSubscriptions.computeIfAbsent(sessionId,
                k -> ConcurrentHashMap.newKeySet());

        // 如果已经订阅过，返回 false
        if (devices.contains(deviceSerialNumber)) {
            return false;
        }

        // 添加订阅关系
        devices.add(deviceSerialNumber);

        // 更新设备被哪些会话订阅
        Set<String> sessions = deviceSessionSubscriptions.computeIfAbsent(deviceSerialNumber,
                k -> ConcurrentHashMap.newKeySet());
        sessions.add(sessionId);

        return true;
    }

    @EventListener
    public void handleStompUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();


        log.info("WebSocket{}取消订阅消息: {}", sessionId, destination);
        if (StringUtils.hasText(destination) && destination.startsWith(deviceTopic)) {
            String[] split = destination.split("/");
            String deviceSerialNumber = split[split.length - 1];

            boolean removed = removeSubscription(sessionId, deviceSerialNumber);
            if (removed) {
                log.info("移除订阅: 会话={} -> 设备={}", sessionId, deviceSerialNumber);
                handleUnsubscribe(deviceSerialNumber);
            }
        }
    }

    /**
     * 移除订阅关系，返回是否真正移除了订阅
     */
    private synchronized boolean removeSubscription(String sessionId, String deviceSerialNumber) {
        // 从会话的设备集合中移除
        Set<String> devices = sessionSubscriptions.get(sessionId);
        if (devices != null) {
            boolean removed = devices.remove(deviceSerialNumber);
            if (removed) {
                // 清理空集合
                if (devices.isEmpty()) {
                    sessionSubscriptions.remove(sessionId);
                }

                // 从设备的会话集合中移除
                Set<String> sessions = deviceSessionSubscriptions.get(deviceSerialNumber);
                if (sessions != null) {
                    sessions.remove(sessionId);
                    // 如果设备没有订阅者了，清理集合
                    if (sessions.isEmpty()) {
                        deviceSessionSubscriptions.remove(deviceSerialNumber);
                    }
                }

                return true;
            }
        }
        return false;
    }

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.debug("WebSocket连接建立: {}", sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.debug("WebSocket连接断开: {}", sessionId);

        // 清理该会话的所有订阅
        Set<String> deviceSubs = sessionSubscriptions.remove(sessionId);
        if (deviceSubs != null && !deviceSubs.isEmpty()) {
            log.info("清理断开连接的会话订阅: 会话={}, 设备数={}", sessionId, deviceSubs.size());

            // 更新每个设备的订阅关系
            for (String deviceSerialNumber : deviceSubs) {
                Set<String> sessions = deviceSessionSubscriptions.get(deviceSerialNumber);
                if (sessions != null) {
                    sessions.remove(sessionId);
                    if (sessions.isEmpty()) {
                        deviceSessionSubscriptions.remove(deviceSerialNumber);
                        // 设备没有订阅者了，停止所有任务
                        handleUnsubscribe(deviceSerialNumber);
                    }
                }
            }
        }
    }

    // ==================== 健康检查和自动恢复 ====================

    /**
     * 检查并恢复异常任务
     */
    private void checkAndRecoverTasks() {
        long now = System.currentTimeMillis();

        // 检查测量任务
        for (Map.Entry<String, MeasureTaskInfo> entry : measureTasks.entrySet()) {
            String deviceSerialNumber = entry.getKey();
            MeasureTaskInfo taskInfo = entry.getValue();

            // 如果任务在运行但长时间没有活动（超过1分钟）
            if (taskInfo.isRunning && (now - taskInfo.lastActiveTime) > 60000) {
                log.warn("测量任务长时间无活动，尝试恢复: {}", deviceSerialNumber);

                // 检查设备状态
                DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
                if (info != null && Constants.CONNECT_DEVICE_SUCCESS.equals(info.getConnectStatus())
                        && Constants.INIT_STATUS_SUCCESS.equals(info.getIsInited())
                        && hasSubscribers(deviceSerialNumber)) {

                    // 尝试重新调度
                    if (taskInfo.scheduledFuture == null || taskInfo.scheduledFuture.isDone()) {
                        scheduleMeasurementExecution(taskInfo, 0);
                        log.info("测量任务恢复成功: {}", deviceSerialNumber);
                    }
                } else {
                    log.warn("测量任务恢复条件不满足，暂停任务: {}", deviceSerialNumber);
                    taskInfo.isRunning = false;
                }
            }
        }
    }

    // ==================== 资源清理 ====================

    @PreDestroy
    public void shutdown() {
        log.info("开始清理WebSocket控制器资源...");

        // 停止所有测量任务
        measureTasks.forEach((deviceSerialNumber, taskInfo) -> {
            taskInfo.isRunning = false;
            if (taskInfo.scheduledFuture != null && !taskInfo.scheduledFuture.isDone()) {
                taskInfo.scheduledFuture.cancel(false);
            }
            log.info("停止设备测量任务: {}", deviceSerialNumber);
        });
        measureTasks.clear();

        // 停止所有状态监控任务
        statusTasks.forEach((deviceSerialNumber, taskInfo) -> {
            taskInfo.isRunning = false;
            if (taskInfo.scheduledFuture != null && !taskInfo.scheduledFuture.isDone()) {
                taskInfo.scheduledFuture.cancel(false);
            }
            log.info("停止设备状态监控任务: {}", deviceSerialNumber);
        });
        statusTasks.clear();

        // 关闭线程池
        if (taskScheduler != null && !taskScheduler.isShutdown()) {
            try {
                taskScheduler.shutdown();
                if (!taskScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    taskScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                taskScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (measurementExecutor != null && !measurementExecutor.isShutdown()) {
            try {
                measurementExecutor.shutdown();
                if (!measurementExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    measurementExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                measurementExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 清理其他资源
        deviceSessionSubscriptions.clear();
        sessionSubscriptions.clear();


        log.info("WebSocket控制器资源清理完成");
    }

    // ==================== 内部类定义 ====================

    /**
     * 测量任务信息
     */
    @Data
    private static class MeasureTaskInfo {
        private String deviceSerialNumber;
        private ScheduledFuture<?> scheduledFuture;
        private volatile boolean isRunning = false;
        private long lastActiveTime;
        private int consecutiveFailures;
    }

    /**
     * 状态任务信息
     */
    @Data
    private static class StatusTaskInfo {
        private String deviceSerialNumber;
        private ScheduledFuture<?> scheduledFuture;
        private volatile boolean isRunning = false;
    }
}