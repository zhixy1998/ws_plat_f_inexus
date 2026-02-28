package com.persee.workstation.uv_inexus.websocket.measure.concise;/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.enums.T600ComIdEnums;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.ColorUtil;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.ActionCmdEnum;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketOperationEnum;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketResponseVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketSendUtil;
import com.persee.workstation.uv_inexus.websocket.measure.param.FSpectrumNewWebSocketParam;
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
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

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
public class ConciseSpectrumOneNewWebSocket {

    private static final Logger log = LoggerFactory.getLogger(ConciseSpectrumOneNewWebSocket.class);
    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();
    // 会话状态管理
    private static final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();
    // WebSocket消息发送队列，避免并发发送冲突
    private static final Map<String, Object> sessionSendLocks = new ConcurrentHashMap<>();

    // 存储状态跟踪
    private static final Map<String, AtomicInteger> pendingStorages = new ConcurrentHashMap<>();
    private static final Map<String, CountDownLatch> storageLatches = new ConcurrentHashMap<>();

    private static UnknowCountMessageQueue unknowCountMessageQueue = UnknowCountMessageQueue.getInstance();

    private static ChangeParam lastChangeParam;

    String deviceSerialNumber;
    String currentSampleCell;

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
    private SamplePoolConfigService fSamplePoolConfigService;
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
    // 注入PythonPathConstant
    @Autowired
    private PythonPathConstant pythonPathConstant;
    @Autowired
    WebSocketSendUtil webSocketSendUtil;
    @Autowired
    private DevOperationUtilService devOperationUtilService;
    @Autowired
    DeviceInfoService infoService;

    // 从yaml配置文件读取台阶消除批次大小
    @Value("${staircase-elimination.data-count}")
    private Integer staircaseEliminationBatchSize;

    // 用于存储原始Y值（未经格式化）
    private Map<String, List<Double>> originalYValues = new ConcurrentHashMap<>();

    /**
     * 会话状态管理类
     */
    private static class SessionState {
        private final AtomicBoolean measurementStarted = new AtomicBoolean(false);
        private final AtomicBoolean stopCommandSent = new AtomicBoolean(false);
        private final AtomicBoolean stopCommandSent2 = new AtomicBoolean(false);
        private final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);
        private final AtomicBoolean sessionClosing = new AtomicBoolean(false);
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
                        count,
                        sessionId,
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
        public BatchData getRemainingBatchBuffer(Integer count, String sampleId,
                                                 Integer digit, FSpectrumParameter parameter,
                                                 FMeasureTask task) {
            return getAndClearBatchBuffer(deviceSerialNumber,count, sampleId, digit, parameter, task);
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
                         Integer count,String sessionId, String sampleId, Integer digit,
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
    public void measureTask(FSpectrumNewWebSocketParam param) {
        log.info("仪器编号: {} 进入扫描 时间: {}", param.getDeviceSerialNumber(), LocalDateTime.now());

        // 初始化会话状态
        SessionState state = new SessionState();
        sessionStates.put(param.getDeviceSerialNumber(), state);

        currentSampleCell=null;

        deviceSerialNumber = param.getDeviceSerialNumber();

        state.setBatchSize(staircaseEliminationBatchSize);

        // 初始化发送锁
        sessionSendLocks.put(deviceSerialNumber, new Object());

        infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);

        clearGlobalData();
        // 停止状态给初始值
        Constants.measureStopStatus.put(deviceSerialNumber, true);

        if (Constants.endTime != null || Constants.startTime != null) {
            sendError(deviceSerialNumber, "当前测量未结束,无法开启新的测试！");
            return;
        }

        String taskId = param.getTaskId();
        String sampleId = param.getSampleId();
        String analysisNumber = param.getAnalysisNumber();
        String deviceSerialNumber = param.getDeviceSerialNumber();
        String updateBy = param.getUpdateBy();
        Integer digit = param.getDigit();

        if (taskId == null) {
            sendError(deviceSerialNumber, "缺少 taskId 参数");
            return;
        }
        if (updateBy == null) {
            sendError(deviceSerialNumber,  "缺少 updateBy 参数");
            return;
        }
        if (sampleId == null) {
            sendError(deviceSerialNumber,  "缺少 sampleId 参数");
            return;
        }
        if (analysisNumber == null) {
            sendError(deviceSerialNumber,  "缺少 analysisNumber 参数");
            return;
        }
        if (deviceSerialNumber == null) {
            sendError(deviceSerialNumber,  "缺少 deviceSerialNumber 参数");
            return;
        }

        // 设置会话状态
        state.deviceSerialNumber = deviceSerialNumber;
        state.digit = digit;

        // 根据任务id查询任务数据
        FMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null || task.getSampleKey() == null || task.getSampleKey().equals("")) {
            sendError(deviceSerialNumber, "请先创建样品");
            return;
        }

