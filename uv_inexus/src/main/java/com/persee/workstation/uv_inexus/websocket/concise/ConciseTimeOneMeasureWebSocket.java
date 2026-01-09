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
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
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

@Component
public class ConciseTimeOneMeasureWebSocket extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ConciseTimeOneMeasureWebSocket.class);

    // 会话管理
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, TimeOneMeasureContext> contexts = new ConcurrentHashMap<>();
    private static final Map<String, Object> sessionSendLocks = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Autowired private TimeSweepConciseService timeSweepConciseService;
    @Autowired private FMeasureTaskService tMeasureTaskService;
    @Autowired private FMeasureMethodService tMeasureMethodService;
    @Autowired private DeviceInfoService deviceInfoService;
    @Autowired private FMeasureDataService tMeasureDataService;
    @Autowired private FMeasureAnalysisDataService tMeasureAnalysisDataService;
    @Autowired private FMeasureAnalysisImageService tMeasureAnalysisImageService;
    @Autowired private SamplePoolConfigService fSamplePoolConfigService;
    @Autowired private SystemAlgorithmConfService systemAlgorithmConfService;
    @Autowired private FMeasureSampleService tMeasureSampleService;
    @Autowired private DevMessageUtil devMessageUtil;
    @Autowired private RestTemplate restTemplate;
    @Autowired private FormulaUtil formulaUtil;
    // 注入PythonPathConstant
    @Autowired
    private PythonPathConstant pythonPathConstant;

    /**
     * 单次时间测量上下文
     */
    private static class TimeOneMeasureContext {
        // 状态控制
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicBoolean measurementStarted = new AtomicBoolean(false);
        final AtomicBoolean stopCommandSent = new AtomicBoolean(false);
        final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);
        final AtomicBoolean sessionClosing = new AtomicBoolean(false);
        final AtomicBoolean dataSaved = new AtomicBoolean(false);
        final CountDownLatch saveLatch = new CountDownLatch(1);

        // 数据
        final List<BigDecimal> xData = new ArrayList<>();
        final List<BigDecimal> yData = new ArrayList<>();
        final List<ChangeParam> originalData = new ArrayList<>();

        // 配置信息
        String deviceSerialNumber;
        String ip;
        Integer digit;
        String currentSampleId;
        FMeasureTask task;
        FTimeSweepParamter param;

        // 清空数据
        void clearData() {
            xData.clear();
            yData.clear();
            originalData.clear();
        }

        // 停止所有任务
        void stopAllTasks() {
            running.set(false);
            stopCommandSent.set(true);
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
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("单次时间测量连接建立: {}", sessionId);

        sessions.put(sessionId, session);

        // 初始化会话状态
        TimeOneMeasureContext context = new TimeOneMeasureContext();
        contexts.put(sessionId, context);

        // 初始化发送锁
        sessionSendLocks.put(sessionId, new Object());

        executor.execute(() -> {
            try {
                initializeAndStart(session, context);
            } catch (Exception e) {
                log.error("单次时间测量初始化失败: {}", sessionId, e);
                safeCleanup(sessionId, true);
            }
        });
    }

    /**
     * 初始化并启动单次时间测量
     * @param session
     * @param context
     */
    private void initializeAndStart(WebSocketSession session, TimeOneMeasureContext context) {
        String sessionId = session.getId();

        try {
            String query = Objects.requireNonNull(session.getUri()).getQuery();
            String taskId = getQueryParam(query, "taskId");
            String sampleId = getQueryParam(query, "sampleId");
            String deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");
            String updateBy = getQueryParam(query, "updateBy");
            Integer digit = Integer.parseInt(getQueryParam(query, "digit"));

            if (taskId == null || sampleId == null || deviceSerialNumber == null || updateBy == null) {
                safeCleanup(sessionId, true);
                return;
            }

            // 设置上下文
            context.deviceSerialNumber = deviceSerialNumber;
            context.digit = digit;
            context.ip = deviceInfoService.getIp(deviceSerialNumber);
            context.currentSampleId = sampleId;

            FMeasureTask task = tMeasureTaskService.getById(taskId);
            if (task == null) {
                safeCleanup(sessionId, true);
                return;
            }
            context.task = task;

            // 占用设备
            R<Boolean> useResult = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);
            if (!useResult.getSuccess()) {
                safeCleanup(sessionId, true);
                return;
            }

            // 设置参数
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
            context.param = timeParam;

            // 配置样品池
            if (!setupSamplePool(session, context, updateBy)) {
                safeCleanup(sessionId, true);
                return;
            }

            // 开始测量
            startSingleMeasurement(session, context);

        } catch (Exception e) {
            log.error("单次时间测量初始化异常: {}", sessionId, e);
            safeCleanup(sessionId, true);
        }
    }

    private boolean setupSamplePool(WebSocketSession session, TimeOneMeasureContext context, String updateBy) {
        try {
            QueryWrapper<SamplePoolConfigPO> wrapper = new QueryWrapper<>();
            wrapper.eq("device_serial_number", context.deviceSerialNumber);
            SamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);

            if (configPO == null || !configPO.getIsAutoChangePool()) {
                return true;
            }

            FMeasureSample sample = tMeasureSampleService.getById(context.currentSampleId);
            if (sample == null) {
                return false;
            }

            boolean poolStatus = JSON.parseArray(configPO.getPoolInfo(), FSamplePoolStatus.class)
                    .stream()
                    .anyMatch(FSamplePoolStatus::getPoolStatus);

            if (poolStatus) {
                R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), context.ip);
                if (!stringR.getSuccess()) {
                    return false;
                }
                fSamplePoolConfigService.updateCurrentPoolNum(context.deviceSerialNumber,
                        Integer.parseInt(sample.getSampleLocation()), updateBy);

                JSONObject object = new JSONObject();
                object.put("currentPoolNum", sample.getSampleLocation());
                sendMessageSafe(session, object.toJSONString());
            }
            return true;

        } catch (Exception e) {
            log.error("设置样品池失败", e);
            return false;
        }
    }

    private void startSingleMeasurement(WebSocketSession session, TimeOneMeasureContext context) {
        String sessionId = session.getId();

        try {
            // 使用BigDecimal避免浮点数精度问题
            BigDecimal interval = new BigDecimal(context.param.getIntervalTime());
            int totalTasks = Integer.parseInt(context.param.getTotalNumber());

            final ChangeParam baselineData = (context.task.getZeroParam() != null && !context.task.getZeroParam().isEmpty())
                    ? JSON.parseObject(context.task.getZeroParam(), ChangeParam.class)
                    : null;

            String luminosity = context.param.getLuminosity();
            String waveLength = context.param.getWaveLength() + "nm";
            String yType = getYType(luminosity);

            // 设置测量开始状态
            context.measurementStarted.set(true);
            Constants.startTime = LocalDateTime.now();

            log.info("开始单次时间测量: 样品={}, 间隔={}s, 总数={}", context.currentSampleId, interval, totalTasks);

            for (int i = 0; i < totalTasks; i++) {
                // 检查停止条件
                if (!context.running.get() || context.sessionClosing.get() || !session.isOpen()) {
                    log.info("检测到停止信号，中断测量");
                    break;
                }

                long taskStart = System.currentTimeMillis();

                try {
                    // 使用BigDecimal精确计算时间点，避免浮点数精度问题
                    BigDecimal xValue = interval.multiply(BigDecimal.valueOf(i))
                            .setScale(1, RoundingMode.HALF_UP);

                    Command command = new Command(SetParamEnum.Measure.getComId(), new Object[]{});
                    ChangeParam changeParam = devMessageUtil.timeSweepGetMeasureData(command, context.ip);

                    if (changeParam == null) {
                        log.warn("测量数据为空，跳过本次测量");
                        continue;
                    }

                    // 计算波长
                    Double v = formulaUtil.calculateWl(Double.parseDouble(changeParam.getWl()), context.deviceSerialNumber);
                    changeParam.setWl(String.valueOf(v));
                    context.originalData.add(changeParam);

                    // 计算Y值
                    BigDecimal yValue = calculateYValue(baselineData, changeParam, luminosity);
                    if (yValue != null) {
                        context.xData.add(xValue);
                        context.yData.add(yValue);
                        sendDataPoint(session, xValue, yValue, context.currentSampleId, waveLength, yType, context.digit);
                    }

                    // 控制测量间隔，使用精确的时间控制
                    long elapsed = System.currentTimeMillis() - taskStart;
                    long sleepTime = interval.multiply(BigDecimal.valueOf(1000)).longValue() - elapsed;
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }

                } catch (Exception e) {
                    if (!context.running.get()) {
                        log.info("测量任务被正常中断");
                        break;
                    }
                    log.error("第 {} 次测量异常", i, e);
                }
            }

            // 测量完成，设置结束时间
            Constants.endTime = LocalDateTime.now();
            context.measurementStarted.set(false);

            // 保存数据（同步）
            if (!context.xData.isEmpty()) {
                log.info("测量完成，开始保存数据，点数: {}", context.xData.size());
                saveMeasurementDataSync(session, context);
            } else {
                context.markDataSaved();
            }

            // 只有在正常完成（没有被停止）的情况下才发送"扫描成功"
            if (context.running.get()) {
                sendMessageSafe(session, "扫描成功");

                // 正常完成 - 等待数据保存后再清理
                log.info("测量任务完成，等待数据保存...");
                if (context.waitForDataSave(30, TimeUnit.SECONDS)) {
                    log.info("数据保存完成，发送完成消息");
                    JSONObject finishMsg = new JSONObject();
                    finishMsg.put("result", "finish");
                    sendMessageAndSafeClose(session, finishMsg.toJSONString());
                } else {
                    log.warn("数据保存超时，强制关闭");
                    safeCleanup(sessionId, true);
                }
            }

        } catch (Exception e) {
            log.error("单次时间测量执行异常: {}", sessionId, e);
            safeCleanup(sessionId, true);
        }
    }

    private BigDecimal calculateYValue(ChangeParam baselineData, ChangeParam changeParam, String luminosity) {
        try {
            switch (luminosity) {
                case "1": // Abs
                    String absValue = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
                    return new BigDecimal(DataCountUtil.changeTPercentOrAbs2(null, absValue));

                case "2": // T%
                    String tPercent = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
                    return new BigDecimal(tPercent);

                case "3": // Es
                    Double sData = Double.valueOf(changeParam.getS());
                    return BigDecimal.valueOf((sData / (4095 * 32)) * 100);

                case "4": // Er
                    Double rData = Double.valueOf(changeParam.getR());
                    return BigDecimal.valueOf((rData / (4095 * 32)) * 100);

                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("计算Y值失败", e);
            return null;
        }
    }

    private void sendDataPoint(WebSocketSession session, BigDecimal xValue, BigDecimal yValue,
                               String sampleId, String waveLength, String yType, Integer digit) {
        JSONObject payload = new JSONObject();
        JSONArray points = new JSONArray();
        JSONObject point = new JSONObject();

        point.put("x", xValue);

        Double decimalY = yValue.doubleValue();
        Double value = DataCountUtil.formatValue(digit, decimalY);
        String valueY = DataCountUtil.addZero(value, digit);

        point.put("y", decimalY);
        point.put("roundOffY", valueY);
        point.put("nm", waveLength);
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
    private void saveMeasurementDataSync(WebSocketSession session, TimeOneMeasureContext context) {
        log.info("开始同步保存测量数据, 样品: {}, 数据点数: {}", context.currentSampleId, context.xData.size());

        try {
            saveMeasurementData(session, context);
            context.markDataSaved();
            log.info("同步保存测量数据完成, 样品: {}", context.currentSampleId);
        } catch (Exception e) {
            log.error("同步保存测量数据异常", e);
            // 即使保存异常也要标记为已完成，避免死锁
            context.markDataSaved();
        }
    }

    /**
     * 保存测量数据
     */
    private void saveMeasurementData(WebSocketSession session, TimeOneMeasureContext context) {
        try {
            if (context.xData.isEmpty() || context.yData.isEmpty()) {
                log.warn("没有数据需要保存");
                return;
            }

            log.info("保存测量数据, 样品: {}, 数据点数: {}", context.currentSampleId, context.xData.size());

            // 寻峰寻谷分析
            SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> analysisBody = new HashMap<>();
            analysisBody.put("x_data", context.xData);
            analysisBody.put("y_data", context.yData);

            double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
            analysisBody.put("height", height);
            analysisBody.put("distance", confVO.getPeakInterval());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(analysisBody, headers);

            String peakUrl = pythonPathConstant.getPeakSearching();
            String valleyUrl = pythonPathConstant.getValleySearching();

            // 添加日志输出（调试用）
            log.info("调用寻峰接口: " + peakUrl);
            log.info("调用寻谷接口: " + valleyUrl);

            // 调用Python服务进行寻峰寻谷
            Map<String, Object> peakResult = restTemplate.postForEntity(peakUrl, request, Map.class).getBody();
            Map<String, Object> valleyResult = restTemplate.postForEntity(valleyUrl, request, Map.class).getBody();

            List<Double> peakX = (List<Double>) peakResult.get("peak_x");
            List<String> peakY = (List<String>) peakResult.get("peak_y");
            List<Double> valleyX = (List<Double>) valleyResult.get("valley_x");
            List<String> valleyY = (List<String>) valleyResult.get("valley_y");

            // 保存到数据库
            saveToDatabase(context, peakX, peakY, valleyX, valleyY);

            log.info("测量数据保存完成: 样品={}, 点数={}", context.currentSampleId, context.xData.size());

        } catch (Exception e) {
            log.error("保存测量数据异常", e);
        }
    }

    private void saveToDatabase(TimeOneMeasureContext context, List<Double> peakX, List<String> peakY,
                                List<Double> valleyX, List<String> valleyY) {
        try {
            // 构建峰谷标记
            List<FPeakValleyMarking> peaks = processPeakValleyMarkings(peakX, peakY, "波峰");
            List<FPeakValleyMarking> valleys = processPeakValleyMarkings(valleyX, valleyY, "波谷");

            // 构建结果数据
            List<String> resultData = new ArrayList<>();
            for (int i = 0; i < context.xData.size(); i++) {
                resultData.add(context.xData.get(i) + "," + context.yData.get(i));
            }

            // 查询或创建测量数据
            FMeasureData measureData = tMeasureDataService.getOne(
                    new QueryWrapper<FMeasureData>()
                            .eq("task_id", context.task.getId())
                            .eq("sample_key", context.currentSampleId)
                            .orderByDesc("serial_number")
                            .last("LIMIT 1")
            );

            if (measureData == null) {
                measureData = new FMeasureData();
                measureData.setSerialNumber(1);
            } else {
                measureData.setSerialNumber(measureData.getSerialNumber() + 1);
            }

            measureData.setTaskId(context.task.getId());
            measureData.setSampleKey(context.currentSampleId);
            measureData.setInstrumentData(JSON.toJSONString(context.originalData));
            measureData.setOriginalCalculationData(JSON.toJSONString(resultData));
            measureData.setMeasurementStartTime(Constants.startTime);
            measureData.setMeasurementEndTime(Constants.endTime);
            measureData.setIsDelete("0");
            measureData.setIsSave("1");

            tMeasureDataService.saveOrUpdate(measureData);

            // 创建分析数据
            FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
            FMeasureAnalysisData lastAnalysis = tMeasureAnalysisDataService.getOne(
                    new QueryWrapper<FMeasureAnalysisData>()
                            .eq("task_id", context.task.getId())
                            .eq("sample_key", context.currentSampleId)
                            .orderByDesc("analysis_number")
                            .last("LIMIT 1")
            );

            analysisData.setAnalysisNumber(lastAnalysis != null ? lastAnalysis.getAnalysisNumber() + 1 : 1);

            FMeasureSample sample = tMeasureSampleService.getById(context.currentSampleId);
            analysisData.setAnalysisName(sample != null ? sample.getSampleName() + "(" + analysisData.getAnalysisNumber() + ")" : "未知样品");
            analysisData.setTaskId(context.task.getId());
            analysisData.setSampleKey(context.currentSampleId);
            analysisData.setMeasureDataId(measureData.getId());
            analysisData.setAnalysisParam(JSON.toJSONString(context.originalData));
            analysisData.setAnalysisResult(JSON.toJSONString(resultData));
            analysisData.setAnalysisType("5");
            analysisData.setPeak(JSON.toJSONString(peaks));
            analysisData.setGrain(JSON.toJSONString(valleys));
            analysisData.setColor("#000000");
            analysisData.setIsDelete("0");
            analysisData.setIsSave("1");

            tMeasureAnalysisDataService.saveOrUpdate(analysisData);

            // 创建分析图片记录
            FMeasureAnalysisImage image = new FMeasureAnalysisImage();
            image.setAnalysisId(analysisData.getId());
            tMeasureAnalysisImageService.save(image);

            // 更新任务状态
            updateTaskStatus(context.task, context.currentSampleId, context.deviceSerialNumber);

        } catch (Exception e) {
            log.error("保存到数据库失败", e);
        }
    }

    private List<FPeakValleyMarking> processPeakValleyMarkings(List<Double> xData, List<String> yData, String type) {
        List<FPeakValleyMarking> markings = new ArrayList<>();
        if (xData != null && yData != null) {
            for (int i = 0; i < Math.min(xData.size(), yData.size()); i++) {
                FPeakValleyMarking marking = new FPeakValleyMarking();
                marking.setX(xData.get(i).toString());
                marking.setY(yData.get(i));
                marking.setType(type);
                markings.add(marking);
            }
        }
        return markings;
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

    private String getYType(String luminosity) {
        switch (luminosity) {
            case "1": return "Abs";
            case "2": return "T%";
            case "3": return "Es";
            case "4": return "Er";
            default: return "Unknown";
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        String payload = message.getPayload();

        log.info("收到单次时间测量消息: {} -> {}", sessionId, payload);

        if ("close".equals(payload)) {
            handleStopRequest(sessionId);
        }
    }

    /**
     * 处理停止请求 - 确保消息发送完成后再关闭连接
     */
    private void handleStopRequest(String sessionId) {
        log.info("处理单次时间测量停止请求: {}", sessionId);

        TimeOneMeasureContext context = contexts.get(sessionId);
        WebSocketSession session = sessions.get(sessionId);

        if (context == null || session == null) {
            log.warn("停止请求处理失败: 上下文或会话不存在");
            return;
        }

        try {
            // 1. 先停止数据采集，但保持上下文可用
            context.stopAllTasks();

            // 3. 设置结束时间
            Constants.endTime = LocalDateTime.now();

            // 4. 如果有数据需要保存，先同步保存数据
            if (!context.xData.isEmpty()) {
                log.info("停止时保存已采集数据, 点数: {}", context.xData.size());

                // 同步保存数据
                saveMeasurementDataSync(session, context);

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

            // 6. 延迟一小段时间确保消息发送完成，然后进行最终清理
            ScheduledExecutorService delayScheduler = Executors.newSingleThreadScheduledExecutor();
            delayScheduler.schedule(() -> {
                try {
                    log.info("开始最终清理，会话: {}", sessionId);

                    // 7. 释放设备资源（在这里释放设备）
                    if (context.deviceSerialNumber != null) {
                        try {
                            deviceInfoService.releaseDevice(context.deviceSerialNumber);
                            log.info("释放单次时间测量设备: {}", context.deviceSerialNumber);
                        } catch (Exception e) {
                            log.error("释放设备异常", e);
                        }
                    }

                    // 8. 清理上下文数据
                    if (context != null) {
                        context.stopAllTasks();
                        context.clearData();
                        contexts.remove(sessionId);
                        log.info("单次时间测量上下文已移除: {}", sessionId);
                    }

                    // 9. 关闭WebSocket会话（这是最后一步）
                    WebSocketSession sessionToClose = sessions.remove(sessionId);
                    if (sessionToClose != null && sessionToClose.isOpen()) {
                        try {
                            // 在关闭前再次检查是否有未发送的消息
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            sessionToClose.close();
                            log.info("WebSocket连接已关闭: {}", sessionId);
                        } catch (IOException e) {
                            log.debug("关闭WebSocket会话异常（可能已关闭）: {}", sessionId);
                        }
                    }

                    // 10. 清理发送锁
                    sessionSendLocks.remove(sessionId);

                    clearGlobalData();
                    log.info("单次时间测量停止处理完成: {}", sessionId);

                } finally {
                    delayScheduler.shutdown();
                }
            }, 300, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            log.error("处理停止请求异常", e);
            // 异常情况下也要确保发送停止成功并清理
            sendStopSuccessWithRetry(session, sessionId);
            // 异常情况下的清理
            cleanupInException(sessionId);
        }
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

    /**
     * 异常情况下的清理
     */
    private void cleanupInException(String sessionId) {
        try {
            TimeOneMeasureContext context = contexts.get(sessionId);

            // 释放设备资源
            if (context != null && context.deviceSerialNumber != null) {
                try {
                    deviceInfoService.releaseDevice(context.deviceSerialNumber);
                    log.info("异常情况下释放设备: {}", context.deviceSerialNumber);
                } catch (Exception e) {
                    log.error("异常情况下释放设备异常", e);
                }
            }

            // 清理上下文
            if (context != null) {
                contexts.remove(sessionId);
            }

            // 清理发送锁
            sessionSendLocks.remove(sessionId);

            // 关闭WebSocket
            WebSocketSession session = sessions.remove(sessionId);
            if (session != null && session.isOpen()) {
                try {
                    session.close();
                } catch (IOException e) {
                    log.debug("异常关闭WebSocket会话: {}", sessionId);
                }
            }

            clearGlobalData();
            log.info("异常清理完成: {}", sessionId);

        } catch (Exception e) {
            log.error("异常清理过程中出错: {}", sessionId, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        log.info("单次时间测量连接关闭: {}, 原因: {}", sessionId, status);

        // 连接关闭时也需要确保数据保存和资源释放
        handleConnectionClosed(sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = session.getId();
        log.error("单次时间测量传输错误: {}", sessionId, exception);

        // 传输错误时也需要确保数据保存和资源释放
        handleConnectionClosed(sessionId);
    }


    /**
     * 处理连接关闭情况 - 统一处理数据保存和资源释放
     */
    private void handleConnectionClosed(String sessionId) {
        TimeOneMeasureContext context = contexts.get(sessionId);
        WebSocketSession session = sessions.get(sessionId);

        if (context == null) {
            // 如果没有上下文，直接清理
            cleanupInException(sessionId);
            return;
        }

        // 检查是否已经在处理停止或连接已关闭
        if (context.cleanupInProgress.get() || context.sessionClosing.get()) {
            log.info("连接关闭时检测到清理已在处理中，忽略: {}", sessionId);
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
                saveMeasurementDataSync(session, context);
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
            cleanupInException(sessionId);
        }
    }



    /**
     * 安全清理 - 这个方法现在主要用于正常完成时的清理
     */
    private void safeCleanup(String sessionId, boolean releaseDevice) {
        log.info("开始安全清理会话: {}", sessionId);

        TimeOneMeasureContext context = contexts.get(sessionId);
        if (context != null) {
            if (context.cleanupInProgress.getAndSet(true)) {
                return;
            }

            // 标记会话正在关闭
            context.sessionClosing.set(true);

            // 先停止任务
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
                    log.info("释放单次时间测量设备: {}", context.deviceSerialNumber);
                } catch (Exception e) {
                    log.error("释放设备异常", e);
                }
            }

            contexts.remove(sessionId);
        }

        // 清理发送锁
        sessionSendLocks.remove(sessionId);

        // 最后关闭WebSocket
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.close();
                log.info("WebSocket连接已关闭: {}", sessionId);
            } catch (IOException e) {
                log.debug("关闭WebSocket会话异常: {}", sessionId);
            }
        }

        clearGlobalData();
        log.info("单次时间测量会话安全清理完成: {}", sessionId);
    }

    /**
     * 发送消息并安全关闭
     */
    private void sendMessageAndSafeClose(WebSocketSession session, String message) {
        try {
            sendMessageSafe(session, message);
        } finally {
            safeCleanup(session.getId(), true);
        }
    }

    /**
     * 同步发送消息 - 避免WebSocket并发发送冲突
     */
    private void sendMessageSynchronized(WebSocketSession session, String message) {
        TimeOneMeasureContext context = contexts.get(session.getId());
        if (context != null && context.sessionClosing.get()) {
            return;
        }

        Object sendLock = sessionSendLocks.get(session.getId());
        if (sendLock == null) {
            return;
        }

        synchronized (sendLock) {
            if (context != null && context.sessionClosing.get()) {
                return;
            }

            if (session.isOpen()) {
                try {
                    boolean wasInterrupted = Thread.interrupted();
                    session.sendMessage(new TextMessage(message));
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
     * 安全发送消息
     */
    private void sendMessageSafe(WebSocketSession session, String message) {
        sendMessageSynchronized(session, message);
    }

    private String getQueryParam(String query, String key) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) return pair[1];
        }
        return null;
    }

    /**
     * 清空全局数据
     */
    private void clearGlobalData() {
        Constants.startTime = null;
        Constants.endTime = null;
    }
}