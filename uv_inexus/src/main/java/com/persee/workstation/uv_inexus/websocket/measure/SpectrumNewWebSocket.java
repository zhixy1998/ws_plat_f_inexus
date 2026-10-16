package com.persee.workstation.uv_inexus.websocket.measure;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumParameter;
import com.persee.workstation.uv_inexus.enums.T600ComIdEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.ScanSpeedEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.TaskStatusEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.SamplePoolConfigPO;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.PeakSearchParam;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.ColorUtil;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.ActionCmdEnum;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketOperationEnum;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketResponseVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketSendUtil;
import com.persee.workstation.uv_inexus.websocket.measure.param.SpectrumNewWebSocketParam;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class SpectrumNewWebSocket {

    private static final Logger log = LoggerFactory.getLogger(SpectrumNewWebSocket.class);
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

    // 灯状态(true:没问题 false:有问题)
    private static boolean lightStatus = true;

    @Autowired
    private SpectrumService spectrumService;
    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private DevMessageUtil devMessageUtil;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private TMeasureDataService traceDataService;
    @Autowired
    private TMeasureMethodService tMeasureMethodService;
    @Autowired
    private SystemAlgorithmConfService systemAlgorithmConfService;
    @Autowired
    private SamplePoolConfigService fSamplePoolConfigService;
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Autowired
    private TMeasureDataService tMeasureDataService;
    @Autowired
    private TMeasureSampleService tMeasureSampleService;
    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;
    @Autowired
    private TMeasureAnalysisImageService tMeasureAnalysisImageService;
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

        private final AtomicBoolean finalMessageSent = new AtomicBoolean(false);

        // 批次数据缓冲区
        private final List<Double> batchXBuffer = Collections.synchronizedList(new ArrayList<>());
        private final List<Double> batchYBuffer = Collections.synchronizedList(new ArrayList<>());

        // 已推送点计数
        private final AtomicInteger pushedPointsCount = new AtomicInteger(0);

        private String deviceSerialNumber;
        private String ip;
        private Integer digit;
        private Integer batchSize; // 批次大小

        private final AtomicInteger batchSeqGenerator = new AtomicInteger(0);
        public int nextBatchSeq() { return batchSeqGenerator.getAndIncrement(); }

        // 匀速推送调度器映射：sampleId_count -> SmoothPushScheduler
        private final Map<String, SmoothPushScheduler> smoothPushSchedulers = new ConcurrentHashMap<>();
        private SpectrumNewWebSocket parent; // 父类引用

        public void setParent(SpectrumNewWebSocket parent) {
            this.parent = parent;
        }

        public SmoothPushScheduler getOrCreateScheduler(String sampleId, Integer count, Integer digit, String luminosity,
                                                        java.util.function.Supplier<String> currentSampleCellSupplier) {
            String key = sampleId + "_" + count;
            return smoothPushSchedulers.computeIfAbsent(key, k ->
                    new SmoothPushScheduler(parent, deviceSerialNumber, sampleId, count, digit, luminosity, currentSampleCellSupplier));
        }

        public void cleanupScheduler(String sampleId, Integer count) {
            String key = sampleId + "_" + count;
            SmoothPushScheduler scheduler = smoothPushSchedulers.remove(key);
            if (scheduler != null) scheduler.cleanup();
        }

        public void cleanupAllSchedulers() {
            for (SmoothPushScheduler scheduler : smoothPushSchedulers.values()) {
                scheduler.cleanup();
            }
            smoothPushSchedulers.clear();
        }

        public void waitForAllSchedulersToFinish(long timeoutMs) {
            long startTime = System.currentTimeMillis();
            for (SmoothPushScheduler scheduler : smoothPushSchedulers.values()) {
                long remaining = timeoutMs - (System.currentTimeMillis() - startTime);
                if (remaining <= 0) {
                    log.warn("等待调度器完成超时，session: {}", deviceSerialNumber);
                    break;
                }
                while (scheduler.isRunning() && System.currentTimeMillis() - startTime < timeoutMs) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            log.info("所有调度器已停止或超时，session: {}", deviceSerialNumber);
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
                                                Integer digit, SpectrumParameter parameter,
                                                TMeasureTask task) {
            synchronized (batchXBuffer) {
                if (batchXBuffer.isEmpty()) {
                    return null;
                }
                int startIndex = pushedPointsCount.get();
                int seq = nextBatchSeq();
                BatchData batchData = new BatchData(
                        new ArrayList<>(batchXBuffer),
                        new ArrayList<>(batchYBuffer),
                        startIndex,
                        count,
                        sessionId,
                        sampleId,
                        digit,
                        parameter,
                        task,
                        seq
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
                                                 Integer digit, SpectrumParameter parameter,
                                                 TMeasureTask task) {
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
     * 匀速推送调度器 - 比例调速版
     * 根据队列长度，在原始间隔基础上按比例调整
     */
    private static class SmoothPushScheduler {
        private final String sessionId;
        private final String sampleId;
        private final Integer count;
        private final Integer digit;
        private final String luminosity;
        private final AtomicBoolean isRunning = new AtomicBoolean(false);
        private final AtomicBoolean isFinished = new AtomicBoolean(false);
        private final Queue<SmoothPushTask> pendingQueue = new PriorityBlockingQueue<>();
        private ScheduledExecutorService scheduler;
        private long currentIntervalMs = 0;          // 当前推送间隔（毫秒）
        private long baseIntervalMs = 0;              // 基础推送间隔（毫秒）
        private boolean intervalCalculated = false;

        // 动态调速阈值
        private static final int ACCELERATE_THRESHOLD = 100;   // 积压阈值：超过100个点加速
        private static final int NORMAL_LOWER_THRESHOLD = 50;  // 正常范围下限：50个点
        private static final int SLOW_UPPER_THRESHOLD = 10;    // 减速阈值：低于10个点减速

        // 调速系数（在原始间隔基础上乘以此系数）
        private static final double ACCELERATE_RATIO = 0.1;    // 加速时：原始间隔 * 0.1
        private static final double SLOW_RATIO = 2.0;          // 减速时：原始间隔 * 2

        private final SpectrumNewWebSocket parent;
        private final java.util.function.Supplier<String> currentSampleCellSupplier;

        public SmoothPushScheduler(SpectrumNewWebSocket parent,
                                   String sessionId,
                                   String sampleId,
                                   Integer count,
                                   Integer digit,
                                   String luminosity,
                                   java.util.function.Supplier<String> currentSampleCellSupplier) {
            this.parent = parent;
            this.sessionId = sessionId;
            this.sampleId = sampleId;
            this.count = count;
            this.digit = digit;
            this.luminosity = luminosity;
            this.currentSampleCellSupplier = currentSampleCellSupplier;
        }

        public synchronized void receiveBatch(List<Double> xBatch, List<Double> yBatch, int batchSeq) {
            for (int i = 0; i < xBatch.size(); i++) {
                pendingQueue.add(new SmoothPushTask(xBatch.get(i), yBatch.get(i), batchSeq, i));
            }

            int queueSize = pendingQueue.size();
            log.info("接收批次数据: session={}, 点数={}, 队列总点数={}",
                    sessionId, xBatch.size(), queueSize);

            if (!intervalCalculated) {
                if (Constants.pushInterval != null && Constants.pushInterval > 0) {
                    baseIntervalMs = (long) (Constants.pushInterval * 1000);
                    if (baseIntervalMs < 1) baseIntervalMs = 1;
                    log.info("使用外部指定推送间隔: {} 秒 = {} ms", Constants.pushInterval, baseIntervalMs);
                } else {
                    baseIntervalMs = 10;
                    log.info("未指定推送间隔，使用默认值: {} ms", baseIntervalMs);
                }
                currentIntervalMs = baseIntervalMs;
                intervalCalculated = true;
                startScheduling();
                return;
            }

            checkAndAdjustSpeed();
        }

        private void checkAndAdjustSpeed() {
            int queueSize = pendingQueue.size();
            long newInterval = currentIntervalMs;

            if (queueSize > ACCELERATE_THRESHOLD) {
                newInterval = Math.max(1, (long) (baseIntervalMs * ACCELERATE_RATIO));
                if (currentIntervalMs != newInterval) {
                    log.info("队列积压({}点)超过阈值{}，加速推送: {}ms -> {}ms",
                            queueSize, ACCELERATE_THRESHOLD, currentIntervalMs, newInterval);
                    currentIntervalMs = newInterval;
                    restartScheduler();
                }
            } else if (queueSize <= SLOW_UPPER_THRESHOLD) {
                newInterval = (long) (baseIntervalMs * SLOW_RATIO);
                if (currentIntervalMs != newInterval) {
                    log.info("队列({}点)低于阈值{}，略微降速: {}ms -> {}ms",
                            queueSize, SLOW_UPPER_THRESHOLD, currentIntervalMs, newInterval);
                    currentIntervalMs = newInterval;
                    restartScheduler();
                }
            } else if (queueSize <= NORMAL_LOWER_THRESHOLD && currentIntervalMs != baseIntervalMs) {
                log.info("队列({}点)恢复正常范围，恢复原始速度: {}ms -> {}ms",
                        queueSize, currentIntervalMs, baseIntervalMs);
                currentIntervalMs = baseIntervalMs;
                restartScheduler();
            }
        }

        private void restartScheduler() {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            isRunning.set(false);
            startScheduling();
        }

        private void startScheduling() {
            if (isRunning.compareAndSet(false, true)) {
                log.info("启动匀速推送调度: session={}, 待推送点数={}, 推送间隔={}ms",
                        sessionId, pendingQueue.size(), currentIntervalMs);
                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(() -> {
                    pushOnePoint();
                }, 0, currentIntervalMs, TimeUnit.MILLISECONDS);
            }
        }

        private void pushOnePoint() {
            if (isFinished.get()) return;

            SmoothPushTask task = pendingQueue.poll();
            if (task == null) return;

            int remainingSize = pendingQueue.size();
            if (remainingSize % 20 == 0) {
                checkAndAdjustSpeed();
            }

            SessionState state = sessionStates.get(sessionId);
            if (state == null || state.sessionClosing.get()) {
                stopScheduling();
                return;
            }

            try {
                JSONArray dataArray = new JSONArray();
                JSONObject point = new JSONObject();
                point.put("x", task.x);

                Double value = DataCountUtil.formatValue(digit, task.y);
                String valueY = DataCountUtil.addZero(value, digit);
                point.put("y", task.y);
                point.put("roundOffY", valueY);
                point.put("xType", "nm");

                String luminosityType = luminosity.equals("1") ? "Abs" :
                        luminosity.equals("2") ? "T%" :
                                luminosity.equals("3") ? "Es" :
                                        luminosity.equals("5") ? "R%" : "Er";
                point.put("yType", luminosityType);
                dataArray.add(point);

                JSONObject payload = new JSONObject();
                payload.put("data", dataArray);
                payload.put("sampleId", sampleId);
                payload.put("count", count);
                payload.put("currentPoolNum", currentSampleCellSupplier.get());
                payload.put("smoothPush", true);

                parent.sendMessage(sessionId, payload);
                log.debug("匀速推送一个点: x={}, y={}, 剩余点数={}", task.x, task.y, remainingSize);
            } catch (Exception e) {
                log.error("匀速推送单点失败", e);
            }
        }

        private void stopScheduling() {
            if (isRunning.compareAndSet(true, false)) {
                if (scheduler != null && !scheduler.isShutdown()) {
                    scheduler.shutdown();
                    try {
                        if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                            scheduler.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        scheduler.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }
                log.info("匀速推送调度停止: session={}", sessionId);
            }
        }

        public synchronized void flush() {
            stopScheduling();
            if (pendingQueue.isEmpty()) return;
            log.info("测量结束，立即推送所有剩余点: session={}, 点数={}", sessionId, pendingQueue.size());
            SmoothPushTask task;
            while ((task = pendingQueue.poll()) != null) {
                pushSinglePointDirect(task.x, task.y);
            }
        }

        private void pushSinglePointDirect(Double x, Double y) {
            if (x == null || y == null) return;
            SessionState state = sessionStates.get(sessionId);
            if (state == null || state.sessionClosing.get()) return;

            try {
                JSONArray dataArray = new JSONArray();
                JSONObject point = new JSONObject();
                point.put("x", x);
                Double value = DataCountUtil.formatValue(digit, y);
                String valueY = DataCountUtil.addZero(value, digit);
                point.put("y", y);
                point.put("roundOffY", valueY);
                point.put("xType", "nm");
                String luminosityType = luminosity.equals("1") ? "Abs" :
                        luminosity.equals("2") ? "T%" :
                                luminosity.equals("3") ? "Es" :
                                        luminosity.equals("5") ? "R%" : "Er";
                point.put("yType", luminosityType);
                dataArray.add(point);

                JSONObject payload = new JSONObject();
                payload.put("data", dataArray);
                payload.put("sampleId", sampleId);
                payload.put("count", count);
                payload.put("currentPoolNum", currentSampleCellSupplier.get());
                payload.put("smoothPush", true);
                parent.sendMessage(sessionId, payload);
            } catch (Exception e) {
                log.error("直接推送单点失败", e);
            }
        }

        public boolean isRunning() {
            return isRunning.get();
        }

        public void cleanup() {
            isFinished.set(true);
            stopScheduling();
            pendingQueue.clear();
        }

        private static class SmoothPushTask implements Comparable<SmoothPushTask> {
            final Double x;
            final Double y;
            final int batchSeq;
            final int pointIndex;

            SmoothPushTask(Double x, Double y, int batchSeq, int pointIndex) {
                this.x = x;
                this.y = y;
                this.batchSeq = batchSeq;
                this.pointIndex = pointIndex;
            }

            @Override
            public int compareTo(SmoothPushTask o) {
                int cmp = Integer.compare(this.batchSeq, o.batchSeq);
                if (cmp != 0) return cmp;
                return Integer.compare(this.pointIndex, o.pointIndex);
            }
        }
    }
    /**
     * 批次数据容器
     */
    private static class BatchData {
        List<Double> xBatch;
        List<Double> yBatch;
        List<ChangeParam> changeParamBatch;
        int startIndex;
        String sessionId;
        Integer count;
        String sampleId;
        Integer digit;
        SpectrumParameter parameter;
        TMeasureTask task;
        int batchSeq;

        public BatchData(List<Double> xBatch, List<Double> yBatch, int startIndex,
                         Integer count,String sessionId, String sampleId, Integer digit,
                         SpectrumParameter parameter, TMeasureTask task, int batchSeq) {
            this.xBatch = new ArrayList<>(xBatch);
            this.yBatch = new ArrayList<>(yBatch);
            this.startIndex = startIndex;
            this.sessionId = sessionId;
            this.count = count;
            this.sampleId = sampleId;
            this.digit = digit;
            this.parameter = parameter;
            this.task = task;
            this.batchSeq = batchSeq;
        }
    }

    /**
     * 当WebSocket连接建立后调用此方法
     */
    public void measureTask(SpectrumNewWebSocketParam param) {
        log.info("仪器编号: {} 进入扫描 时间: {}", param.getDeviceSerialNumber(), LocalDateTime.now());

        // 清理公共变量
        clearGlobalData();

        // 初始化灯状态值
        lightStatus = true;

        Constants.measureStopStatus.put(param.getDeviceSerialNumber(), true);
        currentSampleCell=null;
        // 初始化会话状态
        SessionState state = new SessionState();
        state.setParent(this);
        sessionStates.put(param.getDeviceSerialNumber(), state);

        deviceSerialNumber = param.getDeviceSerialNumber();

        state.setBatchSize(staircaseEliminationBatchSize);

        // 初始化发送锁
        sessionSendLocks.put(deviceSerialNumber, new Object());

        infoService.updateDeviceCurrentStatus(deviceSerialNumber,WebSocketOperationEnum.Spectrum.getOperation(), param.getTaskId(), Constants.DEVICE_STATUS_MEASURE);

        if (Constants.endTime != null || Constants.startTime != null) {
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("error", "当前测量未结束,无法开启新的测试！");
            sendClose(deviceSerialNumber, errorMsg);
            return;
        }

        String taskId = param.getTaskId();
        deviceSerialNumber = param.getDeviceSerialNumber();
        String updateBy = param.getUpdateBy();
        Integer digit = param.getDigit();

        if (taskId == null) {
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("error", "缺少 taskId 参数");
            sendClose(deviceSerialNumber, errorMsg);
            return;
        }
        if (updateBy == null) {
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("error", "缺少 updateBy 参数");
            sendClose(deviceSerialNumber, errorMsg);
            return;
        }

        // 设置会话状态
        state.deviceSerialNumber = deviceSerialNumber;
        state.digit = digit;

        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null || task.getSampleKey() == null || task.getSampleKey().equals("")) {
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("error", "请先创建样品");
            sendClose(deviceSerialNumber, errorMsg);
            return;
        }
        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("error", "方法不存在");
            sendClose(deviceSerialNumber, errorMsg);
            return;
        }

        // 将方法参数提取出来
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        // 将任务中的样品数据取出来
        List<TaskSpecimen> sampleData = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<TaskSpecimen> newSampleData = new ArrayList<>();
        for (TaskSpecimen taskSpecimen : sampleData) {
            // 样品未测量并且未删除
            if ("0".equals(taskSpecimen.getDeleteType()) && "0".equals(taskSpecimen.getIsTested())) {
                newSampleData.add(taskSpecimen);
            }
        }

        boolean repeatMode = "单池重复".equalsIgnoreCase(spectrumParameter.getRepetitionPattern());
        // 重复模式: 样品只要已有测量数据(即使重复次数未测满), 本次批量测量不再续测, 直接跳过该样品
        if (repeatMode) {
            excludeMeasuredSpecimens(newSampleData, taskId);
        }

        // 根据序列号查询对应的附件配置表信息
        QueryWrapper<SamplePoolConfigPO> wrapper = new QueryWrapper<>();
        wrapper.eq("device_serial_number", deviceSerialNumber);
        SamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);
        if (configPO == null) {
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("error", "没有找到对应的附件配置信息");
            sendClose(deviceSerialNumber, errorMsg);
            return;
        }

        String maxLocation = "";
        if (configPO.getIsAutoChangePool()) {
            List<String> sampleIds = newSampleData.stream().map(obj -> obj.getSpecimenKey()).collect(Collectors.toCollection(ArrayList::new));
            QueryWrapper<TMeasureSample> eq = new QueryWrapper<>();
            eq.in("id", sampleIds);
            List<TMeasureSample> list = tMeasureSampleService.list(eq);
            // 过滤掉 null 的 location
            maxLocation = list.stream()
                    .map(TMeasureSample::getSampleLocation)
                    .filter(Objects::nonNull)  // 过滤掉 null
                    .max(String::compareTo)
                    .orElse(null);
        }

        /// 设置参数
        Result result = spectrumService.setUpParametersPort(task.getMethodKey(), deviceSerialNumber);
        if (result.getCode() != 200) {
            sendError(deviceSerialNumber, result.getMessage());
            return;
        }

        boolean poolStatus = false;
        if (configPO.getIsAutoChangePool()) {
            List<FSamplePoolStatus> poolStatuses = configPO.getPoolInfo();
            for (FSamplePoolStatus status : poolStatuses) {
                if (status.getPoolStatus()) {
                    poolStatus = true;
                    break;
                }
            }
        }

        String ip = deviceInfoService.getIp(deviceSerialNumber);
        state.ip = ip;

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setAction(ActionCmdEnum.Scan);
        if (spectrumParameter.getScanSpeed().equals(ScanSpeedEnum.EXTREME_FAST.getCode())) {
            inputParam.setAction(ActionCmdEnum.ScanFast);
        }

        // 往前端推送数据的线程
        String finalMaxLocation = maxLocation!=null?maxLocation:"";
        boolean finalPoolStatus = poolStatus;
        DevOperationInputParam finalParam = inputParam;

        Thread taskThread = new Thread(() -> {
            try {
                if (!repeatMode) {
                    // 不重复
                    for (int i = 0; i < newSampleData.size()&&Constants.measureStopStatus.get(deviceSerialNumber); i++) {


                        // 预留: 设置样品池位置
                        TMeasureSample sample = tMeasureSampleService.getById(newSampleData.get(i).getSpecimenKey());
                        if (configPO.getIsAutoChangePool()) {
                            if (sample == null) {
                                JSONObject errorMsg = new JSONObject();
                                errorMsg.put("error", "请先创建样品");
                                sendClose(deviceSerialNumber, errorMsg);
                                return;
                            }
                            if (finalPoolStatus&&!finalMaxLocation.isEmpty()) {
                                SetParam setParam = new SetParam();
                                setParam.setCellPos(Integer.parseInt(sample.getSampleLocation()));
                                boolean status = spectrumService.setInstrumentParameters(deviceSerialNumber, setParam);
                                if (!status) {
                                    JSONObject errorMsg = new JSONObject();
                                    errorMsg.put("error", "设置出错");
                                    sendClose(deviceSerialNumber, errorMsg);
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

                        doOneMeasurementSerial(finalParam, spectrumParameter, task, taskId, newSampleData.get(i).getSpecimenKey(), ip, 1);
                        newSampleData.get(i).setIsTested("1");

                        if (configPO.getIsAutoChangePool().equals(true)&&Constants.measureStopStatus.get(deviceSerialNumber)) {
                            if ((i + 1) != newSampleData.size() && !sample.getSampleLocation().equals(finalMaxLocation)) {
                                JSONObject payload = new JSONObject();
                                payload.put("result", "finish");
                                sendMessage(deviceSerialNumber, payload);
                            }else{
                                break;
                            }
                        }else{
                            break;
                        }
                    }
                } else {
                    // 单池重复
                    for (int i = 0; i < newSampleData.size()&&Constants.measureStopStatus.get(deviceSerialNumber); i++) {
                        // 预留: 设置样品池位置
                        TMeasureSample sample = tMeasureSampleService.getById(newSampleData.get(i).getSpecimenKey());
                        if (configPO.getIsAutoChangePool()) {
                            if (sample == null) {
                                JSONObject errorMsg = new JSONObject();
                                errorMsg.put("error", "请先创建样品");
                                sendClose(deviceSerialNumber, errorMsg);
                                return;
                            }
                            if (finalPoolStatus&&!finalMaxLocation.isEmpty()) {
                                SetParam setParam = new SetParam();
                                setParam.setCellPos(Integer.parseInt(sample.getSampleLocation()));
                                boolean status = spectrumService.setInstrumentParameters(deviceSerialNumber, setParam);
                                if (!status) {
                                    JSONObject errorMsg = new JSONObject();
                                    errorMsg.put("error", "设置出错");
                                    sendClose(deviceSerialNumber, errorMsg);
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


                        int repetitionTime = Integer.parseInt(spectrumParameter.getRepetitionTime());
                        int startIndex = 0;
                        // 根据样品id去分析数据表进行查询

                        for (int r = startIndex; r < repetitionTime; r++) {


                            // 根据样品id,任务id和遍历的r+1查询分析数据
                            QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                            queryWrapper.eq("task_id", taskId);
                            queryWrapper.eq("sample_key", newSampleData.get(i).getSpecimenKey());
                            queryWrapper.eq("analysis_type", "5");
                            queryWrapper.eq("analysis_number", r + 1);
                            TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getOne(queryWrapper);
                            if (measureData != null && measureData.getAnalysisResult() != null) {
                                continue;
                            }
                            if (measureData != null && measureData.getIsDelete().equals("1")) {
                                continue;
                            }

                            doOneMeasurementSerial(finalParam, spectrumParameter, task, taskId, newSampleData.get(i).getSpecimenKey(), ip, r + 1);

                            if (!Constants.measureStopStatus.get(deviceSerialNumber)){
                                break;
                            }

                            // 修复：如果不是最后一次测量，添加时间间隔
                            if (r < repetitionTime - 1) {
                                try {
                                    // 检查停止请求的时间间隔等待
                                    if (waitWithStopCheck( spectrumParameter.getTimeInterval())&&Constants.measureStopStatus.get(deviceSerialNumber)) {
                                        log.info("等待期间收到停止请求，退出循环");
                                        break;
                                    }
                                } catch (InterruptedException e) {
                                    log.info("重复模式等待时间间隔被中断");
                                    Thread.currentThread().interrupt();
                                    throw e;
                                }
                            }
                        }

                        if (configPO.getIsAutoChangePool().equals(true)&&Constants.measureStopStatus.get(deviceSerialNumber)) {
                            // 如果当前位置不是最大位置
                            if ((i + 1) != newSampleData.size() && !sample.getSampleLocation().equals(finalMaxLocation)) {
                                JSONObject payload = new JSONObject();
                                payload.put("result", "finish");
                                sendMessage(deviceSerialNumber, payload);
                            }else{
                                break;
                            }
                        }else{
                            break;
                        }
                    }
                }

                if (configPO.getIsAutoChangePool()) {
                    SetParam setParam = new SetParam();
                    setParam.setCellPos(Integer.parseInt(Constants.PARAM_ONE));
                    boolean status = spectrumService.setInstrumentParameters(deviceSerialNumber, setParam);
                    if (!status) {
                        sendError(deviceSerialNumber,  "设置出错");
                        return;
                    }
                    Boolean b = fSamplePoolConfigService.updateCurrentPoolNum(deviceSerialNumber, Integer.parseInt(Constants.PARAM_ONE), updateBy);
                    if (b) {
                        log.info("自动切换样品池恢复样品池到1号池");
                        JSONObject object = new JSONObject();
                        object.put("currentPoolNum", Constants.PARAM_ONE);
                        sendMessage(deviceSerialNumber, object);
                    }else{
                        log.info("自动切换样品池恢复样品池到1号池失败");
                    }
                }

                JSONObject payload = new JSONObject();
                if (Constants.measureStopStatus.get(deviceSerialNumber)){
                    if (lightStatus){
                        payload.put("success", "扫描成功");
                    }else {
                        payload.put("error", "能量异常，请重新测量");
                    }
                }else{
                    payload.put("success", "停止成功");
                }
                infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
                sendClose(deviceSerialNumber, payload);


                DevOperationInputParam setInputParam = new DevOperationInputParam();
                setInputParam.setOperationType(OperationTypeEnums.SetParam.getType());
                setInputParam.setDeviceSerialNumber(deviceSerialNumber);
                SetParam setParam = new SetParam();
                setParam.setSetWl(Double.valueOf(spectrumParameter.getStartWaveLength()));
                setInputParam.setParam(setParam);
                // 重新设置波长返回起始波长命令
                R<OperationResultEntity> r = devOperationUtilService.operation(setInputParam);
                if (!r.getSuccess()) {
                    sendError(deviceSerialNumber,r.getMessage());
                }
                log.info("==========仪器返回起始波长");


            } catch (Exception e) {
                log.error("扫描任务执行异常", e);
                sendMessage(deviceSerialNumber, JSONObject.parseObject("执行异常: " + e.getMessage()));

                // 异常时也只发送消息，不清理资源
                JSONObject errorPayload = new JSONObject();
                errorPayload.put("result", "error");
                errorPayload.put("message", "执行异常: " + e.getMessage());
                sendClose(deviceSerialNumber, errorPayload);
            } finally {
                // 标记任务线程已完成
                taskThreads.remove(deviceSerialNumber);
                log.info("任务线程已结束，session: {}", deviceSerialNumber);

                // 在这里可以设置一个标志，表示可以安全关闭了
                if (state != null) {
                    state.currentMeasurementCompleted.set(true);
                }
            }
        });


        taskThreads.put(deviceSerialNumber, taskThread);
        taskThread.start();
    }

    /**
     * 重复模式下, 排除已有测量数据的样品(重复次数未测满, 但已测过至少一次)
     * <p>
     * 这类样品本次批量测量不再续测剩余重复次数, 直接跳过; 剩余重复次数由用户通过单测补测。
     *
     * @param specimens 待测样品列表(原地过滤)
     * @param taskId    任务id
     */
    private void excludeMeasuredSpecimens(List<TaskSpecimen> specimens, String taskId) {
        if (specimens == null || specimens.isEmpty() || taskId == null || taskId.isEmpty()) {
            return;
        }
        List<String> sampleIds = specimens.stream()
                .map(TaskSpecimen::getSpecimenKey)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());
        if (sampleIds.isEmpty()) {
            return;
        }
        QueryWrapper<TMeasureAnalysisData> wrapper = new QueryWrapper<>();
        wrapper.select("sample_key");
        wrapper.eq("task_id", taskId);
        wrapper.in("sample_key", sampleIds);
        wrapper.eq("analysis_type", "5");
        wrapper.eq("is_delete", "0");
        wrapper.isNotNull("analysis_result");
        Set<String> measuredSampleIds = tMeasureAnalysisDataService.list(wrapper).stream()
                .map(TMeasureAnalysisData::getSampleKey)
                .collect(Collectors.toSet());
        if (!measuredSampleIds.isEmpty()) {
            specimens.removeIf(specimen -> measuredSampleIds.contains(specimen.getSpecimenKey()));
        }
    }

    /**
     * 带停止检查的等待
     */
    private boolean waitWithStopCheck(String timeIntervalStr) throws InterruptedException {
        if (timeIntervalStr == null || timeIntervalStr.trim().isEmpty()) {
            return false;
        }

        double intervalSeconds;
        try {
            intervalSeconds = Double.parseDouble(timeIntervalStr.trim());
        } catch (NumberFormatException e) {
            log.warn("无效的时间间隔格式: '{}', 无法解析为数字", timeIntervalStr);
            return false;
        }

        if (intervalSeconds <= 0) {
            return false;
        }

        // 转换为毫秒，保留整数毫秒（可四舍五入或向下取整）
        long totalMillis = Math.max(1, Math.round(intervalSeconds * 1000)); // 至少等待1毫秒

        log.info("重复模式：等待时间间隔 {} 秒（{} 毫秒）", intervalSeconds, totalMillis);

        long remainingMillis = totalMillis;
        long checkInterval = 500; // 每500ms检查一次

        while (remainingMillis > 0) {
            long sleepTime = Math.min(checkInterval, remainingMillis);
            Thread.sleep(sleepTime);
            remainingMillis -= sleepTime;
        }

        return false;
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
            // 若清理由任务线程自身触发（sendClose/sendError），跳过 interrupt+join：
            // 自中断会留下中断标志，导致清理后继续发送的指令在 sleep 中立即抛 InterruptedException
            Thread thread = taskThreads.get(sessionId);
            if (thread != null && thread.isAlive() && thread != Thread.currentThread()) {
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
            // 等待调度器完成推送
            state.waitForAllSchedulersToFinish(5000);
            state.cleanupAllSchedulers();
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
     * 发送消息并关闭连接2
     */
    private void sendClose(String sessionId, JSONObject msg) {

        SessionState state = sessionStates.get(sessionId);
        if (state != null && state.finalMessageSent.getAndSet(true)) {
            log.warn("最终消息已发送过，忽略重复调用: sessionId={}", sessionId);
            return;
        }

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
     * @param ip
     * @param count
     * @throws IOException
     */
    private void doOneMeasurementSerial(DevOperationInputParam command,
                                        SpectrumParameter spectrumParameter,
                                        TMeasureTask task,
                                        String taskId,
                                        String sampleId,
                                        String ip,
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
            result = doOneMeasurementCore(command, spectrumParameter,task, sampleId, ip, count);

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


            try {
                Constants.MEASURE_LOCK.unlock();
                log.info("测量锁已释放");
            } catch (Exception e) {
                log.error("测量过程中发生异常，session: {}", sessionId, e);
                throw e;
            } finally {
                // 标记当前测量完成
                if (state != null) {
                    state.currentMeasurementCompleted.set(true);
                }
            }
        }

        MeasurementResult finalResult = result;
        // 同步保存数据，等待保存完成
        String storageKey = sessionId + "_" + sampleId + "_" + count;
        CountDownLatch latch = new CountDownLatch(1);
        storageLatches.put(storageKey, latch);

        // 保存数据
        saveOneMeasurementWithSnapshots(task, taskId, sampleId, deviceSerialNumber,
                finalResult.x, finalResult.y, count, finalResult.startTime, finalResult.endTime,
                finalResult.changeParams,latch);

        // 等待存储完成，增加超时时间并记录
        try {
            long startWait = System.currentTimeMillis();
            boolean stored = latch.await(5, TimeUnit.SECONDS); // 等待45秒
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
        // 强制刷新调度器（处理只有一组数据的情况）
        SmoothPushScheduler scheduler = state.getOrCreateScheduler(sampleId, count, state.digit,
                spectrumParameter.getLuminosity(), () -> currentSampleCell);
        scheduler.flush();

        // 存储完成后才通知前端
        JSONObject finishMessage = new JSONObject();
        finishMessage.put("result", "measurement_complete");
        finishMessage.put("sampleId", sampleId);
        finishMessage.put("count", count);
        sendMessage(sessionId, finishMessage);
    }

    // doOneMeasurementCore 改造，返回 MeasurementResult
    private MeasurementResult doOneMeasurementCore(DevOperationInputParam command,
                                                   SpectrumParameter spectrumParameter,
                                                   TMeasureTask task,
                                                   String sampleId,
                                                   String ip,
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
            case "5":
                luminosity = "R%";
                break;
            default:
                luminosity = "Abs";
                break;
        }
        // 发送测量命令
        R<OperationResultEntity> r = devOperationUtilService.operation(command);
        if (!r.getSuccess()) {
            sendError(deviceSerialNumber,r.getMessage());
        }
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
        boolean isFastScan = ScanSpeedEnum.EXTREME_FAST.getCode().equals(spectrumParameter.getScanSpeed());

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
                    new ArrayList<>(processedData.changeParams)
            );
        } else {
            boolean whileStatus = true;
            // 普通扫描模式：使用批次处理机制
            try {
                while (whileStatus) {
                    // 检查会话状态
                    if (state != null && (state.sessionClosing.get() || state.stopCommandSent.get())) {
                        log.info("会话正在关闭或停止命令已发送，退出测量循环");
                        break;
                    }

                    // 处理所有可用的数据点
                    while (whileStatus) {
                        if (Thread.currentThread().isInterrupted()) {
                            log.info("线程被中断，退出内部循环");
                            whileStatus = false;   // 终止外层循环
                            break;                 // 退出内层循环
                        }

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
                                case NORMAL_OVERPOWER:
                                    lightStatus = false;
                                    log.error("设备 {} 能量异常，请重新测量", deviceSerialNumber);
                                    break;
                                case NORMAL:
                                    log.info("设备 {} 的测量正常完成", deviceSerialNumber);
                                    break;
                                case NORMAL_STOP:
                                    log.info("设备 {} 的测量已停止", deviceSerialNumber);
                                    break;
                            }
                            // 结束循环
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
                        }else if (spectrumParameter.getLuminosity().equals("5")) {
                            y.add(Double.valueOf(message.getRResult().getRPercent()));
                            Constants.instrumentData.add(message.getWavelength()+","+message.getRResult().getRPercent());
                        }else {
                            y.add(Double.valueOf(message.getErResult().getEr()));
                        }
                        changeParams.add(message.getScanRsd());


                        // 将新点添加到批次缓冲区，并检查是否达到批次大小
                        if (!x.isEmpty() && !y.isEmpty()) {
                            int currentIndex = x.size() - 1;
                            Double currentX = x.get(currentIndex);
                            Double currentY = y.get(currentIndex);

                            boolean isBatchFull = state.addToBatchBuffer(currentX, currentY);

                            if (isBatchFull) {
                                // 批次已满，进行处理
                                BatchData batchData = state.getAndClearBatchBuffer(deviceSerialNumber,count, sampleId, state.digit,
                                        spectrumParameter, task);

                                if (batchData != null) {
                                    // 异步处理台阶消除,消除尖刺
                                    processBatchDataAsync(batchData);

                                }
                            }
                        }

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
                BatchData batchData = state.getRemainingBatchBuffer(count, sampleId, state.digit,
                        spectrumParameter, task);

                if (batchData != null && !batchData.xBatch.isEmpty()) {
                    log.info("处理剩余批次数据，点数: {}", batchData.xBatch.size());
                    String key = batchData.sessionId + "_" + batchData.sampleId + "_" + batchData.count;
                    synchronized (originalYValues) {
                        // 获取已存在的处理后的数据
                        List<Double> existingData = originalYValues.get(key);
                        if (existingData == null) {
                            // 如果没有，创建新的列表
                            existingData = new ArrayList<>();
                        }

                        // 追加当前批次的数据
                        existingData.addAll(batchData.yBatch);

                        // 存储回 originalYValues
                        originalYValues.put(key, existingData);

                        log.info("追加数据到 originalYValues, key: {}, 当前总点数: {}, 本次追加: {}",
                                key, existingData.size(), batchData.yBatch.size());
                    }

                    // ========== 新增：剩余批次数据也需要提交给调度器 ==========
                    java.util.function.Supplier<String> cellSupplier = () -> currentSampleCell;
                    SmoothPushScheduler scheduler = state.getOrCreateScheduler(
                            batchData.sampleId,
                            batchData.count,
                            batchData.digit,
                            spectrumParameter.getLuminosity(),
                            cellSupplier);
                    // 剩余批次数据也需要通过调度器推送（注意：这里传入的是原始 yBatch，因为台阶消除已在 processBatchDataAsync 中处理过）
                    scheduler.receiveBatch(batchData.xBatch, batchData.yBatch, batchData.batchSeq);

                    // 更新已推送点数
                    state.incrementPushedPoints(batchData.xBatch.size());
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

        List<Double> processedYForReturn = new ArrayList<>();
        String key = deviceSerialNumber + "_" + sampleId + "_" + count;

        // 等待一小段时间，确保所有异步处理完成
        try {
            Thread.sleep(500); // 等待500ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 从 originalYValues 获取处理后的数据
        List<Double> storedY = originalYValues.get(key);
        if (storedY != null && !storedY.isEmpty()) {
            log.info("从 originalYValues 获取到处理后的数据，点数: {}", storedY.size());
            processedYForReturn = new ArrayList<>(storedY);
        } else {
            log.warn("未找到处理后的数据，使用原始数据返回，key: {}", key);
            processedYForReturn = new ArrayList<>(y);
        }

        // 数据完整性检查
        log.info("=== 测量结果统计 ===");
        log.info("总数据点数 - X: " + x.size() + ", Y: " + y.size());
        log.info("总数据点数2 - X: " + x.size() + ", Y: " + processedYForReturn.size());
        log.info("测量时间 - 开始: " + Constants.startTime + ", 结束: " + Constants.endTime);
        log.info("仪器数据快照大小: " + Constants.instrumentData.size());
        log.info("原始计算数据快照大小: " + Constants.originalCalculationData.size());
        log.info("阶梯校正状态: " + spectrumParameter.getLadderCorrection());

        // 返回快照 + x/y
        return new MeasurementResult(
                x,
                processedYForReturn,
                Constants.startTime,
                Constants.endTime,
                changeParams
        );
    }


    /**
     * 异步处理批次数据（包含台阶消除,消除尖刺）
     */
    private void processBatchDataAsync(BatchData batchData) {
        // 使用线程池异步处理
        CompletableFuture.runAsync(() -> {
            try {
                log.info("异步处理批次数据，点数: {}, 起始索引: {}, 批次大小配置: {}",
                        batchData.xBatch.size(), batchData.startIndex, staircaseEliminationBatchSize);

                boolean ladderCorrection = batchData.parameter.getLadderCorrection();
                boolean eliminateSpine = batchData.parameter.getSpikeRemoval()!=null?batchData.parameter.getSpikeRemoval():false;
                List<Double> processedY = batchData.yBatch;
                List<Double> yForStorage = batchData.yBatch;

                // 执行台阶消除
                if (ladderCorrection) {
                    log.info("执行台阶消除，数据点数: " + batchData.xBatch.size());
                    processedY = processAndSaveDataStep(batchData.task, batchData.xBatch, batchData.yBatch,
                            batchData.parameter, batchData.sampleId);
                    log.info("台阶消除完成");

                    // 开启阶梯校正时，存储校正后的数据
                    batchData.yBatch = processedY;
                    yForStorage = processedY;
                } else {
                    log.info("跳过台阶消除");
                }

                // 执行消除尖刺
                if (eliminateSpine) {
                    log.info("执行消除尖刺，数据点数: " + batchData.xBatch.size());
                    processedY = processAndSaveDataSpine(batchData.task, batchData.xBatch, processedY,
                            batchData.parameter, batchData.sampleId);
                    log.info("消除尖刺完成");

                    // 开启消除尖刺时，存储消除后的数据
                    batchData.yBatch = processedY;
                    yForStorage = processedY;
                } else {
                    log.info("跳过消除尖刺");
                }

                String key = batchData.sessionId + "_" + batchData.sampleId + "_" + batchData.count;
                synchronized (originalYValues) {
                    // 获取已存在的处理后的数据
                    List<Double> existingData = originalYValues.get(key);
                    if (existingData == null) {
                        // 如果没有，创建新的列表
                        existingData = new ArrayList<>();
                    }

                    // 追加当前批次的数据
                    existingData.addAll(yForStorage);

                    // 存储回 originalYValues
                    originalYValues.put(key, existingData);

                    log.info("追加数据到 originalYValues, key: {}, 当前总点数: {}, 本次追加: {}",
                            key, existingData.size(), yForStorage.size());
                }

                // 判断是否是当前样品的第一次批次
                boolean isFirstBatch = false;
                SessionState state = sessionStates.get(batchData.sessionId);
                if (state != null) {
                    // 如果已推送点数为0，说明是第一次批次
                    isFirstBatch = (state.pushedPointsCount.get() == 0);
                }

                // 调用新的存储方法，处理数据库的首次创建或追加
                handleBatchDataStorage(batchData, isFirstBatch);

                // 获取调度器，并将数据交给调度器（不直接推送）
                java.util.function.Supplier<String> cellSupplier = () -> currentSampleCell;
                SmoothPushScheduler scheduler = state.getOrCreateScheduler(
                        batchData.sampleId,
                        batchData.count,
                        batchData.digit,
                        batchData.parameter.getLuminosity(),
                        cellSupplier);
                scheduler.receiveBatch(batchData.xBatch, processedY, batchData.batchSeq);

                // 更新已推送点数
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
     * 处理批次数据的数据库存储（首次创建，后续追加）
     * @param batchData 批次数据
     * @param isFirstBatch 是否是当前样品的第一次批次
     */
    private void handleBatchDataStorage(BatchData batchData, boolean isFirstBatch) {
        try {
            String taskId = batchData.task.getId();
            String sampleId = batchData.sampleId;
            Integer count = batchData.count;

            log.info("处理批次数据存储: sampleId={}, count={}, 点数={}, isFirstBatch={}",
                    sampleId, count, batchData.xBatch.size(), isFirstBatch);

            // 构建当前批次的结果字符串
            List<String> batchResult = new ArrayList<>();
            for (int i = 0; i < batchData.xBatch.size(); i++) {
                batchResult.add(batchData.xBatch.get(i) + "," + batchData.yBatch.get(i));
            }

            if (isFirstBatch) {
                // 第一次批次，创建新记录

                // 1. 创建测量数据记录（只放数据，其他字段为空）
                TMeasureData measureData = new TMeasureData();
                measureData.setSerialNumber(count);
                measureData.setTaskId(taskId);
                measureData.setSampleKey(sampleId);
                measureData.setOriginalCalculationData(JSON.toJSONString(batchResult));
                // 其他字段暂时不设置
                measureData.setIsDelete("0");
                measureData.setIsSave("0"); // 标记为未完成
                tMeasureDataService.save(measureData);

                // 2. 获取样品名称用于分析数据名称
                TMeasureSample sample = tMeasureSampleService.getById(sampleId);
                String sampleName = sample != null ? sample.getSampleName() : "样品";

                // 3. 创建分析数据记录（只放数据，其他字段为空）
                TMeasureAnalysisData analysisData = new TMeasureAnalysisData();
                analysisData.setAnalysisNumber(count);
                analysisData.setAnalysisName(sampleName + "-" + count);
                analysisData.setTaskId(taskId);
                analysisData.setSampleKey(sampleId);
                analysisData.setMeasureDataId(measureData.getId());
                analysisData.setAnalysisResult(JSON.toJSONString(batchResult));
                analysisData.setAnalysisType("5");
                analysisData.setColor(ColorUtil.getRandomColor());
                analysisData.setIsDelete("0");
                analysisData.setIsSave("0"); // 标记为未完成
                tMeasureAnalysisDataService.save(analysisData);

                // 4. 创建关联的图片记录（空记录）
                TMeasureAnalysisImage analysisImage = new TMeasureAnalysisImage();
                analysisImage.setAnalysisId(analysisData.getId());
                tMeasureAnalysisImageService.save(analysisImage);

                log.info("首次创建数据记录: measureDataId={}, analysisDataId={}",
                        measureData.getId(), analysisData.getId());

            } else {
                // 后续批次，追加数据到已有记录

                // 1. 查询已存在的测量数据记录
                QueryWrapper<TMeasureData> dataQuery = new QueryWrapper<>();
                dataQuery.eq("task_id", taskId)
                        .eq("sample_key", sampleId)
                        .eq("serial_number", count)
                        .last("limit 1");
                TMeasureData existingMeasureData = tMeasureDataService.getOne(dataQuery);

                if (existingMeasureData != null) {
                    // 获取已有数据
                    String existingDataStr = existingMeasureData.getOriginalCalculationData();
                    List<String> existingData = new ArrayList<>();
                    if (existingDataStr != null && !"[]".equals(existingDataStr) && !existingDataStr.isEmpty()) {
                        try {
                            existingData = JSON.parseArray(existingDataStr, String.class);
                        } catch (Exception e) {
                            log.error("解析已有数据失败", e);
                            existingData = new ArrayList<>();
                        }
                    }

                    // 追加新数据
                    existingData.addAll(batchResult);

                    // 更新记录
                    existingMeasureData.setOriginalCalculationData(JSON.toJSONString(existingData));
                    tMeasureDataService.updateById(existingMeasureData);

                    log.info("追加数据到测量记录: 原有{}点, 新增{}点, 总计{}点",
                            existingData.size() - batchResult.size(), batchResult.size(), existingData.size());
                } else {
                    log.warn("未找到对应的测量数据记录，重新创建: taskId={}, sampleId={}, count={}",
                            taskId, sampleId, count);
                    // 如果找不到记录，重新创建（兜底逻辑）
                    TMeasureData newMeasureData = new TMeasureData();
                    newMeasureData.setSerialNumber(count);
                    newMeasureData.setTaskId(taskId);
                    newMeasureData.setSampleKey(sampleId);
                    newMeasureData.setOriginalCalculationData(JSON.toJSONString(batchResult));
                    newMeasureData.setIsDelete("0");
                    newMeasureData.setIsSave("0");
                    tMeasureDataService.save(newMeasureData);
                }

                // 2. 查询已存在的分析数据记录
                QueryWrapper<TMeasureAnalysisData> analysisQuery = new QueryWrapper<>();
                analysisQuery.eq("task_id", taskId)
                        .eq("sample_key", sampleId)
                        .eq("analysis_number", count)
                        .eq("analysis_type", "5")
                        .last("limit 1");
                TMeasureAnalysisData existingAnalysisData = tMeasureAnalysisDataService.getOne(analysisQuery);

                if (existingAnalysisData != null) {
                    // 获取已有数据
                    String existingResultStr = existingAnalysisData.getAnalysisResult();
                    List<String> existingResult = new ArrayList<>();
                    if (existingResultStr != null && !"[]".equals(existingResultStr) && !existingResultStr.isEmpty()) {
                        try {
                            existingResult = JSON.parseArray(existingResultStr, String.class);
                        } catch (Exception e) {
                            log.error("解析已有分析结果失败", e);
                            existingResult = new ArrayList<>();
                        }
                    }

                    // 追加新数据
                    existingResult.addAll(batchResult);

                    // 更新记录
                    existingAnalysisData.setAnalysisResult(JSON.toJSONString(existingResult));
                    tMeasureAnalysisDataService.updateById(existingAnalysisData);

                    log.info("追加数据到分析记录: 原有{}点, 新增{}点, 总计{}点",
                            existingResult.size() - batchResult.size(), batchResult.size(), existingResult.size());
                } else {
                    log.warn("未找到对应的分析数据记录，重新创建: taskId={}, sampleId={}, count={}",
                            taskId, sampleId, count);
                    // 如果找不到记录，重新创建（兜底逻辑）
                    TMeasureSample sample = tMeasureSampleService.getById(sampleId);
                    String sampleName = sample != null ? sample.getSampleName() : "样品";

                    TMeasureAnalysisData newAnalysisData = new TMeasureAnalysisData();
                    newAnalysisData.setAnalysisNumber(count);
                    newAnalysisData.setAnalysisName(sampleName + "-" + count);
                    newAnalysisData.setTaskId(taskId);
                    newAnalysisData.setSampleKey(sampleId);
                    newAnalysisData.setAnalysisResult(JSON.toJSONString(batchResult));
                    newAnalysisData.setAnalysisType("5");
                    newAnalysisData.setColor(ColorUtil.getRandomColor());
                    newAnalysisData.setIsDelete("0");
                    newAnalysisData.setIsSave("0");
                    tMeasureAnalysisDataService.save(newAnalysisData);
                }
            }

        } catch (Exception e) {
            log.error("处理批次数据存储异常", e);
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
        String luminosityType = luminosity.equals("1") ? "Abs" : luminosity.equals("2") ? "T%" : luminosity.equals("3") ? "Es" : luminosity.equals("5") ? "R%" : "Er";
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
    private ProcessedData processAndPushFastScanData(SpectrumParameter spectrumParameter,
                                                     Integer count, TMeasureTask task,
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
                        case NORMAL_OVERPOWER:
                            log.error("设备 {} 能量异常，请重新测量", deviceSerialNumber);
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
                }else if (spectrumParameter.getLuminosity().equals("5")) {
                    processedY.add(Double.valueOf(message.getRResult().getRPercent()));
                    Constants.instrumentData.add(message.getWavelength()+","+message.getRResult().getRPercent());
                }else {
                    processedY.add(Double.valueOf(message.getErResult().getEr()));
                }
                changeParams.add(message.getScanRsd());
            } catch (InterruptedException e) {
                log.error("等待数据采集完成时发生中断异常", e);
                Thread.currentThread().interrupt();
            }
        }
        List<Double> yForStorage = processedY; // 用于存储的Y值
//        // 如果需要台阶校正
//        if (ladderCorrection) {
//            log.info("开始台阶校正");
//            List<Double> correctedY = processAndSaveDataStep(task, processedX, processedY,
//                    spectrumParameter, sampleId);
//            processedY = correctedY;
//            yForStorage = correctedY; // 开启校正时存储校正后的数据
//            log.info("台阶校正完成");
//        } else {
//            // 未开启校正时存储原始数据
//            yForStorage = processedY;
//        }

        // 根据是否开启阶梯校正来决定存储的数据
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
    private int calculateExpectedDataPoints(SpectrumParameter spectrumParameter) {
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
                case "5":
                    luminosityType = "R%";
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
        payload.put("currentPoolNum", currentSampleCell);

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
     * 台阶消除
     */
    public List<Double> processAndSaveDataStep(TMeasureTask task,
                                               List<Double> x,
                                               List<Double> y,
                                               SpectrumParameter parameter,
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
     * 消除尖刺
     */
    public List<Double> processAndSaveDataSpine(TMeasureTask task,
                                                List<Double> x,
                                                List<Double> y,
                                                SpectrumParameter parameter,
                                                String sampleId) {

        String luminosityMode = parameter.getLuminosity();
        boolean eliminateSpine = parameter.getSpikeRemoval();

        log.info("---------------进入消除尖刺处理逻辑---------------");
        log.info("消除尖刺参数 - 数据点数: " + x.size() + ", 光度模式: " + luminosityMode + ", 样品ID: " + sampleId);

        // 如果不需要消除尖刺，直接返回原始数据
        if (!eliminateSpine) {
            log.info("消除尖刺已禁用，返回原始数据");
            return y;
        }

        // 这里调用 Python 的消除尖刺算法，返回处理后的 y 值
        List<Double> processedY = eliminationSpinePython(x, y, parameter);

        log.info("消除尖刺完成，原始数据点数: " + y.size() + ", 处理后数据点数: " + processedY.size());
        log.info("---------------消除尖刺处理逻辑结束---------------");

        return processedY;
    }

    /**
     * 调用 Python 服务处理台阶消除
     */
    private List<Double> callStepEliminationPython(List<Double> x, List<Double> y, Integer threshold, SpectrumParameter parameter) {
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
     * 调用 Python 服务处理消除尖刺
     */
    private List<Double> eliminationSpinePython(List<Double> x, List<Double> y, SpectrumParameter parameter) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            for (Double v : x) {
                if (v.equals(760.0)) {
                    System.out.println("到达760.0");
                }
            }

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("x_data", x);
            requestBody.put("y_data", y);
            requestBody.put("spinedot", parameter.getSpikeArray());
            requestBody.put("wavelength_end", parameter.getEndWaveLength());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 调用 Python API
            ResponseEntity<Map> response = restTemplate.postForEntity(pythonPathConstant.getRemoveSpikes(), request, Map.class);

            // 从响应中获取处理后的 y 值
            List<Double> processedY = (List<Double>) response.getBody().get("smoothed_data");

            return processedY;
        } catch (Exception e) {
            log.error("调用Python消除尖刺服务失败，返回原始数据", e);
            return y;
        }
    }

    // 保存一次测量结果

    /**
     * 使用快照保存一次测量结果，避免与全局变量冲突
     */
    private void saveOneMeasurementWithSnapshots(TMeasureTask task,
                                                 String taskId,
                                                 String sampleId,
                                                 String deviceSerialNumber,
                                                 List<Double> x,
                                                 List<Double> y,
                                                 Integer number,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime,
                                                 List<ChangeParam> changeParamList,
                                                 CountDownLatch latch) {

        String sessionId =deviceSerialNumber;
        String storageKey = sessionId + "_" + sampleId + "_" + number;
        // 检查数据是否为空
        if (x.isEmpty() || y.isEmpty()) {
            log.warn("测量数据为空，不保存: 样品={}, 计数={}", sampleId, number);
            if (latch != null) {
                latch.countDown();  // 仍然要通知完成
            }
            return;
        }

        // 2. 保存测量数据
        List<TaskSpecimen> sampleData = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        TaskSpecimen taskSpecimen = null;
        for (TaskSpecimen data : sampleData) {
            if (data.getSpecimenKey().equals(sampleId)) {
                taskSpecimen = data;
                break;
            }
        }
        if (taskSpecimen == null) {
            log.warn("未找到该样品: " + sampleId);
            return;
        }
        TMeasureSample sample = tMeasureSampleService.getById(taskSpecimen.getSpecimenKey());
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        Boolean isScanFast = spectrumParameter.getScanSpeed().equals(ScanSpeedEnum.EXTREME_FAST.getCode());
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
            List<FPeakValleyMarking> peakValleys = new ArrayList<>();
            List<FPeakValleyMarking> markings = new ArrayList<>();
            if (!isScanFast){
                String peakUrl = pythonPathConstant.getPeakSearching();
                String valleyUrl = pythonPathConstant.getValleySearching();

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
            }

            // 构建结果字符串，存储时使用原始Y值
            List<String> result = new ArrayList<>();
            for (int i = 0; i < x.size(); i++) {
                double originalYValue = originalY != null && i < originalY.size() ? originalY.get(i) : y.get(i);
                result.add(x.get(i) + "," + originalYValue);
            }


            if ("不重复".equals(spectrumParameter.getRepetitionPattern())) {
                // 普通模式
                TMeasureData measureData = traceDataService.getOne(new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("sample_key", sampleId).eq("serial_number", number).last("limit 1"));
                if (measureData == null) {
                    measureData = new TMeasureData();
                }
                measureData.setSerialNumber(1);
                measureData.setTaskId(taskId);
                measureData.setSampleKey(sampleId);
                measureData.setInstrumentData(JSON.toJSONString(changeParamList));
                measureData.setOriginalCalculationData(JSON.toJSONString(result));
                measureData.setMeasurementStartTime(startTime);
                measureData.setMeasurementEndTime(endTime);
                measureData.setIsDelete("0");
                measureData.setIsSave("1");
                traceDataService.saveOrUpdate(measureData);
                TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(new QueryWrapper<TMeasureAnalysisData>().eq("task_id", taskId).eq("sample_key", sampleId).eq("analysis_number", number).last("limit 1"));
                if (analysisData == null) {
                    analysisData = new TMeasureAnalysisData();
                }
                analysisData.setAnalysisNumber(1);
                analysisData.setAnalysisName(sample.getSampleName() + "(" + analysisData.getAnalysisNumber() + ")");
                analysisData.setTaskId(taskId);
                analysisData.setSampleKey(sampleId);
                analysisData.setMeasureDataId(measureData.getId());
                analysisData.setAnalysisParam(JSON.toJSONString(changeParamList));
                analysisData.setAnalysisResult(JSON.toJSONString(result));
                analysisData.setAnalysisType("5");
                if (peakValleys != null) {
                    analysisData.setPeak(JSON.toJSONString(peakValleys));
                }
                if (markings != null) {
                    analysisData.setGrain(JSON.toJSONString(markings));
                }
                // 保存本次寻峰使用的参数
                PeakSearchParam peakSearchParam;
                String existingPeakParam = analysisData.getPeakSearchParam();
                if (existingPeakParam != null) {
                    peakSearchParam = JSON.parseObject(existingPeakParam, PeakSearchParam.class);
                } else {
                    peakSearchParam = new PeakSearchParam();
                }
                peakSearchParam.setPeakValleyDiff(confVO.getPeakValleyDiff());
                peakSearchParam.setPeakInterval(confVO.getPeakInterval());
                peakSearchParam.setIsSelected("1");
                analysisData.setPeakSearchParam(JSON.toJSONString(peakSearchParam));
                analysisData.setColor(ColorUtil.getRandomColor());
                analysisData.setIsDelete("0");
                analysisData.setIsSave("1");
                tMeasureAnalysisDataService.saveOrUpdate(analysisData);
                TMeasureAnalysisImage tMeasureAnalysisImage = tMeasureAnalysisImageService.getOne(new QueryWrapper<TMeasureAnalysisImage>().eq("analysis_id", analysisData.getId()));
                if (tMeasureAnalysisImage == null) {
                    tMeasureAnalysisImage = new TMeasureAnalysisImage();
                }
                tMeasureAnalysisImage.setAnalysisId(analysisData.getId());
                tMeasureAnalysisImageService.saveOrUpdate(tMeasureAnalysisImage);
            } else {
                // 重复模式
                TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(
                        new QueryWrapper<TMeasureAnalysisData>()
                                .eq("task_id", taskId)
                                .eq("sample_key", sampleId)
                                .eq("analysis_number", number)
                );
                if (analysisData != null && Constants.PARAM_ZERO.equals(analysisData.getIsSave())) {
                    // 更新已有分析数据
                    TMeasureData measureData = tMeasureDataService.getOne(
                            new QueryWrapper<TMeasureData>()
                                    .eq("task_id", taskId)
                                    .eq("sample_key", sampleId)
                                    .eq("serial_number", number)
                    );
                    measureData.setInstrumentData(JSON.toJSONString(changeParamList));
                    measureData.setOriginalCalculationData(JSON.toJSONString(result));
                    measureData.setMeasurementStartTime(startTime);
                    measureData.setMeasurementEndTime(endTime);
                    measureData.setIsSave("1");
                    traceDataService.saveOrUpdate(measureData);
                    analysisData.setMeasureDataId(measureData.getId());
                    analysisData.setAnalysisParam(JSON.toJSONString(changeParamList));
                    analysisData.setAnalysisResult(JSON.toJSONString(result));
                    if (peakValleys != null) {
                        analysisData.setPeak(JSON.toJSONString(peakValleys));
                    }
                    if (markings != null) {
                        analysisData.setGrain(JSON.toJSONString(markings));
                    }
                    // 保存本次寻峰使用的参数
                    PeakSearchParam peakSearchParam2;
                    String existingPeakParam2 = analysisData.getPeakSearchParam();
                    if (existingPeakParam2 != null) {
                        peakSearchParam2 = JSON.parseObject(existingPeakParam2, PeakSearchParam.class);
                    } else {
                        peakSearchParam2 = new PeakSearchParam();
                    }
                    peakSearchParam2.setPeakValleyDiff(confVO.getPeakValleyDiff());
                    peakSearchParam2.setPeakInterval(confVO.getPeakInterval());
                    peakSearchParam2.setIsSelected("1");
                    analysisData.setPeakSearchParam(JSON.toJSONString(peakSearchParam2));
                    analysisData.setColor(ColorUtil.getRandomColor());
                    analysisData.setIsSave("1");
                    tMeasureAnalysisDataService.saveOrUpdate(analysisData);
                } else {
                    // 新增分析数据
                    TMeasureData measureData = new TMeasureData();
                    measureData.setSerialNumber(number);
                    measureData.setTaskId(taskId);
                    measureData.setSampleKey(sampleId);
                    measureData.setInstrumentData(JSON.toJSONString(changeParamList));
                    measureData.setOriginalCalculationData(JSON.toJSONString(result));
                    measureData.setMeasurementStartTime(startTime);
                    measureData.setMeasurementEndTime(endTime);
                    measureData.setIsDelete("0");
                    measureData.setIsSave("1");
                    traceDataService.save(measureData);
                    TMeasureAnalysisData analysis = new TMeasureAnalysisData();
                    analysis.setAnalysisNumber(number);
                    analysis.setAnalysisName(sample.getSampleName() + "-" + number);
                    analysis.setTaskId(taskId);
                    analysis.setSampleKey(sampleId);
                    analysis.setMeasureDataId(measureData.getId());
                    analysis.setAnalysisParam(JSON.toJSONString(changeParamList));
                    analysis.setAnalysisResult(JSON.toJSONString(result));
                    analysis.setAnalysisType("5");
                    if (peakValleys != null) {
                        analysis.setPeak(JSON.toJSONString(peakValleys));
                    }
                    if (markings != null) {
                        analysis.setGrain(JSON.toJSONString(markings));
                    }
                    // 保存本次寻峰使用的参数
                    PeakSearchParam peakSearchParam3;
                    String existingPeakParam3 = analysis.getPeakSearchParam();
                    if (existingPeakParam3 != null) {
                        peakSearchParam3 = JSON.parseObject(existingPeakParam3, PeakSearchParam.class);
                    } else {
                        peakSearchParam3 = new PeakSearchParam();
                    }
                    peakSearchParam3.setPeakValleyDiff(confVO.getPeakValleyDiff());
                    peakSearchParam3.setPeakInterval(confVO.getPeakInterval());
                    peakSearchParam3.setIsSelected("1");
                    analysis.setPeakSearchParam(JSON.toJSONString(peakSearchParam3));
                    analysis.setColor(ColorUtil.getRandomColor());
                    analysis.setIsDelete("0");
                    analysis.setIsSave("1");
                    tMeasureAnalysisDataService.save(analysis);
                    TMeasureAnalysisImage tMeasureAnalysisImage = new TMeasureAnalysisImage();
                    tMeasureAnalysisImage.setAnalysisId(analysis.getId());
                    tMeasureAnalysisImageService.save(tMeasureAnalysisImage);
                }
            }

            /// 更新任务状态: 只要该样品已保存测量数据(不论重复次数是否测满)即标记为已测
            QueryWrapper<TMeasureAnalysisData> testedWrapper = new QueryWrapper<>();
            testedWrapper.eq("task_id", taskId);
            testedWrapper.eq("sample_key", sampleId);
            testedWrapper.eq("analysis_type", "5");
            testedWrapper.eq("is_delete", "0");
            testedWrapper.isNotNull("analysis_result");
            if (tMeasureAnalysisDataService.count(testedWrapper) > 0) {
                List<TaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
                if (specimen != null) {
                    for (TaskSpecimen taskSample : specimen) {
                        if (taskSample.getSpecimenKey().equals(sampleId)) {
                            taskSample.setIsTested("1");
                        }
                    }
                    task.setSampleKey(JSON.toJSONString(specimen));
                    task.setDeviceSerialNumber(deviceSerialNumber);
                    task.setTaskStatus(TaskStatusEnum.TESTED.getCode());
                    boolean update = tMeasureTaskService.saveOrUpdate(task);
                    if (!update) {
                        log.error("更新任务失败: " + taskId);
                    }
                }
            }

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
        Constants.pushInterval = null;
        Constants.firstDataTime = null;
        Constants.secondDataTime = null;
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


    private void sendMessage(String deviceSerialNumber, JSONObject object) {

        WebSocketResponseVO vo = new WebSocketResponseVO();
        vo.setOperation(WebSocketOperationEnum.Spectrum.getOperation());
        vo.setData(object);

        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }

    private void sendError(String deviceSerialNumber, String message) {

        SessionState state = sessionStates.get(deviceSerialNumber);
        if (state != null && state.finalMessageSent.getAndSet(true)) {
            log.warn("最终消息已发送过，忽略重复错误发送: sessionId={}", deviceSerialNumber);
            infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
            cleanupResources(deviceSerialNumber);
            return;
        }

        infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
        webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.Spectrum.getOperation(), message);
        cleanupResources(deviceSerialNumber);
    }
}