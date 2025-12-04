/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.concise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTimeSweepParamter;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class ConciseTimeWebSocket extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ConciseTimeWebSocket.class);

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, TimeScanContext> contexts = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 添加连接状态管理
    private static final Map<String, AtomicBoolean> connectionClosingMap = new ConcurrentHashMap<>();
    private static final Map<String, AtomicBoolean> stopProcessingMap = new ConcurrentHashMap<>();

    @Autowired private TimeSweepConciseService timeSweepConciseService;
    @Autowired private FMeasureTaskService tMeasureTaskService;
    @Autowired private FMeasureMethodService tMeasureMethodService;
    @Autowired private DeviceInfoService deviceInfoService;
    @Autowired private FMeasureDataService tMeasureDataService;
    @Autowired private FMeasureAnalysisDataService tMeasureAnalysisDataService;
    @Autowired private FMeasureAnalysisImageService tMeasureAnalysisImageService;
    @Autowired private FSamplePoolConfigService fSamplePoolConfigService;
    @Autowired private FMeasureSampleService tMeasureSampleService;
    @Autowired private SystemAlgorithmConfService systemAlgorithmConfService;
    @Autowired private DevMessageUtil devMessageUtil;
    @Autowired private RestTemplate restTemplate;
    @Autowired private FormulaUtil formulaUtil;
    @Autowired private PythonPathConstant pythonPathConstant;
    /**
     * 时间扫描上下文
     */
    private static class TimeScanContext {
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicBoolean dataSaved = new AtomicBoolean(false);
        final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);
        final AtomicBoolean connectionClosed = new AtomicBoolean(false);
        final CountDownLatch saveLatch = new CountDownLatch(1);

        ScheduledExecutorService scheduler;
        ScheduledFuture<?> scheduledTask;

        final List<BigDecimal> xData = new ArrayList<>();
        final List<BigDecimal> yData = new ArrayList<>();
        final List<ChangeParam> originalData = new ArrayList<>();

        String deviceSerialNumber;
        String ip;
        Integer digit;
        String currentSampleId;
        FMeasureTask task;

        void clearData() {
            xData.clear();
            yData.clear();
            originalData.clear();
        }

        void stopAllTasks() {
            running.set(false);

            if (scheduledTask != null && !scheduledTask.isDone()) {
                scheduledTask.cancel(true);
            }

            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
        }

        // 等待数据保存完成
        boolean waitForDataSave(long timeout, TimeUnit unit) {
            try {
                return saveLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        // 标记数据保存完成
        void markDataSaved() {
            dataSaved.set(true);
            saveLatch.countDown();
        }

        // 标记连接已关闭
        void markConnectionClosed() {
            connectionClosed.set(true);
        }

        boolean isConnectionClosed() {
            return connectionClosed.get();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("时间扫描连接建立: {}", sessionId);

        sessions.put(sessionId, session);
        connectionClosingMap.put(sessionId, new AtomicBoolean(false));
        stopProcessingMap.put(sessionId, new AtomicBoolean(false));

        TimeScanContext context = new TimeScanContext();
        contexts.put(sessionId, context);

        executor.execute(() -> {
            try {
                initializeAndStart(session, context);
            } catch (Exception e) {
                log.error("时间扫描初始化失败: {}", sessionId, e);
                safeCleanup(sessionId, true);
            }
        });
    }


    private void initializeAndStart(WebSocketSession session, TimeScanContext context) {
        String sessionId = session.getId();

        try {
            String query = Objects.requireNonNull(session.getUri()).getQuery();
            String taskId = getQueryParam(query, "taskId");
            String deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");
            String updateBy = getQueryParam(query, "updateBy");
            Integer digit = Integer.parseInt(getQueryParam(query, "digit"));

            if (taskId == null || deviceSerialNumber == null || updateBy == null) {
                safeCleanup(sessionId, true);
                return;
            }

            context.deviceSerialNumber = deviceSerialNumber;
            context.digit = digit;
            context.ip = deviceInfoService.getIp(deviceSerialNumber);

            FMeasureTask task = tMeasureTaskService.getById(taskId);
            if (task == null) {
                safeCleanup(sessionId, true);
                return;
            }
            context.task = task;

            R<Boolean> useResult = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);
            if (!useResult.getSuccess()) {
                safeCleanup(sessionId, true);
                return;
            }

            Result paramResult = timeSweepConciseService.setUpParametersPort(task.getMethodKey(), deviceSerialNumber);
            if (paramResult.getCode() != 200) {
                safeCleanup(sessionId, true);
                return;
            }

            FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method == null) {
                safeCleanup(sessionId, true);
                return;
            }

            FTimeSweepParamter timeParam = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
            startTimeMeasurement(session, context, timeParam, updateBy);

        } catch (Exception e) {
            log.error("时间扫描初始化异常: {}", sessionId, e);
            safeCleanup(sessionId, true);
        }
    }

    private void startTimeMeasurement(WebSocketSession session, TimeScanContext context,
                                      FTimeSweepParamter param, String updateBy) {
        String sessionId = session.getId();

        try {
            List<FTaskSpecimen> specimenList = JSON.parseArray(context.task.getSampleKey(), FTaskSpecimen.class);
            List<FTaskSpecimen> validSamples = new ArrayList<>();

            for (FTaskSpecimen specimen : specimenList) {
                if ("0".equals(specimen.getDeleteType()) && "0".equals(specimen.getIsTested())) {
                    validSamples.add(specimen);
                }
            }

            if (validSamples.isEmpty()) {
                safeCleanup(sessionId, true);
                return;
            }

            configureSamplePool(session, context, validSamples, updateBy);

            for (FTaskSpecimen sample : validSamples) {
                if (!context.running.get()) {
                    break;
                }

                context.currentSampleId = sample.getSpecimenKey();
                executeSampleMeasurement(session, context, param, sample);
            }

            // 只有在正常完成（没有被停止）的情况下才发送"扫描成功"
            if (context.running.get()) {
                sendMessageSafe(session, "扫描成功");
                // 正常完成 - 等待数据保存后再清理
                log.info("所有样品测量完成，等待数据保存...");
                if (context.waitForDataSave(30, TimeUnit.SECONDS)) {
                    log.info("数据保存完成，开始清理会话");
                    safeCleanup(sessionId, true);
                } else {
                    log.warn("数据保存超时，强制清理");
                    safeCleanup(sessionId, true);
                }
            }

        } catch (Exception e) {
            log.error("时间测量执行异常: {}", sessionId, e);
            safeCleanup(sessionId, true);
        }
    }

    /**
     * 配置样品池
     * @param session
     * @param context
     * @param samples
     * @param updateBy
     */
    private void configureSamplePool(WebSocketSession session, TimeScanContext context,
                                     List<FTaskSpecimen> samples, String updateBy) {
        try {
            QueryWrapper<FSamplePoolConfigPO> wrapper = new QueryWrapper<>();
            wrapper.eq("device_serial_number", context.deviceSerialNumber);
            FSamplePoolConfigPO config = fSamplePoolConfigService.getOne(wrapper);

            if (config == null || !config.getIsAutoChangePool()) {
                return;
            }

            List<String> sampleIds = new ArrayList<>();
            for (FTaskSpecimen sample : samples) {
                sampleIds.add(sample.getSpecimenKey());
            }

            QueryWrapper<FMeasureSample> sampleWrapper = new QueryWrapper<>();
            sampleWrapper.in("id", sampleIds);
            List<FMeasureSample> sampleList = tMeasureSampleService.list(sampleWrapper);

            if (sampleList.isEmpty()) {
                return;
            }

            FMeasureSample firstSample = sampleList.get(0);
            if (firstSample.getSampleLocation() != null && !firstSample.getSampleLocation().isEmpty()) {
                R<String> setResult = devMessageUtil.setCurrentCellPos(Integer.valueOf(firstSample.getSampleLocation()), context.ip);
                if (setResult.getSuccess()) {
                    fSamplePoolConfigService.updateCurrentPoolNum(context.deviceSerialNumber,
                            Integer.parseInt(firstSample.getSampleLocation()), updateBy);
                    JSONObject poolMsg = new JSONObject();
                    poolMsg.put("currentPoolNum", firstSample.getSampleLocation());
                    sendMessageSafe(session, poolMsg.toJSONString());
                }
            }

        } catch (Exception e) {
            log.warn("配置样品池异常: {}", e.getMessage());
        }
    }

    /**
     * 执行单个样品测量
     * @param session
     * @param context
     * @param param
     * @param sample
     */
    private void executeSampleMeasurement(WebSocketSession session, TimeScanContext context,
                                          FTimeSweepParamter param, FTaskSpecimen sample) {
        context.clearData();

        // 使用BigDecimal避免浮点数精度问题
        BigDecimal interval = new BigDecimal(param.getIntervalTime());
        int totalTasks = Integer.parseInt(param.getTotalNumber());

        // 计算总时长并增加10秒缓冲时间
        BigDecimal totalDuration = interval.multiply(BigDecimal.valueOf(totalTasks));
        long timeoutMs = totalDuration.multiply(BigDecimal.valueOf(1000)).longValue() + 10000; // 增加10秒缓冲

        log.info("开始样品测量: {}, 间隔: {}s, 总数: {}, 总时长+缓冲: {}ms",
                sample.getSpecimenKey(), interval, totalTasks, timeoutMs);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        context.scheduler = scheduler;

        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch completionLatch = new CountDownLatch(1);

        final ChangeParam baselineData = (context.task.getZeroParam() != null && !context.task.getZeroParam().isEmpty())
                ? JSON.parseObject(context.task.getZeroParam(), ChangeParam.class)
                : null;

        Constants.startTime = LocalDateTime.now();

        context.scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            // 检查运行状态，如果已停止则立即退出
            if (!context.running.get() || Thread.currentThread().isInterrupted()) {
                log.info("检测到任务已停止，退出测量循环");
                completionLatch.countDown();
                return;
            }

            int currentIndex = counter.getAndIncrement();

            if (currentIndex >= totalTasks) {
                log.info("样品测量完成: {}, 总点数: {}", sample.getSpecimenKey(), totalTasks);
                Constants.endTime = LocalDateTime.now();

                // 同步保存数据 - 确保在关闭线程前保存
                saveMeasurementDataSync(session, context, sample.getSpecimenKey());
                completionLatch.countDown();
                return;
            }

            try {
                executeSingleMeasurement(session, context, param, baselineData, currentIndex, interval);
            } catch (Exception e) {
                log.error("单次测量异常, 索引: {}", currentIndex, e);
            }

        }, 0, interval.multiply(BigDecimal.valueOf(1000)).longValue(), TimeUnit.MILLISECONDS);

        try {
            // 使用计算好的超时时间（包含10秒缓冲）
            if (!completionLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                log.warn("样品测量超时: {}, 已采集点数: {}", sample.getSpecimenKey(), context.xData.size());
                // 超时情况下先保存数据，再停止任务
                if (!context.xData.isEmpty()) {
                    Constants.endTime = LocalDateTime.now();
                    log.info("超时情况下保存已收集的数据，点数: {}", context.xData.size());
                    saveMeasurementDataSync(session, context, sample.getSpecimenKey());
                }
                // 标记任务停止
                context.running.set(false);
            }
        } catch (InterruptedException e) {
            log.warn("等待测量完成时被中断: {}", sample.getSpecimenKey());
            Thread.currentThread().interrupt();
            // 被中断时也要保存已收集的数据
            if (!context.xData.isEmpty()) {
                Constants.endTime = LocalDateTime.now();
                saveMeasurementDataSync(session, context, sample.getSpecimenKey());
            }
            context.running.set(false);
        } finally {
            // 确保调度器被关闭 - 在数据保存完成后关闭
            if (!scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
        }
    }

    private void executeSingleMeasurement(WebSocketSession session, TimeScanContext context,
                                          FTimeSweepParamter param, ChangeParam baselineData,
                                          int currentIndex, BigDecimal interval) {
        if (!context.running.get()) {
            return;
        }

        // 使用BigDecimal精确计算时间点，避免浮点数精度问题
        BigDecimal xValue = interval.multiply(BigDecimal.valueOf(currentIndex))
                .setScale(1, RoundingMode.HALF_UP);

        Command command = new Command(SetParamEnum.Measure.getComId(), new Object[]{});
        ChangeParam changeParam = devMessageUtil.timeSweepGetMeasureData(command, context.ip);

        if (changeParam == null) {
            log.error("获取测量数据失败");
            return;
        }

        Double wavelength = formulaUtil.calculateWl(Double.parseDouble(changeParam.getWl()), context.deviceSerialNumber);
        changeParam.setWl(String.valueOf(wavelength));

        context.originalData.add(changeParam);
        calculateAndPushData(session, context, param, baselineData, changeParam, xValue);
    }

    private void calculateAndPushData(WebSocketSession session, TimeScanContext context,
                                      FTimeSweepParamter param, ChangeParam baselineData,
                                      ChangeParam changeParam, BigDecimal xValue) {
        try {
            BigDecimal yValue = null;
            String yType = "";

            switch (param.getLuminosity()) {
                case "1": // Abs
                    String absValue = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
                    String absResult = DataCountUtil.changeTPercentOrAbs2(null, absValue);
                    yValue = new BigDecimal(absResult);
                    yType = "Abs";
                    break;

                case "2": // T%
                    String tPercent = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
                    yValue = new BigDecimal(tPercent);
                    yType = "T%";
                    break;

                case "3": // Es
                    Double sData = Double.valueOf(changeParam.getS());
                    String esValue = String.valueOf((sData / (4095 * 32)) * 100);
                    yValue = new BigDecimal(esValue);
                    yType = "Es";
                    break;

                case "4": // Er
                    Double rData = Double.valueOf(changeParam.getR());
                    String erValue = String.valueOf((rData / (4095 * 32)) * 100);
                    yValue = new BigDecimal(erValue);
                    yType = "Er";
                    break;

                default:
                    return;
            }

            context.xData.add(xValue);
            context.yData.add(yValue);
            pushDataPoint(session, xValue, yValue, context.currentSampleId, param, context.digit, yType);

        } catch (Exception e) {
            log.error("计算数据异常", e);
        }
    }

    private void pushDataPoint(WebSocketSession session, BigDecimal xValue, BigDecimal yValue,
                               String sampleId, FTimeSweepParamter param, Integer digit, String yType) {
        JSONObject payload = new JSONObject();
        JSONArray points = new JSONArray();
        JSONObject point = new JSONObject();

        point.put("x", xValue);

        Double decimalY = yValue.doubleValue();
        Double formattedValue = DataCountUtil.formatValue(digit, decimalY);
        String displayValue = DataCountUtil.addZero(formattedValue, digit);

        point.put("y", decimalY);
        point.put("roundOffY", displayValue);
        point.put("nm", param.getWaveLength() + "nm");
        point.put("xType", "s");
        point.put("yType", yType);

        points.add(point);
        payload.put("data", points);
        payload.put("sampleId", sampleId);

        sendMessageSafe(session, payload.toJSONString());
    }

    /**
     * 同步保存测量数据
     */
    private void saveMeasurementDataSync(WebSocketSession session, TimeScanContext context, String sampleId) {
        log.info("开始同步保存测量数据, 样品: {}, 数据点数: {}", sampleId, context.xData.size());

        try {
            // 在实际保存前再次检查运行状态
            if (!context.running.get()) {
                log.info("任务已停止，但仍保存已收集的数据");
            }

            saveMeasurementData(session, context, sampleId);
            context.markDataSaved();
            log.info("同步保存测量数据完成, 样品: {}", sampleId);
        } catch (Exception e) {
            log.error("同步保存测量数据异常", e);
            // 即使保存异常也要标记为已完成，避免死锁
            context.markDataSaved();
        }
    }

    /**
     * 保存测量数据
     */
    private void saveMeasurementData(WebSocketSession session, TimeScanContext context, String sampleId) {
        try {
            if (context.xData.isEmpty() || context.yData.isEmpty()) {
                log.warn("没有数据需要保存");
                return;
            }

            log.info("保存测量数据, 样品: {}, 数据点数: {}", sampleId, context.xData.size());

            // 保存数据到数据库
            SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> analysisBody = new HashMap<>();
            analysisBody.put("x_data", context.xData.stream().map(BigDecimal::doubleValue).collect(Collectors.toList()));
            analysisBody.put("y_data", context.yData.stream().map(BigDecimal::doubleValue).collect(Collectors.toList()));

            double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
            analysisBody.put("height", height);
            analysisBody.put("distance", confVO.getPeakInterval());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(analysisBody, headers);
            String peakUrl = pythonPathConstant.getPeakSearching();
            String valleyUrl = pythonPathConstant.getValleySearching();

            // 添加日志输出（调试用）
            log.info("调用寻峰接口: " + peakUrl);
            log.info("调用寻谷接口: " + valleyUrl);
            // 寻峰寻谷
            Map<String, Object> peakResult = restTemplate.postForEntity(peakUrl, request, Map.class).getBody();
            Map<String, Object> valleyResult = restTemplate.postForEntity(valleyUrl, request, Map.class).getBody();

            List<Double> peakX = (List<Double>) peakResult.get("peak_x");
            List<String> peakY = (List<String>) peakResult.get("peak_y");
            List<Double> valleyX = (List<Double>) valleyResult.get("valley_x");
            List<String> valleyY = (List<String>) valleyResult.get("valley_y");

            // 保存测量数据
            FMeasureData measureData = new FMeasureData();

            FMeasureData lastData = tMeasureDataService.getOne(
                    new QueryWrapper<FMeasureData>()
                            .eq("task_id", context.task.getId())
                            .eq("sample_key", sampleId)
                            .orderByDesc("serial_number")
                            .last("LIMIT 1")
            );

            measureData.setSerialNumber(lastData != null ? lastData.getSerialNumber() + 1 : 1);
            measureData.setTaskId(context.task.getId());
            measureData.setSampleKey(sampleId);
            measureData.setInstrumentData(JSON.toJSONString(context.originalData));

            List<String> resultData = new ArrayList<>();
            for (int i = 0; i < context.xData.size(); i++) {
                resultData.add(context.xData.get(i) + "," + context.yData.get(i));
            }
            measureData.setOriginalCalculationData(JSON.toJSONString(resultData));

            measureData.setMeasurementStartTime(Constants.startTime);
            measureData.setMeasurementEndTime(Constants.endTime);
            measureData.setIsDelete("0");
            measureData.setIsSave("1");

            tMeasureDataService.save(measureData);

            // 保存分析数据
            FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
            FMeasureAnalysisData lastAnalysis = tMeasureAnalysisDataService.getOne(
                    new QueryWrapper<FMeasureAnalysisData>()
                            .eq("task_id", context.task.getId())
                            .eq("sample_key", sampleId)
                            .orderByDesc("analysis_number")
                            .last("LIMIT 1")
            );

            analysisData.setAnalysisNumber(lastAnalysis != null ? lastAnalysis.getAnalysisNumber() + 1 : 1);

            FMeasureSample sample = tMeasureSampleService.getById(sampleId);
            analysisData.setAnalysisName(sample != null ? sample.getSampleName() + "(" + analysisData.getAnalysisNumber() + ")" : "未知样品");
            analysisData.setTaskId(context.task.getId());
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(measureData.getId());
            analysisData.setAnalysisParam(JSON.toJSONString(context.originalData));
            analysisData.setAnalysisResult(JSON.toJSONString(resultData));
            analysisData.setAnalysisType("5");

            List<FPeakValleyMarking> peaks = new ArrayList<>();
            List<FPeakValleyMarking> valleys = new ArrayList<>();

            for (int i = 0; i < peakX.size(); i++) {
                FPeakValleyMarking peak = new FPeakValleyMarking();
                peak.setX(peakX.get(i).toString());
                peak.setY(peakY.get(i));
                peak.setType("波峰");
                peaks.add(peak);
            }

            for (int i = 0; i < valleyX.size(); i++) {
                FPeakValleyMarking valley = new FPeakValleyMarking();
                valley.setX(valleyX.get(i).toString());
                valley.setY(valleyY.get(i));
                valley.setType("波谷");
                valleys.add(valley);
            }

            analysisData.setPeak(com.alibaba.fastjson2.JSON.toJSONString(peaks));
            analysisData.setGrain(com.alibaba.fastjson2.JSON.toJSONString(valleys));
            analysisData.setColor("#000000");
            analysisData.setIsDelete("0");
            analysisData.setIsSave("1");

            tMeasureAnalysisDataService.save(analysisData);

            // 保存分析图片
            FMeasureAnalysisImage image = new FMeasureAnalysisImage();
            image.setAnalysisId(analysisData.getId());
            tMeasureAnalysisImageService.save(image);

            // 更新任务状态
            updateTaskStatus(context.task, sampleId, context.deviceSerialNumber);

            log.info("测量数据保存完成: 样品={}, 点数={}", sampleId, context.xData.size());

        } catch (Exception e) {
            log.error("保存测量数据异常", e);
        }
    }

    private void updateTaskStatus(FMeasureTask task, String sampleId, String deviceSerialNumber) {
        try {
            List<FTaskSpecimen> specimens = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FTaskSpecimen specimen : specimens) {
                if (specimen.getSpecimenKey().equals(sampleId)) {
                    specimen.setIsTested("1");
                    break;
                }
            }

            task.setSampleKey(JSON.toJSONString(specimens));
            task.setDeviceSerialNumber(deviceSerialNumber);
            tMeasureTaskService.updateById(task);

        } catch (Exception e) {
            log.error("更新任务状态异常", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        String payload = message.getPayload();

        log.info("收到时间扫描消息: {}", payload);

        if ("close".equals(payload)) {
            handleStopRequest(sessionId);
        }
    }

    /**
     * 消息发送完成后再关闭连接
     */
    private void handleStopRequest(String sessionId) {
        log.info("处理时间扫描停止请求: {}", sessionId);

        TimeScanContext context = contexts.get(sessionId);
        WebSocketSession session = sessions.get(sessionId);

        if (context == null || session == null) {
            log.warn("停止请求处理失败: 上下文或会话不存在");
            return;
        }

        // 使用双重检查防止重复处理
        AtomicBoolean stopProcessing = stopProcessingMap.get(sessionId);
        if (stopProcessing == null || !stopProcessing.compareAndSet(false, true)) {
            log.info("停止请求已在处理中，忽略重复请求: {}", sessionId);
            return;
        }

        try {
            // 1. 先停止数据采集，但保持上下文可用
            context.stopAllTasks();

            // 3. 设置结束时间
            Constants.endTime = LocalDateTime.now();

            // 4. 如果有数据需要保存，先同步保存数据
            if (!context.xData.isEmpty() && context.currentSampleId != null) {
                log.info("停止时保存已采集数据, 点数: {}", context.xData.size());

                // 同步保存数据
                saveMeasurementDataSync(session, context, context.currentSampleId);

                // 等待数据保存完成
                log.info("等待数据保存完成...");
                boolean saved = context.waitForDataSave(30, TimeUnit.SECONDS);
                if (saved) {
                    log.info("数据保存完成");
                } else {
                    log.warn("数据保存超时，继续处理停止流程");
                }
            } else {
                // 如果没有数据，直接标记保存完成
                log.info("无数据需要保存");
                context.markDataSaved();
            }

            // 5. 关键改进：在关闭连接前确保停止成功消息发送完成
            sendStopSuccessWithRetry(session, sessionId);

            // 6. 释放设备资源
            if (context.deviceSerialNumber != null) {
                try {
                    deviceInfoService.releaseDevice(context.deviceSerialNumber);
                    log.info("释放时间扫描设备: {}", context.deviceSerialNumber);
                } catch (Exception e) {
                    log.error("释放设备异常", e);
                }
            }

            // 7. 延迟一小段时间确保消息发送完成，然后清理
            ScheduledExecutorService delayScheduler = Executors.newSingleThreadScheduledExecutor();
            delayScheduler.schedule(() -> {
                try {
                    log.info("延迟清理开始，会话: {}", sessionId);
                    cleanupAfterStop(sessionId);
                } finally {
                    delayScheduler.shutdown();
                }
            }, 300, TimeUnit.MILLISECONDS); // 增加到300ms确保消息发送

        } catch (Exception e) {
            log.error("处理停止请求异常", e);
            // 异常情况下也要确保发送停止成功并清理
            sendStopSuccessWithRetry(session, sessionId);
            cleanupAfterStop(sessionId);
        } finally {
            // 重置处理状态，允许后续清理操作
            if (stopProcessing != null) {
                stopProcessing.set(false);
            }
        }
    }


    /**
     * 停止后的清理工作 - 不包含释放设备和关闭WebSocket
     */
    private void cleanupAfterStop(String sessionId) {
        log.info("开始停止后清理: {}", sessionId);

        TimeScanContext context = contexts.get(sessionId);
        if (context != null) {
            // 确保任务已停止
            context.stopAllTasks();

            // 清理上下文数据
            context.clearData();

            // 从上下文中移除
            contexts.remove(sessionId);
            log.info("时间扫描上下文已移除: {}", sessionId);
        }

        // 关闭WebSocket会话
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null && session.isOpen()) {
            try {
                // 在关闭前再次检查是否有未发送的消息
                try {
                    Thread.sleep(100); // 额外延迟确保所有操作完成
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                session.close();
                log.info("WebSocket连接已关闭: {}", sessionId);
            } catch (IOException e) {
                log.debug("关闭WebSocket会话异常（可能已关闭）: {}", sessionId);
            }
        }

        // 清理状态映射
        connectionClosingMap.remove(sessionId);
        stopProcessingMap.remove(sessionId);

        clearGlobalData();
        log.info("停止后清理完成: {}", sessionId);
    }


    /**
     * 带重试的停止成功消息发送
     */
    private void sendStopSuccessWithRetry(WebSocketSession session, String sessionId) {
        if (session == null || !session.isOpen()) {
            log.warn("会话已关闭，无法发送停止成功消息: {}", sessionId);
            return;
        }

        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                // 检查连接状态
                if (!session.isOpen()) {
                    log.warn("会话在重试 {} 时已关闭: {}", i, sessionId);
                    break;
                }

                // 发送停止成功消息
                sendMessageSafe(session, "停止成功");
                log.info("停止成功消息发送完成 (尝试 {}): {}", i + 1, sessionId);

                // 短暂延迟确保消息发送
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // 如果发送成功，退出重试循环
                break;

            } catch (Exception e) {
                log.warn("发送停止成功消息失败 (尝试 {}): {}", i + 1, sessionId, e);
                if (i == maxRetries - 1) {
                    log.error("发送停止成功消息最终失败: {}", sessionId);
                }

                // 短暂延迟后重试
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        log.info("时间扫描连接关闭: {}, 原因: {}", sessionId, status);

        // 标记连接已关闭
        TimeScanContext context = contexts.get(sessionId);
        if (context != null) {
            context.markConnectionClosed();
        }

        // 标记连接正在关闭
        AtomicBoolean connectionClosing = connectionClosingMap.get(sessionId);
        if (connectionClosing != null) {
            connectionClosing.set(true);
        }

        // 连接关闭时也需要确保数据保存和资源释放
        handleConnectionClosed(sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = session.getId();

        // 如果是连接已关闭的异常，不记录为错误
        if (exception instanceof IOException &&
                (exception.getMessage() == null || exception.getMessage().contains("Broken pipe") ||
                        exception.getMessage().contains("Connection reset"))) {
            log.info("时间扫描连接传输错误（正常关闭）: {}", sessionId);
            return;
        }

        log.error("时间扫描传输错误: {}", sessionId, exception);

        // 传输错误时也需要确保数据保存和资源释放
        handleConnectionClosed(sessionId);
    }



    /**
     * 处理连接关闭情况 - 统一处理数据保存和资源释放
     */
    private void handleConnectionClosed(String sessionId) {
        TimeScanContext context = contexts.get(sessionId);
        WebSocketSession session = sessions.get(sessionId);

        if (context == null) {
            // 如果没有上下文，直接清理
            safeCleanup(sessionId, false);
            return;
        }

        // 检查是否已经在处理停止或连接已关闭
        if (context.cleanupInProgress.get() || context.isConnectionClosed()) {
            log.info("连接关闭时检测到清理已在处理中或连接已关闭，忽略: {}", sessionId);
            return;
        }

        context.cleanupInProgress.set(true);

        try {
            // 先停止任务
            context.stopAllTasks();

            // 设置结束时间
            Constants.endTime = LocalDateTime.now();

            // 如果有数据需要保存，尝试保存
            if (!context.xData.isEmpty() && context.currentSampleId != null) {
                log.info("连接关闭时保存已采集数据, 点数: {}", context.xData.size());
                saveMeasurementDataSync(session, context, context.currentSampleId);
            } else {
                context.markDataSaved();
            }

            // 等待数据保存完成（较短超时时间）
            log.info("等待数据保存完成...");
            boolean saved = context.waitForDataSave(10, TimeUnit.SECONDS);
            if (saved) {
                log.info("数据保存完成");
            } else {
                log.warn("数据保存超时");
            }

        } catch (Exception e) {
            log.error("处理连接关闭异常", e);
        } finally {
            // 最终确保资源清理
            safeCleanup(sessionId, true);
        }
    }

    /**
     * 安全清理 - 确保数据保存完成后再关闭WebSocket
     */
    private void safeCleanup(String sessionId, boolean releaseDevice) {
        log.info("开始安全清理会话: {}", sessionId);

        // 标记连接正在关闭
        AtomicBoolean connectionClosing = connectionClosingMap.get(sessionId);
        if (connectionClosing != null) {
            connectionClosing.set(true);
        }

        TimeScanContext context = contexts.get(sessionId);
        if (context != null) {
            // 确保任务已停止
            context.stopAllTasks();

            // 如果数据还未保存，等待一小段时间
            if (!context.dataSaved.get()) {
                log.info("等待最终数据保存...");
                boolean saved = context.waitForDataSave(5, TimeUnit.SECONDS);
                if (saved) {
                    log.info("数据保存完成");
                } else {
                    log.warn("数据保存超时，强制继续清理");
                }
            }

            // 释放设备资源
            if (releaseDevice && context.deviceSerialNumber != null) {
                try {
                    deviceInfoService.releaseDevice(context.deviceSerialNumber);
                    log.info("释放时间扫描设备: {}", context.deviceSerialNumber);
                } catch (Exception e) {
                    log.error("释放设备异常", e);
                }
            }

            // 从上下文中移除
            contexts.remove(sessionId);
            log.info("时间扫描上下文已移除: {}", sessionId);
        }

        // 关闭WebSocket会话
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null && session.isOpen()) {
            try {
                // 在关闭前再次检查是否有未发送的消息
                try {
                    Thread.sleep(100); // 额外延迟确保所有操作完成
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                session.close();
                log.info("WebSocket连接已关闭: {}", sessionId);
            } catch (IOException e) {
                log.debug("关闭WebSocket会话异常（可能已关闭）: {}", sessionId);
            }
        }

        // 清理状态映射
        connectionClosingMap.remove(sessionId);
        stopProcessingMap.remove(sessionId);

        clearGlobalData();
        log.info("时间扫描会话安全清理完成: {}", sessionId);
    }



    private void sendMessageSafe(WebSocketSession session, String message) {
        if (session == null) {
            return;
        }

        String sessionId = session.getId();

        // 检查连接是否正在关闭
        AtomicBoolean connectionClosing = connectionClosingMap.get(sessionId);
        if (connectionClosing != null && connectionClosing.get()) {
            log.debug("连接正在关闭，跳过消息发送: {}", sessionId);
            return;
        }

        if (!session.isOpen()) {
            log.debug("会话已关闭，无法发送消息: {}", sessionId);
            return;
        }

        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        } catch (IOException e) {
            // 如果是连接已关闭的异常，不记录为错误
            if (e.getMessage() != null &&
                    (e.getMessage().contains("Broken pipe") ||
                            e.getMessage().contains("Connection reset") ||
                            e.getMessage().contains("已断开的连接"))) {
                log.debug("发送消息时连接已关闭: {}", sessionId);
            } else {
                log.warn("发送消息失败: {}", sessionId, e);
            }
        } catch (Exception e) {
            log.warn("发送消息异常: {}", sessionId, e);
        }
    }

    private String getQueryParam(String query, String key) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) return pair[1];
        }
        return null;
    }

    private void clearGlobalData() {
        Constants.startTime = null;
        Constants.endTime = null;
    }
}