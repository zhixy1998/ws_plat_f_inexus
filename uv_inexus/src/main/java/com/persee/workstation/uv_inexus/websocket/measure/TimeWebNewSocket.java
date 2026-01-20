/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.measure;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.SamplePoolConfigPO;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketOperationEnum;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketResponseVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketSendUtil;
import com.persee.workstation.uv_inexus.websocket.measure.param.TimeWebNewSocketParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TimeWebNewSocket {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, TimeScanContext> contexts = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 添加连接状态管理
    private static final Map<String, AtomicBoolean> connectionClosingMap = new ConcurrentHashMap<>();
    private static final Map<String, AtomicBoolean> stopProcessingMap = new ConcurrentHashMap<>();

    @Autowired private TimeSweepService timeSweepConciseService;
    @Autowired private TMeasureTaskService tMeasureTaskService;
    @Autowired private TMeasureMethodService tMeasureMethodService;
    @Autowired private DeviceInfoService deviceInfoService;
    @Autowired private TMeasureDataService tMeasureDataService;
    @Autowired private TMeasureAnalysisDataService tMeasureAnalysisDataService;
    @Autowired private TMeasureAnalysisImageService tMeasureAnalysisImageService;
    @Autowired private SamplePoolConfigService fSamplePoolConfigService;
    @Autowired private TMeasureSampleService tMeasureSampleService;
    @Autowired private SystemAlgorithmConfService systemAlgorithmConfService;
    @Autowired private DevMessageUtil devMessageUtil;
    @Autowired private RestTemplate restTemplate;
    @Autowired private FormulaUtil formulaUtil;
    @Autowired private PythonPathConstant pythonPathConstant;
    @Autowired
    WebSocketSendUtil webSocketSendUtil;
    @Autowired
    private DevOperationUtilService devOperationUtilService;
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
        TMeasureTask task;

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

    /**
     * 初始化并开始测量
     * @param param
     */
    public void measureTask(TimeWebNewSocketParam param) {
        log.info("时间扫描连接建立");
        String taskId = param.getTaskId();
        connectionClosingMap.put(taskId, new AtomicBoolean(false));
        stopProcessingMap.put(taskId, new AtomicBoolean(false));

        TimeScanContext context = new TimeScanContext();
        contexts.put(taskId, context);

        executor.execute(() -> {
            try {
                initializeAndStart(param, context);
            } catch (Exception e) {
                log.error("时间扫描初始化失败: {}", taskId, e);
                safeCleanup(taskId, true);
            }
        });
    }


    /**
     * 初始化并开始测量
     */
    private void initializeAndStart(TimeWebNewSocketParam param, TimeScanContext context) {
        String sessionId = param.getTaskId();

        try {
            String taskId = param.getTaskId();
            String deviceSerialNumber = param.getDeviceSerialNumber();
            String updateBy = param.getUpdateBy();
            Integer digit = param.getDigit();

            if (taskId == null || deviceSerialNumber == null || updateBy == null) {
                safeCleanup(sessionId, true);
                return;
            }

            context.deviceSerialNumber = deviceSerialNumber;
            context.digit = digit;
            context.ip = deviceInfoService.getIp(deviceSerialNumber);

            TMeasureTask task = tMeasureTaskService.getById(taskId);
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

            Result paramResult = timeSweepConciseService.setMethodParameters(task.getMethodKey(), deviceSerialNumber);
            if (paramResult.getCode() != 200) {
                safeCleanup(sessionId, true);
                return;
            }

            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method == null) {
                safeCleanup(sessionId, true);
                return;
            }

            TimeSweepParamter timeParam = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);
            startTimeMeasurement(param, context, timeParam, updateBy);

        } catch (Exception e) {
            log.error("时间扫描初始化异常: {}", sessionId, e);
            safeCleanup(sessionId, true);
        }
    }

    private void startTimeMeasurement(TimeWebNewSocketParam session, TimeScanContext context,
                                      TimeSweepParamter param, String updateBy) {
        String sessionId = session.getTaskId();

        try {
            List<TaskSpecimen> specimenList = JSON.parseArray(context.task.getSampleKey(), TaskSpecimen.class);
            List<TaskSpecimen> validSamples = new ArrayList<>();

            for (TaskSpecimen specimen : specimenList) {
                if ("0".equals(specimen.getDeleteType()) && "0".equals(specimen.getIsTested())) {
                    validSamples.add(specimen);
                }
            }

            if (validSamples.isEmpty()) {
                safeCleanup(sessionId, true);
                return;
            }

            // 检查是否启用自动换池
            QueryWrapper<SamplePoolConfigPO> wrapper = new QueryWrapper<>();
            wrapper.eq("device_serial_number", context.deviceSerialNumber);
            SamplePoolConfigPO config = fSamplePoolConfigService.getOne(wrapper);

            if (config == null || !config.getIsAutoChangePool()) {
                // 未启用自动换池，只测量第一个样品
                log.info("未启用自动换池，只测量第一个样品");
                if (!validSamples.isEmpty()) {
                    executeSampleMeasurement(session, context, param, validSamples.get(0), updateBy);
                }
            } else {
                // 启用自动换池，按顺序测量所有样品
                log.info("启用自动换池，按顺序测量所有样品，数量: {}", validSamples.size());
                for (TaskSpecimen sample : validSamples) {
                    if (!context.running.get()) {
                        break;
                    }
                    executeSampleMeasurement(session, context, param, sample, updateBy);
                }
            }

            // 只有在正常完成（没有被停止）的情况下才发送"扫描成功"
            if (context.running.get()) {
                JSONObject object = new JSONObject();
                object.put("success", "扫描成功");
                sendMessage(session.getDeviceSerialNumber(), object);
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
     * 执行单个样品测量
     * @param session
     * @param context
     * @param param
     * @param sample
     */
    private void executeSampleMeasurement(TimeWebNewSocketParam session, TimeScanContext context,
                                          TimeSweepParamter param, TaskSpecimen sample, String updateBy) {
        context.clearData();
        context.currentSampleId = sample.getSpecimenKey();

        // 首先检查是否需要设置样品池位置
        setupSamplePoolIfNeeded(session, context, sample, updateBy);

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
                saveMeasurementDataSync(context, sample.getSpecimenKey());
                completionLatch.countDown();
                return;
            }

            try {
                executeSingleMeasurement(session.getDeviceSerialNumber(), context, param, baselineData, currentIndex, interval);
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
                    saveMeasurementDataSync(context, sample.getSpecimenKey());
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
                saveMeasurementDataSync(context, sample.getSpecimenKey());
            }
            context.running.set(false);
        } finally {
            // 确保调度器被关闭 - 在数据保存完成后关闭
            if (!scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
        }
    }


    /**
     * 设置样品池位置
     */
    private void setupSamplePoolIfNeeded(TimeWebNewSocketParam session,TimeScanContext context,
                                         TaskSpecimen sample, String updateBy) {
        try {
            QueryWrapper<SamplePoolConfigPO> wrapper = new QueryWrapper<>();
            wrapper.eq("device_serial_number", context.deviceSerialNumber);
            SamplePoolConfigPO config = fSamplePoolConfigService.getOne(wrapper);

            // 检查是否启用自动换池
            if (config == null || !config.getIsAutoChangePool()) {
                log.info("未启用自动换池，不设置样品池位置");
                return;
            }

            // 检查样品池状态
            boolean poolStatus = false;
            if (config.getPoolInfo() != null && !config.getPoolInfo().isEmpty()) {
                List<FSamplePoolStatus> poolStatuses = JSON.parseArray(config.getPoolInfo(), FSamplePoolStatus.class);
                for (FSamplePoolStatus status : poolStatuses) {
                    if (status.getPoolStatus()) {
                        poolStatus = true;
                        break;
                    }
                }
            }

            if (!poolStatus) {
                log.info("样品池状态为关闭，不设置样品池位置");
                return;
            }

            // 根据样品ID获取样品信息
            TMeasureSample sampleInfo = tMeasureSampleService.getById(sample.getSpecimenKey());
            if (sampleInfo == null || sampleInfo.getSampleLocation() == null || sampleInfo.getSampleLocation().isEmpty()) {
                log.warn("样品位置信息为空，无法设置样品池位置");
                return;
            }

            // 设置样品池位置
            R<String> setResult = devMessageUtil.setCurrentCellPos(Integer.valueOf(sampleInfo.getSampleLocation()), context.ip);
            if (setResult.getSuccess()) {
                // 更新当前样品池位置
                fSamplePoolConfigService.updateCurrentPoolNum(context.deviceSerialNumber,
                        Integer.parseInt(sampleInfo.getSampleLocation()), updateBy);

                SetParam setParam = new SetParam();
                setParam.setCellPos(Integer.parseInt(sampleInfo.getSampleLocation()));
                timeSweepConciseService.setInstrumentParameters(context.deviceSerialNumber, setParam);

                // 发送样品池位置信息到前端
                JSONObject poolMsg = new JSONObject();
                poolMsg.put("currentPoolNum", sampleInfo.getSampleLocation());
                sendMessage(session.getDeviceSerialNumber(), poolMsg);

                log.info("成功设置样品池位置: {}", sampleInfo.getSampleLocation());
            } else {
                log.warn("设置样品池位置失败: {}", sampleInfo.getSampleLocation());
            }

        } catch (Exception e) {
            log.error("设置样品池位置异常: {}", e.getMessage());
        }
    }

    /**
     * 执行单次测量
     */
    private void executeSingleMeasurement(String deviceSerialNumber, TimeScanContext context,
                                          TimeSweepParamter param, ChangeParam baselineData,
                                          int currentIndex, BigDecimal interval) {
        if (!context.running.get()) {
            return;
        }

        // 使用BigDecimal精确计算时间点，避免浮点数精度问题
        BigDecimal xValue = interval.multiply(BigDecimal.valueOf(currentIndex))
                .setScale(1, RoundingMode.HALF_UP);

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Measure.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        R<OperationResultEntity> r = devOperationUtilService.operation(inputParam);
        OperationResultEntity data = r.getData();
        List<ChangeParam> resultList = data.getOriginalMeasureResult();
        if (resultList.isEmpty() || resultList == null) {
            log.error("获取测量数据失败");
            return;
        }
        ChangeParam changeParam = resultList.get(0);
        Integer luminosity = Integer.valueOf(param.getLuminosity());
        SpectrumPoint point = new SpectrumPoint(luminosity);
        point.setZeroRsd(baselineData);
        point.setMeasureRsd(changeParam);
        SpectrumCalcResult calculate = SpectrumResultCalculator.calculate(luminosity, point, SpectrumSourceType.SINGLE, point);

        context.originalData.add(changeParam);
        calculateAndPushData(deviceSerialNumber, context, param,calculate, xValue);
    }

    private void calculateAndPushData(String deviceSerialNumber, TimeScanContext context,
                                      TimeSweepParamter param, SpectrumCalcResult calculate, BigDecimal xValue) {
        try {
            BigDecimal yValue;
            String yType;
            context.xData.add(xValue);
            switch (param.getLuminosity()){
                case "1":{
                    yValue = new BigDecimal(calculate.getAbs());
                    yType = "Abs";
                    break;
                }
                case "2":{
                    yValue = new BigDecimal(calculate.getTPercent());
                    yType = "T%";
                    break;
                }
                case "3":{
                    yValue = new BigDecimal(calculate.getEs());
                    yType = "Es";
                    break;
                }
                case "4":{
                    yValue = new BigDecimal(calculate.getEr());
                    yType = "Er";
                    break;
                }
                default:{
                    sendError(deviceSerialNumber, "无效的光度参数");
                    return;
                }

            }
            context.yData.add(yValue);

            pushDataPoint(deviceSerialNumber, xValue, yValue, context.currentSampleId, param, context.digit, yType);

        } catch (Exception e) {
            log.error("计算数据异常", e);
        }
    }

    private void pushDataPoint(String deviceSerialNumber, BigDecimal xValue, BigDecimal yValue,
                               String sampleId, TimeSweepParamter param, Integer digit, String yType) {
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

        log.info("推送数据点: {}", payload);

        sendMessage(deviceSerialNumber, payload);
    }

    /**
     * 同步保存测量数据
     */
    private void saveMeasurementDataSync(TimeScanContext context, String sampleId) {
        log.info("开始同步保存测量数据, 样品: {}, 数据点数: {}", sampleId, context.xData.size());

        try {
            // 在实际保存前再次检查运行状态
            if (!context.running.get()) {
                log.info("任务已停止，但仍保存已收集的数据");
            }

            saveMeasurementData(context, sampleId);
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
    private void saveMeasurementData(TimeScanContext context, String sampleId) {
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
            TMeasureData measureData = new TMeasureData();

            TMeasureData lastData = tMeasureDataService.getOne(
                    new QueryWrapper<TMeasureData>()
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
            TMeasureAnalysisData analysisData = new TMeasureAnalysisData();
            TMeasureAnalysisData lastAnalysis = tMeasureAnalysisDataService.getOne(
                    new QueryWrapper<TMeasureAnalysisData>()
                            .eq("task_id", context.task.getId())
                            .eq("sample_key", sampleId)
                            .orderByDesc("analysis_number")
                            .last("LIMIT 1")
            );

            analysisData.setAnalysisNumber(lastAnalysis != null ? lastAnalysis.getAnalysisNumber() + 1 : 1);

            TMeasureSample sample = tMeasureSampleService.getById(sampleId);
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
            TMeasureAnalysisImage image = new TMeasureAnalysisImage();
            image.setAnalysisId(analysisData.getId());
            tMeasureAnalysisImageService.save(image);

            // 更新任务状态
            updateTaskStatus(context.task, sampleId, context.deviceSerialNumber);

            log.info("测量数据保存完成: 样品={}, 点数={}", sampleId, context.xData.size());

        } catch (Exception e) {
            log.error("保存测量数据异常", e);
        }
    }

    private void updateTaskStatus(TMeasureTask task, String sampleId, String deviceSerialNumber) {
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




    private void clearGlobalData() {
        Constants.startTime = null;
        Constants.endTime = null;
    }

    private void sendMessage(String deviceSerialNumber, JSONObject object) {

        WebSocketResponseVO vo = new WebSocketResponseVO();
        vo.setOperation(WebSocketOperationEnum.TimeMeasure.getOperation());
        vo.setData(object);

        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }

    private void sendError(String deviceSerialNumber, String message) {
        webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.TimeMeasure.getOperation(), message);
    }
}