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
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@Slf4j
public class LiveDetectionManyDevicesWebSocket {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private DeviceInfoService infoService;

    @Autowired
    private DevOperationUtilService operationUtilService;

    private static final DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

    // ==================== 配置参数 ====================
    @Value("/topic/detection")
    private String measureTopic;

    @Value("/topic/deviceStatus")
    private String deviceStatusTopic;

    @Value("/topic/deviceControl")
    private String deviceControlTopic;

    @Value("2500")
    private long measurementInterval;

    @Value("500")
    private long statusInterval;

    @Value("3")
    private int maxRetryCount;

    // ==================== 核心数据结构 ====================
    // 实时回显任务管理（按设备序列号）
    private final Map<String, MeasureTaskInfo> measureTasks = new ConcurrentHashMap<>();

    // 状态推送任务管理（按设备序列号）
    private final Map<String, StatusTaskInfo> statusTasks = new ConcurrentHashMap<>();

    // 设备订阅者计数（按设备序列号和主题）
    private final Map<String, Map<String, AtomicInteger>> deviceSubscriptions = new ConcurrentHashMap<>();

    // WebSocket会话订阅关系
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    // 一次性任务标记（用于防止重复执行）
    private final Map<String, String> oneTimeTaskFlags = new ConcurrentHashMap<>();

    // ==================== 线程池配置 ====================
    private ScheduledExecutorService taskScheduler;
    private ExecutorService measurementExecutor;

    @PostConstruct
    public void init() {
        log.info("初始化WebSocket控制器线程池...");

        // 任务调度线程池
        taskScheduler = Executors.newScheduledThreadPool(10);

        // 测量执行线程池
        measurementExecutor = Executors.newFixedThreadPool(20);

        // 启动健康检查
        initHealthCheck();

        log.info("WebSocket控制器初始化完成");
    }

    // ==================== WebSocket消息处理 ====================

