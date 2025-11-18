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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * WebSocket处理器类，继承自TextWebSocketHandler
 * 用于处理WebSocket连接的建立、消息接收和连接关闭等事件
 */
@Component
public class ConciseBaseLineWebSocket extends TextWebSocketHandler {
    // 创建日志记录器，用于记录类中的日志信息
    private static final Logger log = LoggerFactory.getLogger(ConciseBaseLineWebSocket.class);

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储正在执行的任务
    private final Map<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();

    private String ip;
    private String deviceSerialNumber;

    // 设备占用状态
    private boolean occupancyStatus = false;

    // 测量状态
    private boolean measureStatus = false;

    // 用于存储数据点索引（适用于所有扫描模式）
    private Map<String, Integer> dataPointIndexMap = new ConcurrentHashMap<>();

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
     * 当WebSocket连接建立后调用此方法
     *
     * @param session 当前WebSocket会话，包含连接信息
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 设备占用状态
        occupancyStatus = false;

        // 测量状态
        measureStatus = false;
        log.info("客户端进入: " + session.getId() + " 时间: " + LocalDateTime.now());
        sessions.put(session.getId(), session);

        // 往前端推送数据的线程
        Thread taskThread = new Thread(() -> {
            try {
                runBusinessLogic(session);
            } catch (InterruptedException e) {
                // 线程被中断，立即停止
                log.info("任务被中断: " + session.getId());
                sendAndClose(session, "操作已取消，连接关闭");
            } catch (Exception e) {
                log.error("基线任务执行异常", e);
                sendAndClose(session, "执行异常: " + e.getMessage());
            }
        });

