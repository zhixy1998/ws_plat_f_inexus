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

package com.persee.workstation.uv_inexus.websocket.concise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.LinearSegment;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureMethodService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskAttachmentService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * WebSocket处理器类，继承自TextWebSocketHandler
 * 用于处理WebSocket连接的建立、消息接收和连接关闭等事件
 */
@Component
public class ConciseBaseLineWebSocket extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ConciseBaseLineWebSocket.class);

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();

    // 用于存储数据点索引（适用于所有扫描模式）
    private static final Map<String, Integer> dataPointIndexMap = new ConcurrentHashMap<>();

    // 会话状态管理
    private static final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    @Autowired
    private SpectrumConciseService spectrumService;

    @Autowired
    private FMeasureTaskService tMeasureTaskService;

    @Autowired
    private FMeasureTaskAttachmentService tMeasureTaskAttachmentService;

    @Autowired
    private FMeasureMethodService tMeasureMethodService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private SystemUserManageDAO systemUserManageDAO;

    @Autowired
    private FormulaUtil formulaUtil;

    /**
     * 自定义异常类，用于区分用户主动停止和其他异常
     */
    private static class UserInterruptedException extends RuntimeException {
        public UserInterruptedException(String message) {
            super(message);
        }
    }

    /**
     * 会话状态管理类
     */
    private static class SessionState {
        private final AtomicBoolean deviceOccupied = new AtomicBoolean(false);
        private final AtomicBoolean measurementStarted = new AtomicBoolean(false);
        private final AtomicBoolean stopCommandSent = new AtomicBoolean(false);
        private final AtomicBoolean stopSuccessSent = new AtomicBoolean(false);
        private String deviceSerialNumber;
        private String ip;

        public void reset() {
            deviceOccupied.set(false);
            measurementStarted.set(false);
            stopCommandSent.set(false);
            stopSuccessSent.set(false);
            deviceSerialNumber = null;
            ip = null;
        }
    }

    /**
     * 当WebSocket连接建立后调用此方法
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("客户端进入: {} 时间: {}", session.getId(), LocalDateTime.now());
        sessions.put(session.getId(), session);

        // 初始化会话状态
        SessionState state = new SessionState();
        sessionStates.put(session.getId(), state);

        // 往前端推送数据的线程
        Thread taskThread = new Thread(() -> {
            try {
                runBusinessLogic(session);
            } catch (UserInterruptedException e) {
                log.info("用户主动停止任务: {}", session.getId());
                // 不在这里发送停止成功，由 handleStopRequest 统一发送
            } catch (Exception e) {
                log.error("基线任务执行异常", e);
                sendMessage(session, "执行异常: " + e.getMessage());
            } finally {
                cleanupResources(session);
            }
        });

        taskThreads.put(session.getId(), taskThread);
        taskThread.start();
    }

    /**
     * 处理接收到的文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("收到消息: {} -> {}", session.getId(), message.getPayload());

        if ("close".equals(message.getPayload())) {
            handleStopRequest(session);
        }
    }

    /**
     * 处理停止请求
     */
    private void handleStopRequest(WebSocketSession session) {
        SessionState state = sessionStates.get(session.getId());
        if (state == null) {
            return;
        }

        try {
            // 中断任务线程
            Thread thread = taskThreads.get(session.getId());
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }

            // 发送停止命令
            sendStopCommandIfNeeded(session);

            // 立即响应停止请求 - 确保只发送一次
            if (!state.stopSuccessSent.getAndSet(true)) {
                sendMessageSafe(session, "停止成功");
            }

        } catch (Exception e) {
            log.error("处理停止消息异常, session: {}", session.getId(), e);
            // 即使有异常也要确保发送停止成功
            if (state != null && !state.stopSuccessSent.getAndSet(true)) {
                sendMessageSafe(session, "停止成功");
            }
        }
    }

    /**
     * 如果需要，发送停止命令
     */
    private void sendStopCommandIfNeeded(WebSocketSession session) {
        SessionState state = sessionStates.get(session.getId());
        if (state == null) {
            return;
        }

        // 只有在测量已开始且停止命令未发送过的情况下才发送停止命令
        if (state.measurementStarted.get() && !state.stopCommandSent.getAndSet(true)) {
            if (state.ip != null) {
                try {
                    R<String> stopResult = devMessageUtil.stop(buildCommand(SetParamEnum.Stop, new Object[]{}), state.ip);
                    if (stopResult.getSuccess()) {
                        log.info("设备停止命令发送成功, session: {}", session.getId());
                    } else {
                        log.error("设备停止命令发送失败, session: {}", session.getId());
                    }
                } catch (Exception e) {
                    log.error("发送停止命令异常, session: {}", session.getId(), e);
                }
            }
        }
    }

    /**
     * 连接异常时触发
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("连接异常, session: {}, 异常: {}", session.getId(), exception.getMessage(), exception);
        // 发送停止命令（如果测量已开始）
        sendStopCommandIfNeeded(session);
        cleanupResources(session);
        sessions.remove(session.getId());

        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            log.debug("关闭会话异常（连接可能已关闭）, session: {}", session.getId());
        }
    }

    /**
     * 重写afterConnectionClosed方法，用于处理WebSocket连接关闭后的操作
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("客户端退出: {} 时间: {} 原因: {}", session.getId(), LocalDateTime.now(), status);

        // 发送停止命令（如果测量已开始）
        sendStopCommandIfNeeded(session);

        // 中断任务线程
        Thread taskThread = taskThreads.remove(session.getId());
        if (taskThread != null && taskThread.isAlive()) {
            taskThread.interrupt();
        }

        sessions.remove(session.getId());
        cleanupResources(session);
        exceptionHandling();

        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            log.debug("关闭会话异常（连接可能已关闭）, session: {}", session.getId());
        }
    }

    /**
     * 主要的业务逻辑
     */
    private void runBusinessLogic(WebSocketSession session) {
        // 获取参数
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String methodId = getQueryParam(query, "methodId");
        String deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");

        // 参数验证
        if (!validateParameters(session, methodId, deviceSerialNumber)) {
            return;
        }

        // 占用设备
        if (!occupyDevice(session, deviceSerialNumber)) {
            return;
        }

        try {
            // 设置参数
            if (!setupParameters(session, methodId, deviceSerialNumber)) {
                return;
            }

            // 获取方法参数
            FMeasureMethod method = tMeasureMethodService.getById(methodId);
            if (method == null) {
                sendAndClose(session, "方法不存在");
                return;
            }

            // 开始测量
            if (!startMeasurement(session, method, deviceSerialNumber)) {
                return;
            }

            // 收集数据
            List<ChangeParam> dataList = collectBaselineData(session, method, deviceSerialNumber);
            if (dataList == null) {
                return; // 数据收集被中断或失败
            }

            // 保存任务结果
            if (!saveTaskResult(session, taskId, methodId, deviceSerialNumber, method, dataList)) {
                return;
            }

            sendAndClose(session, "基线成功");

        } catch (UserInterruptedException e) {
            // 用户主动停止，不处理，由 handleStopRequest 统一发送停止成功
            log.info("用户主动停止任务, session: {}", session.getId());
        } catch (Exception e) {
            log.error("基线任务执行异常, session: {}", session.getId(), e);
            sendAndClose(session, "基线任务执行异常: " + e.getMessage());
        } finally {
            // 标记测量结束
            SessionState state = sessionStates.get(session.getId());
            if (state != null) {
                state.measurementStarted.set(false);
            }
        }
    }

    /**
     * 参数验证
     */
    private boolean validateParameters(WebSocketSession session, String methodId, String deviceSerialNumber) {
        if (methodId == null || methodId.trim().isEmpty()) {
            sendClose(session, "方法编号不能为空");
            return false;
        }
        if (deviceSerialNumber == null || deviceSerialNumber.trim().isEmpty()) {
            sendClose(session, "设备编号不能为空");
            return false;
        }
        return true;
    }

    /**
     * 占用设备
     */
    private boolean occupyDevice(WebSocketSession session, String deviceSerialNumber) {
        SessionState state = sessionStates.get(session.getId());
        if (state == null) {
            sendClose(session, "会话状态异常");
            return false;
        }

        if (state.deviceOccupied.get()) {
            log.warn("设备已被占用，重复占用请求, session: {}", session.getId());
            return true;
        }

        R<Boolean> occupyResult = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);
        if (!occupyResult.getSuccess()) {
            sendClose(session, occupyResult.getMessage());
            return false;
        }

        state.deviceOccupied.set(true);
        state.deviceSerialNumber = deviceSerialNumber;
        log.info("设备占用成功: {}, session: {}", deviceSerialNumber, session.getId());
        return true;
    }

    /**
     * 设置参数
     */
    private boolean setupParameters(WebSocketSession session, String methodId, String deviceSerialNumber) {
        try {
            checkInterrupted(session);

            Result result = spectrumService.setUpParametersPort(methodId, deviceSerialNumber);
            if (result.getCode() != 200) {
                sendAndClose(session, "设置参数失败");
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            log.info("设置参数被中断, session: {}", session.getId());
            throw new UserInterruptedException("用户主动停止");
        }
    }

    /**
     * 开始测量
     */
    private boolean startMeasurement(WebSocketSession session, FMeasureMethod method, String deviceSerialNumber) {
        try {
            checkInterrupted(session);

            SessionState state = sessionStates.get(session.getId());
            if (state == null) {
                sendAndClose(session, "会话状态异常");
                return false;
            }

            String ip = deviceInfoService.getIp(deviceSerialNumber);
            if (ip == null) {
                sendAndClose(session, "获取设备IP失败");
                return false;
            }

            state.ip = ip;

            FSpectrumParameter param = JSONObject.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
            Constants.instrumentData.clear();
            Constants.measurd_data.clear();

            // 发送基线命令
            Command command = buildCommand(SetParamEnum.BaseLine, new Object[]{});
            boolean isFastScan = "3".equals(param.getScanSpeed());
            if (isFastScan) {
                command = buildCommand(SetParamEnum.BaseLineFast, new Object[]{});
            }

            dataPointIndexMap.put(session.getId(), 0);
            devMessageUtil.spectrumScanGetBaselineRealTime(command, ip);

            // 标记测量已开始
            state.measurementStarted.set(true);
            log.info("基线测量开始, 设备: {}, 模式: {}, session: {}",
                    deviceSerialNumber, isFastScan ? "极快扫描" : "普通扫描", session.getId());

            return true;
        } catch (InterruptedException e) {
            log.info("开始测量被中断, session: {}", session.getId());
            throw new UserInterruptedException("用户主动停止");
        }
    }

    /**
     * 收集基线数据
     */
    private List<ChangeParam> collectBaselineData(WebSocketSession session, FMeasureMethod method, String deviceSerialNumber) {
        try {
            List<ChangeParam> dataList = new ArrayList<>();
            FSpectrumParameter param = JSONObject.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
            List<LinearSegment> linearSegment = formulaUtil.getLinearSegment(deviceSerialNumber);

            double wavelengthInterval = Double.parseDouble(param.getWaveLengthInterval());
            double startWl = Double.parseDouble(param.getStartWaveLength());
            double endWl = Double.parseDouble(param.getEndWaveLength());
            boolean isDescending = startWl > endWl;

            int expectedPoints = calculateExpectedDataPoints(startWl, endWl, wavelengthInterval);
            log.info("期望数据点数: {}, 波长范围: {} - {}, 间隔: {}, session: {}",
                    expectedPoints, startWl, endWl, wavelengthInterval, session.getId());

            while (true) {
                checkInterrupted(session);

                // 处理测量数据
                while (!Constants.measurd_data.isEmpty()) {
                    ChangeParam changeParam = Constants.measurd_data.remove(0);
                    Double originalWavelength = Double.parseDouble(changeParam.getWl());
                    Double correctedWavelength = formulaUtil.calculateWavelength(originalWavelength, linearSegment);

                    Double constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                            wavelengthInterval, isDescending, session.getId());

                    if (constrainedWavelength != null) {
                        pushBaselineDataPoint(session, constrainedWavelength);

                        ChangeParam processedParam = new ChangeParam();
                        processedParam.setWl(constrainedWavelength.toString());
                        processedParam.setS(changeParam.getS());
                        processedParam.setR(changeParam.getR());
                        processedParam.setD(changeParam.getD());
                        dataList.add(processedParam);

                        log.debug("基线数据 - 原始波长: {}, 修约后波长: {}, session: {}",
                                correctedWavelength, constrainedWavelength, session.getId());
                    }
                }

                // 检查测量是否结束
                if (isMeasurementComplete()) {
                    SessionState state = sessionStates.get(session.getId());
                    if (state != null) {
                        state.measurementStarted.set(false);
                    }

                    int actualPoints = dataPointIndexMap.get(session.getId());
                    log.info("基线数据收集完成，实际数据点数: {}, 期望数据点数: {}, session: {}",
                            actualPoints, expectedPoints, session.getId());

                    if (actualPoints < expectedPoints * 0.8) {
                        log.warn("基线数据点数不足，可能采集不完整, session: {}", session.getId());
                    }
                    break;
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    log.info("数据收集被中断, session: {}", session.getId());
                    throw new UserInterruptedException("用户主动停止");
                }
            }

            if (dataList.isEmpty()) {
                log.error("基线数据为空，失败, session: {}", session.getId());
                sendAndClose(session, "基线数据为空，失败");
                return null;
            }

            log.info("基线数据收集完成, dataList 大小: {}, session: {}", dataList.size(), session.getId());
            return dataList;
        } catch (InterruptedException e) {
            log.info("数据收集被中断, session: {}", session.getId());
            throw new UserInterruptedException("用户主动停止");
        }
    }

    /**
     * 保存任务结果
     */
    private boolean saveTaskResult(WebSocketSession session, String taskId, String methodId,
                                   String deviceSerialNumber, FMeasureMethod method, List<ChangeParam> dataList) {
        try {
            checkInterrupted(session);

            FMeasureTask task;
            if (taskId != null && !taskId.trim().isEmpty()) {
                task = tMeasureTaskService.getById(taskId);
                if (task == null) {
                    sendAndClose(session, "任务不存在");
                    return false;
                }
            } else {
                task = new FMeasureTask();
                SystemUserManagePO user = systemUserManageDAO
                        .selectOne(new QueryWrapper<SystemUserManagePO>().eq("user_name", method.getMethodCreator()));
                if (user != null) {
                    task.setUserId(user.getId());
                }

                String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                String collect = new Random().ints(8, 0, chars.length())
                        .mapToObj(i -> String.valueOf(chars.charAt(i)))
                        .collect(Collectors.joining());

                task.setTaskName(method.getMeasurementType() + "task_" + collect);
                task.setCreateTime(LocalDateTime.now());
                task.setMethodKey(methodId);
                task.setMeasureType(method.getMeasurementType());
                task.setProjectItemKey(method.getProjectItemKey());
                task.setDeviceSerialNumber(deviceSerialNumber);

                boolean save1 = tMeasureTaskService.save(task);
                if (!save1) {
                    sendAndClose(session, "保存任务失败");
                    return false;
                }

                FMeasureTaskAttachment taskAttachment = new FMeasureTaskAttachment();
                taskAttachment.setTaskId(task.getId());
                boolean save = tMeasureTaskAttachmentService.save(taskAttachment);
                if (!save) {
                    sendAndClose(session, "保存任务附件失败");
                    return false;
                }
            }

            checkInterrupted(session);
            task.setZeroParam(JSON.toJSONString(dataList));
            boolean update = tMeasureTaskService.updateById(task);
            if (!update) {
                sendAndClose(session, "更新任务失败");
                return false;
            }

            Constants.measureStatus = true;
            return true;
        } catch (InterruptedException e) {
            log.info("保存任务结果被中断, session: {}", session.getId());
            throw new UserInterruptedException("用户主动停止");
        }
    }

    /**
     * 清理资源
     */
    private void cleanupResources(WebSocketSession session) {
        String sessionId = session.getId();
        SessionState state = sessionStates.remove(sessionId);

        if (state != null) {
            // 只在设备被占用的情况下释放
            if (state.deviceOccupied.compareAndSet(true, false)) {
                if (state.deviceSerialNumber != null) {
                    try {
                        deviceInfoService.releaseDevice(state.deviceSerialNumber);
                        log.info("设备释放成功: {}, session: {}", state.deviceSerialNumber, sessionId);
                    } catch (Exception e) {
                        log.error("释放设备异常, session: {}", sessionId, e);
                    }
                }
            }
        }

        // 清理数据点索引
        dataPointIndexMap.remove(sessionId);

        // 清理线程引用
        taskThreads.remove(sessionId);
    }

    /**
     * 强制约束波长值，确保符合设定的波长间隔和范围
     */
    private Double forceConstrainWavelength(Double originalWl, double startWl, double endWl,
                                            double interval, boolean isDescending, String sessionId) {
        if (originalWl == null) {
            return null;
        }

        // 获取当前数据点索引
        Integer currentIndex = dataPointIndexMap.get(sessionId);
        if (currentIndex == null) {
            currentIndex = 0;
        }

        // 计算期望的波长值 - 强制使用期望值
        double expectedWl;
        if (isDescending) {
            // 递减扫描
            expectedWl = startWl - (currentIndex * interval);
            // 确保不超过结束波长
            if (expectedWl < endWl) {
                expectedWl = endWl;
            }
        } else {
            // 递增扫描
            expectedWl = startWl + (currentIndex * interval);
            // 确保不超过结束波长
            if (expectedWl > endWl) {
                expectedWl = endWl;
            }
        }

        // 强制修约到合适的小数位数
        int decimalPlaces = getDecimalPlaces(interval);
        double constrainedWl = roundToPrecision(expectedWl, decimalPlaces);

        // 确保波长在合理范围内
        if (isDescending) {
            if (constrainedWl > startWl) constrainedWl = startWl;
            if (constrainedWl < endWl) constrainedWl = endWl;
        } else {
            if (constrainedWl < startWl) constrainedWl = startWl;
            if (constrainedWl > endWl) constrainedWl = endWl;
        }

        // 记录修约信息（如果原始值与修约值不同）
        if (Math.abs(originalWl - constrainedWl) > 0.001) {
            log.debug("基线波长强制修约: 原始值={}, 修约值={}, 期望值={}, 索引={}, session: {}",
                    originalWl, constrainedWl, expectedWl, currentIndex, sessionId);
        }

        // 更新索引
        dataPointIndexMap.put(sessionId, currentIndex + 1);

        return constrainedWl;
    }

    /**
     * 计算期望的数据点数
     */
    private int calculateExpectedDataPoints(double startWl, double endWl, double interval) {
        if (interval <= 0) {
            return 1000; // 默认值
        }

        int points = (int) Math.abs((startWl - endWl) / interval) + 1;
        log.debug("计算期望数据点数: 范围={}-{}, 间隔={}, 点数={}", startWl, endWl, interval, points);
        return Math.max(points, 100); // 至少100个点
    }

    /**
     * 根据间隔值确定需要保留的小数位数
     */
    private int getDecimalPlaces(double interval) {
        if (interval == Math.floor(interval)) {
            // 整数间隔，保留0位小数
            return 0;
        } else {
            // 小数间隔，根据间隔的小数位数确定
            String intervalStr = String.valueOf(interval);
            if (intervalStr.contains(".")) {
                return intervalStr.length() - intervalStr.indexOf(".") - 1;
            }
            return 1; // 默认保留1位小数
        }
    }

    /**
     * 精确舍入到指定小数位
     */
    private double roundToPrecision(double value, int decimalPlaces) {
        double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }

    /**
     * 推送基线数据点 - 逐点推送
     */
    private void pushBaselineDataPoint(WebSocketSession session, Double wavelength) {
        JSONObject point = new JSONObject();
        point.put("nm", wavelength);
        JSONObject payload = new JSONObject();
        payload.put("data", point);
        sendMessageSafe(session, payload.toJSONString());
    }

    /**
     * 检查测量是否完成
     */
    private boolean isMeasurementComplete() {
        List<String> snapshot = new ArrayList<>(Constants.instrumentData);
        return !snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData);
    }

    /**
     * 中断检测
     */
    private void checkInterrupted(WebSocketSession session) throws InterruptedException {
        if (Thread.currentThread().isInterrupted() || !session.isOpen()) {
            throw new InterruptedException("任务被中断或连接关闭");
        }
    }

    /**
     * 发送消息（安全的，不会抛出异常）
     */
    private void sendMessageSafe(WebSocketSession session, String message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.debug("发送消息失败，连接可能已关闭, session: {}, 消息: {}", session.getId(), message);
                // 忽略异常，连接可能已关闭
            }
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, String message) {
        sendMessageSafe(session, message);
    }

    /**
     * 发送消息并关闭连接
     */
    private void sendAndClose(WebSocketSession session, String msg) {
        try {
            sendMessageSafe(session, msg);
            sessions.remove(session.getId());
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            log.debug("发送消息并关闭连接异常（连接可能已关闭）, session: {}", session.getId());
        } finally {
            cleanupResources(session);
        }
    }

    /**
     * 发送消息并关闭连接2
     */
    private void sendClose(WebSocketSession session, String msg) {
        try {
            sendMessageSafe(session, msg);
            sessions.remove(session.getId());
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            log.debug("发送关闭消息异常（连接可能已关闭）, session: {}", session.getId());
        } finally {
            cleanupResources(session);
        }
    }

    /**
     * 获取查询参数
     */
    private String getQueryParam(String query, String key) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) return pair[1];
        }
        return null;
    }

    /**
     * 构建命令
     */
    private Command buildCommand(SetParamEnum cmdEnum, Object value) {
        Command cmd = new Command();
        cmd.setCmdId(cmdEnum.getComId());
        cmd.setParams(new Object[]{value});
        return cmd;
    }

    /**
     * 全局数据清空
     */
    private void exceptionHandling() {
        Constants.startTime = null;
        Constants.endTime = null;
        Constants.measurd_data.clear();
        Constants.instrumentData.clear();
        Constants.measureStatus = false;
    }
}