    /**
     * 前端订阅测量主题
     */
//    @MessageMapping("/detection/subscribe")
    @MessageMapping("/detection/start")
    public void subscribeMeasurement(String deviceSerialNumber) {
//        String deviceSerialNumber = request.getDeviceSerialNumber();
        String sessionId = measureTopic;

        if (StringUtils.isEmpty(deviceSerialNumber)) {
            sendErrorResponse(sessionId, "缺少设备序列号");
            return;
        }

        try {
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info == null) {
                sendErrorResponse(sessionId, "设备不存在: " + deviceSerialNumber);
                return;
            }

            if (!Constants.CONNECT_DEVICE_SUCCESS.equals(info.getConnectStatus())) {
                sendErrorResponse(sessionId, "设备未连接: " + deviceSerialNumber);
                return;
            }

            if (!Constants.INIT_STATUS_SUCCESS.equals(info.getIsInited())) {
                sendErrorResponse(sessionId, "设备未初始化: " + deviceSerialNumber);
                return;
            }

            // 更新订阅关系
            updateSubscription(deviceSerialNumber, getDeviceTopic(deviceSerialNumber),
                    sessionId, true);

            // 启动测量任务
            startOrResumeMeasurement(deviceSerialNumber);

            sendSuccessResponse(sessionId, "已订阅设备测量数据");

        } catch (Exception e) {
            log.error("订阅测量失败: {}", deviceSerialNumber, e);
            sendErrorResponse(sessionId, "订阅失败: " + e.getMessage());
        }
    }

    /**
     * 前端取消订阅测量主题
     */
    @MessageMapping("/detection/unsubscribe")
    public void unsubscribeMeasurement(@Payload SubscribeRequest request) {
        String deviceSerialNumber = request.getDeviceSerialNumber();
        String sessionId = request.getSessionId();

        if (StringUtils.isEmpty(deviceSerialNumber)) {
            return;
        }

        try {
            // 更新订阅关系
            updateSubscription(deviceSerialNumber, getDeviceTopic(deviceSerialNumber),
                    sessionId, false);

            // 如果没有订阅者，暂停测量
            if (!hasSubscribers(deviceSerialNumber, getDeviceTopic(deviceSerialNumber))) {
                pauseMeasurement(deviceSerialNumber);
            }

            sendSuccessResponse(sessionId, "已取消订阅设备测量数据");

        } catch (Exception e) {
            log.error("取消订阅测量失败: {}", deviceSerialNumber, e);
            sendErrorResponse(sessionId, "取消订阅失败: " + e.getMessage());
        }
    }

    /**
     * 前端订阅设备状态
     */
    @MessageMapping("/device/status/subscribe")
    public void subscribeDeviceStatus(@Payload SubscribeRequest request) {
        String deviceSerialNumber = request.getDeviceSerialNumber();
        String sessionId = request.getSessionId();

        if (StringUtils.isEmpty(deviceSerialNumber)) {
            sendErrorResponse(sessionId, "缺少设备序列号");
            return;
        }

        try {
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info == null) {
                sendErrorResponse(sessionId, "设备不存在: " + deviceSerialNumber);
                return;
            }

            if (!Constants.CONNECT_DEVICE_SUCCESS.equals(info.getConnectStatus())) {
                sendErrorResponse(sessionId, "设备未连接: " + deviceSerialNumber);
                return;
            }

            // 更新订阅关系
            updateSubscription(deviceSerialNumber, getDeviceStatusTopic(deviceSerialNumber),
                    sessionId, true);

            // 启动状态监控任务
            startOrResumeStatusMonitoring(deviceSerialNumber);

            sendSuccessResponse(sessionId, "已订阅设备状态");

        } catch (Exception e) {
            log.error("订阅设备状态失败: {}", deviceSerialNumber, e);
            sendErrorResponse(sessionId, "订阅失败: " + e.getMessage());
        }
    }

    /**
     * 前端订阅设备控制主题（用于接收控制结果）
     */
    @MessageMapping("/device/control/subscribe")
    public void subscribeDeviceControl(@Payload SubscribeRequest request) {
        String deviceSerialNumber = request.getDeviceSerialNumber();
        String sessionId = request.getSessionId();

        if (StringUtils.isEmpty(deviceSerialNumber)) {
            sendErrorResponse(sessionId, "缺少设备序列号");
            return;
        }

        try {
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info == null) {
                sendErrorResponse(sessionId, "设备不存在: " + deviceSerialNumber);
                return;
            }

            // 更新订阅关系
            updateSubscription(deviceSerialNumber, getDeviceControlTopic(deviceSerialNumber),
                    sessionId, true);

            sendSuccessResponse(sessionId, "已订阅设备控制反馈");

        } catch (Exception e) {
            log.error("订阅设备控制失败: {}", deviceSerialNumber, e);
            sendErrorResponse(sessionId, "订阅失败: " + e.getMessage());
        }
    }

    /**
     * 前端发送设备控制命令
     */
    @MessageMapping("/device/control/command")
    public void handleControlCommand(@Payload ControlCommandRequest request) {
        String deviceSerialNumber = request.getDeviceSerialNumber();
        String command = request.getCommand();
        String sessionId = request.getSessionId();

        if (StringUtils.isEmpty(deviceSerialNumber) || StringUtils.isEmpty(command)) {
            sendErrorResponse(sessionId, "缺少设备序列号或命令");
            return;
        }

        try {
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info == null) {
                sendErrorResponse(sessionId, "设备不存在: " + deviceSerialNumber);
                return;
            }

            if (!Constants.CONNECT_DEVICE_SUCCESS.equals(info.getConnectStatus())) {
                sendErrorResponse(sessionId, "设备未连接: " + deviceSerialNumber);
                return;
            }

            // 执行不同的控制命令
            executeControlCommand(deviceSerialNumber, command, sessionId);

        } catch (Exception e) {
            log.error("处理控制命令失败: {}, {}", deviceSerialNumber, command, e);
            sendErrorResponse(sessionId, "命令执行异常: " + e.getMessage());
        }
    }

    /**
     * 执行控制命令
     */
    private void executeControlCommand(String deviceSerialNumber, String command, String sessionId) {
        String taskKey = deviceSerialNumber + ":" + command;

        // 检查是否已经在执行相同命令
        if (oneTimeTaskFlags.containsKey(taskKey)) {
            log.warn("任务已在执行中: {}", taskKey);
            sendControlResult(deviceSerialNumber, "warning", "任务已在执行中，请勿重复操作");
            sendErrorResponse(sessionId, "任务已在执行中，请勿重复操作");
            return;
        }

        try {
            // 标记任务开始
            oneTimeTaskFlags.put(taskKey, "running");

            switch (command) {
                case "startZeroing":
                    startZeroingOperation(deviceSerialNumber, sessionId);
                    break;

                case "stopZeroing":
                    stopZeroingOperation(deviceSerialNumber, sessionId);
                    break;

                case "saveResult":
                    saveMeasurementResult(deviceSerialNumber, sessionId);
                    break;

                case "forceMeasurement":
                    executeForceMeasurement(deviceSerialNumber, sessionId);
                    break;

                case "startMeasurement":
                    startOneTimeMeasurement(deviceSerialNumber, sessionId);
                    break;

                case "stopMeasurement":
                    stopMeasurement(deviceSerialNumber, sessionId);
                    break;

                default:
                    log.warn("未知的控制命令: {}", command);
                    sendControlResult(deviceSerialNumber, "error", "未知命令: " + command);
                    sendErrorResponse(sessionId, "未知命令: " + command);
                    break;
            }
        } finally {
            // 延迟清除任务标记
            taskScheduler.schedule(() -> {
                oneTimeTaskFlags.remove(taskKey);
            }, 3, TimeUnit.SECONDS);
        }
    }

    // ==================== 测量任务管理 ====================

    /**
     * 启动或恢复测量任务
     */
    private synchronized void startOrResumeMeasurement(String deviceSerialNumber) {
        MeasureTaskInfo taskInfo = measureTasks.get(deviceSerialNumber);

        if (taskInfo == null) {
            // 创建新的测量任务
            taskInfo = new MeasureTaskInfo();
            taskInfo.deviceSerialNumber = deviceSerialNumber;
            taskInfo.isRunning = true;
            taskInfo.isPaused = false;
            taskInfo.lastActiveTime = System.currentTimeMillis();
            taskInfo.consecutiveFailures = 0;

            // 立即启动第一次测量
            scheduleNextMeasurement(taskInfo, 0);

            measureTasks.put(deviceSerialNumber, taskInfo);
            log.info("启动设备测量任务: {}", deviceSerialNumber);

        } else if (taskInfo.isPaused) {
            // 恢复暂停的任务
            taskInfo.isPaused = false;
            taskInfo.isRunning = true;

            // 如果当前没有待执行的任务，重新调度
            if (taskInfo.currentFuture == null || taskInfo.currentFuture.isDone()) {
                scheduleNextMeasurement(taskInfo, 0);
            }

            log.info("恢复设备测量任务: {}", deviceSerialNumber);

        } else if (taskInfo.currentFuture != null && taskInfo.currentFuture.isDone()) {
            // 任务已结束，重新调度
            scheduleNextMeasurement(taskInfo, 0);
        }
    }

    /**
     * 调度下一次测量
     */
    private void scheduleNextMeasurement(MeasureTaskInfo taskInfo, long delayMillis) {
        if (!taskInfo.isRunning || taskInfo.isPaused) {
            return;
        }

        // 检查是否有订阅者
        if (!hasSubscribers(taskInfo.deviceSerialNumber, getDeviceTopic(taskInfo.deviceSerialNumber))) {
            taskInfo.isPaused = true;
            return;
        }

        taskInfo.currentFuture = taskScheduler.schedule(
                () -> executeMeasurementCycle(taskInfo),
                delayMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 执行测量周期
     */
    private void executeMeasurementCycle(MeasureTaskInfo taskInfo) {
        String deviceSerialNumber = taskInfo.deviceSerialNumber;

        if (!taskInfo.isRunning || taskInfo.isPaused) {
            return;
        }

        long startTime = System.currentTimeMillis();

        measurementExecutor.execute(() -> {
            try {
                DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
                if (info == null) {
                    log.warn("设备信息不存在: {}", deviceSerialNumber);
                    pauseMeasurement(deviceSerialNumber);
                    return;
                }

                // 检查设备连接状态
                if (!Constants.CONNECT_DEVICE_SUCCESS.equals(info.getConnectStatus())) {
                    log.info("设备已断开，暂停测量: {}", deviceSerialNumber);
                    pauseMeasurement(deviceSerialNumber);
                    return;
                }

                // 检查设备是否初始化
                if (!Constants.INIT_STATUS_SUCCESS.equals(info.getIsInited())) {
                    log.warn("设备未初始化: {}", deviceSerialNumber);
                    return;
                }

                String deviceId = info.getDeviceId();

                // 检查设备是否空闲
                DeviceConnectInfo device = deviceManager.getDevice(deviceId);
                if (device != null && Boolean.FALSE.equals(device.getIsIdle())) {
                    log.debug("设备正忙，跳过本次测量: {}", deviceSerialNumber);
                    return;
                }

                // 执行测量操作
                DevOperationInputParam inputParam = new DevOperationInputParam();
                inputParam.setDeviceSerialNumber(deviceSerialNumber);
                inputParam.setOperationType(OperationTypeEnums.LiveDetection.getType());

                R<OperationResultEntity> operation = operationUtilService.operation(inputParam);

                if (!operation.getSuccess()) {
                    log.warn("设备测量操作失败: {}, 错误: {}", deviceSerialNumber, operation.getMessage());
                    taskInfo.consecutiveFailures++;

                    // 连续失败过多，暂停任务
                    if (taskInfo.consecutiveFailures >= maxRetryCount) {
                        log.error("测量连续失败次数过多，暂停任务: {}", deviceSerialNumber);
                        pauseMeasurement(deviceSerialNumber);
                        pushErrorToFrontend(deviceSerialNumber, "MEASUREMENT_ERROR",
                                "测量连续失败，已暂停任务");
                        return;
                    }
                } else {
                    // 测量成功
                    taskInfo.consecutiveFailures = 0;

                    // 处理测量结果
                    OperationResultEntity data = operation.getData();
                    DetectionManyDevicesResponseInfoVO vo = new DetectionManyDevicesResponseInfoVO();
                    BeanUtils.copyProperties(data, vo);

                    if (device != null) {
                        BeanUtils.copyProperties(device, vo);
                    }

                    // 推送测量结果到前端
                    pushMeasurementResult(deviceSerialNumber, vo);
                }

                // 更新最后活跃时间
                taskInfo.lastActiveTime = System.currentTimeMillis();

            } catch (Exception e) {
                log.error("测量执行异常: {}", deviceSerialNumber, e);
                taskInfo.consecutiveFailures++;

                if (taskInfo.consecutiveFailures >= maxRetryCount) {
                    log.error("测量异常次数过多，暂停任务: {}", deviceSerialNumber);
                    pauseMeasurement(deviceSerialNumber);
                }
            } finally {
                // 计算下一次执行延迟
                long executionTime = System.currentTimeMillis() - startTime;
                long actualDelay = Math.max(0, measurementInterval - executionTime);

                // 调度下一次测量
                if (taskInfo.isRunning && !taskInfo.isPaused) {
                    scheduleNextMeasurement(taskInfo, actualDelay);
                }
            }
        });
    }

    /**
     * 暂停测量任务
     */
    private synchronized void pauseMeasurement(String deviceSerialNumber) {
        MeasureTaskInfo taskInfo = measureTasks.get(deviceSerialNumber);
        if (taskInfo != null) {
            taskInfo.isPaused = true;

            // 取消待执行的任务
            if (taskInfo.currentFuture != null && !taskInfo.currentFuture.isDone()) {
                taskInfo.currentFuture.cancel(false);
            }

            log.info("暂停设备测量任务: {}", deviceSerialNumber);
        }
    }

    /**
     * 停止测量任务
     */
    private synchronized void stopMeasurement(String deviceSerialNumber, String sessionId) {
        MeasureTaskInfo taskInfo = measureTasks.get(deviceSerialNumber);
        if (taskInfo != null) {
            taskInfo.isRunning = false;
            taskInfo.isPaused = true;

            // 取消待执行的任务
            if (taskInfo.currentFuture != null && !taskInfo.currentFuture.isDone()) {
                taskInfo.currentFuture.cancel(false);
            }

            measureTasks.remove(deviceSerialNumber);

            log.info("停止设备测量任务: {}", deviceSerialNumber);
            sendControlResult(deviceSerialNumber, "success", "测量任务已停止");
            sendSuccessResponse(sessionId, "测量任务已停止");
            return;
        }

        sendControlResult(deviceSerialNumber, "warning", "测量任务不存在");
        sendSuccessResponse(sessionId, "测量任务不存在");
    }

    // ==================== 状态任务管理 ====================

    /**
     * 启动或恢复状态推送任务
     */
    private synchronized void startOrResumeStatusMonitoring(String deviceSerialNumber) {
        StatusTaskInfo taskInfo = statusTasks.get(deviceSerialNumber);

        if (taskInfo == null) {
            // 创建新的状态任务
            taskInfo = new StatusTaskInfo();
            taskInfo.deviceSerialNumber = deviceSerialNumber;
            taskInfo.isRunning = true;
            taskInfo.isPaused = false;

            // 使用固定速率调度
            StatusTaskInfo finalTaskInfo = taskInfo;
            taskInfo.scheduledFuture = taskScheduler.scheduleAtFixedRate(
                    () -> executeStatusPush(finalTaskInfo),
                    0,
                    statusInterval,
                    TimeUnit.MILLISECONDS
            );

            statusTasks.put(deviceSerialNumber, taskInfo);
            log.info("启动设备状态监控任务: {}", deviceSerialNumber);

        } else if (taskInfo.isPaused) {
            // 恢复暂停的任务
            taskInfo.isPaused = false;
            log.info("恢复设备状态监控任务: {}", deviceSerialNumber);
        }
    }

    /**
     * 执行单次状态推送
     */
    private void executeStatusPush(StatusTaskInfo taskInfo) {
        String deviceSerialNumber = taskInfo.deviceSerialNumber;

        // 检查任务是否暂停
        if (taskInfo.isPaused || !taskInfo.isRunning) {
            return;
        }

        // 检查是否有订阅者
        if (!hasSubscribers(deviceSerialNumber, getDeviceStatusTopic(deviceSerialNumber))) {
            taskInfo.isPaused = true;
            return;
        }

        try {
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info == null) {
                log.warn("设备信息不存在: {}", deviceSerialNumber);
                return;
            }

            // 检查设备是否已断开
            if (!Constants.CONNECT_DEVICE_SUCCESS.equals(info.getConnectStatus())) {
                log.info("设备已断开，停止状态监控: {}", deviceSerialNumber);
                stopStatusMonitoring(deviceSerialNumber);
                return;
            }

            // 推送设备状态
            pushDeviceStatus(deviceSerialNumber, info);

            // 特殊处理：校零中推送校零波长
            if (Constants.DEVICE_STATUS_ZERO.equals(info.getDeviceCurrentStatus())
                    && StringUtils.hasText(DetectionManyDevicesConstants.zeroWavelength)) {
                pushZeroingWavelength(deviceSerialNumber);
            }

        } catch (Exception e) {
            log.error("状态推送异常: {}", deviceSerialNumber, e);
        }
    }

    /**
     * 停止状态监控
     */
    private synchronized void stopStatusMonitoring(String deviceSerialNumber) {
        StatusTaskInfo taskInfo = statusTasks.get(deviceSerialNumber);
        if (taskInfo != null) {
            taskInfo.isRunning = false;
            if (taskInfo.scheduledFuture != null && !taskInfo.scheduledFuture.isDone()) {
                taskInfo.scheduledFuture.cancel(false);
            }
            statusTasks.remove(deviceSerialNumber);
            log.info("停止设备状态监控: {}", deviceSerialNumber);
        }
    }

    // ==================== 设备操作方法 ====================

    /**
     * 启动校零操作
     */
    private void startZeroingOperation(String deviceSerialNumber, String sessionId) {
        try {
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info == null) {
                sendControlResult(deviceSerialNumber, "error", "设备信息不存在");
                sendErrorResponse(sessionId, "设备信息不存在");
                return;
            }

            String deviceId = info.getDeviceId();

            // 执行校零操作
            DevOperationInputParam inputParam = new DevOperationInputParam();
            inputParam.setDeviceSerialNumber(deviceSerialNumber);
//            inputParam.setOperationType(OperationTypeEnums.Zeroing.getType());

            R<OperationResultEntity> operation = operationUtilService.operation(inputParam);

            if (!operation.getSuccess()) {
                log.error("启动校零失败: {}, 错误: {}", deviceSerialNumber, operation.getMessage());
                sendControlResult(deviceSerialNumber, "error", "启动校零失败: " + operation.getMessage());
                sendErrorResponse(sessionId, "启动校零失败: " + operation.getMessage());
                return;
            }

            // 设置设备为非空闲状态
            deviceManager.setDeviceIdleStatus(deviceId, false);

            log.info("设备开始校零: {}", deviceSerialNumber);
            sendControlResult(deviceSerialNumber, "success", "校零操作已启动");
            sendSuccessResponse(sessionId, "校零操作已启动");

        } catch (Exception e) {
            log.error("启动校零异常: {}", deviceSerialNumber, e);
            sendControlResult(deviceSerialNumber, "error", "启动校零异常: " + e.getMessage());
            sendErrorResponse(sessionId, "启动校零异常: " + e.getMessage());
        }
    }

    /**
     * 停止校零操作
     */
    private void stopZeroingOperation(String deviceSerialNumber, String sessionId) {
        try {
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info == null) {
                sendControlResult(deviceSerialNumber, "error", "设备信息不存在");
                sendErrorResponse(sessionId, "设备信息不存在");
                return;
            }

            String deviceId = info.getDeviceId();

            // 执行停止校零操作
            DevOperationInputParam inputParam = new DevOperationInputParam();
            inputParam.setDeviceSerialNumber(deviceSerialNumber);
//            inputParam.setOperationType(OperationTypeEnums.StopZeroing.getType());

            R<OperationResultEntity> operation = operationUtilService.operation(inputParam);

            if (!operation.getSuccess()) {
                log.error("停止校零失败: {}, 错误: {}", deviceSerialNumber, operation.getMessage());
                sendControlResult(deviceSerialNumber, "error", "停止校零失败: " + operation.getMessage());
                sendErrorResponse(sessionId, "停止校零失败: " + operation.getMessage());
                return;
            }

            // 恢复设备空闲状态
            deviceManager.setDeviceIdleStatus(deviceId, true);

            log.info("设备停止校零: {}", deviceSerialNumber);
            sendControlResult(deviceSerialNumber, "success", "校零操作已停止");
            sendSuccessResponse(sessionId, "校零操作已停止");

        } catch (Exception e) {
            log.error("停止校零异常: {}", deviceSerialNumber, e);
            sendControlResult(deviceSerialNumber, "error", "停止校零异常: " + e.getMessage());
            sendErrorResponse(sessionId, "停止校零异常: " + e.getMessage());
        }
    }

    /**
     * 保存测量结果
     */
    private void saveMeasurementResult(String deviceSerialNumber, String sessionId) {
        try {
            // 这里需要根据实际业务实现保存逻辑
            log.info("保存设备测量结果: {}", deviceSerialNumber);
            sendControlResult(deviceSerialNumber, "success", "测量结果已保存");
            sendSuccessResponse(sessionId, "测量结果已保存");

        } catch (Exception e) {
            log.error("保存测量结果异常: {}", deviceSerialNumber, e);
            sendControlResult(deviceSerialNumber, "error", "保存测量结果异常: " + e.getMessage());
            sendErrorResponse(sessionId, "保存测量结果异常: " + e.getMessage());
        }
    }

    /**
     * 执行强制测量
     */
    private void executeForceMeasurement(String deviceSerialNumber, String sessionId) {
        measurementExecutor.execute(() -> {
            try {
                DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
                if (info == null) {
                    sendControlResult(deviceSerialNumber, "error", "设备信息不存在");
                    sendErrorResponse(sessionId, "设备信息不存在");
                    return;
                }

                // 执行测量操作
                DevOperationInputParam inputParam = new DevOperationInputParam();
                inputParam.setDeviceSerialNumber(deviceSerialNumber);
                inputParam.setOperationType(OperationTypeEnums.LiveDetection.getType());

                R<OperationResultEntity> operation = operationUtilService.operation(inputParam);

                if (!operation.getSuccess()) {
                    log.error("强制测量失败: {}, 错误: {}", deviceSerialNumber, operation.getMessage());
                    sendControlResult(deviceSerialNumber, "error", "强制测量失败: " + operation.getMessage());
                    sendErrorResponse(sessionId, "强制测量失败: " + operation.getMessage());
                    return;
                }

                // 处理测量结果
                OperationResultEntity data = operation.getData();
                DetectionManyDevicesResponseInfoVO vo = new DetectionManyDevicesResponseInfoVO();
                BeanUtils.copyProperties(data, vo);

                // 获取设备实时信息
                String deviceId = info.getDeviceId();
                DeviceConnectInfo device = deviceManager.getDevice(deviceId);
                if (device != null) {
                    BeanUtils.copyProperties(device, vo);
                }

                // 推送测量结果到前端
                pushMeasurementResult(deviceSerialNumber, vo);

                log.info("强制测量完成: {}", deviceSerialNumber);
                sendControlResult(deviceSerialNumber, "success", "强制测量完成");
                sendSuccessResponse(sessionId, "强制测量完成");

            } catch (Exception e) {
                log.error("强制测量异常: {}", deviceSerialNumber, e);
                sendControlResult(deviceSerialNumber, "error", "强制测量异常: " + e.getMessage());
                sendErrorResponse(sessionId, "强制测量异常: " + e.getMessage());
            }
        });
    }

    /**
     * 执行一次性测量
     */
    private void startOneTimeMeasurement(String deviceSerialNumber, String sessionId) {
        measurementExecutor.execute(() -> {
            try {
                DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
                if (info == null) {
                    sendControlResult(deviceSerialNumber, "error", "设备信息不存在");
                    sendErrorResponse(sessionId, "设备信息不存在");
                    return;
                }

                // 执行一次性测量操作
                DevOperationInputParam inputParam = new DevOperationInputParam();
                inputParam.setDeviceSerialNumber(deviceSerialNumber);
//                inputParam.setOperationType(OperationTypeEnums.SingleDetection.getType());

                R<OperationResultEntity> operation = operationUtilService.operation(inputParam);

                if (!operation.getSuccess()) {
                    log.error("一次性测量失败: {}, 错误: {}", deviceSerialNumber, operation.getMessage());
                    sendControlResult(deviceSerialNumber, "error", "一次性测量失败: " + operation.getMessage());
                    sendErrorResponse(sessionId, "一次性测量失败: " + operation.getMessage());
                    return;
                }

                // 处理测量结果
                OperationResultEntity data = operation.getData();
                DetectionManyDevicesResponseInfoVO vo = new DetectionManyDevicesResponseInfoVO();
                BeanUtils.copyProperties(data, vo);

                // 获取设备实时信息
                String deviceId = info.getDeviceId();
                DeviceConnectInfo device = deviceManager.getDevice(deviceId);
                if (device != null) {
                    BeanUtils.copyProperties(device, vo);
                }

                // 创建一次性测量结果消息
                JSONObject result = new JSONObject();
                result.put("type", "one_time_measurement");
                result.put("deviceSerialNumber", deviceSerialNumber);
                result.put("timestamp", System.currentTimeMillis());
                result.put("data", vo);

                // 推送到前端
                messagingTemplate.convertAndSend(getDeviceTopic(deviceSerialNumber), result.toJSONString());

                log.info("一次性测量完成: {}", deviceSerialNumber);
                sendControlResult(deviceSerialNumber, "success", "一次性测量完成");
                sendSuccessResponse(sessionId, "一次性测量完成");

            } catch (Exception e) {
                log.error("一次性测量异常: {}", deviceSerialNumber, e);
                sendControlResult(deviceSerialNumber, "error", "一次性测量异常: " + e.getMessage());
                sendErrorResponse(sessionId, "一次性测量异常: " + e.getMessage());
            }
        });
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查设备是否有订阅者
     */
    private boolean hasSubscribers(String deviceSerialNumber, String topic) {
        Map<String, AtomicInteger> deviceSubs = deviceSubscriptions.get(deviceSerialNumber);
        if (deviceSubs == null) {
            return false;
        }

        AtomicInteger count = deviceSubs.get(topic);
        return count != null && count.get() > 0;
    }

    /**
     * 更新订阅关系
     */
    private synchronized void updateSubscription(String deviceSerialNumber, String topic,
                                                 String sessionId, boolean isSubscribe) {
        // 更新设备订阅计数
        Map<String, AtomicInteger> deviceSubs = deviceSubscriptions
                .computeIfAbsent(deviceSerialNumber, k -> new ConcurrentHashMap<>());

        AtomicInteger count = deviceSubs.computeIfAbsent(topic, k -> new AtomicInteger(0));

        if (isSubscribe) {
            count.incrementAndGet();
        } else {
            count.decrementAndGet();
            if (count.get() <= 0) {
                deviceSubs.remove(topic);
            }
        }

        // 更新会话订阅关系
        Set<String> sessionSubs = sessionSubscriptions
                .computeIfAbsent(sessionId, k -> new HashSet<>());

        if (isSubscribe) {
            sessionSubs.add(topic);
        } else {
            sessionSubs.remove(topic);
            if (sessionSubs.isEmpty()) {
                sessionSubscriptions.remove(sessionId);
            }
        }

        log.debug("订阅更新: 设备={}, 主题={}, 会话={}, 操作={}, 当前计数={}",
                deviceSerialNumber, topic, sessionId,
                isSubscribe ? "订阅" : "取消订阅", count.get());
    }

    /**
     * 获取设备专属主题
     */
    private String getDeviceTopic(String deviceSerialNumber) {
//        return measureTopic + "/" + deviceSerialNumber;
        return measureTopic;
    }

    private String getDeviceStatusTopic(String deviceSerialNumber) {
//        return deviceStatusTopic + "/" + deviceSerialNumber;
        return deviceStatusTopic;
    }

    private String getDeviceControlTopic(String deviceSerialNumber) {
//        return deviceControlTopic + "/" + deviceSerialNumber;
        return deviceControlTopic;
    }

    // ==================== 消息推送方法 ====================

    /**
     * 推送测量结果到前端
     */
    private void pushMeasurementResult(String deviceSerialNumber, DetectionManyDevicesResponseInfoVO vo) {
        try {
            JSONObject result = JSON.parseObject(JSON.toJSONString(vo));
            result.put("type", "measurement");
            result.put("deviceSerialNumber", deviceSerialNumber);
            result.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(getDeviceTopic(deviceSerialNumber), result.toJSONString());
        } catch (Exception e) {
            log.error("推送测量结果失败: {}", deviceSerialNumber, e);
        }
    }

    /**
     * 推送设备状态到前端
     */
    private void pushDeviceStatus(String deviceSerialNumber, DeviceInfoPO info) {
        try {
            JSONObject status = new JSONObject();
            status.put("type", "status");
            status.put("deviceSerialNumber", deviceSerialNumber);
            status.put("timestamp", System.currentTimeMillis());
            status.put("devCurrentStatus", info.getDeviceCurrentStatus());
            status.put("connectStatus", info.getConnectStatus());
            status.put("isInited", info.getIsInited());
            status.put("ip", info.getIp());
            status.put("port", info.getDevicePort());

            String deviceId = info.getDeviceId();
            DeviceConnectInfo device = deviceManager.getDevice(deviceId);
            if (device != null) {
                status.put("isIdle", device.getIsIdle());
                status.put("currentWavelength", device.getCurrentWavelength());
                status.put("luminosityMode", device.getLuminosityMode());
            }

            messagingTemplate.convertAndSend(getDeviceStatusTopic(deviceSerialNumber), status.toJSONString());
        } catch (Exception e) {
            log.error("推送设备状态失败: {}", deviceSerialNumber, e);
        }
    }

    /**
     * 推送校零波长
     */
    private void pushZeroingWavelength(String deviceSerialNumber) {
        try {
            JSONObject zeroingInfo = new JSONObject();
            zeroingInfo.put("type", "zeroing");
            zeroingInfo.put("deviceSerialNumber", deviceSerialNumber);
            zeroingInfo.put("wavelength", DetectionManyDevicesConstants.zeroWavelength);
            zeroingInfo.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(getDeviceTopic(deviceSerialNumber), zeroingInfo.toJSONString());
        } catch (Exception e) {
            log.error("推送校零波长失败: {}", deviceSerialNumber, e);
        }
    }

    /**
     * 推送错误信息
     */
    private void pushErrorToFrontend(String deviceSerialNumber, String errorType, String message) {
        try {
            JSONObject error = new JSONObject();
            error.put("type", "error");
            error.put("errorType", errorType);
            error.put("deviceSerialNumber", deviceSerialNumber);
            error.put("message", message);
            error.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(getDeviceTopic(deviceSerialNumber), error.toJSONString());
        } catch (Exception e) {
            log.error("推送错误信息失败: {}", deviceSerialNumber, e);
        }
    }

    /**
     * 发送控制结果
     */
    private void sendControlResult(String deviceSerialNumber, String resultType, String message) {
        try {
            JSONObject result = new JSONObject();
            result.put("type", "control_result");
            result.put("resultType", resultType);
            result.put("deviceSerialNumber", deviceSerialNumber);
            result.put("message", message);
            result.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(getDeviceControlTopic(deviceSerialNumber), result.toJSONString());
        } catch (Exception e) {
            log.error("发送控制结果失败: {}", deviceSerialNumber, e);
        }
    }

    /**
     * 发送成功响应
     */
    private void sendSuccessResponse(String sessionId, String message) {
        try {
            JSONObject response = new JSONObject();
            response.put("type", "response");
            response.put("success", true);
            response.put("message", message);
            response.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSendToUser(sessionId, "/queue/response", response.toJSONString());
        } catch (Exception e) {
            log.error("发送成功响应失败: {}", sessionId, e);
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(String sessionId, String message) {
        try {
            JSONObject response = new JSONObject();
            response.put("type", "response");
            response.put("success", false);
            response.put("message", message);
            response.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(sessionId, response.toJSONString());

//            messagingTemplate.convertAndSendToUser(sessionId, response.toJSONString());
        } catch (Exception e) {
            log.error("发送错误响应失败: {}", sessionId, e);
        }
    }

    // ==================== WebSocket事件监听 ====================

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket连接建立: {}", sessionId);
    }

    @EventListener
    public void handleStompSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        log.info("WebSocket订阅: {}, 目的地: {}", sessionId, destination);

        // 解析设备序列号
        String deviceSerialNumber = extractDeviceSerialNumber(headerAccessor);
        if (StringUtils.hasText(deviceSerialNumber)) {
            // 更新会话订阅关系
            sessionSubscriptions.computeIfAbsent(sessionId, k -> new HashSet<>()).add(destination);

            // 更新设备订阅计数
            deviceSubscriptions.computeIfAbsent(deviceSerialNumber, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(destination, k -> new AtomicInteger(0))
                    .incrementAndGet();

            // 根据订阅的目的地执行相应操作
            handleSubscriptionAction(deviceSerialNumber, destination);
        }
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket连接断开: {}", sessionId);

        // 清理会话订阅关系
        Set<String> subscriptions = sessionSubscriptions.remove(sessionId);
        if (subscriptions != null) {
            for (String destination : subscriptions) {
                String deviceSerialNumber = extractDeviceSerialNumber(headerAccessor);
                if (StringUtils.hasText(deviceSerialNumber)) {
                    // 更新设备订阅计数
                    Map<String, AtomicInteger> deviceSubs = deviceSubscriptions.get(deviceSerialNumber);
                    if (deviceSubs != null) {
                        AtomicInteger count = deviceSubs.get(destination);
                        if (count != null && count.decrementAndGet() <= 0) {
                            deviceSubs.remove(destination);

                            // 如果没有订阅者了，暂停相应任务
                            if (destination.contains("/detection/")) {
                                pauseMeasurement(deviceSerialNumber);
                            }

                            if (destination.contains("/deviceStatus/")) {
                                StatusTaskInfo taskInfo = statusTasks.get(deviceSerialNumber);
                                if (taskInfo != null) {
                                    taskInfo.isPaused = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理订阅动作
     */
    private void handleSubscriptionAction(String deviceSerialNumber, String destination) {
        // 根据订阅的目的地执行不同的操作
        if (destination.contains("/detection/") && !destination.contains("/deviceControl/")) {
            // 测量订阅：启动或恢复测量任务
            startOrResumeMeasurement(deviceSerialNumber);

        } else if (destination.contains("/deviceStatus/")) {
            // 状态订阅：启动或恢复状态监控
            startOrResumeStatusMonitoring(deviceSerialNumber);

        } else if (destination.contains("/deviceControl/")) {
            // 控制订阅：执行一次性控制操作
            String controlCommand = extractControlCommandFromDestination(destination);
            if (controlCommand != null) {
                executeOneTimeControl(deviceSerialNumber, controlCommand);
            }
        }
    }

    /**
     * 执行一次性控制操作
     */
    private void executeOneTimeControl(String deviceSerialNumber, String controlCommand) {
        // 使用任务标记防止重复执行
        String taskKey = deviceSerialNumber + ":" + controlCommand;
        if (oneTimeTaskFlags.containsKey(taskKey)) {
            log.debug("任务已在执行中，跳过: {}", taskKey);
            return;
        }

        log.info("执行一次性控制操作: 设备={}, 指令={}", deviceSerialNumber, controlCommand);

        // 根据控制指令执行相应操作
        switch (controlCommand) {
            case "startZeroing":
                startZeroingOperation(deviceSerialNumber, "system");
                break;

            case "stopZeroing":
                stopZeroingOperation(deviceSerialNumber, "system");
                break;

            case "saveResult":
                saveMeasurementResult(deviceSerialNumber, "system");
                break;

            case "forceMeasurement":
                executeForceMeasurement(deviceSerialNumber, "system");
                break;

            case "startOneTimeMeasurement":
                startOneTimeMeasurement(deviceSerialNumber, "system");
                break;

            default:
                log.warn("未知的控制指令: {}", controlCommand);
        }
    }

    /**
     * 从目的地中提取设备序列号
     */
    private String extractDeviceSerialNumberFromDestination(String destination) {
        if (StringUtils.isEmpty(destination)) {
            return null;
        }

        String[] parts = destination.split("/");
        if (parts.length >= 4) {
            return parts[parts.length - 1];
        }

        return null;
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

    // ==================== 设备操作方法 ====================

    /**
     * 从目的地中提取控制命令
     */
    private String extractControlCommandFromDestination(String destination) {
        if (StringUtils.isEmpty(destination)) {
            return null;
        }

        // 匹配模式：/topic/deviceControl/{deviceSerialNumber}/{command}
        String[] parts = destination.split("/");
        if (parts.length >= 5) {
            return parts[parts.length - 1];
        }

        return null;
    }

    // ==================== 健康检查和自动恢复 ====================

    /**
     * 定期检查任务健康状态
     */
    private void initHealthCheck() {
        // 每30秒检查一次任务健康状态
        taskScheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndRecoverTasks();
            } catch (Exception e) {
                log.error("健康检查异常", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

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
            if (taskInfo.isRunning && !taskInfo.isPaused &&
                    (now - taskInfo.lastActiveTime) > 60000) {

                log.warn("测量任务长时间无活动，尝试恢复: {}", deviceSerialNumber);

                // 尝试重新调度
                if (taskInfo.currentFuture == null || taskInfo.currentFuture.isDone()) {
                    scheduleNextMeasurement(taskInfo, 0);
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
            if (taskInfo.currentFuture != null && !taskInfo.currentFuture.isDone()) {
                taskInfo.currentFuture.cancel(false);
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
        sessionSubscriptions.clear();
        deviceSubscriptions.clear();
        oneTimeTaskFlags.clear();

        log.info("WebSocket控制器资源清理完成");
    }

    // ==================== 内部类定义 ====================

    /**
     * 测量任务信息
     */
    @Data
    private static class MeasureTaskInfo {
        private String deviceSerialNumber;
        private ScheduledFuture<?> currentFuture;
        private volatile boolean isRunning;
        private volatile boolean isPaused;
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
        private volatile boolean isRunning;
        private volatile boolean isPaused;
    }

    /**
     * 订阅请求
     */
    @Data
    public static class SubscribeRequest {
        private String deviceSerialNumber;
        private String sessionId;
    }

    /**
     * 控制命令请求
     */
    @Data
    public static class ControlCommandRequest {
        private String deviceSerialNumber;
        private String command;
        private String sessionId;
    }
}