        taskThreads.put(session.getId(), taskThread);
        taskThread.start();
    }

    /**
     * 处理接收到的文本消息
     *
     * @param session 当前WebSocket会话
     * @param message 接收到的文本消息对象
     * @throws Exception 可能抛出的异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 打印接收到的消息内容，包括发送方会话ID和消息内容
        log.info("收到消息: " + session.getId() + " -> " + message.getPayload());

        try {
            if (message.getPayload().equals("close")) {
                Thread thread = taskThreads.get(session.getId());
                // 强制停止线程
                if (thread != null) {
                    thread.interrupt();
                    taskThreads.remove(session.getId());
                }
                if (measureStatus) {
                    R<String> stop = devMessageUtil.stop(buildCommand(SetParamEnum.Stop, new Object[]{}), ip);
                    if (stop.getSuccess()) {
                        log.info("停止成功");
                    } else {
                        log.info("停止失败");
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理停止消息异常", e);
            throw new RuntimeException(e);
        } finally {
            log.info("停止成功-释放资源");
            session.sendMessage(new TextMessage("停止成功"));
            sessions.remove(session.getId());
            session.close();
            if (occupancyStatus) {
                deviceInfoService.releaseDevice(deviceSerialNumber);
            }
            // 清理数据
            dataPointIndexMap.remove(session.getId());
        }
    }

    /**
     * 连接异常时触发
     *
     * @param session
     * @param exception
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
        try {
            session.close();
        } catch (IOException e) {
            log.error("关闭会话异常", e);
            Thread.currentThread().interrupt();
            return;
        } finally {
            log.error("连接异常-释放资源");
            deviceInfoService.releaseDevice(deviceSerialNumber);
            // 清理数据
            dataPointIndexMap.remove(session.getId());
        }
        log.error("连接异常: " + exception.getMessage());
    }

    /**
     * 重写afterConnectionClosed方法，用于处理WebSocket连接关闭后的操作
     *
     * @param session WebSocket会话对象，包含连接的相关信息
     * @param status  关闭状态，包含连接关闭的原因和代码
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("客户端退出: " + session.getId() + " 时间: " + LocalDateTime.now() + " 原因: " + status);
        sessions.remove(session.getId());

        if (Constants.sendMeasureThread != null) {
            Constants.sendMeasureThread.interrupt();
        }

        // 中断正在执行的任务
        Future<?> future = runningTasks.remove(session.getId());
        if (future != null) {
            future.cancel(true);
        }

        Thread taskThread = taskThreads.remove(session.getId());
        if (taskThread != null) {
            taskThread.interrupt();
        }

        exceptionHandling();

        // 清理数据
        dataPointIndexMap.remove(session.getId());

        try {
            session.close();
        } catch (IOException e) {
            log.error("关闭会话异常", e);
        }
    }

    /**
     * 发送消息并关闭连接
     *
     * @param session
     * @param msg
     */
    private void sendAndClose(WebSocketSession session, String msg) {
        try {
            if (occupancyStatus) {
                // 释放线程
                deviceInfoService.releaseDevice(deviceSerialNumber);
            }
            session.sendMessage(new TextMessage(msg));
            sessions.remove(session.getId());
            session.close();
            // 清理数据
            dataPointIndexMap.remove(session.getId());
        } catch (IOException e) {
            log.error("发送消息并关闭连接异常", e);
        }
    }

    /**
     * 发送消息并关闭连接2
     *
     * @param session
     * @param msg
     */
    private void sendClose(WebSocketSession session, String msg) {
        try {
            session.sendMessage(new TextMessage(msg));
            sessions.remove(session.getId());
            session.close();
            // 清理数据
            dataPointIndexMap.remove(session.getId());
        } catch (IOException e) {
            log.error("发送关闭消息异常", e);
        }
    }

    private void runBusinessLogic(WebSocketSession session) throws InterruptedException {
        // 检查线程中断
        checkInterrupted(session);

        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String methodId = getQueryParam(query, "methodId");
        deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");

        if (methodId == null || methodId.equals("")) {
            sendClose(session, "方法编号不能为空");
            return;
        }
        if (deviceSerialNumber == null || deviceSerialNumber.equals("")) {
            sendClose(session, "设备编号不能为空");
            return;
        }

        //占用仪器
        R<Boolean> message = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);
        if (!message.getSuccess()) {
            sendClose(session, message.getMessage());
            return;
        }

        occupancyStatus = true;

        checkInterrupted(session);
        /// 设置参数
        Result result = spectrumService.setUpParametersPort(methodId, deviceSerialNumber);
        if (result.getCode() != 200) {
            sendAndClose(session, "设置参数失败");
            return;
        }

        // 根据方法id查询方法参数
        FMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            sendAndClose(session, "方法不存在");
            return;
        }
        // 将方法参数从JSON转换为FSpectrumParameter对象
        FSpectrumParameter param = JSONObject.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

        Constants.instrumentData.clear();
        Constants.measurd_data.clear();
        List<ChangeParam> dataList = new ArrayList<>();

        checkInterrupted(session);
        ip = deviceInfoService.getIp(deviceSerialNumber);
        Command command = buildCommand(SetParamEnum.BaseLine, new Object[]{});

        // 判断是否为极快扫描模式
        boolean isFastScan = "3".equals(param.getScanSpeed());
        if (isFastScan) {
            command = buildCommand(SetParamEnum.BaseLineFast, new Object[]{});
        }

        // 初始化数据点索引（适用于所有模式）
        dataPointIndexMap.put(session.getId(), 0);

        devMessageUtil.spectrumScanGetBaselineRealTime(command, ip);
        measureStatus = true;
        List<LinearSegment> linearSegment = formulaUtil.getLinearSegment(deviceSerialNumber);

        // 新增日志：开始收集数据
        log.info("开始基线数据收集, deviceSerialNumber: {}, 模式: {}", deviceSerialNumber, isFastScan ? "极快扫描" : "普通扫描");

        // 获取波长间隔参数
        double wavelengthInterval = Double.parseDouble(param.getWaveLengthInterval());
        double startWl = Double.parseDouble(param.getStartWaveLength());
        double endWl = Double.parseDouble(param.getEndWaveLength());
        boolean isDescending = startWl > endWl;

        // 计算期望的数据点数
        int expectedPoints = calculateExpectedDataPoints(startWl, endWl, wavelengthInterval);
        log.info("期望数据点数: {}, 波长范围: {} - {}, 间隔: {}", expectedPoints, startWl, endWl, wavelengthInterval);

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("线程被中断，终止基线任务");
                return;
            }

            // 处理测量数据 - 逐点处理
            while (!Constants.measurd_data.isEmpty()) {
                ChangeParam changeParam = Constants.measurd_data.remove(0);
                Double originalWavelength = Double.parseDouble(changeParam.getWl());
                Double correctedWavelength = formulaUtil.calculateWavelength(originalWavelength, linearSegment);

                // 对所有模式都进行强制修约
                Double constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                        wavelengthInterval, isDescending, session.getId());

                if (constrainedWavelength != null) {
                    // 立即推送修约后的波长（逐点推送）
                    pushBaselineDataPoint(session, constrainedWavelength);

                    // 存储修约后的数据
                    ChangeParam processedParam = new ChangeParam();
                    processedParam.setWl(constrainedWavelength.toString());
                    processedParam.setS(changeParam.getS());
                    processedParam.setR(changeParam.getR());
                    processedParam.setD(changeParam.getD());
                    dataList.add(processedParam);

                    log.debug("基线数据 - 原始波长: {}, 修约后波长: {}", correctedWavelength, constrainedWavelength);
                }
            }

            // 检查是否结束
            List<String> snapshot = new ArrayList<>(Constants.instrumentData);
            if (!snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData)) {
                measureStatus = false;

                // 检查数据完整性
                int actualPoints = dataPointIndexMap.get(session.getId());
                log.info("基线数据收集完成，实际数据点数: {}, 期望数据点数: {}", actualPoints, expectedPoints);

                if (actualPoints < expectedPoints * 0.8) {
                    log.warn("基线数据点数不足，可能采集不完整");
                }

                break;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // 检查数据是否为空
        if (dataList.isEmpty()) {
            log.error("基线数据为空，失败");
            sendAndClose(session, "基线数据为空，失败");
            return;
        }

        log.info("基线数据收集完成, dataList 大小: {}", dataList.size());

        checkInterrupted(session);
        FMeasureTask task;
        if (taskId != null && !taskId.equals("")) {
            task = tMeasureTaskService.getById(taskId);
        } else {
            task = new FMeasureTask();
            SystemUserManagePO user = systemUserManageDAO
                    .selectOne(new QueryWrapper<>(SystemUserManagePO.class).eq("user_name", method.getMethodCreator()));
            task.setUserId(user.getId());

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
                sendAndClose(session, "基线失败");
                return;
            }

            FMeasureTaskAttachment taskAttachment = new FMeasureTaskAttachment();
            taskAttachment.setTaskId(task.getId());
            boolean save = tMeasureTaskAttachmentService.save(taskAttachment);
            if (!save) {
                sendAndClose(session, "基线失败");
                return;
            }
        }

        checkInterrupted(session);
        task.setZeroParam(JSON.toJSONString(dataList));
        boolean update = tMeasureTaskService.updateById(task);
        if (!update) {
            sendAndClose(session, "基线失败");
            return;
        }

        Constants.measureStatus = true;
        sendAndClose(session, "基线成功");
    }

    /**
     * 强制约束波长值，确保符合设定的波长间隔和范围
     * 无论原始波长是多少，都强制使用期望的波长值
     * @param originalWl 原始波长值
     * @param startWl 起始波长
     * @param endWl 结束波长
     * @param interval 波长间隔
     * @param isDescending 是否递减扫描
     * @param sessionId 会话ID
     * @return 修正后的波长值
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
            log.debug("基线波长强制修约: 原始值={}, 修约值={}, 期望值={}, 索引={}",
                    originalWl, constrainedWl, expectedWl, currentIndex);
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
        log.info("计算期望数据点数: 范围={}-{}, 间隔={}, 点数={}", startWl, endWl, interval, points);
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
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(payload.toJSONString()));
            } catch (IOException e) {
                log.error("推送基线数据异常", e);
            }
        }
    }

    // 中断检测
    private void checkInterrupted(WebSocketSession session) throws InterruptedException {
        if (Thread.currentThread().isInterrupted() || !session.isOpen()) {
            throw new InterruptedException("任务被中断或连接关闭");
        }
    }

    /**
     * 获取查询参数
     *
     * @param query
     * @param key
     * @return
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
     *
     * @param cmdEnum
     * @param value
     * @return
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