        /// 设置参数
        Result result = spectrumService.setUpParametersPort(task.getMethodKey(), deviceSerialNumber);
        if (result.getCode() != 200) {
            sendError(deviceSerialNumber, result.getMessage());
            return;
        }
        // 根据方法id查询方法数据
        FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 将方法参数提取出来
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

        String ip = deviceInfoService.getIp(deviceSerialNumber);
        state.ip = ip;

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setAction(ActionCmdEnum.Scan);
        if (spectrumParameter.getScanSpeed().equals("3")) {
            inputParam.setAction(ActionCmdEnum.ScanFast);
        }

        // 根据序列号查询对应的附件配置表信息
        QueryWrapper<SamplePoolConfigPO> wrapper = new QueryWrapper<>();
        wrapper.eq("device_serial_number", deviceSerialNumber);
        SamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);
        if (configPO == null) {
            sendError(deviceSerialNumber, "没有找到对应的附件配置信息");
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
                sendError(deviceSerialNumber, "请先创建样品");
                return;
            }

            if (poolStatus) {
                SetParam setParam = new SetParam();
                setParam.setCellPos(Integer.parseInt(sample.getSampleLocation()));
                boolean status = spectrumService.setInstrumentParameters(deviceSerialNumber, setParam);
                if (!status) {
                    sendError(deviceSerialNumber, "设置出错");
                    return;
                }
                Boolean b = fSamplePoolConfigService.updateCurrentPoolNum(deviceSerialNumber, Integer.parseInt(sample.getSampleLocation()), updateBy);
                if (b) {
                    currentSampleCell=sample.getSampleLocation();
                }
                JSONObject object = new JSONObject();
                object.put("currentPoolNum", sample.getSampleLocation());
                sendMessage(deviceSerialNumber, object);
            }
        }



        Thread taskThread = new Thread(() -> {
            try {
                Integer number = Integer.valueOf(analysisNumber);
                doOneMeasurementSerial(inputParam,spectrumParameter, task, taskId, sampleId, number);

                JSONObject payload = new JSONObject();
                if (Constants.measureStopStatus.get(deviceSerialNumber)){
                    payload.put("success", "扫描成功");
                }else{
                    payload.put("success", "停止成功");
                }
                sendMessage(deviceSerialNumber, payload);
                infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
                // 全部完成后再告诉前端 finish
                payload.clear();
                payload.put("result", "finish");
                sendClose(deviceSerialNumber, payload);

            } catch (Exception e) {
                log.error("单次测量异常", e);
                JSONObject payload = new JSONObject();
                payload.put("error", "执行异常: " + e.getMessage());
                sendClose(deviceSerialNumber, payload);
            }
        });

        taskThreads.put(deviceSerialNumber, taskThread);
        taskThread.start();
    }


    /**
     * 清理资源
     */
    private void cleanupResources(String sessionId) {
        SessionState state = sessionStates.get(sessionId);

        if (state == null) {
            sessionSendLocks.remove(sessionId);
            return;
        }

        // 等待当前测量完成，包括数据保存
        if (state.measurementStarted.get() || !state.currentMeasurementCompleted.get()) {
            log.info("测量或数据保存仍在进行中，等待完成后再清理资源，session: {}", sessionId);

            // 等待最多10秒让测量和数据保存完成
            long startTime = System.currentTimeMillis();
            long timeout = 10000;

            while ((state.measurementStarted.get() || !state.currentMeasurementCompleted.get())
                    && (System.currentTimeMillis() - startTime) < timeout) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (state.measurementStarted.get() || !state.currentMeasurementCompleted.get()) {
                log.warn("测量或数据保存未在10秒内完成，强制清理资源，session: {}", sessionId);
            }
        }

        if (state.cleanupInProgress.getAndSet(true)) {
            log.debug("清理已在进行中, session: {}", sessionId);
            return;
        }

        try {
            log.info("开始清理资源, session: {}", sessionId);

            // 1. 标记会话正在关闭，阻止新的消息发送
            state.sessionClosing.set(true);

            // 等待任务线程完全结束（包括数据保存）
            Thread thread = taskThreads.get(sessionId);
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
                log.info("已中断任务线程, session: {}", sessionId);

                // 等待线程结束，但增加等待时间
                try {
                    thread.join(5000); // 等待5秒
                } catch (InterruptedException e) {
                    log.warn("等待任务线程结束被中断, session: {}", sessionId);
                    Thread.currentThread().interrupt();
                }
            }
            // 等待所有存储操作完成
            boolean allStorageCompleted = waitForAllStorageComplete(sessionId);
            if (!allStorageCompleted) {
                log.warn("部分存储操作未完成，但继续清理资源，session: {}", sessionId);
            }

            // 5. 清理各种存储
            pendingStorages.remove(sessionId);
            storageLatches.remove(sessionId);
            sessionSendLocks.remove(sessionId);
            sessionStates.remove(sessionId);

            // 6. 清理原始Y值（放在最后）
            originalYValues.entrySet().removeIf(entry -> entry.getKey().startsWith(sessionId + "_"));

            // 7. 清空全局数据（放在设备释放之后）
            clearGlobalData();

            log.info("资源清理完成, session: {}", sessionId);

        } catch (Exception e) {
            log.error("清理资源异常, session: {}", sessionId, e);
        }
    }

    /**
     * 等待所有存储操作完成
     */
    private boolean waitForAllStorageComplete(String sessionId) {
        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 等待5秒

        try {
            // 检查是否有未完成的存储操作
            while (hasPendingStorageOperations(sessionId) &&
                    (System.currentTimeMillis() - startTime) < timeout) {
                Thread.sleep(100);
            }

            boolean completed = !hasPendingStorageOperations(sessionId);
            log.info("存储操作等待结果: {}, 耗时: {}ms, session: {}",
                    completed ? "完成" : "超时",
                    System.currentTimeMillis() - startTime,
                    sessionId);
            return completed;

        } catch (InterruptedException e) {
            log.warn("等待存储操作完成时被中断, session: {}", sessionId);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 检查是否有未完成的存储操作
     */
    private boolean hasPendingStorageOperations(String sessionId) {
        // 检查存储锁
        boolean hasLatches = storageLatches.entrySet().stream()
                .anyMatch(entry -> entry.getKey().startsWith(sessionId + "_"));

        // 检查原始Y值（如果还有原始Y值，说明存储可能未完成）
        boolean hasOriginalY = originalYValues.entrySet().stream()
                .anyMatch(entry -> entry.getKey().startsWith(sessionId + "_"));

        log.debug("存储操作检查 - latches: {}, originalY: {}, session: {}",
                hasLatches, hasOriginalY, sessionId);

        return hasLatches || hasOriginalY;
    }


    /**
     * 等待线程退出
     *
     * @param thread        要等待的线程
     * @param timeoutMillis 超时时间（毫秒）
     * @param sessionId     会话ID（用于日志）
     * @return true表示线程已退出，false表示超时
     */
    private boolean waitForThreadToExit(Thread thread, long timeoutMillis, String sessionId) {
        if (thread == null || !thread.isAlive()) {
            return true;
        }

        long startTime = System.currentTimeMillis();
        long checkInterval = 100; // 检查间隔100ms

        try {
            while (thread.isAlive()) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= timeoutMillis) {
                    log.warn("等待线程退出超时，已等待 {} ms, session: {}", elapsed, sessionId);
                    return false;
                }

                // 计算剩余等待时间
                long remaining = Math.min(checkInterval, timeoutMillis - elapsed);
                Thread.sleep(remaining);
            }

            log.info("线程已正常退出，等待时间: {} ms, session: {}",
                    System.currentTimeMillis() - startTime, sessionId);
            return true;

        } catch (InterruptedException e) {
            log.warn("等待线程退出时被中断, session: {}", sessionId);
            Thread.currentThread().interrupt();
            return false;
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
     * 发送消息并关闭连接2
     */
    private void sendClose(String sessionId, JSONObject msg) {
        log.info("【发送关闭】发送消息并关闭连接, session: {}, message: {}", sessionId, msg);
        try {
            sendMessage(sessionId, msg);
        } finally {
            cleanupResources(sessionId);
        }
    }



    /**
     * 设置默认参数
     *
     * @param command
     * @param spectrumParameter
     * @param task
     * @param taskId
     * @param sampleId
     * @param count
     * @throws IOException
     */
    private void doOneMeasurementSerial(DevOperationInputParam command,
                                        FSpectrumParameter spectrumParameter,
                                        FMeasureTask task,
                                        String taskId,
                                        String sampleId,
                                        Integer count) throws IOException {

        String sessionId = deviceSerialNumber;
        SessionState state = sessionStates.get(sessionId);

        synchronized (originalYValues) {
            // 清理当前session和sampleId的所有数据（不管number是多少）
            String prefix = sessionId + "_" + sampleId + "_";
            originalYValues.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
            log.info("清理原始Y值: sessionId={}, sampleId={}, 剩余keys: {}",
                    sessionId, sampleId, originalYValues.keySet());
        }

        // 标记当前测量开始
        if (state != null) {
            state.currentMeasurementCompleted.set(false);
        }

        // 设置测量开始状态
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
        MeasurementResult result = doOneMeasurementCore(command, spectrumParameter,task, sampleId, count);

        // 立即设置结束时间，表示测量完成
        Constants.endTime = LocalDateTime.now();

        // 确保即使发生异常也能释放资源
        if (result == null) {
            result = new MeasurementResult(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Constants.startTime,
                    Constants.endTime != null ? Constants.endTime : LocalDateTime.now(),
                    Collections.emptyList()
            );
        }

        // 标记测量结束
        if (state != null) {
            state.measurementStarted.set(false);
        }

        // 设置结束时间
        if (Constants.endTime == null) {
            Constants.endTime = LocalDateTime.now();
            log.info("设置测量结束时间: " + Constants.endTime);
        }

        // 立即清空缓存，释放锁
        Constants.instrumentData.clear();
        Constants.measurd_data.clear();
        Constants.originalCalculationData.clear();
        Constants.startTime = null;
        Constants.endTime = null;
        // 标记当前测量完成
        if (state != null) {
            state.currentMeasurementCompleted.set(true);
        }

        // 异步保存数据，不阻塞测量流程
        MeasurementResult finalResult = result;
        // 同步保存数据，等待保存完成
        String storageKey = sessionId + "_" + sampleId + "_" + count;
        CountDownLatch latch = new CountDownLatch(1);
        storageLatches.put(storageKey, latch);

        // 保存数据
        saveOneMeasurementWithSnapshots(task, taskId, sampleId, deviceSerialNumber,
                finalResult.x, finalResult.y, count, finalResult.startTime, finalResult.endTime,
                finalResult.changeParams, latch);

        // 等待存储完成，增加超时时间并记录
        try {
            long startWait = System.currentTimeMillis();
            boolean stored = latch.await(45, TimeUnit.SECONDS); // 等待45秒
            long waitTime = System.currentTimeMillis() - startWait;

            if (!stored) {
                log.warn("数据存储超时，耗时: {}ms, sampleId: {}, count: {}",
                        waitTime, sampleId, count);
            } else {
                log.info("数据存储完成，耗时: {}ms, sampleId: {}, count: {},",
                        waitTime, sampleId, count);
            }
        } catch (InterruptedException e) {
            log.warn("等待数据存储时被中断，但数据保存会继续在后台进行, sampleId: {}, count: {},",
                    sampleId, count);
            Thread.currentThread().interrupt();
            // 不抛出异常，让清理流程继续
        } finally {
            storageLatches.remove(storageKey);
        }

        // 存储完成后才通知前端
        JSONObject finishMessage = new JSONObject();
        finishMessage.put("result", "measurement_complete");
        finishMessage.put("sampleId", sampleId);
        finishMessage.put("count", count);
        sendMessage(sessionId, finishMessage);
    }

    // doOneMeasurementCore 改造，返回 MeasurementResult
    private MeasurementResult doOneMeasurementCore(DevOperationInputParam command,
                                                   FSpectrumParameter spectrumParameter,
                                                   FMeasureTask task,
                                                   String sampleId,
                                                   Integer count) throws IOException {
        String luminosity = null;
        switch (spectrumParameter.getLuminosity()) {
            case "1":
                luminosity = "Abs";
                break;
            case "2":
                luminosity = "T%";
                break;
            case "3":
                luminosity = "Es";
                break;
            case "4":
                luminosity = "Er";
                break;
            default:
                luminosity = "Abs";
                break;
        }
        // 发送测量命令
        devOperationUtilService.operation(command);
        Constants.startTime = LocalDateTime.now();
        lastChangeParam=null;

        SessionState state = sessionStates.get(deviceSerialNumber);
        if (state != null) {
            state.measurementStarted.set(true);
        }
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        List<ChangeParam> changeParams = new ArrayList<>();

        // 判断是否为极快扫描模式
        boolean isFastScan = "3".equals(spectrumParameter.getScanSpeed());

        if (isFastScan) {
            // 极快扫描模式：优化版本（保持不变）
            ProcessedData processedData = processAndPushFastScanData(spectrumParameter, count,
                    task, sampleId, luminosity, spectrumParameter.getLadderCorrection(), deviceSerialNumber);

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
                    processedData.changeParams
            );
        } else {
            boolean whileStatus = true;
            // 普通扫描模式：使用批次处理机制
            try {
                while (whileStatus) {

                    // 处理所有可用的数据点
                    while (whileStatus) {

                        Object result = unknowCountMessageQueue.pollMessage(deviceSerialNumber, T600ComIdEnums.Scan.getComId());
                        // 如果队列为空（可选：根据你的 pollMessage 实现决定是否需要）
                        if (result == null) {
                            // 可选：短暂休眠避免 CPU 空转
                            Thread.sleep(10); // 比如 10ms
                            continue;
                        }
                        // 判断是否是结束标记
                        if (result instanceof EndMarker) {
                            EndMarker endMarker = (EndMarker) result;
                            // 可选：记录结束原因或打日志
                            switch (endMarker.getReason()) {
                                case TIMEOUT:
                                    log.warn("设备 {} 的测量超时", deviceSerialNumber);
                                    break;
                                case UN_NORMAL:
                                    log.error("设备 {} 的测量异常结束", deviceSerialNumber);
                                    break;
                                case NORMAL:
                                    log.info("设备 {} 的测量正常完成", deviceSerialNumber);
                                    break;
                                case NORMAL_STOP:
                                    log.info("设备 {} 的测量已停止", deviceSerialNumber);
                                    break;
                            }
                            whileStatus = false;
                            break;
                        }

                        SpectrumPoint message = (SpectrumPoint) result;
                        Double startWaveLength = Double.valueOf(spectrumParameter.getStartWaveLength());
                        Double endWaveLength = Double.valueOf(spectrumParameter.getEndWaveLength());
                        if (message.getWavelength()>startWaveLength||message.getWavelength()<endWaveLength){
                            log.info("当前波长不在测量范围内，跳过该波长: "+message.getWavelength());
                            continue;
                        }
                        x.add(message.getWavelength());
                        if (spectrumParameter.getLuminosity().equals("1")) {
                            y.add(Double.valueOf(message.getAbsResult().getAbs()));
                        }else if (spectrumParameter.getLuminosity().equals("2")) {
                            y.add(Double.valueOf(message.getTResult().getTPercent()));
                        }else if (spectrumParameter.getLuminosity().equals("3")) {
                            y.add(Double.valueOf(message.getEsResult().getEs()));
                        }else {
                            y.add(Double.valueOf(message.getErResult().getEr()));
                        }
                        changeParams.add(message.getMeasureRsd());


                        // 将新点添加到批次缓冲区，并检查是否达到批次大小
                        if (!x.isEmpty() && !y.isEmpty()) {
                            int currentIndex = x.size() - 1;
                            Double currentX = x.get(currentIndex);
                            Double currentY = y.get(currentIndex);

                            boolean isBatchFull = state.addToBatchBuffer(currentX, currentY);

                            if (isBatchFull) {
                                // 批次已满，进行处理
                                BatchData batchData = state.getAndClearBatchBuffer(deviceSerialNumber, count, sampleId, state.digit,
                                        spectrumParameter, task);

                                if (batchData != null) {
                                    // 异步处理台阶消除
                                    processBatchDataAsync(batchData);
                                }
                            }
                        }
                    }
                }

                // 测量结束，处理所有剩余数据
                log.info("开始处理剩余数据，批次大小配置: {}", state.getBatchSize());

                // 处理剩余的批次数据
                BatchData remainingBatch = state.getRemainingBatchBuffer(count, sampleId, state.digit,
                        spectrumParameter, task);

                if (remainingBatch != null && !remainingBatch.xBatch.isEmpty()) {
                    log.info("处理剩余批次数据，点数: {}", remainingBatch.xBatch.size());
                    processBatchDataAsync(remainingBatch);
                }

                // 等待所有异步处理完成
                waitForAsyncProcessingComplete(state);

                log.info("测量完成，总数据点: " + x.size());

            } catch (Exception e) {
                log.error("测量过程中发生异常", e);
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
                changeParams
        );
    }


    /**
     * 异步处理批次数据（包含台阶消除）
     */
    private void processBatchDataAsync(BatchData batchData) {
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

                String key = batchData.sessionId + "_" + batchData.sampleId + "_" + batchData.count;
                synchronized (originalYValues) {
                    // 如果key已经存在，先清理旧数据
                    if (originalYValues.containsKey(key)) {
                        log.warn("发现重复的key，清理旧数据: {}", key);
                    }
                    // 总是创建新的列表，避免追加到旧列表
                    originalYValues.put(key, new ArrayList<>(yForStorage));
                }

                // 推送处理后的数据
                pushBatchDataDirectly(batchData.xBatch, processedY,
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
                    pushBatchDataDirectly(batchData.xBatch, batchData.yBatch,
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
    private void pushBatchDataDirectly(List<Double> xBatch,
                                       List<Double> yBatch,
                                       Integer count,
                                       String sampleId,
                                       String luminosity,
                                       Integer digit) throws IOException {
        if (xBatch.isEmpty() || yBatch.isEmpty()) {
            return;
        }

        SessionState state = sessionStates.get(deviceSerialNumber);
        if (state == null || state.sessionClosing.get()) {
            return;
        }

        // 逐个推送数据点
        for (int i = 0; i < xBatch.size(); i++) {
            pushSinglePoint( xBatch.get(i), yBatch.get(i),
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
    private void waitForAsyncProcessingComplete(SessionState state) {
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
     * 处理并推送一批数据（进行台阶消除后逐个推送）
     */
    private void processAndPushBatchData(WebSocketSession session,
                                         List<Double> xBatch,
                                         List<Double> yBatch,
                                         int startIndex,
                                         Integer count,
                                         FMeasureTask task,
                                         String sampleId,
                                         FSpectrumParameter parameter,
                                         Integer digit) throws IOException {
        if (xBatch.isEmpty() || yBatch.isEmpty()) {
            return;
        }
        boolean ladderCorrection = parameter.getLadderCorrection();
        String luminosity = parameter.getLuminosity();
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

            pushSinglePoint(xBatch.get(i), processedY.get(i),
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
    }

    /**
     * 推送单个数据点
     */
    private void pushSinglePoint(Double x,
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
        SessionState state = sessionStates.get(deviceSerialNumber);
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
        point.put("roundOffY", valueY);

        point.put("xType", "nm");
        // 光度模式是1.Abs 2.T% 3.Es 4.Er
        String luminosityType = luminosity.equals("1") ? "Abs" : luminosity.equals("2") ? "T%" : luminosity.equals("3") ? "Es" : "Er";
        point.put("yType", luminosityType);
        dataArray.add(point);

        JSONObject payload = new JSONObject();
        payload.put("data", dataArray);
        payload.put("sampleId", sampleId);
        payload.put("count", count);
        payload.put("currentPoolNum", currentSampleCell);

        sendMessage(deviceSerialNumber, payload);

        // 添加小延迟，避免推送过快
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("推送线程被中断");
        }
    }

    /**
     * 处理极快扫描模式的数据 - 修复版本
     */
    private ProcessedData processAndPushFastScanData(FSpectrumParameter spectrumParameter,
                                                     Integer count, FMeasureTask task,
                                                     String sampleId, String luminosity,
                                                     boolean ladderCorrection,
                                                     String deviceSerialNumber) throws IOException {

        log.info("开始极快扫描模式数据处理");

        // 等待数据采集完成
        int minExpectedPoints = calculateExpectedDataPoints(spectrumParameter);

        log.info("期望数据点数: " + minExpectedPoints);

        List<Double> processedX = new ArrayList<>();
        List<Double> processedY = new ArrayList<>();
        List<ChangeParam> changeParams = new ArrayList<>();

        while (true) {
            try {
                Object result = unknowCountMessageQueue.pollMessage(deviceSerialNumber, T600ComIdEnums.ScanFast.getComId());
                // 如果队列为空（可选：根据你的 pollMessage 实现决定是否需要）
                if (result == null) {
                    // 可选：短暂休眠避免 CPU 空转
                    Thread.sleep(10); // 比如 10ms
                    continue;
                }
                // 判断是否是结束标记
                if (result instanceof EndMarker) {
                    EndMarker endMarker = (EndMarker) result;
                    // 可选：记录结束原因或打日志
                    switch (endMarker.getReason()) {
                        case TIMEOUT:
                            log.warn("设备 {} 的测量超时", deviceSerialNumber);
                            break;
                        case UN_NORMAL:
                            log.error("设备 {} 的测量异常结束", deviceSerialNumber);
                            break;
                        case NORMAL:
                            log.info("设备 {} 的测量正常完成", deviceSerialNumber);
                            break;
                        case NORMAL_STOP:
                            log.info("设备 {} 的测量已停止", deviceSerialNumber);
                            break;
                    }
                    // 结束循环
                    break;
                }
                SpectrumPoint message = (SpectrumPoint) result;
                Double startWaveLength = Double.valueOf(spectrumParameter.getStartWaveLength());
                Double endWaveLength = Double.valueOf(spectrumParameter.getEndWaveLength());
                if (message.getWavelength()>startWaveLength||message.getWavelength()<endWaveLength){
                    log.info("当前波长不在测量范围内，跳过该波长: "+message.getWavelength());
                    continue;
                }
                processedX.add(message.getWavelength());
                if (spectrumParameter.getLuminosity().equals("1")) {
                    processedY.add(Double.valueOf(message.getAbsResult().getAbs()));
                }else if (spectrumParameter.getLuminosity().equals("2")) {
                    processedY.add(Double.valueOf(message.getTResult().getTPercent()));
                }else if (spectrumParameter.getLuminosity().equals("3")) {
                    processedY.add(Double.valueOf(message.getEsResult().getEs()));
                }else {
                    processedY.add(Double.valueOf(message.getErResult().getEr()));
                }
                changeParams.add(message.getMeasureRsd());
            } catch (InterruptedException e) {
                log.error("等待数据采集完成时发生中断异常", e);
                Thread.currentThread().interrupt();
            }
        }
        List<Double> yForStorage = processedY; // 用于存储的Y值
        // 如果需要台阶校正
        if (ladderCorrection) {
            log.info("开始台阶校正");
            List<Double> correctedY = processAndSaveData(task, processedX, processedY,
                    spectrumParameter, sampleId);
            processedY = correctedY;
            yForStorage = correctedY; // 开启校正时存储校正后的数据
            log.info("台阶校正完成");
        } else {
            // 未开启校正时存储原始数据
            yForStorage = processedY;
        }

        // 关键修改：根据是否开启阶梯校正来决定存储的数据
        String key = deviceSerialNumber + "_" + sampleId + "_" + count;
        synchronized (originalYValues) {
            // 清理当前key的旧数据
            if (originalYValues.containsKey(key)) {
                log.warn("极快扫描模式：清理重复key的旧数据: {}", key);
            }
            // 根据阶梯校正状态决定存储的数据
            originalYValues.put(key, new ArrayList<>(yForStorage));
        }

        // 一次性推送所有数据 - 使用处理后的数据（校正或原始）
        pushAllDataAtOnce( processedX, processedY, count, sampleId, luminosity);

        log.info("极快扫描模式数据处理完成");
        return new ProcessedData(processedX, processedY, changeParams);
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
     * 精确舍入到指定小数位
     */
    private double roundToPrecision(double value, int decimalPlaces) {
        double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }

    /**
     * 一次性推送所有数据（推送时格式化Y值）- 修正版本
     */
    private void pushAllDataAtOnce(List<Double> x, List<Double> y,
                                   Integer count, String sampleId,
                                   String luminosity) throws IOException {
        if (x.isEmpty() || y.isEmpty()) {
            log.warn("没有数据可推送，x大小: " + x.size() + ", y大小: " + y.size());
            return;
        }

        SessionState state = sessionStates.get(deviceSerialNumber);
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
            point.put("roundOffY", valueY);
            point.put("xType", "nm");

            // 光度模式
            String luminosityType;
            switch (luminosity) {
                case "1":
                    luminosityType = "Abs";
                    break;
                case "2":
                    luminosityType = "T%";
                    break;
                case "3":
                    luminosityType = "Es";
                    break;
                case "4":
                    luminosityType = "Er";
                    break;
                default:
                    luminosityType = "Abs";
                    break;
            }
            point.put("yType", luminosityType);

            dataArray.add(point);
        }

        JSONObject payload = new JSONObject();
        payload.put("data", dataArray);
        payload.put("sampleId", sampleId);
        payload.put("count", count);
        payload.put("scanMode", "fast");

        log.info("推送数据信息:");
        log.info("数据点数: " + dataArray.size());
        log.info("光度模式: " + luminosity);

        sendMessage(deviceSerialNumber, payload);
        log.info("数据推送完成");
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
        if (stopWaveLength.equals(constrainedWavelength)) {
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
            if (true) {
                // 判断光度模式
                if ("1".equals(luminosityMode)) {
                    // Abs的台阶消除数值
                    if (confVO.getAbs() != null && !confVO.getAbs().trim().isEmpty()) {
                        threshold = Integer.valueOf(confVO.getAbs());
                    }
                } else if ("2".equals(luminosityMode)) {
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
        List<Double> processedY = callStepEliminationPython(x, y, threshold, parameter);

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

    // 保存一次测量结果

    /**
     * 使用快照保存一次测量结果，避免与全局变量冲突
     */
    private void saveOneMeasurementWithSnapshots(FMeasureTask task,
                                                 String taskId,
                                                 String sampleId,
                                                 String deviceSerialNumber,
                                                 List<Double> x,
                                                 List<Double> y,
                                                 Integer number,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime,
                                                 List<ChangeParam> changeParams,
                                                 CountDownLatch latch) {
        // 检查数据是否为空
        if (x.isEmpty() || y.isEmpty()) {
            log.warn("测量数据为空，不保存: 样品={}, 计数={}", sampleId, number);
            if (latch != null) {
                latch.countDown();  // 仍然要通知完成
            }
            return;
        }

        String sessionId =deviceSerialNumber;
        String storageKey = sessionId + "_" + sampleId + "_" + number;

        try {
            SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
            // 1. 处理峰谷数据
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();

            // 使用原始Y值进行峰谷分析
            String key =deviceSerialNumber+ "_" + sampleId + "_" + number;
            List<Double> originalY = originalYValues.get(key);
            if (originalY == null) {
                log.warn("未找到原始Y值，使用处理后的Y值进行峰谷分析: key={}, x.size={}, y.size={}",
                        key, x.size(), y.size());
                originalY = y;
            } else {
                // 验证数据长度是否匹配
                if (originalY.size() != x.size()) {
                    log.error("原始Y值与X值长度不匹配: originalY.size={}, x.size={}, key={}",
                            originalY.size(), x.size(), key);
                    // 如果长度不匹配，使用处理后的Y值
                    originalY = y;
                } else {
                    // 检查是否有0值
                    int zeroCount = 0;
                    for (Double value : originalY) {
                        if (value == null || Math.abs(value) < 1e-10) {
                            zeroCount++;
                        }
                    }
                    if (zeroCount > x.size() * 0.5) { // 如果超过50%是0值
                        log.error("原始Y值中有过多0值({}/{}), 使用处理后的Y值: key={}",
                                zeroCount, x.size(), key);
                        originalY = y;
                    }
                }
            }

            String peakUrl = pythonPathConstant.getPeakSearching();
            String valleyUrl = pythonPathConstant.getValleySearching();

            List<FPeakValleyMarking> peakValleys = new ArrayList<>();
            List<FPeakValleyMarking> markings = new ArrayList<>();
            try {
                // 添加日志输出（调试用）
                System.out.println("调用寻峰接口: " + peakUrl);
                System.out.println("调用寻谷接口: " + valleyUrl);

                body.put("x_data", x);
                body.put("y_data", originalY);
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
                body.clear();
                body.put("x_data", x);
                body.put("y_data", originalY);
                double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
                body.put("height", height);
                body.put("distance", confVO.getPeakInterval());
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
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
                // 失败时不处理，继续执行保存原始数据
            }

            // 构建结果字符串，存储时使用原始Y值
            List<String> result = new ArrayList<>();
            for (int i = 0; i < x.size(); i++) {
                double originalYValue = originalY != null && i < originalY.size() ? originalY.get(i) : y.get(i);
                result.add(x.get(i) + "," + originalYValue);
            }

            // 2. 保存测量数据
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
            FMeasureSample sample = tMeasureSampleService.getById(taskSpecimen.getSpecimenKey());
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
                    existingMeasureData.setInstrumentData(JSON.toJSONString(changeParams));
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
                existingAnalysisData.setAnalysisParam(JSON.toJSONString(changeParams));
                existingAnalysisData.setAnalysisResult(JSON.toJSONString(result));
                existingAnalysisData.setPeak(JSON.toJSONString(peakValleys));
                existingAnalysisData.setGrain(JSON.toJSONString(markings));
                existingAnalysisData.setColor(ColorUtil.getRandomColor());
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
                    measureData.setInstrumentData(JSON.toJSONString(changeParams));
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
                    analysisData.setAnalysisParam(JSON.toJSONString(changeParams));
                    analysisData.setAnalysisResult(JSON.toJSONString(result));
                    analysisData.setAnalysisType("5");
                    analysisData.setPeak(JSON.toJSONString(peakValleys));
                    analysisData.setGrain(JSON.toJSONString(markings));
                    analysisData.setColor(ColorUtil.getRandomColor());
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
                    measureData.setInstrumentData(JSON.toJSONString(changeParams));
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
                    analysisData.setAnalysisParam(JSON.toJSONString(changeParams));
                    analysisData.setAnalysisResult(JSON.toJSONString(result));
                    analysisData.setAnalysisType("5");
                    analysisData.setPeak(JSON.toJSONString(peakValleys));
                    analysisData.setGrain(JSON.toJSONString(markings));
                    analysisData.setColor(ColorUtil.getRandomColor());
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
            // 确保在finally中清理，无论是否异常
            // 1. 清理存储的原始Y值
            originalYValues.remove(storageKey);

            // 2. 清理存储锁
            storageLatches.remove(storageKey);

            // 3. 通知存储完成
            if (latch != null) {
                latch.countDown();
                log.debug("存储完成通知已发送, key: {}", storageKey);
            }

            // 4. 更新会话状态（如果会话还存在）
            SessionState state = sessionStates.get(sessionId);
            if (state != null) {
                state.currentMeasurementCompleted.set(true);
            }

            log.info("数据保存完成: 样品={}, 计数={}, session={}", sampleId, number, sessionId);
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
        Constants.measureStopStatus.clear();
    }

    // 返回测量结果的类
    class MeasurementResult {
        List<Double> x;
        List<Double> y;
        LocalDateTime startTime;
        LocalDateTime endTime;
        List<ChangeParam> changeParams;

        MeasurementResult(List<Double> x,
                          List<Double> y,
                          LocalDateTime startTime,
                          LocalDateTime endTime,
                          List<ChangeParam> changeParams) {
            this.x = x;
            this.y = y;
            this.startTime = startTime;
            this.endTime = endTime;
            this.changeParams = changeParams;
        }
    }

    /**
     * 处理后的数据容器
     */
    class ProcessedData {
        List<Double> x;
        List<Double> y;
        List<ChangeParam> changeParams;

        ProcessedData(List<Double> x, List<Double> y, List<ChangeParam> changeParams) {
            this.x = x;
            this.y = y;
            this.changeParams = changeParams;
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


    private void sendMessage(String deviceSerialNumber, JSONObject object) {

        WebSocketResponseVO vo = new WebSocketResponseVO();
        vo.setOperation(WebSocketOperationEnum.ConciseSpectrumOne.getOperation());
        vo.setData(object);

        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }

    private void sendError(String deviceSerialNumber, String message) {
        infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
        webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.ConciseSpectrumOne.getOperation(), message);
        cleanupResources(deviceSerialNumber);
    }
}