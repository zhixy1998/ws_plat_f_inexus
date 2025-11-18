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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ConciseTimeOneMeasureWebSocket extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ConciseTimeOneMeasureWebSocket.class);
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, AtomicBoolean> sessionStopFlags = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ExecutorService messageSender = Executors.newFixedThreadPool(2);

    private String ip;
    private String deviceSerialNumber;
    private boolean occupancyStatus = false;
    private Integer digit;
    private volatile boolean shouldStop = false;

    @Autowired private TimeSweepConciseService timeSweepConciseService;
    @Autowired private FMeasureTaskService tMeasureTaskService;
    @Autowired private FMeasureMethodService tMeasureMethodService;
    @Autowired private DeviceInfoService deviceInfoService;
    @Autowired private FMeasureDataService tMeasureDataService;
    @Autowired private FMeasureAnalysisDataService tMeasureAnalysisDataService;
    @Autowired private FMeasureAnalysisImageService tMeasureAnalysisImageService;
    @Autowired private FSamplePoolConfigService fSamplePoolConfigService;
    @Autowired private SystemAlgorithmConfService systemAlgorithmConfService;
    @Autowired private FMeasureSampleService tMeasureSampleService;
    @Autowired private DevMessageUtil devMessageUtil;
    @Autowired private RestTemplate restTemplate;
    @Autowired private FormulaUtil formulaUtil;

    private final ThreadLocal<JSONObject> messageTemplate = ThreadLocal.withInitial(JSONObject::new);
    private final ThreadLocal<JSONArray> pointsArray = ThreadLocal.withInitial(JSONArray::new);
    private final ThreadLocal<JSONObject> pointObject = ThreadLocal.withInitial(JSONObject::new);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        occupancyStatus = false;
        shouldStop = false;
        sessionStopFlags.put(session.getId(), new AtomicBoolean(false));

        executor.execute(() -> {
            try {
                log.info("WebSocket连接已建立: {}", session.getId());
                sessions.put(session.getId(), session);

                String query = Objects.requireNonNull(session.getUri()).getQuery();
                String taskId = getQueryParam(query, "taskId");
                String sampleId = getQueryParam(query, "sampleId");
                deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");
                String updateBy = getQueryParam(query, "updateBy");
                digit = Integer.parseInt(getQueryParam(query, "digit"));

                if (taskId == null || sampleId == null || deviceSerialNumber == null || updateBy == null) {
                    sendClose(session, "缺少必要参数");
                    return;
                }

                FMeasureTask task = tMeasureTaskService.getById(taskId);
                if (task == null) {
                    sendClose(session, "任务不存在");
                    return;
                }

                R<Boolean> message = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);
                if (!message.getSuccess()) {
                    sendClose(session, message.getMessage());
                    return;
                }
                occupancyStatus = true;

                Result result = timeSweepConciseService.setUpParametersPort(task.getMethodKey(), deviceSerialNumber);
                if (result.getCode() != 200) {
                    sendAndClose(session, "设置参数失败");
                    return;
                }

                FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
                if (method == null) {
                    sendAndClose(session, "方法不存在");
                    return;
                }

                FTimeSweepParamter timeSweepParameter = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
                startSingleSampleTask(session, timeSweepParameter, deviceSerialNumber, task, sampleId, updateBy);

            } catch (Exception e) {
                log.error("连接建立逻辑异常", e);
                sendAndClose(session, "执行异常: " + e.getMessage());
            }
        });
    }

    private void startSingleSampleTask(WebSocketSession session, FTimeSweepParamter param,
                                       String deviceSerialNumber, FMeasureTask task,
                                       String sampleId, String updateBy) {
        try {
            final ChangeParam baselineData = (task.getZeroParam() != null && !task.getZeroParam().isEmpty())
                    ? JSON.parseObject(task.getZeroParam(), ChangeParam.class)
                    : null;

            ip = deviceInfoService.getIp(deviceSerialNumber);

            if (!setupSamplePool(session, deviceSerialNumber, sampleId, updateBy)) {
                return;
            }

            executeDirectMeasurement(session, param, baselineData, sampleId, task, deviceSerialNumber);

        } catch (Exception e) {
            log.error("单样品任务执行异常", e);
            sendAndClose(session, "单样品任务执行异常: " + e.getMessage());
        }
    }

    private boolean setupSamplePool(WebSocketSession session, String deviceSerialNumber, String sampleId, String updateBy) {
        try {
            QueryWrapper<FSamplePoolConfigPO> wrapper = new QueryWrapper<>();
            wrapper.eq("device_serial_number", deviceSerialNumber);
            FSamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);

            if (configPO == null || !configPO.getIsAutoChangePool()) {
                return true;
            }

            FMeasureSample sample = tMeasureSampleService.getById(sampleId);
            if (sample == null) {
                sendAndClose(session, "请先创建样品");
                return false;
            }

            boolean poolStatus = JSON.parseArray(configPO.getPoolInfo(), FSamplePoolStatus.class)
                    .stream()
                    .anyMatch(FSamplePoolStatus::getPoolStatus);

            if (poolStatus) {
                R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
                if (!stringR.getSuccess()) {
                    sendAndClose(session, "设置出错");
                    return false;
                }
                fSamplePoolConfigService.updateCurrentPoolNum(deviceSerialNumber,
                        Integer.parseInt(sample.getSampleLocation()), updateBy);

                messageSender.execute(() -> {
                    try {
                        JSONObject object = new JSONObject();
                        object.put("currentPoolNum", sample.getSampleLocation());
                        session.sendMessage(new TextMessage(object.toJSONString()));
                    } catch (IOException e) {
                        log.error("发送样品池位置消息失败", e);
                    }
                });
            }
            return true;

        } catch (Exception e) {
            log.error("设置样品池失败", e);
            return false;
        }
    }

    private void executeDirectMeasurement(WebSocketSession session, FTimeSweepParamter param,
                                          ChangeParam baselineData, String sampleId,
                                          FMeasureTask task, String deviceSerialNumber) {

        double interval = Double.parseDouble(param.getIntervalTime());
        int totalTasks = Integer.parseInt(param.getTotalNumber());

        List<BigDecimal> x_data = new ArrayList<>(totalTasks);
        List<BigDecimal> y_data = new ArrayList<>(totalTasks);
        List<ChangeParam> originalData = new ArrayList<>(totalTasks);

        String luminosity = param.getLuminosity();
        String waveLength = param.getWaveLength() + "nm";
        String yType = getYType(luminosity);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalTasks; i++) {
            // 检查停止标志和会话状态
            if (shouldStop || !session.isOpen() ||
                    (sessionStopFlags.get(session.getId()) != null && sessionStopFlags.get(session.getId()).get())) {
                log.info("检测到停止信号，中断测量任务");
                break;
            }

            long taskStart = System.currentTimeMillis();

            try {
                BigDecimal xValue = BigDecimal.valueOf(i * interval).setScale(1, RoundingMode.DOWN);

                Command command = new Command(SetParamEnum.Measure.getComId(), new Object[]{});
                ChangeParam changeParam = devMessageUtil.timeSweepGetMeasureData(command, ip);

                if (changeParam == null) {
                    log.warn("测量数据为空，跳过本次测量");
                    continue;
                }

                Double v = formulaUtil.calculateWl(Double.parseDouble(changeParam.getWl()), deviceSerialNumber);
                changeParam.setWl(String.valueOf(v));
                originalData.add(changeParam);

                BigDecimal yValue = calculateYValueFast(baselineData, changeParam, luminosity);
                if (yValue != null) {
                    x_data.add(xValue);
                    y_data.add(yValue);
                    sendDataPointFast(session, xValue, yValue, sampleId, waveLength, yType);
                }

                long elapsed = System.currentTimeMillis() - taskStart;
                long sleepTime = (long)(interval * 1000) - elapsed;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                } else {
                    log.debug("任务执行超时: {}ms", -sleepTime);
                }

            } catch (Exception e) {
                if (shouldStop) {
                    log.info("测量任务被正常中断");
                    break;
                }
                log.error("第 {} 次测量异常", i, e);
            }
        }

        // 只有在正常完成时才保存数据
        if (!shouldStop && !x_data.isEmpty()) {
            Constants.endTime = LocalDateTime.now();
            executor.execute(() -> {
                saveDataFast(session, originalData, x_data, y_data, sampleId, task, deviceSerialNumber);
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("result", "finish");
                    session.sendMessage(new TextMessage(payload.toJSONString()));
                    // 正常完成后关闭连接
                    Thread.sleep(1000); // 给前端一点时间接收完成消息
                    closeSession(session);
                } catch (Exception e) {
                    log.error("发送完成消息失败", e);
                    closeSession(session);
                }
            });
        } else {
            // 被中断时直接关闭连接
            closeSession(session);
        }
    }

    private BigDecimal calculateYValueFast(ChangeParam baselineData, ChangeParam changeParam, String luminosity) {
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

    private void sendDataPointFast(WebSocketSession session, BigDecimal xValue, BigDecimal yValue,
                                   String sampleId, String waveLength, String yType) {
        try {
            if (!session.isOpen()) {
                return;
            }

            JSONObject payload = messageTemplate.get();
            JSONArray points = pointsArray.get();
            JSONObject point = pointObject.get();

            payload.clear();
            points.clear();
            point.clear();

            Double decimalY = yValue.doubleValue();
            Double value = DataCountUtil.formatValue(digit, decimalY);
            String valueY = DataCountUtil.addZero(value, digit);

            point.put("x", xValue);
            point.put("y", decimalY);
            point.put("roundOffY", valueY);
            point.put("nm", waveLength);
            point.put("xType", "s");
            point.put("yType", yType);

            points.add(point);
            payload.put("data", points);
            payload.put("sampleId", sampleId);

            session.sendMessage(new TextMessage(payload.toJSONString()));

        } catch (IOException e) {
            log.error("发送数据点失败", e);
        }
    }

    private void saveDataFast(WebSocketSession session, List<ChangeParam> originalData,
                              List<BigDecimal> x_data, List<BigDecimal> y_data,
                              String sampleId, FMeasureTask task, String deviceSerialNumber) {
        try {
            SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();

            Map<String, Object> body = new HashMap<>();
            body.put("x_data", x_data);
            body.put("y_data", y_data);
            body.put("height", Double.parseDouble(confVO.getPeakValleyDiff()) / 100);
            body.put("distance", confVO.getPeakInterval());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            Map<String, Object> peakResult = restTemplate.postForObject(PythonPathConstant.PEAK_SEARCHING, request, Map.class);
            Map<String, Object> valleyResult = restTemplate.postForObject(PythonPathConstant.VALLEY_SEARCHING, request, Map.class);

            saveToDatabaseFast(originalData, x_data, y_data, sampleId, task, deviceSerialNumber, peakResult, valleyResult);

        } catch (Exception e) {
            log.error("保存数据失败", e);
        }
    }

    private void saveToDatabaseFast(List<ChangeParam> originalData, List<BigDecimal> x_data,
                                    List<BigDecimal> y_data, String sampleId, FMeasureTask task,
                                    String deviceSerialNumber, Map<String, Object> peakResult,
                                    Map<String, Object> valleyResult) {
        try {
            boolean dataStatus = false;

            List<FPeakValleyMarking> peakValleys = processPeakValleyMarkings(
                    (List<Double>) peakResult.get("peak_x"),
                    (List<String>) peakResult.get("peak_y"), "波峰");
            List<FPeakValleyMarking> valleys = processPeakValleyMarkings(
                    (List<Double>) valleyResult.get("valley_x"),
                    (List<String>) valleyResult.get("valley_y"), "波谷");

            FMeasureData tMeasureData = tMeasureDataService.getOne(
                    new QueryWrapper<FMeasureData>()
                            .eq("task_id", task.getId())
                            .eq("sample_key", sampleId)
                            .orderByDesc("serial_number")
                            .last("LIMIT 1")
            );

            List<String> listData = new ArrayList<>(x_data.size());
            for (int i = 0; i < x_data.size(); i++) {
                listData.add(x_data.get(i) + "," + y_data.get(i));
            }

            if (tMeasureData == null) {
                tMeasureData = new FMeasureData();
                tMeasureData.setSerialNumber(1);
                tMeasureData.setTaskId(task.getId());
                tMeasureData.setSampleKey(sampleId);
                tMeasureData.setInstrumentData(JSON.toJSONString(originalData));
                tMeasureData.setOriginalCalculationData(JSON.toJSONString(listData));
                tMeasureData.setMeasurementStartTime(Constants.startTime);
                tMeasureData.setMeasurementEndTime(Constants.endTime);
                tMeasureData.setIsSave("1");
                tMeasureData.setIsDelete("0");
            } else {
                dataStatus = true;
                tMeasureData.setInstrumentData(JSON.toJSONString(originalData));
                tMeasureData.setOriginalCalculationData(JSON.toJSONString(listData));
                tMeasureData.setMeasurementStartTime(Constants.startTime);
                tMeasureData.setMeasurementEndTime(Constants.endTime);
            }

            boolean save = tMeasureDataService.saveOrUpdate(tMeasureData);
            if (!save) {
                log.error("保存测量数据失败");
                return;
            }

            if (dataStatus) {
                QueryWrapper<FMeasureAnalysisData> wrapper = new QueryWrapper<>();
                wrapper.eq("measure_data_id", tMeasureData.getId());
                wrapper.eq("task_id", task.getId());
                wrapper.eq("is_delete", "0");
                wrapper.last("limit 1");
                FMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(wrapper);
                if (analysisData == null) {
                    log.error("查询分析数据失败");
                    return;
                }
                analysisData.setAnalysisParam(JSON.toJSONString(originalData));
                analysisData.setAnalysisResult(JSON.toJSONString(listData));
                analysisData.setPeak(JSON.toJSONString(peakValleys));
                analysisData.setGrain(JSON.toJSONString(valleys));

                boolean save1 = tMeasureAnalysisDataService.saveOrUpdate(analysisData);
                if (!save1) {
                    log.error("保存分析数据失败");
                    return;
                }
            } else {
                FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
                analysisData.setAnalysisNumber(1);
                FMeasureSample sample = tMeasureSampleService.getById(sampleId);
                analysisData.setAnalysisName(sample.getSampleName() + "(" + analysisData.getAnalysisNumber() + ")");
                analysisData.setTaskId(task.getId());
                analysisData.setSampleKey(sampleId);
                analysisData.setMeasureDataId(tMeasureData.getId());
                analysisData.setAnalysisParam(JSON.toJSONString(originalData));
                analysisData.setAnalysisResult(JSON.toJSONString(listData));
                analysisData.setAnalysisType("5");
                analysisData.setPeak(JSON.toJSONString(peakValleys));
                analysisData.setGrain(JSON.toJSONString(valleys));
                analysisData.setColor("#000000");
                analysisData.setIsSave("1");
                analysisData.setIsDelete("0");

                boolean save1 = tMeasureAnalysisDataService.saveOrUpdate(analysisData);
                if (!save1) {
                    log.error("保存分析数据失败");
                    return;
                }

                FMeasureAnalysisImage analysisImage = new FMeasureAnalysisImage();
                analysisImage.setAnalysisId(analysisData.getId());
                boolean save2 = tMeasureAnalysisImageService.save(analysisImage);
                if (!save2) {
                    log.error("新增分析数据图片数据失败");
                }
            }

            List<FTaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FTaskSpecimen taskSpecimen : specimen) {
                if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                    taskSpecimen.setIsTested("1");
                    break;
                }
            }

            task.setSampleKey(JSON.toJSONString(specimen));
            task.setDeviceSerialNumber(deviceSerialNumber);
            boolean update = tMeasureTaskService.updateById(task);
            if (!update) {
                log.error("更新任务失败");
                return;
            }

            Constants.measureStatus = true;
            log.info("数据保存完成，样品: {}", sampleId);

        } catch (Exception e) {
            log.error("快速保存到数据库失败", e);
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

    private String getYType(String luminosity) {
        switch (luminosity) {
            case "1": return "Abs";
            case "2": return "T%";
            case "3": return "Es";
            case "4": return "Er";
            default: return "Unknown";
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

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("收到客户端消息: {}", message.getPayload());
        if ("close".equals(message.getPayload())) {
            log.info("收到关闭指令，准备关闭连接");
            try {
                session.sendMessage(new TextMessage("停止成功"));
            } catch (IOException e) {
                log.error("发送停止成功消息失败", e);
            }
            // 设置停止标志
            shouldStop = true;
            AtomicBoolean stopFlag = sessionStopFlags.get(session.getId());
            if (stopFlag != null) {
                stopFlag.set(true);
            }
            // 立即关闭连接
            closeSession(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket连接关闭: {}，原因: {}", session.getId(), status);
        cleanupResources(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输错误", exception);
        cleanupResources(session);
    }

    private void cleanupResources(WebSocketSession session) {
        try {
            log.info("开始清理资源，session: {}", session.getId());

            // 设置停止标志
            shouldStop = true;
            AtomicBoolean stopFlag = sessionStopFlags.get(session.getId());
            if (stopFlag != null) {
                stopFlag.set(true);
            }

            if (occupancyStatus && deviceSerialNumber != null) {
                deviceInfoService.releaseDevice(deviceSerialNumber);
                occupancyStatus = false;
            }

            sessions.remove(session.getId());
            sessionStopFlags.remove(session.getId());

            messageTemplate.remove();
            pointsArray.remove();
            pointObject.remove();

            Constants.startTime = null;
            Constants.endTime = null;
            Constants.measureStatus = false;

            log.info("资源清理完成");

        } catch (Exception e) {
            log.error("清理资源异常", e);
        }
    }

    private void closeSession(WebSocketSession session) {
        try {
            log.info("正在关闭WebSocket连接: {}", session.getId());
            if (session.isOpen()) {
                session.close();
            }
            cleanupResources(session);
            log.info("WebSocket连接已关闭: {}", session.getId());
        } catch (IOException e) {
            log.error("关闭WebSocket连接异常", e);
        }
    }

    private void sendAndClose(WebSocketSession session, String msg) {
        try {
            if (deviceSerialNumber != null) {
                deviceInfoService.releaseDevice(deviceSerialNumber);
            }
            session.sendMessage(new TextMessage(msg));
            closeSession(session);
        } catch (IOException e) {
            log.error("发送消息并关闭连接异常", e);
        }
    }

    private void sendClose(WebSocketSession session, String msg) {
        try {
            session.sendMessage(new TextMessage(msg));
            closeSession(session);
        } catch (IOException e) {
            log.error("发送关闭消息异常", e);
        }
    }

    private Command buildCommand(SetParamEnum cmdEnum, Object value) {
        Command cmd = new Command();
        cmd.setCmdId(cmdEnum.getComId());
        cmd.setParams(new Object[]{value});
        return cmd;
    }
}