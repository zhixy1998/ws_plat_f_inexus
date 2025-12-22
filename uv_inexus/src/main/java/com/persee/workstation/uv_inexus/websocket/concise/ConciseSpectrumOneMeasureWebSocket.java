/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.concise;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.LinearSegment;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ConciseSpectrumOneMeasureWebSocket extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ConciseSpectrumOneMeasureWebSocket.class);

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();
    // 会话状态管理
    private static final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();
    // WebSocket消息发送队列，避免并发发送冲突
    private static final Map<String, Object> sessionSendLocks = new ConcurrentHashMap<>();

    // 存储状态跟踪
    private static final Map<String, AtomicInteger> pendingStorages = new ConcurrentHashMap<>();
    private static final Map<String, CountDownLatch> storageLatches = new ConcurrentHashMap<>();

    private static ChangeParam lastChangeParam;

    @Autowired
    private SpectrumConciseService spectrumService;
    @Autowired
    private FMeasureTaskService tMeasureTaskService;
    @Autowired
    private DevMessageUtil devMessageUtil;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private FMeasureDataService traceDataService;
    @Autowired
    private FMeasureMethodService tMeasureMethodService;
    @Autowired
    private SystemAlgorithmConfService systemAlgorithmConfService;
    @Autowired
    private FSamplePoolConfigService fSamplePoolConfigService;
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Autowired
    private FMeasureDataService tMeasureDataService;
    @Autowired
    private FMeasureSampleService tMeasureSampleService;
    @Autowired
    private FMeasureAnalysisDataService tMeasureAnalysisDataService;
    @Autowired
    private FMeasureAnalysisImageService tMeasureAnalysisImageService;
    @Autowired
    private FormulaUtil formulaUtil;
    // 注入PythonPathConstant
    @Autowired
    private PythonPathConstant pythonPathConstant;

    // 用于存储原始Y值（未经格式化）
    private Map<String, List<Double>> originalYValues = new ConcurrentHashMap<>();

    // 从yaml配置文件读取台阶消除批次大小
    @Value("${staircase-elimination.data-count}")
    private Integer staircaseEliminationBatchSize;

    /**
     * 会话状态管理类
     */
    private static class SessionState {
        private final AtomicBoolean deviceOccupied = new AtomicBoolean(false);
        private final AtomicBoolean measurementStarted = new AtomicBoolean(false);
        private final AtomicBoolean stopCommandSent = new AtomicBoolean(false);
        private final AtomicBoolean stopCommandSent2 = new AtomicBoolean(false);
        private final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);
        private final AtomicBoolean sessionClosing = new AtomicBoolean(false);
        // 停止请求标志
        private final AtomicBoolean stopRequested = new AtomicBoolean(false);
        // 当前测量完成标志
        private final AtomicBoolean currentMeasurementCompleted = new AtomicBoolean(true);

        // 批次数据缓冲区
        private final List<Double> batchXBuffer = Collections.synchronizedList(new ArrayList<>());
        private final List<Double> batchYBuffer = Collections.synchronizedList(new ArrayList<>());

        // 已推送点计数
        private final AtomicInteger pushedPointsCount = new AtomicInteger(0);

        private String deviceSerialNumber;
        private String ip;
        private Integer digit;
        private Integer batchSize; // 批次大小

        public void reset() {
            deviceOccupied.set(false);
            measurementStarted.set(false);
            stopCommandSent.set(false);
            stopCommandSent2.set(false);
            cleanupInProgress.set(false);
            sessionClosing.set(false);
            stopRequested.set(false);
            currentMeasurementCompleted.set(true);
            batchXBuffer.clear();
            batchYBuffer.clear();
            pushedPointsCount.set(0);
            deviceSerialNumber = null;
            ip = null;
            digit = null;
            batchSize = null;
        }

        /**
         * 设置批次大小
         */
        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
            log.info("设置批次大小: {}", batchSize);
        }

        /**
         * 获取批次大小
         */
        public Integer getBatchSize() {
            return batchSize != null ? batchSize : 10; // 默认10
        }

        /**
         * 添加数据点到批次缓冲区
         */
        public boolean addToBatchBuffer(Double x, Double y) {
            synchronized (batchXBuffer) {
                batchXBuffer.add(x);
                batchYBuffer.add(y);
                return batchXBuffer.size() >= getBatchSize();
            }
        }

        /**
         * 获取并清空批次缓冲区
         */
        public BatchData getAndClearBatchBuffer(String sessionId, Integer count, String sampleId,
                                                Integer digit, FSpectrumParameter parameter,
                                                FMeasureTask task) {
            synchronized (batchXBuffer) {
                if (batchXBuffer.isEmpty()) {
                    return null;
                }
                int startIndex = pushedPointsCount.get();
                BatchData batchData = new BatchData(
                        new ArrayList<>(batchXBuffer),
                        new ArrayList<>(batchYBuffer),
                        startIndex,
                        sessionId,
                        count,
                        sampleId,
                        digit,
                        parameter,
                        task
                );
                batchXBuffer.clear();
                batchYBuffer.clear();
                return batchData;
            }
        }

        /**
         * 获取剩余数据（测量结束时使用）
         */
        public BatchData getRemainingBatchBuffer(String sessionId, Integer count, String sampleId,
                                                 Integer digit, FSpectrumParameter parameter,
                                                 FMeasureTask task) {
            return getAndClearBatchBuffer(sessionId, count, sampleId, digit, parameter, task);
        }

        /**
         * 增加已推送点数
         */
        public void incrementPushedPoints(int count) {
            pushedPointsCount.addAndGet(count);
        }
    }

    /**
     * 批次数据容器
     */
    private static class BatchData {
        List<Double> xBatch;
        List<Double> yBatch;
        int startIndex;
        String sessionId;
        Integer count;
        String sampleId;
        Integer digit;
        FSpectrumParameter parameter;
        FMeasureTask task;

        public BatchData(List<Double> xBatch, List<Double> yBatch, int startIndex,
                         String sessionId, Integer count, String sampleId, Integer digit,
                         FSpectrumParameter parameter, FMeasureTask task) {
            this.xBatch = new ArrayList<>(xBatch);
            this.yBatch = new ArrayList<>(yBatch);
            this.startIndex = startIndex;
            this.sessionId = sessionId;
            this.count = count;
            this.sampleId = sampleId;
            this.digit = digit;
            this.parameter = parameter;
            this.task = task;
        }
    }

    /**
     * 当WebSocket连接建立后调用此方法
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("客户端进入: {} 时间: {}", session.getId(), LocalDateTime.now());

        // 初始化会话状态
        SessionState state = new SessionState();
        sessionStates.put(session.getId(), state);
        // 设置批次大小
        state.setBatchSize(staircaseEliminationBatchSize);

        // 初始化发送锁
        sessionSendLocks.put(session.getId(), new Object());

        sessions.put(session.getId(), session);
        clearGlobalData();

        if (Constants.endTime != null || Constants.startTime != null) {
            sendClose(session, "当前测量未结束,无法开启新的测试");
            return;
        }

        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String sampleId = getQueryParam(query, "sampleId");
        String analysisNumber = getQueryParam(query, "analysisNumber");
        String deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");
        String updateBy = getQueryParam(query, "updateBy");
        Integer digit = Integer.parseInt(getQueryParam(query, "digit"));

        if (taskId == null) {
            sendClose(session, "缺少 taskId 参数");
            return;
        }
        if (sampleId == null) {
            sendClose(session, "缺少 sampleId 参数");
            return;
        }
        if (analysisNumber == null) {
            sendClose(session, "缺少 analysisNumber 参数");
            return;
        }
        if (deviceSerialNumber == null) {
            sendClose(session, "缺少 deviceSerialNumber 参数");
            return;
        }
        if (updateBy == null) {
            sendClose(session, "缺少 updateBy 参数");
            return;
        }

        // 设置会话状态
        state.deviceSerialNumber = deviceSerialNumber;
        state.digit = digit;

        // 根据任务id查询任务数据
        FMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null || task.getSampleKey() == null || task.getSampleKey().equals("")) {
            sendClose(session, "请先创建样品");
            return;
        }

        //占用仪器
        R<Boolean> message = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);
        if (!message.getSuccess()) {
            sendClose(session, message.getMessage());
            return;
        }
        state.deviceOccupied.set(true);

        /// 设置参数
        Result result = spectrumService.setUpParametersPort(task.getMethodKey(), deviceSerialNumber);
        if (result.getCode() != 200) {
            sendAndClose(session, "设置参数失败");
            return;
        }

        // 根据方法id查询方法数据
        FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 将方法参数提取出来
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

        String ip = deviceInfoService.getIp(deviceSerialNumber);
        state.ip = ip;

        // 根据序列号查询对应的附件配置表信息
        QueryWrapper<FSamplePoolConfigPO> wrapper = new QueryWrapper<>();
        wrapper.eq("device_serial_number", deviceSerialNumber);
        FSamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);
        if (configPO == null) {
            sendAndClose(session, "没有找到对应的附件配置信息");
            return;
        }

        boolean poolStatus = false;
        if (configPO.getIsAutoChangePool()) {
            List<FSamplePoolStatus> poolStatuses = com.alibaba.fastjson.JSON.parseArray(configPO.getPoolInfo(), FSamplePoolStatus.class);
            for (FSamplePoolStatus status : poolStatuses) {
                if (status.getPoolStatus()) {
                    poolStatus = true;
                    break;
                }
            }
        }

        /// 设置样品池位置
        FMeasureSample sample = tMeasureSampleService.getById(sampleId);
        if (configPO.getIsAutoChangePool()) {
            if (sample == null) {
                sendAndClose(session, "请先创建样品");
                return;
            }

            if (poolStatus) {
                R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
                if (!stringR.getSuccess()) {
                    sendAndClose(session, "设置出错");
                    return;
                }
                fSamplePoolConfigService.updateCurrentPoolNum(deviceSerialNumber, Integer.parseInt(sample.getSampleLocation()), updateBy);
                JSONObject object = new JSONObject();
                object.put("currentPoolNum", sample.getSampleLocation());
                sendMessageSafe(session, object.toJSONString());
            }
        }

        Command command = buildCommand(SetParamEnum.Scan, new Object[]{});
        if (spectrumParameter.getScanSpeed().equals("3")) {
            command = buildCommand(SetParamEnum.ScanFast, new Object[]{});
        }

        // 启动测量任务线程
        Command finalCommand = command;
        Thread taskThread = new Thread(() -> {
            try {
                Integer number = Integer.valueOf(analysisNumber);
                doOneMeasurementSerial(session, finalCommand, spectrumParameter, task, taskId, sampleId, deviceSerialNumber, ip, number);

                sendMessageSafe(session, "扫描成功");
                // 发送完成消息
                JSONObject payload = new JSONObject();
                payload.put("result", "finish");
                sendAndClose(session, payload.toJSONString());

            } catch (Exception e) {
                log.error("单次测量异常", e);
                sendMessageSafe(session, "执行异常: " + e.getMessage());
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

        if ("close".equals(message.getPayload())){
            handleStopRequest(session);
        }
    }

    /**
     * 处理停止请求
     */
    private void handleStopRequest(WebSocketSession session) {
        String sessionId = session.getId();
        SessionState state = sessionStates.get(sessionId);

        if (state == null || state.cleanupInProgress.getAndSet(true)) {
            // 已经在清理中或状态为空，避免重复清理
            return;
        }

        try {
            log.info("开始清理资源, session: {}", sessionId);

            sendMessageSafe(session, "停止成功");

            // 1. 标记会话正在关闭，阻止新的消息发送
            state.sessionClosing.set(true);

            state.stopCommandSent.set(true);

            // 2. 发送停止命令（参考前端close逻辑）
            sendStopCommandIfNeeded(session);

            // 3. 停止数据接收线程（关键：先停止接收数据）
            stopDataReceivingThread();


            // 4. 中断任务线程（参考前端close逻辑）
            Thread thread = taskThreads.get(sessionId);
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
                log.info("已中断任务线程, session: {}", sessionId);

                // 等待线程结束，但不要等太久
                try {
                    thread.join(1000); // 等待1秒
                    log.info("任务线程已停止, session: {}", sessionId);
                } catch (InterruptedException e) {
                    log.warn("等待任务线程结束被中断, session: {}", sessionId);
                    Thread.currentThread().interrupt();
                }
            }

            // 5. 释放设备（关键步骤）
            if (state.deviceOccupied.compareAndSet(true, false)) {
                if (state.deviceSerialNumber != null) {
                    try {
                        // 不再使用Thread.sleep(500)，改为检查线程状态
                        int waitCount = 0;
                        while (Constants.sendMeasureThread != null &&
                                Constants.sendMeasureThread.isAlive() &&
                                waitCount < 10) { // 最多等待5秒
                            try {
                                Thread.sleep(500);
                                waitCount++;
                            } catch (InterruptedException e) {
                                log.warn("等待数据接收线程时被中断, session: {}", sessionId);
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }

                        deviceInfoService.releaseDevice(state.deviceSerialNumber);
                        log.info("设备释放成功: {}, session: {}", state.deviceSerialNumber, sessionId);
                    } catch (Exception e) {
                        log.error("释放设备异常, session: {}", sessionId, e);
                    }
                }
            }

            // 6. 清理数据点索引
            pendingStorages.remove(sessionId);
            storageLatches.remove(sessionId);

            // 7. 清理发送锁
            sessionSendLocks.remove(sessionId);


            // 8. 清空全局数据
            clearGlobalData();

            // 9. 清空原始Y值存储
            originalYValues.entrySet().removeIf(entry -> entry.getKey().startsWith(sessionId + "_"));



            // 10. 从会话管理中移除
            sessionStates.remove(sessionId);
            sessions.remove(sessionId);

            log.info("资源清理完成, session: {}", sessionId);

        } catch (Exception e) {
            log.error("清理资源异常, session: {}", sessionId, e);
        }
    }


    /**
     * 如果需要，发送停止命令
     */
    private void sendStopCommandIfNeeded(WebSocketSession session) {
        String sessionId = session.getId();
        SessionState state = sessionStates.get(sessionId);
        if (state == null) {
            return;
        }

        // 只有在测量已开始且停止命令未发送过的情况下才发送停止命令
        if (state.measurementStarted.get() && state.stopCommandSent.getAndSet(true) && !state.stopCommandSent2.getAndSet(true)) {
            if (state.ip != null) {
                try {
                    log.info("【发送设备停止】向设备发送停止命令, session: {}, ip: {}", sessionId, state.ip);
                    R<String> stopResult = devMessageUtil.stop(buildCommand(SetParamEnum.Stop, new Object[]{}), state.ip);
                    if (stopResult.getSuccess()) {
                        log.info("【设备停止成功】设备停止命令发送成功, session: {}", sessionId);
                    } else {
                        log.error("【设备停止失败】设备停止命令发送失败, session: {}", sessionId);
                    }
                } catch (Exception e) {
                    log.error("【发送停止异常】发送停止命令异常, session: {}", sessionId, e);
                }
            } else {
                log.warn("【无IP地址】无法发送停止命令，IP地址为空, session: {}", sessionId);
            }
        } else {
            log.info("【无需停止】测量未开始或停止命令已发送, session: {}, measurementStarted: {}, stopCommandSent: {}",
                    sessionId, state.measurementStarted.get(), state.stopCommandSent.get());
        }
    }

    /**
     * 停止数据接收线程
     */
    private void stopDataReceivingThread() {
        try {
            Thread dataReceivingThread = Constants.sendMeasureThread;
            if (dataReceivingThread != null && dataReceivingThread.isAlive()) {
                log.info("【停止数据线程】开始停止数据接收线程");

                // 中断数据接收线程
                dataReceivingThread.interrupt();

                // 等待线程结束，但不要等太久
                try {
                    dataReceivingThread.join(500); // 等待500ms
                    log.info("【数据线程停止】数据接收线程已停止");
                } catch (InterruptedException e) {
                    log.warn("【等待线程中断】等待数据接收线程结束被中断");
                    // 重新中断当前线程
                    Thread.currentThread().interrupt();
                }
            } else {
                log.info("【数据线程状态】数据接收线程不存在或未运行");
            }

            log.info("【数据线程清理】数据接收线程引用已清理");

        } catch (Exception e) {
            log.error("【停止数据线程异常】停止数据接收线程异常", e);
        }
    }

    /**
     * 连接关闭时触发
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("客户端退出: {} 时间: {} 原因: {}", session.getId(), LocalDateTime.now(), status);
        cleanupResources(session);
    }

    /**
     * 连接异常时触发
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("连接异常, session: {}, 异常: {}", session.getId(), exception.getMessage(), exception);
        cleanupResources(session);
    }

    /**
     * 清理资源
     */
    private void cleanupResources(WebSocketSession session) {
        String sessionId = session.getId();
        SessionState state = sessionStates.get(sessionId);

        if (state == null || state.cleanupInProgress.getAndSet(true)) {
            // 已经在清理中或状态为空，避免重复清理
            return;
        }

        try {
            log.info("开始清理资源, session: {}", sessionId);

            // 1. 标记会话正在关闭，阻止新的消息发送
            state.sessionClosing.set(true);

            // 2. 停止数据接收线程
            stopDataReceivingThread();

            // 3. 清理线程引用
            Thread taskThread = taskThreads.remove(sessionId);
            if (taskThread != null && taskThread.isAlive()) {
                taskThread.interrupt();
            }

            // 4. 释放设备（关键步骤）
            if (state.deviceOccupied.compareAndSet(true, false)) {
                if (state.deviceSerialNumber != null) {
                    try {
                        // 等待测量线程完全停止
                        while (Constants.sendMeasureThread != null && Constants.sendMeasureThread.isAlive()) {
                            Thread.sleep(500);
                        }
                        deviceInfoService.releaseDevice(state.deviceSerialNumber);
                        log.info("设备释放成功: {}, session: {}", state.deviceSerialNumber, sessionId);
                    } catch (Exception e) {
                        log.error("释放设备异常, session: {}", sessionId, e);
                    }
                }
            }

            // 5. 清理数据点索引
            pendingStorages.remove(sessionId);
            storageLatches.remove(sessionId);

            // 6. 清理发送锁
            sessionSendLocks.remove(sessionId);

            // 7. 从会话管理中移除
            sessionStates.remove(sessionId);
            sessions.remove(sessionId);

            // 8. 清空全局数据
            clearGlobalData();

            // 9. 清空原始Y值存储
            originalYValues.entrySet().removeIf(entry -> entry.getKey().startsWith(sessionId + "_"));

            log.info("资源清理完成, session: {}", sessionId);

        } catch (Exception e) {
            log.error("清理资源异常, session: {}", sessionId, e);
        }
    }

    /**
     * 同步发送消息 - 避免WebSocket并发发送冲突
     */
    private void sendMessageSynchronized(WebSocketSession session, String message) {
        SessionState state = sessionStates.get(session.getId());
        if (state != null && state.sessionClosing.get()) {
            return;
        }

        Object sendLock = sessionSendLocks.get(session.getId());
        if (sendLock == null) {
            return;
        }

        synchronized (sendLock) {
            if (state != null && state.sessionClosing.get()) {
                return;
            }

            if (session.isOpen()) {
                try {
                    // 确保线程没有被中断
                    boolean wasInterrupted = Thread.interrupted();
                    session.sendMessage(new TextMessage(message));
                    // 如果之前是中断状态，恢复中断状态
                    if (wasInterrupted) {
                        Thread.currentThread().interrupt();
                    }
                } catch (IOException e) {
                    log.debug("发送消息失败，连接可能已关闭, session: {}, 消息: {}", session.getId(), message);
                }
            }
        }
    }

    /**
     * 发送消息（安全的，不会抛出异常）
     */
    private void sendMessageSafe(WebSocketSession session, String message) {
        sendMessageSynchronized(session, message);
    }

    /**
     * 发送消息并关闭连接
     */
    private void sendAndClose(WebSocketSession session, String msg) {
        try {
            // 先发送消息
            sendMessageSafe(session, msg);
        } finally {
            // 确保资源被清理
            cleanupResources(session);
            // 最后关闭会话
            closeSessionQuietly(session);
        }
    }

    /**
     * 发送消息并关闭连接2
     */
    private void sendClose(WebSocketSession session, String msg) {
        try {
            sendMessageSafe(session, msg);
        } finally {
            cleanupResources(session);
            closeSessionQuietly(session);
        }
    }

    /**
     * 安静关闭会话
     */
    private void closeSessionQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                // 在关闭前标记会话正在关闭
                SessionState state = sessionStates.get(session.getId());
                if (state != null) {
                    state.sessionClosing.set(true);
                }
                session.close();
            }
        } catch (IOException e) {
            log.debug("关闭会话异常（连接可能已关闭）, session: {}", session.getId());
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
     * 执行单次测量序列
     */
    private void doOneMeasurementSerial(WebSocketSession session,
                                        Command command,
                                        FSpectrumParameter spectrumParameter,
                                        FMeasureTask task,
                                        String taskId,
                                        String sampleId,
                                        String deviceSerialNumber,
                                        String ip,
                                        Integer number) throws IOException {
        // 1. 抢锁
        try {
            Constants.MEASURE_LOCK.lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        MeasurementResult result = null;
        try {
            // 设置测量开始状态
            SessionState state = sessionStates.get(session.getId());
            if (state != null) {
                state.measurementStarted.set(true);
            }

            // 清空全局缓存
            Constants.instrumentData.clear();
            Constants.measurd_data.clear();
            Constants.originalCalculationData.clear();
            Constants.startTime = LocalDateTime.now();
            Constants.endTime = null;

            // 采集数据 & 返回测量结果
            result = doOneMeasurementCore(session, command, spectrumParameter, task, taskId, sampleId, deviceSerialNumber, ip, number);

            // 立即设置结束时间，表示测量完成
            Constants.endTime = LocalDateTime.now();

        } finally {
            // 确保即使发生异常也能释放资源
            if (result == null) {
                result = new MeasurementResult(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Constants.startTime,
                        Constants.endTime != null ? Constants.endTime : LocalDateTime.now(),
                        new ArrayList<>(Constants.instrumentData),
                        new ArrayList<>(Constants.originalCalculationData)
                );
            }

            // 标记测量结束
            SessionState state = sessionStates.get(session.getId());
            if (state != null) {
                state.measurementStarted.set(false);
            }

            // 立即清空缓存，释放锁
            Constants.instrumentData.clear();
            Constants.measurd_data.clear();
            Constants.originalCalculationData.clear();
            Constants.startTime = null;
            Constants.endTime = null;
            Constants.MEASURE_LOCK.unlock();
        }

        // 同步保存数据，等待保存完成
        String storageKey = session.getId() + "_" + sampleId + "_" + number;
        CountDownLatch latch = new CountDownLatch(1);
        storageLatches.put(storageKey, latch);

        // 保存数据
        saveOneMeasurementWithSnapshots(task, taskId, sampleId, deviceSerialNumber,
                result.x, result.y, session, number, result.startTime, result.endTime,
                result.instrumentSnapshot, result.originalCalcSnapshot, latch);

        // 等待存储完成
        try {
            boolean stored = latch.await(30, TimeUnit.SECONDS);
            if (!stored) {
                log.warn("数据存储超时，sampleId: {}, count: {}", sampleId, number);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待数据存储时被中断", e);
        } finally {
            storageLatches.remove(storageKey);
        }

        // 存储完成后才通知前端
        JSONObject finishMessage = new JSONObject();
        finishMessage.put("result", "measurement_complete");
        finishMessage.put("sampleId", sampleId);
        finishMessage.put("count", number);

        if (session.isOpen()) {
            sendMessageSafe(session, finishMessage.toJSONString());
        }
    }

    /**
     * 执行单次测量核心逻辑
     */
    private MeasurementResult doOneMeasurementCore(WebSocketSession session,
                                                   Command command,
                                                   FSpectrumParameter spectrumParameter,
                                                   FMeasureTask task,
                                                   String taskId,
                                                   String sampleId,
                                                   String deviceSerialNumber,
                                                   String ip,
                                                   Integer number) throws IOException {


        // 发送测量命令
        devMessageUtil.getMeasureData(command, ip);
        Constants.startTime = LocalDateTime.now();
        isEndData = false;

        SessionState state = sessionStates.get(session.getId());
        if (state != null) {
            state.measurementStarted.set(true);
        }

        List<ChangeParam> baselineCopy = (task.getZeroParam() != null && !task.getZeroParam().isEmpty())
                ? JSON.parseArray(task.getZeroParam(), ChangeParam.class)
                : null;

        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();

        // 判断是否为极快扫描模式
        boolean isFastScan = "3".equals(spectrumParameter.getScanSpeed());

        if (isFastScan) {
            // 极快扫描模式：优化版本
            ProcessedData processedData = processAndPushFastScanData(session, spectrumParameter, x, y, number,
                    task, sampleId, spectrumParameter.getLuminosity(), spectrumParameter.getLadderCorrection(), deviceSerialNumber);

            // 立即完成测量
            Constants.endTime = LocalDateTime.now();
            if (state != null) {
                state.measurementStarted.set(false);
            }

            // 返回处理后的数据
            return new MeasurementResult(
                    processedData.x,
                    processedData.y,
                    Constants.startTime,
                    Constants.endTime,
                    new ArrayList<>(Constants.instrumentData),
                    new ArrayList<>(Constants.originalCalculationData)
            );
        } else {
            // 普通扫描模式：使用批次处理机制
            boolean endWaveLengthState = false;

            try {
                while (true) {
                    if (Thread.currentThread().isInterrupted() || !session.isOpen()) {
                        log.info("线程被中断或会话关闭，退出测量循环");
                        // 立即发送停止命令
                        sendStopCommandIfNeeded(session);
                        break;
                    }

                    // 检查会话状态
                    if (state != null && (state.sessionClosing.get() || state.stopCommandSent.get())) {
                        log.info("会话正在关闭或停止命令已发送，退出测量循环");
                        break;
                    }

                    // 采集 & 计算一个点
                    List<String> snapshot = new ArrayList<>(Constants.instrumentData);
                    boolean hasEndData = !snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData);

                    if (hasEndData && snapshot.get(0).equals(Constants.EndData)) {
                        log.info("检测到结束标志，准备结束测量");
                        break;
                    }

                    // 处理所有可用的数据点
                    while (!Constants.measurd_data.isEmpty()) {
                        if (Thread.currentThread().isInterrupted()) {
                            log.info("线程被中断，退出内部循环");
                            break;
                        }

                        calcOnePoint(spectrumParameter, baselineCopy, x, y, deviceSerialNumber, session);

                        if (x.size()>2){
                            Double x_endOne = x.get(x.size() - 1);
                            Double x_endTwo = x.get(x.size() - 2);
                            if (x_endOne.equals(x_endTwo)) {
                                x.remove(x.size() - 1);
                                y.remove(y.size() - 1);
                                endWaveLengthState = true;
                            }
                        }

                        // 将新点添加到批次缓冲区，并检查是否达到批次大小
                        if (!x.isEmpty() && !y.isEmpty() && !endWaveLengthState) {
                            int currentIndex = x.size() - 1;
                            Double currentX = x.get(currentIndex);
                            Double currentY = y.get(currentIndex);

                            boolean isBatchFull = state.addToBatchBuffer(currentX, currentY);

                            if (isBatchFull) {
                                // 批次已满，进行处理
                                BatchData batchData = state.getAndClearBatchBuffer(
                                        session.getId(), number, sampleId, state.digit,
                                        spectrumParameter, task);

                                if (batchData != null) {
                                    // 异步处理台阶消除
                                    processBatchDataAsync(batchData, session);
                                }
                            }
                        }
                    }

                    // 检查是否应该结束（基于结束标志）
                    if (hasEndData) {
                        log.info("检测到结束标志，结束数据采集");
                        break;
                    }

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.info("测量线程被中断");
                        break;
                    }
                }

                // 测量结束，处理所有剩余数据
                log.info("开始处理剩余数据，批次大小配置: {}", state.getBatchSize());

                // 处理剩余的批次数据
                BatchData remainingBatch = state.getRemainingBatchBuffer(
                        session.getId(), number, sampleId, state.digit,
                        spectrumParameter, task);

                if (remainingBatch != null && !remainingBatch.xBatch.isEmpty()) {
                    log.info("处理剩余批次数据，点数: {}", remainingBatch.xBatch.size());
                    processBatchDataAsync(remainingBatch, session);
                }

                // 等待所有异步处理完成
                waitForAsyncProcessingComplete(state, session);

                log.info("测量完成，总数据点: " + x.size());

            } catch (Exception e) {
                log.error("测量过程中发生异常", e);
                throw e;
            }
        }

        Constants.endTime = LocalDateTime.now();
        if (state != null) {
            state.measurementStarted.set(false);
        }

        // 数据完整性检查
        log.info("=== 测量结果统计 ===");
        log.info("总数据点数 - X: " + x.size() + ", Y: " + y.size());
        log.info("测量时间 - 开始: " + Constants.startTime + ", 结束: " + Constants.endTime);
        log.info("仪器数据快照大小: " + Constants.instrumentData.size());
        log.info("原始计算数据快照大小: " + Constants.originalCalculationData.size());
        log.info("阶梯校正状态: " + spectrumParameter.getLadderCorrection());

        // 返回快照 + x/y
        return new MeasurementResult(
                x,
                y, // 这里的y已经是处理后的数据（校正或原始）
                Constants.startTime,
                Constants.endTime,
                new ArrayList<>(Constants.instrumentData),
                new ArrayList<>(Constants.originalCalculationData)
        );
    }


    /**
     * 异步处理批次数据（包含台阶消除）
     */
    private void processBatchDataAsync(BatchData batchData, WebSocketSession session) {
        // 使用线程池异步处理
        CompletableFuture.runAsync(() -> {
            try {
                log.info("异步处理批次数据，点数: {}, 起始索引: {}, 批次大小配置: {}",
                        batchData.xBatch.size(), batchData.startIndex, staircaseEliminationBatchSize);

                boolean ladderCorrection = batchData.parameter.getLadderCorrection();
                List<Double> processedY = batchData.yBatch;
                List<Double> yForStorage = batchData.yBatch;

                // 执行台阶消除
                if (ladderCorrection) {
                    log.info("执行台阶消除，数据点数: " + batchData.xBatch.size());
                    processedY = processAndSaveData(batchData.task, batchData.xBatch, batchData.yBatch,
                            batchData.parameter, batchData.sampleId);
                    log.info("台阶消除完成");

                    // 开启阶梯校正时，存储校正后的数据
                    yForStorage = processedY;
                } else {
                    log.info("跳过台阶消除");
                }

                // 根据是否开启阶梯校正来决定存储的数据
                String key = batchData.sessionId + "_" + batchData.sampleId + "_" + batchData.count;
                synchronized (originalYValues) {
                    if (!originalYValues.containsKey(key)) {
                        // 根据阶梯校正状态决定存储的数据
                        originalYValues.put(key, new ArrayList<>(yForStorage));
                    } else {
                        List<Double> existingY = originalYValues.get(key);
                        // 确保不重复添加
                        if (existingY.size() < batchData.startIndex + yForStorage.size()) {
                            for (int i = existingY.size(); i < batchData.startIndex; i++) {
                                existingY.add(0.0); // 填充空缺
                            }
                            // 根据阶梯校正状态决定存储的数据
                            existingY.addAll(yForStorage);
                        }
                    }
                }

                // 推送处理后的数据
                pushBatchDataDirectly(session, batchData.xBatch, processedY,
                        batchData.count, batchData.sampleId,
                        batchData.parameter.getLuminosity(), batchData.digit);

                // 更新已推送点数
                SessionState state = sessionStates.get(batchData.sessionId);
                if (state != null) {
                    state.incrementPushedPoints(batchData.xBatch.size());
                }

                log.info("批次数据推送完成，点数: " + batchData.xBatch.size());
            } catch (Exception e) {
                log.error("处理批次数据异常", e);
                // 如果发生异常，尝试推送原始数据
                try {
                    pushBatchDataDirectly(session, batchData.xBatch, batchData.yBatch,
                            batchData.count, batchData.sampleId,
                            batchData.parameter.getLuminosity(), batchData.digit);
                } catch (IOException ioException) {
                    log.error("推送原始数据也失败", ioException);
                }
            }
        });
    }

    /**
     * 直接推送批次数据（不进行台阶消除）
     */
    private void pushBatchDataDirectly(WebSocketSession session,
                                       List<Double> xBatch,
                                       List<Double> yBatch,
                                       Integer count,
                                       String sampleId,
                                       String luminosity,
                                       Integer digit) throws IOException {
        if (xBatch.isEmpty() || yBatch.isEmpty()) {
            return;
        }

        SessionState state = sessionStates.get(session.getId());
        if (state == null || state.sessionClosing.get()) {
            return;
        }

        // 逐个推送数据点
        for (int i = 0; i < xBatch.size(); i++) {
            if (!session.isOpen()) {
                log.warn("会话已关闭，停止推送数据");
                break;
            }

            pushSinglePoint(session, xBatch.get(i), yBatch.get(i),
                    count, sampleId, luminosity, digit);

            // 添加小延迟，避免推送过快
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("推送线程被中断");
                break;
            }
        }
    }

    /**
     * 等待所有异步处理完成
     */
    private void waitForAsyncProcessingComplete(SessionState state, WebSocketSession session) {
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30秒超时

        try {
            // 这里可以根据实际情况实现等待逻辑
            // 例如，等待所有CompletableFuture完成
            Thread.sleep(1000); // 简单等待1秒

            log.info("异步处理等待完成，耗时: {}ms", System.currentTimeMillis() - startTime);
        } catch (InterruptedException e) {
            log.warn("等待异步处理完成时被中断");
            Thread.currentThread().interrupt();
        }
    }


    /**
     * 处理并推送一批数据并返回处理后的Y值
     */
    private List<Double> processAndPushBatchDataAndReturn(WebSocketSession session,
                                                          List<Double> xBatch,
                                                          List<Double> yBatch,
                                                          int startIndex,
                                                          Integer count,
                                                          FMeasureTask task,
                                                          String sampleId,
                                                          FSpectrumParameter parameter,
                                                          Integer digit) throws IOException {
        if (xBatch.isEmpty() || yBatch.isEmpty()) {
            return new ArrayList<>();
        }

        String luminosity = parameter.getLuminosity();
        boolean ladderCorrection = parameter.getLadderCorrection();

        log.info("处理批次数据，点数: " + xBatch.size() + ", 起始索引: " + startIndex + ", 台阶消除: " + ladderCorrection);

        List<Double> processedY = yBatch;
        List<Double> yForStorage = yBatch; // 用于存储的Y值

        // 如果需要台阶校正，处理数据
        if (ladderCorrection) {
            log.info("执行台阶消除，数据点数: " + xBatch.size());
            processedY = processAndSaveData(task, xBatch, yBatch, parameter, sampleId);
            log.info("台阶消除完成");
            // 开启阶梯校正时，存储校正后的数据
            yForStorage = processedY;
        } else {
            log.info("跳过台阶消除");
            // 未开启阶梯校正时，存储原始数据
            yForStorage = yBatch;
        }

        // 关键修改：根据是否开启阶梯校正来决定存储的数据
        String key = session.getId() + "_" + sampleId + "_" + count;
        synchronized (originalYValues) {
            if (!originalYValues.containsKey(key)) {
                // 根据阶梯校正状态决定存储的数据
                originalYValues.put(key, new ArrayList<>(yForStorage));
            } else {
                List<Double> existingY = originalYValues.get(key);
                // 确保不重复添加
                if (existingY.size() < startIndex + yForStorage.size()) {
                    for (int i = existingY.size(); i < startIndex; i++) {
                        existingY.add(0.0); // 填充空缺
                    }
                    // 根据阶梯校正状态决定存储的数据
                    existingY.addAll(yForStorage);
                }
            }
        }

        // 逐个推送数据点 - 使用处理后的数据（校正或原始）
        for (int i = 0; i < xBatch.size(); i++) {
            if (!session.isOpen()) {
                log.warn("会话已关闭，停止推送数据");
                break;
            }

            pushSinglePoint(session, xBatch.get(i), processedY.get(i),
                    count, sampleId, luminosity, digit);

            // 添加小延迟，避免推送过快
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("推送线程被中断");
                break;
            }
        }

        log.info("批次数据推送完成，点数: " + xBatch.size());

        // 返回处理后的Y值
        return processedY;
    }


    private ProcessedData processAndPushFastScanData(WebSocketSession session,
                                                     FSpectrumParameter spectrumParameter,
                                                     List<Double> x, List<Double> y,
                                                     Integer number, FMeasureTask task,
                                                     String sampleId, String luminosity,
                                                     boolean ladderCorrection,
                                                     String deviceSerialNumber) throws IOException {

        log.info("开始极快扫描模式数据处理");

        // 等待数据采集完成
        long startWaitTime = System.currentTimeMillis();
        long timeout = 60000; // 60秒超时
        int minExpectedPoints = calculateExpectedDataPoints(spectrumParameter);

        log.info("期望数据点数: " + minExpectedPoints);

        boolean dataComplete = false;
        int lastDataSize = 0;
        int noDataChangeCount = 0;

        while (true) {
            if (Thread.currentThread().isInterrupted() || !session.isOpen()) {
                log.warn("线程被中断或会话关闭");
                return new ProcessedData(x, y);
            }

            List<String> snapshot = new ArrayList<>(Constants.instrumentData);

            // 检查是否有结束标志
            boolean hasEndData = !snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData);

            // 检查数据是否稳定（不再变化）
            int currentDataSize = Constants.measurd_data.size();
            if (currentDataSize == lastDataSize) {
                noDataChangeCount++;
            } else {
                noDataChangeCount = 0;
                lastDataSize = currentDataSize;
            }

            log.debug("等待数据: measurd_data大小=" + currentDataSize + ", 结束标志=" + hasEndData + ", 无变化计数=" + noDataChangeCount);

            // 满足任一条件即可退出等待：
            if ((hasEndData && currentDataSize >= minExpectedPoints * 0.8) ||
                    (currentDataSize >= minExpectedPoints && noDataChangeCount >= 10) ||
                    (noDataChangeCount >= 50 && currentDataSize > 0)) {
                log.info("数据采集完成，开始处理数据");
                dataComplete = true;
                break;
            }

            // 超时检查
            if (System.currentTimeMillis() - startWaitTime > timeout) {
                log.warn("数据采集超时，强制结束等待。当前数据点数: " + currentDataSize);
                break;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new ProcessedData(x, y);
            }
        }

        // 计算所有数据点
        List<ChangeParam> baselineCopy = (task.getZeroParam() != null && !task.getZeroParam().isEmpty())
                ? JSON.parseArray(task.getZeroParam(), ChangeParam.class)
                : null;

        // 使用原始的measurd_data进行计算 - 创建副本避免并发修改
        List<ChangeParam> measurdDataCopy = new ArrayList<>(Constants.measurd_data);

        log.info("开始计算数据点，原始数据量: " + measurdDataCopy.size());

        // 清空x,y列表，重新计算
        x.clear();
        y.clear();

        int processedCount = 0;
        while (!measurdDataCopy.isEmpty()) {
            if (calcOnePointForFastScan(spectrumParameter, baselineCopy, x, y, measurdDataCopy, deviceSerialNumber)) {
                processedCount++;
            }
            if (processedCount % 100 == 0) {
                log.debug("已处理数据点: " + processedCount);
            }
        }

        log.info("数据点计算完成，共处理: " + processedCount + " 个点");

        // 获取起始和结束波长
        double startWl = Double.parseDouble(spectrumParameter.getStartWaveLength());
        double endWl = Double.parseDouble(spectrumParameter.getEndWaveLength());
        double interval = Double.parseDouble(spectrumParameter.getWaveLengthInterval());

        // 调试信息
        log.info("=== 极快扫描数据信息 ===");
        log.info("设定范围: " + startWl + " - " + endWl + ", 间隔: " + interval);
        log.info("计算得到的数据点数: " + x.size());
        if (!x.isEmpty()) {
            log.info("实际波长范围: " + x.get(0) + " - " + x.get(x.size() - 1));
        }

        // 检查数据完整性
        if (x.size() < minExpectedPoints * 0.5) {
            log.error("数据点数量不足，可能采集失败。期望: " + minExpectedPoints + ", 实际: " + x.size());
            sendAndClose(session, "数据采集失败，请重试");
            return new ProcessedData(new ArrayList<>(), new ArrayList<>());
        }

        // 处理数据：修复波长序列
        ProcessedData processedData = repairWavelengthSequenceWithConstraint(x, y, startWl, endWl, interval);

        List<Double> processedX = processedData.x;
        List<Double> processedY = processedData.y;
        List<Double> yForStorage = processedY; // 用于存储的Y值

        log.info("波长序列修复完成，修复后点数: " + processedX.size());

        // 如果需要台阶校正
        if (ladderCorrection) {
            log.info("开始台阶校正");
            List<Double> correctedY = processAndSaveData(task, processedX, processedY,spectrumParameter
                    , sampleId);
            processedY = correctedY;
            yForStorage = correctedY; // 开启校正时存储校正后的数据
            log.info("台阶校正完成");
        } else {
            // 未开启校正时存储原始数据
            yForStorage = processedY;
        }

        // 关键修改：根据是否开启阶梯校正来决定存储的数据
        String key = session.getId() + "_" + sampleId + "_" + number;
        originalYValues.put(key, new ArrayList<>(yForStorage));

        // 一次性推送所有数据 - 使用处理后的数据（校正或原始）
        pushAllDataAtOnce(session, processedX, processedY, number, sampleId, luminosity);

        log.info("极快扫描模式数据处理完成");
        return new ProcessedData(processedX, processedY);
    }

    /**
     * 计算期望的数据点数
     */
    private int calculateExpectedDataPoints(FSpectrumParameter spectrumParameter) {
        double startWl = Double.parseDouble(spectrumParameter.getStartWaveLength());
        double endWl = Double.parseDouble(spectrumParameter.getEndWaveLength());
        double interval = Double.parseDouble(spectrumParameter.getWaveLengthInterval());

        if (interval <= 0) {
            return 1000; // 默认值
        }

        int points = (int) Math.abs((startWl - endWl) / interval) + 1;
        log.info("计算期望数据点数: 范围=" + startWl + "-" + endWl + ", 间隔=" + interval + ", 点数=" + points);
        return Math.max(points, 100); // 至少100个点
    }

    /**
     * 为极快扫描模式计算数据点
     */
    private boolean calcOnePointForFastScan(FSpectrumParameter spectrumParameter,
                                            List<ChangeParam> baselineCopy,
                                            List<Double> x, List<Double> y,
                                            List<ChangeParam> measurdData,
                                            String deviceSerialNumber) {
        if (measurdData.isEmpty()) return false;

        List<LinearSegment> linearSegment = formulaUtil.getLinearSegment(deviceSerialNumber);


        // 获取波长参数用于约束
        double startWl = Double.parseDouble(spectrumParameter.getStartWaveLength());
        double endWl = Double.parseDouble(spectrumParameter.getEndWaveLength());
        double interval = Double.parseDouble(spectrumParameter.getWaveLengthInterval());
        boolean isDescending = startWl > endWl;

        try {

            switch (spectrumParameter.getLuminosity()) {
                case "1": // Abs
                {
                    if (lastChangeParam == null) {
                        if (baselineCopy!=null&&baselineCopy.size()>0){
                            baselineCopy.forEach(c -> {
                                baselineMap.put(c.getWl(), c);
                            });
                        }
                        ChangeParam remove = measurdData.remove(0);
                        Double originalWavelength = Double.parseDouble(remove.getWl());
                        // 步数转波长
                        Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                        remove.setWl(correctedWavelength.toString());
                        lastChangeParam = remove;
                        return true;
                    }
                    ChangeParam changeParam = measurdData.get(0);
                    Double originalWavelength = Double.parseDouble(changeParam.getWl());
                    Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                    changeParam.setWl(correctedWavelength.toString());
                    // 强制约束波长
                    Double constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                            interval, isDescending, x.size());

                    while (constrainedWavelength > correctedWavelength && !isEndData) {

                        if (constrainedWavelength == endWl) {
                            isEndData = true;
                        }
                        // 计算拟合数据
                        ChangeParam param = calculate(lastChangeParam, changeParam, constrainedWavelength);
                        String s;
                        if (baselineMap != null && !baselineMap.isEmpty()) {
                            s = DataCountUtil.countTPercent(baselineMap.get(constrainedWavelength), param, 1.0);
                        }else{
                            s = DataCountUtil.countTPercent(null, param, 1.0);
                        }

                        String yy = DataCountUtil.changeTPercentOrAbs2(null, s);
                        Constants.originalCalculationData.add(param.getWl() + "," + yy);
                        double parsed = Double.parseDouble(param.getWl());
                        synchronized (this) {
                            Constants.originalCalculationData.add(parsed + "," + yy);
                            x.add(parsed);
                            y.add(Double.parseDouble(yy));
                        }

                        constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                                interval, isDescending, x.size());
                    }
                    lastChangeParam = changeParam;
                    break;
                }
                case "2": // T%
                {
                    if (lastChangeParam == null) {
                        if (baselineCopy!=null&&baselineCopy.size()>0){
                            baselineCopy.forEach(c -> {
                                baselineMap.put(c.getWl(), c);
                            });
                        }
                        ChangeParam remove = measurdData.remove(0);
                        Double originalWavelength = Double.parseDouble(remove.getWl());
                        // 步数转波长
                        Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                        remove.setWl(correctedWavelength.toString());
                        lastChangeParam = remove;
                        return true;
                    }
                    ChangeParam changeParam = measurdData.get(0);
                    Double originalWavelength = Double.parseDouble(changeParam.getWl());
                    Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                    changeParam.setWl(correctedWavelength.toString());
                    // 强制约束波长
                    Double constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                            interval, isDescending, x.size());

                    while (constrainedWavelength > correctedWavelength && !isEndData) {

                        if (constrainedWavelength == endWl) {
                            isEndData = true;
                        }
                        // 计算拟合数据
                        ChangeParam param = calculate(lastChangeParam, changeParam, constrainedWavelength);
                        String yy;
                        if (baselineMap != null && !baselineMap.isEmpty()) {
                            yy = DataCountUtil.countTPercent(baselineMap.get(constrainedWavelength), param, 1.0);
                        }else{
                            yy = DataCountUtil.countTPercent(null, param, 1.0);
                        }
                        Constants.originalCalculationData.add(param.getWl() + "," + yy);
                        double parsed = Double.parseDouble(param.getWl());
                        synchronized (this) {
                            Constants.originalCalculationData.add(parsed + "," + yy);
                            x.add(parsed);
                            y.add(Double.parseDouble(yy));
                        }

                        constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                                interval, isDescending, x.size());
                    }
                    lastChangeParam = changeParam;
                    break;
                }
                case "3": // Es
                {
                    if (lastChangeParam == null) {
                        if (baselineCopy!=null&&baselineCopy.size()>0){
                            baselineCopy.forEach(c -> {
                                baselineMap.put(c.getWl(), c);
                            });
                        }
                        ChangeParam remove = measurdData.remove(0);
                        Double originalWavelength = Double.parseDouble(remove.getWl());
                        // 步数转波长
                        Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                        remove.setWl(correctedWavelength.toString());
                        lastChangeParam = remove;
                        return true;
                    }
                    ChangeParam changeParam = measurdData.get(0);
                    Double originalWavelength = Double.parseDouble(changeParam.getWl());
                    Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                    changeParam.setWl(correctedWavelength.toString());
                    // 强制约束波长
                    Double constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                            interval, isDescending, x.size());

                    while (constrainedWavelength > correctedWavelength && !isEndData) {

                        if (constrainedWavelength == endWl) {
                            isEndData = true;
                        }
                        // 计算拟合数据
                        ChangeParam param = calculate(lastChangeParam, changeParam, constrainedWavelength);
                        Double sData = Double.valueOf(param.getS());
                        String yy = String.valueOf((sData / 8388607) * 100);
                        Constants.originalCalculationData.add(param.getWl() + "," + yy);
                        double parsed = Double.parseDouble(param.getWl());
                        synchronized (this) {
                            Constants.originalCalculationData.add(parsed + "," + yy);
                            x.add(parsed);
                            y.add(Double.parseDouble(yy));
                        }

                        constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                                interval, isDescending, x.size());
                    }
                    lastChangeParam = changeParam;
                    break;
                }
                case "4": // Er
                {
                    if (lastChangeParam == null) {
                        if (baselineCopy!=null&&baselineCopy.size()>0){
                            baselineCopy.forEach(c -> {
                                baselineMap.put(c.getWl(), c);
                            });
                        }
                        ChangeParam remove = measurdData.remove(0);
                        Double originalWavelength = Double.parseDouble(remove.getWl());
                        // 步数转波长
                        Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                        remove.setWl(correctedWavelength.toString());
                        lastChangeParam = remove;
                        return true;
                    }
                    ChangeParam changeParam = measurdData.get(0);
                    Double originalWavelength = Double.parseDouble(changeParam.getWl());
                    Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                    changeParam.setWl(correctedWavelength.toString());
                    // 强制约束波长
                    Double constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                            interval, isDescending, x.size());

                    while (constrainedWavelength > correctedWavelength && !isEndData) {

                        if (constrainedWavelength == endWl) {
                            isEndData = true;
                        }
                        // 计算拟合数据
                        ChangeParam param = calculate(lastChangeParam, changeParam, constrainedWavelength);
                        Double rData = Double.valueOf(param.getR());
                        String yy = String.valueOf((rData / 8388607) * 100);
                        Constants.originalCalculationData.add(param.getWl() + "," + yy);
                        double parsed = Double.parseDouble(param.getWl());
                        synchronized (this) {
                            Constants.originalCalculationData.add(parsed + "," + yy);
                            x.add(parsed);
                            y.add(Double.parseDouble(yy));
                        }

                        constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                                interval, isDescending, x.size());
                    }
                    lastChangeParam = changeParam;
                    break;
                }
            }
            measurdData.remove(0);
            return true;
        } catch (Exception e) {
            log.error("计算数据点时发生异常: " + e.getMessage(), e);
            measurdData.remove(0);
            return false;
        }
    }

    /**
     * 强制约束波长值，确保符合设定的波长间隔和范围
     */
    private Double forceConstrainWavelength(Double originalWl, double startWl, double endWl,
                                            double interval, boolean isDescending, int currentIndex) {
        if (originalWl == null) {
            return null;
        }

        // 计算期望的波长值 - 强制使用期望值
        double expectedWl;
        if (isDescending) {
            // 递减扫描
            expectedWl = startWl - (currentIndex * interval);
        } else {
            // 递增扫描
            expectedWl = startWl + (currentIndex * interval);
        }

        // 强制修约到1位小数（如果间隔是整数）或根据间隔的小数位数修约
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

        return constrainedWl;
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
     * 修复波长序列，确保数据完整性 - 增强约束版本
     */
    private ProcessedData repairWavelengthSequenceWithConstraint(List<Double> originalX, List<Double> originalY,
                                                                 double startWl, double endWl, double interval) {
        if (originalX.isEmpty()) {
            log.warn("原始数据为空，无法修复波长序列");
            return new ProcessedData(new ArrayList<>(), new ArrayList<>());
        }

        log.info("开始修复波长序列，原始数据点数: " + originalX.size());

        // 确定扫描方向
        boolean isDescending = startWl > endWl;

        // 生成完整的波长序列
        List<Double> expectedWavelengths = generateCompleteWavelengthSequence(startWl, endWl, interval, isDescending);
        log.info("生成的完整波长序列点数: " + expectedWavelengths.size());

        // 创建映射：实际波长 -> Y值
        Map<Double, Double> wavelengthMap = new LinkedHashMap<>();
        for (int i = 0; i < originalX.size(); i++) {
            double wavelength = originalX.get(i);
            wavelengthMap.put(wavelength, originalY.get(i));
        }

        List<Double> repairedX = new ArrayList<>();
        List<Double> repairedY = new ArrayList<>();

        // 使用严格的波长约束
        int matchedCount = 0;
        int interpolatedCount = 0;
        int correctedCount = 0;

        for (int i = 0; i < expectedWavelengths.size(); i++) {
            Double expectedWl = expectedWavelengths.get(i);
            Double actualValue = null;

            // 首先尝试精确匹配
            if (wavelengthMap.containsKey(expectedWl)) {
                actualValue = wavelengthMap.get(expectedWl);
                matchedCount++;
            } else {
                // 寻找最接近的波长点
                Double closestWl = findClosestWavelength(expectedWl, wavelengthMap.keySet());
                if (closestWl != null && Math.abs(closestWl - expectedWl) <= interval) {
                    actualValue = wavelengthMap.get(closestWl);
                    correctedCount++;
                } else {
                    // 线性插值填充缺失点
                    actualValue = linearInterpolate(expectedWl, wavelengthMap, isDescending);
                    if (actualValue != null) {
                        interpolatedCount++;
                    }
                }
            }

            if (actualValue != null) {
                repairedX.add(expectedWl);
                repairedY.add(actualValue);
            }
        }

        log.info("波长序列修复完成: 匹配点=" + matchedCount + ", 修正点=" + correctedCount +
                ", 插值点=" + interpolatedCount + ", 总数=" + repairedX.size());

        return new ProcessedData(repairedX, repairedY);
    }

    /**
     * 寻找最接近的波长
     */
    private Double findClosestWavelength(double targetWl, Set<Double> wavelengths) {
        if (wavelengths.isEmpty()) {
            return null;
        }

        Double closest = null;
        double minDifference = Double.MAX_VALUE;

        for (Double wl : wavelengths) {
            double difference = Math.abs(wl - targetWl);
            if (difference < minDifference) {
                minDifference = difference;
                closest = wl;
            }
        }

        return closest;
    }

    /**
     * 生成完整的波长序列
     */
    private List<Double> generateCompleteWavelengthSequence(double startWl, double endWl,
                                                            double interval, boolean isDescending) {
        List<Double> wavelengths = new ArrayList<>();

        int decimalPlaces = getDecimalPlaces(interval);

        if (isDescending) {
            for (double wl = startWl; wl >= endWl; wl -= interval) {
                wavelengths.add(roundToPrecision(wl, decimalPlaces));
            }
        } else {
            for (double wl = startWl; wl <= endWl; wl += interval) {
                wavelengths.add(roundToPrecision(wl, decimalPlaces));
            }
        }

        return wavelengths;
    }

    /**
     * 线性插值
     */
    private Double linearInterpolate(double targetWl, Map<Double, Double> dataMap, boolean isDescending) {
        if (dataMap.isEmpty()) {
            return null;
        }

        List<Double> sortedWavelengths = new ArrayList<>(dataMap.keySet());
        Collections.sort(sortedWavelengths);

        if (isDescending) {
            Collections.reverse(sortedWavelengths);
        }

        // 找到目标波长前后的数据点
        Double lowerWl = null;
        Double upperWl = null;

        for (Double wl : sortedWavelengths) {
            if (wl <= targetWl) {
                lowerWl = wl;
            }
            if (wl >= targetWl) {
                upperWl = wl;
                break;
            }
        }

        // 处理边界情况
        if (lowerWl == null && upperWl != null) {
            return dataMap.get(upperWl);
        }
        if (upperWl == null && lowerWl != null) {
            return dataMap.get(lowerWl);
        }

        if (lowerWl != null && upperWl != null && !lowerWl.equals(upperWl)) {
            double lowerValue = dataMap.get(lowerWl);
            double upperValue = dataMap.get(upperWl);

            // 线性插值公式: y = y1 + (x - x1) * (y2 - y1) / (x2 - x1)
            double interpolatedValue = lowerValue + (targetWl - lowerWl) * (upperValue - lowerValue) / (upperWl - lowerWl);
            return interpolatedValue;
        }

        return null;
    }

    /**
     * 精确舍入到指定小数位
     */
    private double roundToPrecision(double value, int decimalPlaces) {
        double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }


    /**
     * 推送单个数据点
     */
    private void pushSinglePoint(WebSocketSession session,
                                 Double x,
                                 Double y,
                                 Integer count,
                                 String sampleId,
                                 String luminosity,
                                 Integer digit) throws IOException {
        // 安全检查
        if (x == null || y == null) {
            log.warn("推送数据点为空，跳过推送");
            return;
        }

        // 检查会话状态
        SessionState state = sessionStates.get(session.getId());
        if (state == null || state.sessionClosing.get()) {
            log.debug("会话正在关闭，跳过推送");
            return;
        }

        JSONArray dataArray = new JSONArray();
        JSONObject point = new JSONObject();
        point.put("x", x);

        // 推送时：根据指定位数格式化Y值
        Double value = DataCountUtil.formatValue(digit, y);
        String valueY = DataCountUtil.addZero(value, digit);
        point.put("y", y);
        point.put("roundOffY", valueY);  // 修复：这里应该是valueY而不是y.get(i)

        point.put("xType", "nm");
        // 光度模式是1.Abs 2.T% 3.Es 4.Er
        String luminosityType = luminosity.equals("1")?"Abs":luminosity.equals("2")?"T%":luminosity.equals("3")?"Es":"Er";
        point.put("yType", luminosityType);
        dataArray.add(point);

        JSONObject payload = new JSONObject();
        payload.put("data", dataArray);
        payload.put("sampleId", sampleId);
        payload.put("count", count);

        sendMessageSafe(session, payload.toJSONString());

        // 添加小延迟，避免推送过快
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("推送线程被中断");
        }
    }


    /**
     * 一次性推送所有数据（推送时格式化Y值）- 修正版本
     */
    private void pushAllDataAtOnce(WebSocketSession session,
                                   List<Double> x, List<Double> y,
                                   Integer number, String sampleId,
                                   String luminosity) throws IOException {
        if (x.isEmpty() || y.isEmpty()) {
            log.warn("没有数据可推送，x大小: " + x.size() + ", y大小: " + y.size());
            return;
        }

        SessionState state = sessionStates.get(session.getId());
        if (state == null) {
            return;
        }

        Double x_endOne = x.get(x.size() - 1);
        Double x_endTwo = x.get(x.size() - 2);
        if (x_endOne.equals(x_endTwo)) {
            x.remove(x.size() - 1);
            y.remove(y.size() - 1);
        }

        JSONArray dataArray = new JSONArray();

        for (int i = 0; i < x.size(); i++) {
            JSONObject point = new JSONObject();

            // 使用修正后的规整化波长值
            double wavelength = x.get(i);
            point.put("x", wavelength);

            // 推送时：根据指定位数格式化Y值
            Double value = DataCountUtil.formatValue(state.digit, y.get(i));
            String valueY = DataCountUtil.addZero(value, state.digit);
            point.put("y", y.get(i));
            point.put("roundOffY",valueY);
            point.put("xType", "nm");

            // 光度模式
            String luminosityType = getLuminosityType(luminosity);
            point.put("yType", luminosityType);

            dataArray.add(point);
        }

        JSONObject payload = new JSONObject();
        payload.put("data", dataArray);
        payload.put("sampleId", sampleId);
        payload.put("count", number);
        payload.put("scanMode", "fast");

        log.info("推送数据信息:");
        log.info("数据点数: " + dataArray.size());
        log.info("光度模式: " + luminosity);

        if (session.isOpen()) {
            sendMessageSafe(session, payload.toJSONString());
            log.info("数据推送完成");
        } else {
            log.warn("会话已关闭，无法推送数据");
        }
    }


    private static boolean isEndData = false;
    private static Map<String, ChangeParam> baselineMap = new HashMap<>();

    /**
     * 计算光度模式的Abs/T%
     */
    private void calcOnePoint(FSpectrumParameter spectrumParameter,
                              List<ChangeParam> baselineCopy,
                              List<Double> x,
                              List<Double> y,
                              String deviceSerialNumber,
                              WebSocketSession session) {
        List<LinearSegment> linearSegment = formulaUtil.getLinearSegment(deviceSerialNumber);
        String sessionId = session.getId();
        // 获取波长参数用于约束
        double startWl = Double.parseDouble(spectrumParameter.getStartWaveLength());
        double endWl = Double.parseDouble(spectrumParameter.getEndWaveLength());
        double interval = Double.parseDouble(spectrumParameter.getWaveLengthInterval());
        boolean isDescending = startWl > endWl;
        Double stopWaveLength = endWl + (interval * 3);

        if (spectrumParameter.getLuminosity().equals("1")) {
            if (lastChangeParam == null) {
                if (baselineCopy!=null&&baselineCopy.size()>0){
                    baselineCopy.forEach(c -> {
                        baselineMap.put(c.getWl(), c);
                    });
                }
                ChangeParam remove = Constants.measurd_data.remove(0);
                Double originalWavelength = Double.parseDouble(remove.getWl());
                // 步数转波长
                Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                remove.setWl(correctedWavelength.toString());
                lastChangeParam = remove;
                return;
            }
            ChangeParam changeParam = Constants.measurd_data.get(0);
            Double originalWavelength = Double.parseDouble(changeParam.getWl());
            Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
            changeParam.setWl(correctedWavelength.toString());
            // 强制约束波长
            Double constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                    interval, isDescending, x.size());

            while (constrainedWavelength > correctedWavelength && !isEndData) {

                if (constrainedWavelength == endWl) {
                    isEndData = true;
                }
                // 计算拟合数据
                ChangeParam param = calculate(lastChangeParam, changeParam, constrainedWavelength);
                String s;
                if (baselineMap != null && !baselineMap.isEmpty()) {
                    s = DataCountUtil.countTPercent(baselineMap.get(constrainedWavelength), param, 1.0);
                }else{
                    s = DataCountUtil.countTPercent(null, param, 1.0);
                }

                String yy = DataCountUtil.changeTPercentOrAbs2(null, s);
                Constants.originalCalculationData.add(param.getWl() + "," + yy);
                double parsed = Double.parseDouble(param.getWl());
                synchronized (this) {
                    calcOnePoint(stopWaveLength, parsed, sessionId);

                    Constants.originalCalculationData.add(parsed + "," + yy);
                    x.add(parsed);
                    y.add(Double.parseDouble(yy));
                }
                constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                        interval, isDescending, x.size());
            }
            lastChangeParam = changeParam;
        } else if (spectrumParameter.getLuminosity().equals("2")) {
            if (lastChangeParam == null) {
                if (baselineCopy!=null&&baselineCopy.size()>0){
                    baselineCopy.forEach(c -> {
                        baselineMap.put(c.getWl(), c);
                    });
                }
                ChangeParam remove = Constants.measurd_data.remove(0);
                Double originalWavelength = Double.parseDouble(remove.getWl());
                // 步数转波长
                Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                remove.setWl(correctedWavelength.toString());
                lastChangeParam = remove;
                return;
            }
            ChangeParam changeParam = Constants.measurd_data.get(0);
            Double originalWavelength = Double.parseDouble(changeParam.getWl());
            Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
            changeParam.setWl(correctedWavelength.toString());
            // 强制约束波长
            Double constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                    interval, isDescending, x.size());

            while (constrainedWavelength > correctedWavelength && !isEndData) {

                if (constrainedWavelength == endWl) {
                    isEndData = true;
                }
                // 计算拟合数据
                ChangeParam param = calculate(lastChangeParam, changeParam, constrainedWavelength);


                String yy;
                if (baselineMap != null && !baselineMap.isEmpty()) {
                    yy = DataCountUtil.countTPercent(baselineMap.get(constrainedWavelength), param, 1.0);
                }else{
                    yy = DataCountUtil.countTPercent(null, param, 1.0);
                }
                Constants.originalCalculationData.add(param.getWl() + "," + yy);
                double parsed = Double.parseDouble(param.getWl());
                synchronized (this) {
                    calcOnePoint(stopWaveLength, parsed, sessionId);

                    Constants.originalCalculationData.add(parsed + "," + yy);
                    x.add(parsed);
                    y.add(Double.parseDouble(yy));
                }
                constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                        interval, isDescending, x.size());
            }
            lastChangeParam = changeParam;
        } else if (spectrumParameter.getLuminosity().equals("3")) {
            if (lastChangeParam == null) {
                ChangeParam remove = Constants.measurd_data.remove(0);
                Double originalWavelength = Double.parseDouble(remove.getWl());
                // 步数转波长
                Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                remove.setWl(correctedWavelength.toString());
                lastChangeParam = remove;
                return;
            }
            ChangeParam changeParam = Constants.measurd_data.get(0);
            Double originalWavelength = Double.parseDouble(changeParam.getWl());
            Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
            changeParam.setWl(correctedWavelength.toString());
            // 强制约束波长
            Double constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                    interval, isDescending, x.size());

            while (constrainedWavelength > correctedWavelength && !isEndData) {

                if (constrainedWavelength == endWl) {
                    isEndData = true;
                }
                // 计算拟合数据
                ChangeParam param = calculate(lastChangeParam, changeParam, constrainedWavelength);


                Double sData = Double.valueOf(param.getS());
                String yy = String.valueOf((sData / 8388607) * 100);
                Constants.originalCalculationData.add(param.getWl() + "," + yy);
                double parsed = Double.parseDouble(param.getWl());
                synchronized (this) {
                    calcOnePoint(stopWaveLength, parsed, sessionId);

                    Constants.originalCalculationData.add(parsed + "," + yy);
                    x.add(parsed);
                    y.add(Double.parseDouble(yy));
                }
                constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                        interval, isDescending, x.size());
            }
            lastChangeParam = changeParam;
        } else if (spectrumParameter.getLuminosity().equals("4")) {
            if (lastChangeParam == null) {
                ChangeParam remove = Constants.measurd_data.remove(0);
                Double originalWavelength = Double.parseDouble(remove.getWl());
                // 步数转波长
                Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                remove.setWl(correctedWavelength.toString());
                lastChangeParam = remove;
                return;
            }
            ChangeParam changeParam = Constants.measurd_data.get(0);
            Double originalWavelength = Double.parseDouble(changeParam.getWl());
            Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
            changeParam.setWl(correctedWavelength.toString());
            // 强制约束波长
            Double constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                    interval, isDescending, x.size());

            while (constrainedWavelength > correctedWavelength && !isEndData) {

                if (constrainedWavelength == endWl) {
                    isEndData = true;
                }
                // 计算拟合数据
                ChangeParam param = calculate(lastChangeParam, changeParam, constrainedWavelength);


                Double rData = Double.valueOf(param.getR());
                String yy = String.valueOf((rData / 8388607) * 100);
                Constants.originalCalculationData.add(param.getWl() + "," + yy);
                double parsed = Double.parseDouble(param.getWl());
                synchronized (this) {
                    calcOnePoint(stopWaveLength, parsed, sessionId);

                    Constants.originalCalculationData.add(parsed + "," + yy);
                    x.add(parsed);
                    y.add(Double.parseDouble(yy));
                }
                constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                        interval, isDescending, x.size());
            }
            lastChangeParam = changeParam;
        }
        Constants.measurd_data.remove(0);

    }


    /**
     * 根据起始点和终止点，计算指定波长对应的ChangeParam
     *
     * @param start    起始点
     * @param end      终止点
     * @param targetWl 目标波长，支持字符串或数字
     * @return 计算得到的ChangeParam
     */
    public static ChangeParam calculate(ChangeParam start, ChangeParam end, Double targetWl) {

        // 解析参数
        double startWl = Double.parseDouble(start.getWl());
        double endWl = Double.parseDouble(end.getWl());
        int startR = Integer.parseInt(start.getR());
        int endR = Integer.parseInt(end.getR());
        int startS = Integer.parseInt(start.getS());
        int endS = Integer.parseInt(end.getS());
        int dValue = Integer.parseInt(start.getD());

        // 计算拟合曲线
        double slopeR = (double) (endR - startR) / (endWl - startWl);
        double interceptR = startR - slopeR * startWl;

        double slopeS = (double) (endS - startS) / (endWl - startWl);
        double interceptS = startS - slopeS * startWl;

        // 计算r和s值
        int r = (int) Math.round(slopeR * targetWl + interceptR);
        int s = (int) Math.round(slopeS * targetWl + interceptS);

        // 格式化wl字符串
        String wlStr = String.valueOf(targetWl);

        return new ChangeParam(wlStr, wlStr, String.valueOf(r), String.valueOf(s), String.valueOf(dValue));
    }


    public void calcOnePoint(Double stopWaveLength,
                             Double constrainedWavelength,
                             String sessionId) {
        if (stopWaveLength.equals(constrainedWavelength)){
            log.info("【停止命令不会再发送】session: {}", sessionId);
            SessionState state = sessionStates.get(sessionId);
            state.stopCommandSent2.set(true);
        }
    }

    /**
     * 处理数据并保存
     */
    public List<Double> processAndSaveData(FMeasureTask task,
                                           List<Double> x,
                                           List<Double> y,
                                           FSpectrumParameter parameter,
                                           String sampleId) {

        String luminosityMode = parameter.getLuminosity();
        boolean ladderCorrection = parameter.getLadderCorrection();
        log.info("---------------进入台阶消除处理逻辑---------------");
        log.info("台阶消除参数 - 数据点数: " + x.size() + ", 光度模式: " + luminosityMode + ", 样品ID: " + sampleId);

        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();

        // 如果不需要台阶校正，直接返回原始数据
        if (!ladderCorrection) {
            log.info("台阶消除已禁用，返回原始数据");
            return y;
        }

        Integer threshold = 100; // 设置默认值

        try {
            /// 预留是否打开积分球的门限系数的校验
            if (true){
                // 判断光度模式
                if ("1".equals(luminosityMode)) {
                    // Abs的台阶消除数值
                    if (confVO.getAbs() != null && !confVO.getAbs().trim().isEmpty()) {
                        threshold = Integer.valueOf(confVO.getAbs());
                    }
                } else if ("2".equals(luminosityMode)){
                    // 透过率的台阶消除数值
                    if (confVO.getTransmittance() != null && !confVO.getTransmittance().trim().isEmpty()) {
                        threshold = Integer.valueOf(confVO.getTransmittance());
                    }
                } else {
                    // 其他光度模式的默认值
                    threshold = 100;
                }
            } else {
                /// 积分球的门限系数
                if (confVO.getIntegratingSphere() != null && !confVO.getIntegratingSphere().trim().isEmpty()) {
                    threshold = Integer.valueOf(confVO.getIntegratingSphere());
                }
            }
        } catch (NumberFormatException e) {
            log.error("门限系数转换失败，使用默认值100", e);
            threshold = 100;
        } catch (Exception e) {
            log.error("获取门限系数时发生异常，使用默认值100", e);
            threshold = 100;
        }

        // 确保threshold不为null
        if (threshold == null) {
            threshold = 100;
        }

        log.info("台阶消除阈值: " + threshold);

        // 这里调用 Python 的台阶消除算法，返回处理后的 y 值
        List<Double> processedY = callStepEliminationPython(x, y, threshold,parameter);

        log.info("台阶消除完成，原始数据点数: " + y.size() + ", 处理后数据点数: " + processedY.size());
        log.info("---------------台阶消除处理逻辑结束---------------");

        return processedY;
    }

    /**
     * 调用 Python 服务处理台阶消除
     */
    private List<Double> callStepEliminationPython(List<Double> x, List<Double> y, Integer threshold, FSpectrumParameter parameter) {
        // 确保threshold不为null
        if (threshold == null) {
            threshold = 100;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("x_data", x);
            requestBody.put("y_data", y);
            requestBody.put("k", threshold);
            requestBody.put("change_lamp_wavelength", parameter.getChangeLampWaveLength());
            requestBody.put("wavelength_end", parameter.getEndWaveLength());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 调用 Python API
            ResponseEntity<Map> response = restTemplate.postForEntity(pythonPathConstant.getRemoveStepOrSpikes(), request, Map.class);

            // 从响应中获取处理后的 y 值
            List<Double> processedY = (List<Double>) response.getBody().get("smoothed_data");

            return processedY;
        } catch (Exception e) {
            log.error("调用Python台阶消除服务失败，返回原始数据", e);
            return y;
        }
    }

    /**
     * 使用快照保存一次测量结果 - 修复重测覆盖问题
     */
    private void saveOneMeasurementWithSnapshots(FMeasureTask task,
                                                 String taskId,
                                                 String sampleId,
                                                 String deviceSerialNumber,
                                                 List<Double> x,
                                                 List<Double> y, // 这里的y已经是处理后的数据（校正或原始）
                                                 WebSocketSession session,
                                                 Integer number,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime,
                                                 List<String> instrumentSnapshot,
                                                 List<String> originalCalcSnapshot,
                                                 CountDownLatch latch) {
        if (instrumentSnapshot == null || originalCalcSnapshot == null) {
            log.warn("测量数据为空，无法保存");
            if (latch != null) {
                latch.countDown();
            }
            return;
        }

        try {
            SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();

            // 处理峰谷数据
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();

            String peakUrl = pythonPathConstant.getPeakSearching();
            String valleyUrl = pythonPathConstant.getValleySearching();


            List<FPeakValleyMarking> peakValleys = new ArrayList<>();
            List<FPeakValleyMarking> markings = new ArrayList<>();
            try {

                // 添加日志输出（调试用）
                log.info("调用寻峰接口: " + peakUrl);
                log.info("调用寻谷接口: " + valleyUrl);

                // 关键修改：直接使用传入的y值（处理后的）进行峰谷分析
                // 这样峰谷分析基于的数据与前端显示的数据一致
                body.put("x_data", x);
                body.put("y_data", y); // 使用处理后的Y值（校正或原始）
                double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
                body.put("height", height);
                body.put("distance", confVO.getPeakInterval());

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                Map<String, Object> resultMap = restTemplate.postForEntity(peakUrl, request, Map.class).getBody();


                List<Double> xDataFeng = (List<Double>) resultMap.get("peak_x");
                List<String> yDataFeng = (List<String>) resultMap.get("peak_y");
                for (int i = 0; i < xDataFeng.size(); i++) {
                    FPeakValleyMarking marking = new FPeakValleyMarking();
                    marking.setX(xDataFeng.get(i).toString());
                    marking.setY(yDataFeng.get(i));
                    marking.setType("波峰");
                    peakValleys.add(marking);
                }

            } catch (Exception e) {
                log.error("调用寻峰算法失败: {}", e.getMessage(), e);
            }
            try {
                // 这样峰谷分析基于的数据与前端显示的数据一致
                body.clear();
                body.put("x_data", x);
                body.put("y_data", y); // 使用处理后的Y值（校正或原始）
                double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
                body.put("height", height);
                body.put("distance", confVO.getPeakInterval());

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                Map<String, Object> resultMap = restTemplate.postForEntity(peakUrl, request, Map.class).getBody();

                Map<String, Object> resultMap1 = restTemplate.postForEntity(valleyUrl, request, Map.class).getBody();
                List<Double> xDataGu = (List<Double>) resultMap1.get("valley_x");
                List<String> yDataGu = (List<String>) resultMap1.get("valley_y");

                for (int i = 0; i < xDataGu.size(); i++) {
                    FPeakValleyMarking marking = new FPeakValleyMarking();
                    marking.setX(xDataGu.get(i).toString());
                    marking.setY(yDataGu.get(i));
                    marking.setType("波谷");
                    markings.add(marking);
                }
            } catch (Exception e) {
                log.error("调用寻谷算法失败: {}", e.getMessage(), e);
            }

            // 关键修改：构建结果字符串，直接使用传入的y值（处理后的）
            List<String> result = new ArrayList<>();
            for (int i = 0; i < x.size(); i++) {
                result.add(x.get(i) + "," + y.get(i)); // 使用处理后的Y值（校正或原始）
            }

            // 保存测量数据 - 修复重测覆盖逻辑
            List<FTaskSpecimen> sampleData = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            FTaskSpecimen taskSpecimen = null;
            for (FTaskSpecimen data : sampleData) {
                if (data.getSpecimenKey().equals(sampleId)) {
                    taskSpecimen = data;
                    break;
                }
            }
            if (taskSpecimen == null) {
                log.warn("未找到该样品: " + sampleId);
                return;
            }

            FMeasureSample sample = tMeasureSampleService.getById(sampleId);
            FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

            // 检查是否已经存在数据（重测情况）
            QueryWrapper<FMeasureAnalysisData> analysisQuery = new QueryWrapper<>();
            analysisQuery.eq("task_id", taskId)
                    .eq("sample_key", sampleId)
                    .eq("analysis_number", number)
                    .eq("analysis_type", "5");
            FMeasureAnalysisData existingAnalysisData = tMeasureAnalysisDataService.getOne(analysisQuery);

            if (existingAnalysisData != null) {
                // 重测情况：更新现有数据
                log.info("重测情况，更新现有数据 - 任务ID: {}, 样品ID: {}, 编号: {}", taskId, sampleId, number);

                // 更新测量数据
                FMeasureData existingMeasureData = tMeasureDataService.getById(existingAnalysisData.getMeasureDataId());
                if (existingMeasureData != null) {
                    existingMeasureData.setInstrumentData(JSON.toJSONString(instrumentSnapshot));
                    existingMeasureData.setOriginalCalculationData(JSON.toJSONString(result));
                    existingMeasureData.setMeasurementStartTime(startTime);
                    existingMeasureData.setMeasurementEndTime(endTime);
                    existingMeasureData.setIsSave("1");
                    boolean updateMeasure = traceDataService.updateById(existingMeasureData);
                    if (!updateMeasure) {
                        log.error("更新测量数据失败");
                    }
                }

                // 更新分析数据
                existingAnalysisData.setAnalysisParam(JSON.toJSONString(instrumentSnapshot));
                existingAnalysisData.setAnalysisResult(JSON.toJSONString(result));
                existingAnalysisData.setPeak(JSON.toJSONString(peakValleys));
                existingAnalysisData.setGrain(JSON.toJSONString(markings));
                existingAnalysisData.setColor("#000000");
                existingAnalysisData.setIsSave("1");
                boolean updateAnalysis = tMeasureAnalysisDataService.updateById(existingAnalysisData);
                if (!updateAnalysis) {
                    log.error("更新分析数据失败");
                }

                log.info("重测数据更新完成");

            } else {
                // 新建测量情况
                log.info("新建测量情况 - 任务ID: {}, 样品ID: {}, 编号: {}", taskId, sampleId, number);

                if ("不重复".equals(spectrumParameter.getRepetitionPattern())) {
                    FMeasureData measureData = new FMeasureData();
                    measureData.setSerialNumber(1);
                    measureData.setTaskId(taskId);
                    measureData.setSampleKey(sampleId);
                    measureData.setInstrumentData(JSON.toJSONString(instrumentSnapshot));
                    measureData.setOriginalCalculationData(JSON.toJSONString(result));
                    measureData.setMeasurementStartTime(startTime);
                    measureData.setMeasurementEndTime(endTime);
                    measureData.setIsDelete("0");
                    measureData.setIsSave("1");
                    boolean saveMeasure = traceDataService.save(measureData);
                    if (!saveMeasure) {
                        log.error("测量数据保存失败");
                        return;
                    }

                    FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
                    analysisData.setAnalysisNumber(1);
                    analysisData.setAnalysisName(sample.getSampleName() + "(" + analysisData.getAnalysisNumber() + ")");
                    analysisData.setTaskId(taskId);
                    analysisData.setSampleKey(sampleId);
                    analysisData.setMeasureDataId(measureData.getId());
                    analysisData.setAnalysisParam(JSON.toJSONString(instrumentSnapshot));
                    analysisData.setAnalysisResult(JSON.toJSONString(result));
                    analysisData.setAnalysisType("5");
                    analysisData.setPeak(JSON.toJSONString(peakValleys));
                    analysisData.setGrain(JSON.toJSONString(markings));
                    analysisData.setColor("#000000");
                    analysisData.setIsDelete("0");
                    analysisData.setIsSave("1");
                    boolean saveAnalysis = tMeasureAnalysisDataService.save(analysisData);
                    if (!saveAnalysis) {
                        log.error("分析数据保存失败");
                        return;
                    }

                    FMeasureAnalysisImage tMeasureAnalysisImage = new FMeasureAnalysisImage();
                    tMeasureAnalysisImage.setAnalysisId(analysisData.getId());
                    boolean saveImage = tMeasureAnalysisImageService.save(tMeasureAnalysisImage);
                    if (!saveImage) {
                        log.error("分析图像保存失败");
                        return;
                    }
                } else {
                    // 重复模式
                    FMeasureData measureData = new FMeasureData();
                    measureData.setSerialNumber(number);
                    measureData.setTaskId(taskId);
                    measureData.setSampleKey(sampleId);
                    measureData.setInstrumentData(JSON.toJSONString(instrumentSnapshot));
                    measureData.setOriginalCalculationData(JSON.toJSONString(result));
                    measureData.setMeasurementStartTime(startTime);
                    measureData.setMeasurementEndTime(endTime);
                    measureData.setIsDelete("0");
                    measureData.setIsSave("1");
                    boolean saveMeasure = traceDataService.save(measureData);
                    if (!saveMeasure) {
                        log.error("测量数据保存失败");
                        return;
                    }

                    FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
                    analysisData.setAnalysisNumber(number);
                    analysisData.setAnalysisName(sample.getSampleName() + "-" + number);
                    analysisData.setTaskId(taskId);
                    analysisData.setSampleKey(sampleId);
                    analysisData.setMeasureDataId(measureData.getId());
                    analysisData.setAnalysisParam(JSON.toJSONString(instrumentSnapshot));
                    analysisData.setAnalysisResult(JSON.toJSONString(result));
                    analysisData.setAnalysisType("5");
                    analysisData.setPeak(JSON.toJSONString(peakValleys));
                    analysisData.setGrain(JSON.toJSONString(markings));
                    analysisData.setColor("#000000");
                    analysisData.setIsDelete("0");
                    analysisData.setIsSave("1");
                    boolean saveAnalysis = tMeasureAnalysisDataService.save(analysisData);
                    if (!saveAnalysis) {
                        log.error("分析数据保存失败");
                        return;
                    }

                    FMeasureAnalysisImage tMeasureAnalysisImage = new FMeasureAnalysisImage();
                    tMeasureAnalysisImage.setAnalysisId(analysisData.getId());
                    boolean saveImage = tMeasureAnalysisImageService.save(tMeasureAnalysisImage);
                    if (!saveImage) {
                        log.error("分析图像保存失败");
                        return;
                    }
                }
            }

            // 更新任务状态
            updateTaskStatus(task, taskId, sampleId, deviceSerialNumber, spectrumParameter);

            log.info("数据保存完成: 样品=" + sampleId + ", 计数=" + number);
        } catch (Exception e) {
            log.error("保存测量数据时发生异常", e);
        } finally {
            // 清理存储的Y值
            String key = session.getId() + "_" + sampleId + "_" + number;
            originalYValues.remove(key);

            // 通知存储完成
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(FMeasureTask task, String taskId, String sampleId,
                                  String deviceSerialNumber, FSpectrumParameter spectrumParameter) {
        try {
            List<FTaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

            if ("不重复".equals(spectrumParameter.getRepetitionPattern())) {
                // 不重复模式：直接标记为已测试
                for (FTaskSpecimen taskSample : specimen) {
                    if (taskSample.getSpecimenKey().equals(sampleId)) {
                        taskSample.setIsTested("1");
                        break;
                    }
                }
                task.setSampleKey(JSON.toJSONString(specimen));
                task.setDeviceSerialNumber(deviceSerialNumber);
                boolean update = tMeasureTaskService.updateById(task);
                if (!update) {
                    log.error("更新任务失败: " + taskId);
                }
            } else {
                // 重复模式：检查是否全部测完
                boolean allCompleted = true;
                Integer repetitionCount = Integer.valueOf(spectrumParameter.getRepetitionTime());

                for (int i = 1; i <= repetitionCount; i++) {
                    QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("task_id", taskId)
                            .eq("sample_key", sampleId)
                            .eq("analysis_number", i)
                            .eq("analysis_type", "5");
                    FMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(queryWrapper);

                    if (analysisData == null || analysisData.getAnalysisResult() == null) {
                        allCompleted = false;
                        break;
                    }
                }

                if (allCompleted) {
                    // 所有重复次数都完成了，标记为已测试
                    for (FTaskSpecimen taskSample : specimen) {
                        if (taskSample.getSpecimenKey().equals(sampleId)) {
                            taskSample.setIsTested("1");
                            break;
                        }
                    }
                    task.setSampleKey(JSON.toJSONString(specimen));
                    task.setDeviceSerialNumber(deviceSerialNumber);
                    boolean update = tMeasureTaskService.updateById(task);
                    if (!update) {
                        log.error("更新任务失败: " + taskId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("更新任务状态时发生异常", e);
        }
    }

    /**
     * 获取光度类型
     */
    private String getLuminosityType(String luminosityCode) {
        switch (luminosityCode) {
            case "1": return "Abs";
            case "2": return "T%";
            case "3": return "Es";
            case "4": return "Er";
            default: return "Abs";
        }
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
    private void clearGlobalData() {
        Constants.x_data.clear();
        Constants.y_data.clear();
        Constants.field3Values.clear();
        Constants.instrumentData.clear();
        Constants.originalCalculationData.clear();
        Constants.sendMeasureThread = null;
        Constants.startTime = null;
        Constants.endTime = null;
    }

    /**
     * 测量结果类
     */
    class MeasurementResult {
        List<Double> x;
        List<Double> y;
        LocalDateTime startTime;
        LocalDateTime endTime;
        List<String> instrumentSnapshot;
        List<String> originalCalcSnapshot;

        MeasurementResult(List<Double> x, List<Double> y,
                          LocalDateTime startTime, LocalDateTime endTime,
                          List<String> instrumentSnapshot, List<String> originalCalcSnapshot) {
            this.x = x;
            this.y = y;
            this.startTime = startTime;
            this.endTime = endTime;
            this.instrumentSnapshot = instrumentSnapshot;
            this.originalCalcSnapshot = originalCalcSnapshot;
        }
    }

    /**
     * 处理后的数据容器
     */
    class ProcessedData {
        List<Double> x;
        List<Double> y;

        ProcessedData(List<Double> x, List<Double> y) {
            this.x = x;
            this.y = y;
        }
    }
}