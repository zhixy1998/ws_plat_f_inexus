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
import com.persee.workstation.uv_inexus.enums.measurementEnums.ScanSpeedEnum;
import com.persee.workstation.uv_inexus.model.R;
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
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.ActionCmdEnum;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class ConciseSpectrumNewWebSocket {

    private static final Logger log = LoggerFactory.getLogger(ConciseSpectrumNewWebSocket.class);
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
    @Autowired
    DeviceInfoService infoService;
    // 注入PythonPathConstant
    @Autowired
    private PythonPathConstant pythonPathConstant;
    @Autowired
    WebSocketSendUtil webSocketSendUtil;
    @Autowired
    private DevOperationUtilService devOperationUtilService;

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
        // 匀速推送调度器映射：sampleId_count -> SmoothPushScheduler
        private final Map<String, SmoothPushScheduler> smoothPushSchedulers = new ConcurrentHashMap<>();
        private final AtomicBoolean finalMessageSent = new AtomicBoolean(false);
        private final AtomicInteger batchSeqGenerator = new AtomicInteger(0);
        private ConciseSpectrumNewWebSocket parent;

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

        public int nextBatchSeq() {
            return batchSeqGenerator.getAndIncrement();
        }

        public void setParent(ConciseSpectrumNewWebSocket parent) {
            this.parent = parent;
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
                // 获取当前批次序号
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
         * 等待所有调度器完成推送
         * @param timeoutMs 超时时间（毫秒）
         */
        public void waitForAllSchedulersToFinish(long timeoutMs) {
            long startTime = System.currentTimeMillis();
            for (SmoothPushScheduler scheduler : smoothPushSchedulers.values()) {
                long remaining = timeoutMs - (System.currentTimeMillis() - startTime);
                if (remaining <= 0) {
                    log.warn("等待调度器完成超时，session: {}", deviceSerialNumber);
                    break;
                }
                // 等待该调度器停止
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

        /**
         * 获取或创建匀速推送调度器
         */
        public SmoothPushScheduler getOrCreateScheduler(String sampleId, Integer count, Integer digit, String luminosity,
                                                        java.util.function.Supplier<String> currentSampleCellSupplier) {
            String key = sampleId + "_" + count;
            return smoothPushSchedulers.computeIfAbsent(key, k ->
                    new SmoothPushScheduler(parent, deviceSerialNumber, sampleId, count, digit, luminosity, currentSampleCellSupplier));
        }

        /**
         * 清理指定样品的调度器
         */
        public void cleanupScheduler(String sampleId, Integer count) {
            String key = sampleId + "_" + count;
            SmoothPushScheduler scheduler = smoothPushSchedulers.remove(key);
            if (scheduler != null) {
                scheduler.cleanup();
            }
        }

        /**
         * 清理所有调度器
         */
        public void cleanupAllSchedulers() {
            for (SmoothPushScheduler scheduler : smoothPushSchedulers.values()) {
                scheduler.cleanup();
            }
            smoothPushSchedulers.clear();
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
        private static final double ACCELERATE_RATIO = 0.1;    // 加速时：原始间隔 * 0.1（比如 82ms -> 8.2ms）
        private static final double SLOW_RATIO = 2.0;          // 减速时：原始间隔 * 2（比如 82ms -> 164ms）

        private final ConciseSpectrumNewWebSocket parent;
        private final java.util.function.Supplier<String> currentSampleCellSupplier;

        public SmoothPushScheduler(ConciseSpectrumNewWebSocket parent,
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
            // 将本批次所有点加入队列
            for (int i = 0; i < xBatch.size(); i++) {
                pendingQueue.add(new SmoothPushTask(xBatch.get(i), yBatch.get(i), batchSeq, i));
            }

            int queueSize = pendingQueue.size();
            log.info("接收批次数据: session={}, 点数={}, 队列总点数={}",
                    sessionId, xBatch.size(), queueSize);

            // 如果还没有计算出间隔，先计算基础间隔
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

                // 启动调度器
                startScheduling();
                return;
            }

            // 间隔已计算，检查是否需要动态调速
            checkAndAdjustSpeed();
        }

        /**
         * 根据队列积压情况动态调整推送速度（基于原始间隔的比例）
         */
        private void checkAndAdjustSpeed() {
            int queueSize = pendingQueue.size();
            long newInterval = currentIntervalMs;

            if (queueSize > ACCELERATE_THRESHOLD) {
                // 队列积压超过100个点，加速推送（原始间隔 * 0.1）
                newInterval = Math.max(1, (long) (baseIntervalMs * ACCELERATE_RATIO));
                if (currentIntervalMs != newInterval) {
                    log.info("队列积压({}点)超过阈值{}，加速推送: {}ms -> {}ms (基础间隔{}ms * {})",
                            queueSize, ACCELERATE_THRESHOLD, currentIntervalMs, newInterval, baseIntervalMs, ACCELERATE_RATIO);
                    currentIntervalMs = newInterval;
                    restartScheduler();
                }
            } else if (queueSize <= SLOW_UPPER_THRESHOLD) {
                // 队列低于10个点，略微降低推送速度（原始间隔 * 2）
                newInterval = (long) (baseIntervalMs * SLOW_RATIO);
                if (currentIntervalMs != newInterval) {
                    log.info("队列({}点)低于阈值{}，略微降速: {}ms -> {}ms (基础间隔{}ms * {})",
                            queueSize, SLOW_UPPER_THRESHOLD, currentIntervalMs, newInterval, baseIntervalMs, SLOW_RATIO);
                    currentIntervalMs = newInterval;
                    restartScheduler();
                }
            } else if (queueSize <= NORMAL_LOWER_THRESHOLD && currentIntervalMs != baseIntervalMs) {
                // 队列在10-50之间，恢复原始速度
                log.info("队列({}点)恢复正常范围，恢复原始速度: {}ms -> {}ms",
                        queueSize, currentIntervalMs, baseIntervalMs);
                currentIntervalMs = baseIntervalMs;
                restartScheduler();
            }
            // 队列在50-100之间，保持当前速度不变
        }

        /**
         * 重启调度器（应用新的推送间隔）
         */
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
            if (task == null) {
                return;
            }

            // 每20个点检查一次队列长度，动态调整速度
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
                                luminosity.equals("3") ? "Es" : "Er";
                point.put("yType", luminosityType);
                dataArray.add(point);

                JSONObject payload = new JSONObject();
                payload.put("data", dataArray);
                payload.put("sampleId", sampleId);
                payload.put("count", count);
                payload.put("currentPoolNum", currentSampleCellSupplier.get());
                payload.put("smoothPush", true);

                parent.sendMessage(sessionId, payload);
                log.info("匀速推送一个点: x={}, y={}, 剩余点数={}", task.x, task.y, remainingSize);
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

        /**
         * 测量结束时调用：立即停止调度器并推送所有剩余点
         */
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
                                luminosity.equals("3") ? "Es" : "Er";
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
            final int batchSeq;      // 所属批次序号
            final int pointIndex;    // 批次内第几个点（从0开始）

            SmoothPushTask(Double x, Double y, int batchSeq, int pointIndex) {
                this.x = x;
                this.y = y;
                this.batchSeq = batchSeq;
                this.pointIndex = pointIndex;
            }

            @Override
            public int compareTo(SmoothPushTask other) {
                // 先按批次序号排序，相同批次再按点内顺序排序
                int cmp = Integer.compare(this.batchSeq, other.batchSeq);
                if (cmp != 0) return cmp;
                return Integer.compare(this.pointIndex, other.pointIndex);
            }
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
        int batchSeq;

        public BatchData(List<Double> xBatch, List<Double> yBatch, int startIndex,
                         Integer count,String sessionId, String sampleId, Integer digit,
                         FSpectrumParameter parameter, FMeasureTask task, int batchSeq) {
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
    public void measureTask(FSpectrumNewWebSocketParam param) {
        log.info("仪器编号: {} 进入扫描 时间: {}", param.getDeviceSerialNumber(), LocalDateTime.now());

        // 检查是否已有测量任务正在运行
        Thread existingThread = taskThreads.get(param.getDeviceSerialNumber());
        if (existingThread != null && existingThread.isAlive()) {
            log.warn("设备 {} 已有测量任务正在执行，忽略新请求", deviceSerialNumber);
            sendError(deviceSerialNumber, "设备正在测量中，请勿重复操作");
            return;
        }

        // 初始化会话状态
        SessionState state = new SessionState();
        state.setParent(this);
        sessionStates.put(param.getDeviceSerialNumber(), state);

        currentSampleCell=null;

        deviceSerialNumber = param.getDeviceSerialNumber();

        state.setBatchSize(staircaseEliminationBatchSize);

        // 初始化发送锁
        sessionSendLocks.put(deviceSerialNumber, new Object());

        infoService.updateDeviceCurrentStatus(deviceSerialNumber,WebSocketOperationEnum.ConciseSpectrum.getOperation(), param.getTaskId(), Constants.DEVICE_STATUS_MEASURE);

        clearGlobalData();
        // 停止状态给初始值
        Constants.measureStopStatus.put(deviceSerialNumber, true);

        if (Constants.endTime != null || Constants.startTime != null) {
            sendError(deviceSerialNumber, "当前测量未结束,无法开启新的测试！");
            return;
        }

        String taskId = param.getTaskId();
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

        // 设置会话状态
        state.deviceSerialNumber = deviceSerialNumber;
        state.digit = digit;

        // 根据任务id查询任务数据
        FMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null || task.getSampleKey() == null || task.getSampleKey().equals("")) {
            sendError(deviceSerialNumber,  "请先创建样品");
            return;
        }
        // 根据方法id查询方法数据
        FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            sendError(deviceSerialNumber,  "方法不存在");
            return;
        }

        // 将方法参数提取出来
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        // 将任务中的样品数据取出来
        List<FTaskSpecimen> sampleData = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        List<FTaskSpecimen> newSampleData = new ArrayList<>();
        for (FTaskSpecimen taskSpecimen : sampleData) {
            // 样品未测量并且未删除
            if ("0".equals(taskSpecimen.getDeleteType()) && "0".equals(taskSpecimen.getIsTested())) {
                newSampleData.add(taskSpecimen);
            }
        }

        // 根据序列号查询对应的附件配置表信息
        QueryWrapper<SamplePoolConfigPO> wrapper = new QueryWrapper<>();
        wrapper.eq("device_serial_number", deviceSerialNumber);
        SamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);
        if (configPO == null) {
            sendError(deviceSerialNumber,  "没有找到对应的附件配置信息");
            return;
        }

        String maxLocation = "";
        if (configPO.getIsAutoChangePool()) {
            List<String> sampleIds = newSampleData.stream().map(obj -> obj.getSpecimenKey()).collect(Collectors.toCollection(ArrayList::new));
            QueryWrapper<FMeasureSample> eq = new QueryWrapper<>();
            eq.in("id", sampleIds);
            List<FMeasureSample> list = tMeasureSampleService.list(eq);
            maxLocation = list.stream().map(FMeasureSample::getSampleLocation).max(String::compareTo).orElse(null);
        }

/// 设置参数
//        Result result = spectrumService.setUpParametersPort(task.getMethodKey(), deviceSerialNumber);
//        if (result.getCode() != 200) {
//            sendError(deviceSerialNumber, result.getMessage());
//            return;
//        }

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

        boolean repeatMode = "单池重复".equalsIgnoreCase(spectrumParameter.getRepetitionPattern());

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
        String finalMaxLocation = maxLocation;
        boolean finalPoolStatus = poolStatus;
        DevOperationInputParam finalParam = inputParam;

        Thread taskThread = new Thread(() -> {
            try {
                if (!repeatMode) {
                    // 不重复
                    for (int i = 0; i < newSampleData.size()&&Constants.measureStopStatus.get(deviceSerialNumber); i++) {


                        // 预留: 设置样品池位置
                        FMeasureSample sample = tMeasureSampleService.getById(newSampleData.get(i).getSpecimenKey());
                        if (configPO.getIsAutoChangePool()) {
                            if (sample == null) {
                                sendError(deviceSerialNumber,  "请先创建样品");
                                return;
                            }
                            if (finalPoolStatus) {
                                SetParam setParam = new SetParam();
                                setParam.setCellPos(Integer.parseInt(sample.getSampleLocation()));
                                boolean status = spectrumService.setInstrumentParameters(deviceSerialNumber, setParam);
                                if (!status) {
                                    sendError(deviceSerialNumber,  "设置出错");
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
                        } else {
                            break;
                        }
                    }
                } else {
                    // 单池重复
                    for (int i = 0; i < newSampleData.size()&&Constants.measureStopStatus.get(deviceSerialNumber); i++) {
                        // 预留: 设置样品池位置
                        FMeasureSample sample = tMeasureSampleService.getById(newSampleData.get(i).getSpecimenKey());
                        if (configPO.getIsAutoChangePool()) {
                            if (sample == null) {
                                sendError(deviceSerialNumber,  "请先创建样品");
                                return;
                            }
                            if (finalPoolStatus) {
                                SetParam setParam = new SetParam();
                                setParam.setCellPos(Integer.parseInt(sample.getSampleLocation()));
                                boolean status = spectrumService.setInstrumentParameters(deviceSerialNumber, setParam);
                                if (!status) {
                                    sendError(deviceSerialNumber,  "设置出错");
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
                            QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                            queryWrapper.eq("task_id", taskId);
                            queryWrapper.eq("sample_key", newSampleData.get(i).getSpecimenKey());
                            queryWrapper.eq("analysis_type", "5");
                            queryWrapper.eq("analysis_number", r + 1);
                            queryWrapper.last("LIMIT 1");
                            FMeasureAnalysisData measureData = tMeasureAnalysisDataService.getOne(queryWrapper);
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
                                    if (waitWithStopCheck( spectrumParameter.getTimeInterval())) {
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
                            if ((i + 1) != newSampleData.size() && !sample.getSampleLocation().equals(finalMaxLocation)) {
                                JSONObject payload = new JSONObject();
                                payload.put("result", "finish");
                                sendMessage(deviceSerialNumber, payload);
                            }else{
                               break;
                            }
                        } else {
                            break;
                        }
                    }
                }

                // 返回一号样品池
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
                    payload.put("success", "扫描成功");
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
                sendError(deviceSerialNumber,  "执行异常: " + e.getMessage());
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
            if (state != null) {
                // 等待所有调度器完成推送（最多等待5秒）
                state.waitForAllSchedulersToFinish(5000);
                // 然后清理资源（确保不再引用）
                state.cleanupAllSchedulers();
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
        SessionState state = sessionStates.get(sessionId);

        // ========== 防重检查 ==========
        if (state != null && state.finalMessageSent.getAndSet(true)) {
            log.warn("最终消息已发送过，忽略重复调用: sessionId={}", sessionId);
            return;
        }
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
                                        FSpectrumParameter spectrumParameter,
                                        FMeasureTask task,
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
        MeasurementResult result = doOneMeasurementCore(command, spectrumParameter,task, sampleId, ip, count);

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
        log.info("数据处理结束时间: {}", System.currentTimeMillis());
        // 保存数据
        saveOneMeasurementWithSnapshots(task, taskId, sampleId, deviceSerialNumber,
                finalResult.x, finalResult.y, count, finalResult.startTime, finalResult.endTime,
                finalResult.changeParams, latch);
        log.info("保存数据结束时间: {}", System.currentTimeMillis());
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
            default:
                luminosity = "Abs";
                break;
        }
        // 发送测量命令
        R<OperationResultEntity> r = devOperationUtilService.operation(command);
        if (!r.getSuccess()) {
            sendError(deviceSerialNumber,r.getMessage());
        }
        log.info("发送命令结束时间: {}", System.currentTimeMillis());
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
                    processedData.changeParams
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
                                case NORMAL:
                                    log.info("设备 {} 的测量正常完成", deviceSerialNumber);
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
                            Constants.instrumentData.add(message.getWavelength()+","+message.getAbsResult().getAbs());
                        }else if (spectrumParameter.getLuminosity().equals("2")) {
                            y.add(Double.valueOf(message.getTResult().getTPercent()));
                            Constants.instrumentData.add(message.getWavelength()+","+message.getAbsResult().getTPercent());
                        }else if (spectrumParameter.getLuminosity().equals("3")) {
                            y.add(Double.valueOf(message.getEsResult().getEs()));
                            Constants.instrumentData.add(message.getWavelength()+","+message.getAbsResult().getEs());
                        }else {
                            y.add(Double.valueOf(message.getErResult().getEr()));
                            Constants.instrumentData.add(message.getWavelength()+","+message.getAbsResult().getEr());
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
        log.info("扫描结束结束时间: {}", System.currentTimeMillis());
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
        log.info("测量时间 - 开始: " + Constants.startTime + ", 结束: " + Constants.endTime);
        log.info("仪器数据快照大小: " + Constants.instrumentData.size());
        log.info("原始计算数据快照大小: " + Constants.originalCalculationData.size());
        log.info("阶梯校正状态: " + spectrumParameter.getLadderCorrection());

        // 返回快照 + x/y
        return new MeasurementResult(
                x,
                processedYForReturn, // 这里的y已经是处理后的数据（校正或原始）
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

                java.util.function.Supplier<String> cellSupplier = () -> currentSampleCell;

                SmoothPushScheduler scheduler = state.getOrCreateScheduler(
                        batchData.sampleId,
                        batchData.count,
                        batchData.digit,
                        batchData.parameter.getLuminosity(),
                        cellSupplier);

                // 将处理后的数据交给调度器（不直接推送）
                scheduler.receiveBatch(batchData.xBatch, processedY, batchData.batchSeq);

                // 更新已推送点数
                if (state != null) {
                    state.incrementPushedPoints(batchData.xBatch.size());
                }

                log.info("批次数据已提交给匀速推送调度器，点数: " + batchData.xBatch.size());
            } catch (Exception e) {
                log.error("处理批次数据异常", e);
                // 如果发生异常，尝试立即推送原始数据作为降级方案
                try {
                    if (batchData.xBatch != null && !batchData.xBatch.isEmpty()) {
                        pushBatchDataDirectly(batchData.xBatch, batchData.yBatch,
                                batchData.count, batchData.sampleId,
                                batchData.parameter.getLuminosity(), batchData.digit);
                    }
                } catch (IOException ioException) {
                    log.error("降级推送原始数据也失败", ioException);
                }
            }
        });
    }

    /**
     * 直接推送批次数据（不进行台阶消除）
     */
    private void  pushBatchDataDirectly(List<Double> xBatch,
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
                FMeasureData measureData = new FMeasureData();
                measureData.setSerialNumber(count);
                measureData.setTaskId(taskId);
                measureData.setSampleKey(sampleId);
                measureData.setOriginalCalculationData(JSON.toJSONString(batchResult));
                // 其他字段暂时不设置
                measureData.setIsDelete("0");
                measureData.setIsSave("0"); // 标记为未完成
                tMeasureDataService.saveOrUpdate(measureData);

                // 2. 获取样品名称用于分析数据名称
                FMeasureSample sample = tMeasureSampleService.getById(sampleId);
                String sampleName = sample != null ? sample.getSampleName() : "样品";

                // 3. 创建分析数据记录（只放数据，其他字段为空）
                FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
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
                tMeasureAnalysisDataService.saveOrUpdate(analysisData);

                // 4. 创建关联的图片记录（空记录）
                FMeasureAnalysisImage analysisImage = new FMeasureAnalysisImage();
                analysisImage.setAnalysisId(analysisData.getId());
                tMeasureAnalysisImageService.saveOrUpdate(analysisImage);

                log.info("首次创建数据记录: measureDataId={}, analysisDataId={}",
                        measureData.getId(), analysisData.getId());

            } else {
                // 后续批次，追加数据到已有记录

                // 1. 查询已存在的测量数据记录
                QueryWrapper<FMeasureData> dataQuery = new QueryWrapper<>();
                dataQuery.eq("task_id", taskId)
                        .eq("sample_key", sampleId)
                        .eq("serial_number", count)
                        .last("limit 1");
                FMeasureData existingMeasureData = tMeasureDataService.getOne(dataQuery);

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
                    tMeasureDataService.saveOrUpdate(existingMeasureData);

                    log.info("追加数据到测量记录: 原有{}点, 新增{}点, 总计{}点",
                            existingData.size() - batchResult.size(), batchResult.size(), existingData.size());
                } else {
                    log.warn("未找到对应的测量数据记录，重新创建: taskId={}, sampleId={}, count={}",
                            taskId, sampleId, count);
                    // 如果找不到记录，重新创建（兜底逻辑）
                    FMeasureData newMeasureData = new FMeasureData();
                    newMeasureData.setSerialNumber(count);
                    newMeasureData.setTaskId(taskId);
                    newMeasureData.setSampleKey(sampleId);
                    newMeasureData.setOriginalCalculationData(JSON.toJSONString(batchResult));
                    newMeasureData.setIsDelete("0");
                    newMeasureData.setIsSave("0");
                    tMeasureDataService.saveOrUpdate(newMeasureData);
                }

                // 2. 查询已存在的分析数据记录
                QueryWrapper<FMeasureAnalysisData> analysisQuery = new QueryWrapper<>();
                analysisQuery.eq("task_id", taskId)
                        .eq("sample_key", sampleId)
                        .eq("analysis_number", count)
                        .eq("analysis_type", "5")
                        .last("limit 1");
                FMeasureAnalysisData existingAnalysisData = tMeasureAnalysisDataService.getOne(analysisQuery);

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
                    tMeasureAnalysisDataService.saveOrUpdate(existingAnalysisData);

                    log.info("追加数据到分析记录: 原有{}点, 新增{}点, 总计{}点",
                            existingResult.size() - batchResult.size(), batchResult.size(), existingResult.size());
                } else {
                    log.warn("未找到对应的分析数据记录，重新创建: taskId={}, sampleId={}, count={}",
                            taskId, sampleId, count);
                    // 如果找不到记录，重新创建（兜底逻辑）
                    FMeasureSample sample = tMeasureSampleService.getById(sampleId);
                    String sampleName = sample != null ? sample.getSampleName() : "样品";

                    FMeasureAnalysisData newAnalysisData = new FMeasureAnalysisData();
                    newAnalysisData.setAnalysisNumber(count);
                    newAnalysisData.setAnalysisName(sampleName + "-" + count);
                    newAnalysisData.setTaskId(taskId);
                    newAnalysisData.setSampleKey(sampleId);
                    newAnalysisData.setAnalysisResult(JSON.toJSONString(batchResult));
                    newAnalysisData.setAnalysisType("5");
                    newAnalysisData.setColor(ColorUtil.getRandomColor());
                    newAnalysisData.setIsDelete("0");
                    newAnalysisData.setIsSave("0");
                    tMeasureAnalysisDataService.saveOrUpdate(newAnalysisData);
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
    public List<Double> processAndSaveDataStep(FMeasureTask task,
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
     * 消除尖刺
     */
    public List<Double> processAndSaveDataSpine(FMeasureTask task,
                                                List<Double> x,
                                                List<Double> y,
                                                FSpectrumParameter parameter,
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
     * 调用 Python 服务处理消除尖刺
     */
    private List<Double> eliminationSpinePython(List<Double> x, List<Double> y, FSpectrumParameter parameter) {

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
     * 修复重复模式下的重复存储问题
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
                latch.countDown();
            }
            return;
        }
        String sessionId = deviceSerialNumber;
        String storageKey = sessionId + "_" + sampleId + "_" + number;

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
            if (latch != null) latch.countDown();
            return;
        }

        FMeasureSample sample = tMeasureSampleService.getById(taskSpecimen.getSpecimenKey());
        FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        Boolean isScanFast = spectrumParameter.getScanSpeed().equals(ScanSpeedEnum.EXTREME_FAST.getCode());

        try {
            SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
            // 1. 处理峰谷数据
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            // 使用原始Y值进行峰谷分析
            String key = deviceSerialNumber + "_" + sampleId + "_" + number;
            List<Double> originalY = originalYValues.get(key);
            if (originalY == null) {
                log.warn("未找到原始Y值，使用处理后的Y值进行峰谷分析: key={}, x.size={}, y.size={}",
                        key, x.size(), y.size());
                originalY = y;
            } else {
                if (originalY.size() != x.size()) {
                    log.error("原始Y值与X值长度不匹配: originalY.size={}, x.size={}, key={}",
                            originalY.size(), x.size(), key);
                    originalY = y;
                } else {
                    int zeroCount = 0;
                    for (Double value : originalY) {
                        if (value == null || Math.abs(value) < 1e-10) {
                            zeroCount++;
                        }
                    }
                    if (zeroCount > x.size() * 0.5) {
                        log.error("原始Y值中有过多0值({}/{}), 使用处理后的Y值: key={}",
                                zeroCount, x.size(), key);
                        originalY = y;
                    }
                }
            }

            List<FPeakValleyMarking> peakValleys = new ArrayList<>();
            List<FPeakValleyMarking> markings = new ArrayList<>();
            if (!isScanFast) {
                String peakUrl = pythonPathConstant.getPeakSearching();
                String valleyUrl = pythonPathConstant.getValleySearching();
                try {
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
                }
            }

            // 构建结果字符串，存储时使用原始Y值
            List<String> result = new ArrayList<>();
            for (int i = 0; i < x.size(); i++) {
                double originalYValue = originalY != null && i < originalY.size() ? originalY.get(i) : y.get(i);
                result.add(x.get(i) + "," + originalYValue);
            }

            // 判断测量模式
            boolean isRepeatMode = !"不重复".equals(spectrumParameter.getRepetitionPattern());

            if (!isRepeatMode) {
                // ========== 普通模式（不重复） - 保持不变 ==========
                FMeasureData measureData = traceDataService.getOne(new QueryWrapper<FMeasureData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", sampleId)
                        .eq("serial_number", number)
                        .last("limit 1"));
                if (measureData == null) {
                    measureData = new FMeasureData();
                }
                measureData.setSerialNumber(1);
                measureData.setTaskId(taskId);
                measureData.setSampleKey(sampleId);
                measureData.setInstrumentData(JSON.toJSONString(Constants.instrumentData));
                measureData.setOriginalCalculationData(JSON.toJSONString(result));
                measureData.setMeasurementStartTime(startTime);
                measureData.setMeasurementEndTime(endTime);
                measureData.setIsDelete("0");
                measureData.setIsSave("1");
                traceDataService.saveOrUpdate(measureData);

                FMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(new QueryWrapper<FMeasureAnalysisData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", sampleId)
                        .eq("analysis_number", number)
                        .last("limit 1"));
                if (analysisData == null) {
                    analysisData = new FMeasureAnalysisData();
                }
                analysisData.setAnalysisNumber(1);
                analysisData.setAnalysisName(sample.getSampleName() + "(" + analysisData.getAnalysisNumber() + ")");
                analysisData.setTaskId(taskId);
                analysisData.setSampleKey(sampleId);
                analysisData.setMeasureDataId(measureData.getId());
                analysisData.setAnalysisParam(JSON.toJSONString(changeParams));
                analysisData.setAnalysisResult(JSON.toJSONString(result));
                analysisData.setAnalysisType("5");
                if (peakValleys != null) analysisData.setPeak(JSON.toJSONString(peakValleys));
                if (markings != null) analysisData.setGrain(JSON.toJSONString(markings));
                analysisData.setColor(ColorUtil.getRandomColor());
                analysisData.setIsDelete("0");
                analysisData.setIsSave("1");
                tMeasureAnalysisDataService.saveOrUpdate(analysisData);

                FMeasureAnalysisImage tMeasureAnalysisImage = tMeasureAnalysisImageService.getOne(
                        new QueryWrapper<FMeasureAnalysisImage>().eq("analysis_id", analysisData.getId()));
                if (tMeasureAnalysisImage == null) {
                    tMeasureAnalysisImage = new FMeasureAnalysisImage();
                }
                tMeasureAnalysisImage.setAnalysisId(analysisData.getId());
                tMeasureAnalysisImageService.saveOrUpdate(tMeasureAnalysisImage);
            } else {
                // ========== 重复模式 - 修复重复存储问题 ==========
                // 查询已存在的分析记录（不限 isSave 状态）
                QueryWrapper<FMeasureAnalysisData> analysisQuery = new QueryWrapper<>();
                analysisQuery.eq("task_id", taskId)
                        .eq("sample_key", sampleId)
                        .eq("analysis_number", number)
                        .eq("analysis_type", "5");
                List<FMeasureAnalysisData> existingAnalysisList = tMeasureAnalysisDataService.list(analysisQuery);

                // 如果存在多条记录（可能是异步批次保存造成的），保留第一条，删除其余
                FMeasureAnalysisData analysisData = null;
                if (existingAnalysisList != null && !existingAnalysisList.isEmpty()) {
                    if (existingAnalysisList.size() > 1) {
                        log.warn("重复模式下发现多条分析记录（数量：{}），将保留第一条并删除多余记录。taskId={}, sampleId={}, number={}",
                                existingAnalysisList.size(), taskId, sampleId, number);
                        // 保留第一条
                        analysisData = existingAnalysisList.get(0);
                        // 删除其余记录
                        for (int i = 1; i < existingAnalysisList.size(); i++) {
                            FMeasureAnalysisData redundant = existingAnalysisList.get(i);
                            tMeasureAnalysisDataService.removeById(redundant.getId());
                            log.info("已删除多余分析记录：analysisId={}", redundant.getId());
                        }
                    } else {
                        analysisData = existingAnalysisList.get(0);
                    }
                }

                // 处理关联的测量数据
                FMeasureData measureData = null;
                if (analysisData != null) {
                    // 已有分析记录，查找对应的测量数据
                    measureData = tMeasureDataService.getById(analysisData.getMeasureDataId());
                }
                if (measureData == null) {
                    // 如果测量数据不存在，先查询是否存在该测量记录（可能由批次保存创建）
                    QueryWrapper<FMeasureData> dataQuery = new QueryWrapper<>();
                    dataQuery.eq("task_id", taskId)
                            .eq("sample_key", sampleId)
                            .eq("serial_number", number);
                    measureData = tMeasureDataService.getOne(dataQuery);
                }

                if (measureData == null) {
                    // 创建新的测量数据记录
                    measureData = new FMeasureData();
                    measureData.setSerialNumber(number);
                    measureData.setTaskId(taskId);
                    measureData.setSampleKey(sampleId);
                    measureData.setIsDelete("0");
                }
                // 更新测量数据
                measureData.setInstrumentData(JSON.toJSONString(Constants.instrumentData));
                measureData.setOriginalCalculationData(JSON.toJSONString(result));
                measureData.setMeasurementStartTime(startTime);
                measureData.setMeasurementEndTime(endTime);
                measureData.setIsSave("1");
                traceDataService.saveOrUpdate(measureData);

                if (analysisData == null) {
                    // 创建新的分析数据记录
                    analysisData = new FMeasureAnalysisData();
                    analysisData.setAnalysisNumber(number);
                    analysisData.setAnalysisName(sample.getSampleName() + "-" + number);
                    analysisData.setTaskId(taskId);
                    analysisData.setSampleKey(sampleId);
                    analysisData.setAnalysisType("5");
                    analysisData.setIsDelete("0");
                }
                // 更新分析数据（无论是新建还是已存在）
                analysisData.setMeasureDataId(measureData.getId());
                analysisData.setAnalysisParam(JSON.toJSONString(changeParams));
                analysisData.setAnalysisResult(JSON.toJSONString(result));
                if (peakValleys != null) analysisData.setPeak(JSON.toJSONString(peakValleys));
                if (markings != null) analysisData.setGrain(JSON.toJSONString(markings));
                analysisData.setColor(ColorUtil.getRandomColor());
                analysisData.setIsSave("1");
                tMeasureAnalysisDataService.saveOrUpdate(analysisData);

                // 处理关联的图片记录
                FMeasureAnalysisImage tMeasureAnalysisImage = tMeasureAnalysisImageService.getOne(
                        new QueryWrapper<FMeasureAnalysisImage>().eq("analysis_id", analysisData.getId()));
                if (tMeasureAnalysisImage == null) {
                    tMeasureAnalysisImage = new FMeasureAnalysisImage();
                    tMeasureAnalysisImage.setAnalysisId(analysisData.getId());
                    tMeasureAnalysisImageService.save(tMeasureAnalysisImage);
                }
            }

            // 更新任务状态
            List<FTaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            if (!isRepeatMode) {
                // 普通模式：直接标记该样品已测
                for (FTaskSpecimen taskSample : specimen) {
                    if (taskSample.getSpecimenKey().equals(sampleId)) {
                        taskSample.setIsTested("1");
                    }
                }
                task.setSampleKey(JSON.toJSONString(specimen));
                task.setDeviceSerialNumber(deviceSerialNumber);
                tMeasureTaskService.saveOrUpdate(task);
            } else {
                // 重复模式：检查该样品是否所有重复次数都已保存完成
                Integer repetitionCount = Integer.valueOf(spectrumParameter.getRepetitionTime());
                boolean allTested = true;
                for (int i = 1; i <= repetitionCount; i++) {
                    QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("task_id", taskId);
                    queryWrapper.eq("sample_key", sampleId);
                    queryWrapper.eq("analysis_number", i);
                    queryWrapper.eq("analysis_type", "5");
                    queryWrapper.eq("is_save", "1");  // 只有已保存完成的才算
                    FMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(queryWrapper);
                    if (one == null) {
                        allTested = false;
                        break;
                    }
                }
                if (allTested) {
                    for (FTaskSpecimen taskSample : specimen) {
                        if (taskSample.getSpecimenKey().equals(sampleId)) {
                            taskSample.setIsTested("1");
                        }
                    }
                    task.setSampleKey(JSON.toJSONString(specimen));
                    task.setDeviceSerialNumber(deviceSerialNumber);
                    tMeasureTaskService.saveOrUpdate(task);
                }
            }
            log.info("数据保存完成: 样品={}, 计数={}", sampleId, number);
        } catch (Exception e) {
            log.error("保存测量数据时发生异常", e);
        } finally {
            // 清理存储的原始Y值
            originalYValues.remove(storageKey);
            // 清理存储锁
            storageLatches.remove(storageKey);
            // 通知存储完成
            if (latch != null) {
                latch.countDown();
                log.debug("存储完成通知已发送, key: {}", storageKey);
            }
            // 更新会话状态
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
        vo.setOperation(WebSocketOperationEnum.ConciseSpectrum.getOperation());
        vo.setData(object);

        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }

    private void sendError(String deviceSerialNumber, String message) {
        SessionState state = sessionStates.get(deviceSerialNumber);

        // ========== 防重检查 ==========
        if (state != null && state.finalMessageSent.getAndSet(true)) {
            log.warn("最终消息已发送过，忽略重复错误发送: sessionId={}, message={}", deviceSerialNumber, message);
            // 但仍然需要清理资源（cleanupResources 内部有幂等处理）
            cleanupResources(deviceSerialNumber);
            return;
        }

        infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
        webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.ConciseSpectrum.getOperation(), message);
        cleanupResources(deviceSerialNumber);
    }
}