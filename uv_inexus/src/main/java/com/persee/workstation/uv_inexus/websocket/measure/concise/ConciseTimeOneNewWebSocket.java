package com.persee.workstation.uv_inexus.websocket.measure.concise;/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.enums.T600ComIdEnums;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTimeSweepParamter;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.ColorUtil;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.utils.OperateLog.LogManualUtil;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketOperationEnum;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketResponseVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketSendUtil;
import com.persee.workstation.uv_inexus.websocket.measure.param.FTimeNewWebSocketParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConciseTimeOneNewWebSocket {

    private static final Map<String, TimeScanContext> contexts = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 添加连接状态管理
    private static final Map<String, AtomicBoolean> connectionClosingMap = new ConcurrentHashMap<>();
    private static final Map<String, AtomicBoolean> stopProcessingMap = new ConcurrentHashMap<>();
    private static UnknowCountMessageQueue unknowCountMessageQueue = UnknowCountMessageQueue.getInstance();

    @Autowired
    private TimeSweepConciseService timeSweepConciseService;
    @Autowired
    private FMeasureTaskService tMeasureTaskService;
    @Autowired
    private FMeasureMethodService tMeasureMethodService;
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Autowired
    private FMeasureDataService tMeasureDataService;
    @Autowired
    private FMeasureAnalysisDataService tMeasureAnalysisDataService;
    @Autowired
    private FMeasureAnalysisImageService tMeasureAnalysisImageService;
    @Autowired
    private SamplePoolConfigService fSamplePoolConfigService;
    @Autowired
    private FMeasureSampleService tMeasureSampleService;
    @Autowired
    private SystemAlgorithmConfService systemAlgorithmConfService;
    @Autowired
    private DevMessageUtil devMessageUtil;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private FormulaUtil formulaUtil;
    @Autowired
    private PythonPathConstant pythonPathConstant;
    @Autowired
    WebSocketSendUtil webSocketSendUtil;
    @Autowired
    private DevOperationUtilService devOperationUtilService;
    @Autowired
    DeviceInfoService infoService;

    String currentSampleCell;

    /**
     * 时间扫描上下文
     */
    private static class TimeScanContext {
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicBoolean dataSaved = new AtomicBoolean(false);
        final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);
        final AtomicBoolean connectionClosed = new AtomicBoolean(false);
        final CountDownLatch saveLatch = new CountDownLatch(1);

        // 添加重新测量标志
        final AtomicBoolean isRemeasurement = new AtomicBoolean(false);
        final AtomicBoolean dataClearedForRemeasure = new AtomicBoolean(false);

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
        Integer currentSerialNumber = 1;

        void clearData() {
            xData.clear();
            yData.clear();
            originalData.clear();
        }

        void resetForRemeasure() {
            clearData();
            dataClearedForRemeasure.set(true);
        }

        void setRemeasurement(boolean isRemeasure) {
            isRemeasurement.set(isRemeasure);
            if (isRemeasure) {
                dataClearedForRemeasure.set(false);
            }
            log.info("设置重新测量标志: sampleId={}, serialNumber={}, isRemeasure={}",
                    currentSampleId, currentSerialNumber, isRemeasure);
        }

        boolean isRemeasurement() {
            return isRemeasurement.get();
        }

        boolean isDataClearedForRemeasure() {
            return dataClearedForRemeasure.get();
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
     *
     * @param param
     */
    public void measureTask(FTimeNewWebSocketParam param) {
        log.info("单次时间测量连接建立");
        clearGlobalData();
        currentSampleCell=null;
        Constants.measureStopStatus.put(param.getDeviceSerialNumber(), true);
        String taskId = param.getTaskId();
        connectionClosingMap.put(taskId, new AtomicBoolean(false));
        stopProcessingMap.put(taskId, new AtomicBoolean(false));

        TimeScanContext context = new TimeScanContext();
        contexts.put(taskId, context);
        FMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            sendError(param.getDeviceSerialNumber(), "任务不存在");
            return;
        }
        context.task = task;

        try {
            initializeAndStart(param, context);
        } catch (Exception e) {
            log.error("时间扫描初始化失败: {}", taskId, e);
            sendError(param.getDeviceSerialNumber(), "时间扫描初始化失败");
        }
    }


    /**
     * 初始化并开始测量
     */
    private void initializeAndStart(FTimeNewWebSocketParam param, TimeScanContext context) {
        String sessionId = param.getTaskId();
        try {
            String taskId = param.getTaskId();
            String sampleId = param.getSampleId();
            String deviceSerialNumber = param.getDeviceSerialNumber();
            String updateBy = param.getUpdateBy();
            Integer digit = param.getDigit();
            infoService.updateDeviceCurrentStatus(deviceSerialNumber,WebSocketOperationEnum.ConciseTimeMeasureOne.getOperation(), taskId, Constants.DEVICE_STATUS_MEASURE);
            if (taskId == null || deviceSerialNumber == null || updateBy == null || sampleId == null) {
                sendError(deviceSerialNumber, "参数错误");
                return;
            }

            context.deviceSerialNumber = deviceSerialNumber;
            context.digit = digit;
            context.ip = deviceInfoService.getIp(deviceSerialNumber);
            context.currentSampleId = sampleId;

            FMeasureTask task = tMeasureTaskService.getById(taskId);
            if (task == null) {
                sendError(deviceSerialNumber,"任务不存在");
                return;
            }

            context.task = task;

            // 设置参数
//            Result paramResult = timeSweepConciseService.setUpParametersPort(task.getMethodKey(), deviceSerialNumber);
//            if (paramResult.getCode() != 200) {
//                sendError(deviceSerialNumber,paramResult.getMessage());
//                return;
//            }

            // 查询方法参数
            FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method == null) {
                sendError(deviceSerialNumber,"方法不存在");
                return;
            }

            FTimeSweepParamter timeParam = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
            // 配置样品池
            if (!setupSamplePoolIfNeeded(param, context, updateBy)) {
                sendError(deviceSerialNumber,"样品池配置失败");
                return;
            }
            // 开始测量
            startTimeMeasurement(param, context, timeParam, updateBy);

        } catch (Exception e) {
            log.error("时间扫描初始化异常: {}", sessionId, e);
            sendError(context.deviceSerialNumber, "时间扫描初始化异常");
        }
    }

    private void startTimeMeasurement(FTimeNewWebSocketParam session, TimeScanContext context,
                                      FTimeSweepParamter param, String updateBy) {
        String sessionId = session.getTaskId();

        try {
            // 设置测量开始状态
            Constants.startTime = LocalDateTime.now();

            executeSampleMeasurement(session, context, param , session.getSampleId(), updateBy);
            // 根据序列号查询对应的附件配置表信息
            QueryWrapper<SamplePoolConfigPO> wrapper1 = new QueryWrapper<>();
            wrapper1.eq("device_serial_number", context.deviceSerialNumber);
            SamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper1);
            if (configPO == null) {
                sendError(context.deviceSerialNumber,  "没有找到对应的附件配置信息");
                return;
            }
            if (configPO.getIsAutoChangePool()) {
                SetParam setParam = new SetParam();
                setParam.setCellPos(Integer.parseInt(Constants.PARAM_ONE));
                boolean status = timeSweepConciseService.setInstrumentParameters(context.deviceSerialNumber, setParam);
                if (!status) {
                    sendError(context.deviceSerialNumber,  "设置出错");
                    return;
                }
                Boolean b = fSamplePoolConfigService.updateCurrentPoolNum(context.deviceSerialNumber, Integer.parseInt(Constants.PARAM_ONE), updateBy);
                if (b) {
                    log.info("自动切换样品池恢复样品池到1号池");
                    JSONObject object = new JSONObject();
                    object.put("currentPoolNum", Constants.PARAM_ONE);
                    sendMessage(context.deviceSerialNumber, object);
                }else{
                    log.info("自动切换样品池恢复样品池到1号池失败");
                }
            }

            // 只有在正常完成（没有被停止）的情况下才发送"扫描成功"
            if (context.running.get()&&Constants.measureStopStatus.get(session.getDeviceSerialNumber())) {
                JSONObject object = new JSONObject();
                object.put("success", "扫描成功");
                sendMessage(session.getDeviceSerialNumber(), object);
                // 正常完成 - 等待数据保存后再清理
                log.info("所有样品测量完成，等待数据保存...");
            }else{
                JSONObject object = new JSONObject();
                object.put("success", "停止成功");
                sendMessage(session.getDeviceSerialNumber(), object);
                // 正常完成 - 等待数据保存后再清理
                log.info("测量停止成功，等待数据保存...");
            }
            infoService.updateDeviceCurrentStatus(session.getDeviceSerialNumber(), Constants.DEVICE_STATUS_IDLE);
            if (context.waitForDataSave(30, TimeUnit.SECONDS)) {
                log.info("数据保存完成，开始清理会话");
                safeCleanup(sessionId, true);
            } else {
                log.warn("数据保存超时，强制清理");
                safeCleanup(sessionId, true);
            }

        } catch (Exception e) {
            log.error("时间测量执行异常: {}", sessionId, e);
            sendError(context.deviceSerialNumber,"时间测量执行异常");
        }
    }

    /**
     * 执行单个样品测量
     *
     * @param session
     * @param context
     * @param param
     * @param sampleId
     */
    private void executeSampleMeasurement(FTimeNewWebSocketParam session, TimeScanContext context,
                                          FTimeSweepParamter param, String sampleId, String updateBy) {
        context.clearData();
        context.currentSampleId = sampleId;

        // 获取当前样品的信息
        FMeasureSample sample = tMeasureSampleService.getById(sampleId);

        // 使用BigDecimal避免浮点数精度问题
        BigDecimal interval = new BigDecimal(param.getIntervalTime());
        int totalTasks = Integer.parseInt(param.getTotalNumber());

        // 判断是否是重新测量
        // 检查是否已经存在已完成的测量数据（isSave = "1"）
        QueryWrapper<FMeasureData> checkQuery = new QueryWrapper<>();
        checkQuery.eq("task_id", context.task.getId())
                .eq("sample_key", sampleId)
                .eq("serial_number", context.currentSerialNumber)
                .eq("is_save", "1");
        FMeasureData existingCompletedData = tMeasureDataService.getOne(checkQuery);
        boolean isRemeasure = existingCompletedData != null;
        context.setRemeasurement(isRemeasure);

        if (isRemeasure) {
            log.info("检测到重新测量: sampleId={}, serialNumber={}, 将清空原有数据",
                    sampleId, context.currentSerialNumber);
            // 清空已存在的记录，准备重新测量
            clearExistingMeasurementData(context.task.getId(), sampleId, context.currentSerialNumber);

            /// 打印日志是否重新测量
            LogManualUtil.saveReMeasureLog(
                    "实验测试-经典模式-时间扫描-重新测量",
                    String.format("重测样品：编号[%s]，名称[%s]", sample.getSampleNumber(), sample.getSampleName()),
                    updateBy,   // 传入用户名
                    null,
                    session
            );
        }

        log.info("开始样品测量: {}, 序列号: {}, 间隔: {}s, 总数: {}, 是否重新测量: {}",
                sampleId, context.currentSerialNumber, interval, totalTasks, isRemeasure);


        Constants.startTime = LocalDateTime.now();

        try {
            // 调用时间扫描的扫描
            executeSingleMeasurement(session.getDeviceSerialNumber(), context, param);

            // 接取时间扫描数据
            retrieveTimeScanData(session.getDeviceSerialNumber(), context, param, interval, totalTasks);


            Constants.endTime = LocalDateTime.now();

            log.info("样品测量完成: {}, 总点数: {}", sampleId, context.xData.size());
        } catch (Exception e) {
            log.error("样品测量异常: {}", sampleId, e);
            throw e;
        }


        try {
            saveMeasurementDataSync(context, sampleId);
        } catch (Exception e) {
            log.error("保存测量数据异常: {}", sampleId, e);
            throw e;
        }
    }

    private void retrieveTimeScanData(String deviceSerialNumber, TimeScanContext context,
                                      FTimeSweepParamter param, BigDecimal interval, int totalTasks) {
        int currentIndex = 0;
        while (Constants.isRunning) {
            try {
                Object result = unknowCountMessageQueue.pollMessage(deviceSerialNumber, T600ComIdEnums.Measure.getComId());
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
                        case TIME_STOP:
                            log.info("设备 {} 的测量停止结束", deviceSerialNumber);
                            break;
                    }
                    // 结束循环
                    break;
                }
                // 使用BigDecimal精确计算时间点，避免浮点数精度问题
                BigDecimal xValue = interval.multiply(BigDecimal.valueOf(currentIndex))
                        .setScale(1, RoundingMode.HALF_UP);
                SpectrumPoint message = (SpectrumPoint) result;
                context.originalData.add(message.getMeasureRsd());
                calculateAndPushData(deviceSerialNumber, context, param, message, xValue);
                currentIndex++;
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("接收数据被中断");
                break;
            } catch (Exception e) {
                log.error("处理测量数据异常, 索引: {}", currentIndex, e);
                // 继续处理下一个点，避免单个点失败导致整个测量失败
                currentIndex++;
            }
        }
    }


    /**
     * 清空已存在的测量数据（用于重新测量）
     */
    private void clearExistingMeasurementData(String taskId, String sampleId, Integer serialNumber) {
        try {
            // 1. 清空测量数据
            QueryWrapper<FMeasureData> dataQuery = new QueryWrapper<>();
            dataQuery.eq("task_id", taskId)
                    .eq("sample_key", sampleId)
                    .eq("serial_number", serialNumber);
            FMeasureData existingMeasureData = tMeasureDataService.getOne(dataQuery);

            if (existingMeasureData != null) {
                // 清空数据，重置状态
                existingMeasureData.setOriginalCalculationData(JSON.toJSONString(new ArrayList<>()));
                existingMeasureData.setInstrumentData(JSON.toJSONString(new ArrayList<>()));
                existingMeasureData.setMeasurementStartTime(null);
                existingMeasureData.setMeasurementEndTime(null);
                existingMeasureData.setIsSave("0");
                tMeasureDataService.updateById(existingMeasureData);
                log.info("重新测量：已清空测量数据记录, measureDataId={}", existingMeasureData.getId());
            }

            // 2. 清空分析数据
            QueryWrapper<FMeasureAnalysisData> analysisQuery = new QueryWrapper<>();
            analysisQuery.eq("task_id", taskId)
                    .eq("sample_key", sampleId)
                    .eq("analysis_number", serialNumber)
                    .eq("analysis_type", "5");
            FMeasureAnalysisData existingAnalysisData = tMeasureAnalysisDataService.getOne(analysisQuery);

            if (existingAnalysisData != null) {
                existingAnalysisData.setAnalysisResult(JSON.toJSONString(new ArrayList<>()));
                existingAnalysisData.setAnalysisParam(JSON.toJSONString(new ArrayList<>()));
                existingAnalysisData.setPeak(null);
                existingAnalysisData.setGrain(null);
                existingAnalysisData.setIsSave("0");
                tMeasureAnalysisDataService.updateById(existingAnalysisData);
                log.info("重新测量：已清空分析数据记录, analysisDataId={}", existingAnalysisData.getId());
            }

        } catch (Exception e) {
            log.error("清空已存在数据失败", e);
        }
    }


    /**
     * 设置样品池位置
     */
    private boolean setupSamplePoolIfNeeded(FTimeNewWebSocketParam session, TimeScanContext context, String updateBy) {
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
                // 设置样品池位置
                // 更新当前样品池位置
                fSamplePoolConfigService.updateCurrentPoolNum(context.deviceSerialNumber,
                        Integer.parseInt(sample.getSampleLocation()), updateBy);

                SetParam setParam = new SetParam();
                setParam.setCellPos(Integer.parseInt(sample.getSampleLocation()));
                boolean b = timeSweepConciseService.setInstrumentParameters(context.deviceSerialNumber, setParam);
                if (b){
                    // 发送样品池位置信息到前端
                    JSONObject poolMsg = new JSONObject();
                    poolMsg.put("currentPoolNum", sample.getSampleLocation());
                    currentSampleCell=sample.getSampleLocation();
                    sendMessage(session.getDeviceSerialNumber(), poolMsg);
                    log.info("成功设置样品池位置: {}", sample.getSampleLocation());
                    return true;
                }else{
                    log.warn("设置样品池位置失败: {}", sample.getSampleLocation());
                    return false;
                }
            }
            return true;

        } catch (Exception e) {
            log.error("设置样品池失败", e);
            return false;
        }
    }

    /**
     * 执行单次测量
     */
    private void executeSingleMeasurement(String deviceSerialNumber, TimeScanContext context,
                                          FTimeSweepParamter param) {

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.TimeMeasure.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setWavelength(Collections.singletonList(param.getWaveLength()));
        Double scanTime;
        if (param.getScanUnit().equals("0")){
            scanTime = Double.parseDouble(param.getScanTime()) * 60+Double.parseDouble(param.getIntervalTime());
        }else{
            scanTime = Double.parseDouble(param.getScanTime())+Double.parseDouble(param.getIntervalTime());
        }

        Double intervalTime = Double.parseDouble(param.getIntervalTime());

        // 时间校验逻辑
        if (intervalTime > 0) {
            // 计算倍数
            double times = scanTime / intervalTime;

            // 判断是否为整数倍数
            boolean isExactMultiple = Math.abs(times - Math.round(times)) < 0.000001;

            if (!isExactMultiple) {
                // 方法1：向下取整倍数
                int fullIntervals = (int) Math.floor(times);
                scanTime = fullIntervals * intervalTime;

                // 确保至少有一个间隔的时间
                if (scanTime < intervalTime) {
                    scanTime = intervalTime;
                }
            }
        }
        inputParam.setTotalTime(scanTime);
        inputParam.setIntervalSeconds(Double.parseDouble(param.getIntervalTime()));
        R<OperationResultEntity> r = devOperationUtilService.operation(inputParam);
        if (!r.getSuccess()) {
            sendError(deviceSerialNumber,r.getMessage());
        }
    }

    private void calculateAndPushData(String deviceSerialNumber, TimeScanContext context,
                                      FTimeSweepParamter param, SpectrumPoint calculate, BigDecimal xValue) {
        try {
            BigDecimal yValue;
            String yType;
            context.xData.add(xValue);
            switch (param.getLuminosity()) {
                case "1": {
                    yValue = new BigDecimal(calculate.getAbsResult().getAbs());
                    yType = "Abs";
                    break;
                }
                case "2": {
                    yValue = new BigDecimal(calculate.getTResult().getTPercent());
                    yType = "T%";
                    break;
                }
                case "3": {
                    yValue = new BigDecimal(calculate.getEsResult().getEs());
                    yType = "Es";
                    break;
                }
                case "4": {
                    yValue = new BigDecimal(calculate.getErResult().getEr());
                    yType = "Er";
                    break;
                }
                default: {
                    sendError(deviceSerialNumber, "无效的光度参数");
                    return;
                }

            }
            context.yData.add(yValue);

            // 推送数据点
            pushDataPoint(deviceSerialNumber, xValue, yValue, context.currentSampleId, param, context.digit, yType);

            // 每次推送后立即保存到数据库
            handleTimePointDataStorage(
                    deviceSerialNumber,
                    context.task.getId(),
                    context.currentSampleId,
                    context.currentSerialNumber,  // 需要在context中添加这个字段
                    xValue,
                    yValue,
                    calculate.getMeasureRsd()
            );


        } catch (Exception e) {
            log.error("计算数据异常", e);
        }
    }

    private void pushDataPoint(String deviceSerialNumber, BigDecimal xValue, BigDecimal yValue,
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
        payload.put("currentPoolNum", currentSampleCell);

        log.info("推送数据点: {}", payload);

        sendMessage(deviceSerialNumber, payload);
    }

    /**
     * 处理单点数据的数据库存储（支持首次创建、重新测量和追加）
     * @param deviceSerialNumber 设备编号
     * @param taskId 任务ID
     * @param sampleId 样品ID
     * @param serialNumber 序列号
     * @param xValue 当前时间点
     * @param yValue 当前测量值
     * @param originalData 当前原始数据
     */
    private void handleTimePointDataStorage(String deviceSerialNumber, String taskId, String sampleId,
                                            Integer serialNumber, BigDecimal xValue, BigDecimal yValue,
                                            ChangeParam originalData) {
        try {
            // 获取当前上下文
            TimeScanContext context = contexts.values().stream()
                    .filter(ctx -> ctx.deviceSerialNumber.equals(deviceSerialNumber)
                            && ctx.currentSampleId.equals(sampleId))
                    .findFirst()
                    .orElse(null);

            boolean isRemeasurement = context != null && context.isRemeasurement();

            // 构建当前点的结果字符串
            String pointResult = xValue + "," + yValue;

            // 查询是否已存在测量数据记录
            QueryWrapper<FMeasureData> dataQuery = new QueryWrapper<>();
            dataQuery.eq("task_id", taskId)
                    .eq("sample_key", sampleId)
                    .eq("serial_number", serialNumber);
            FMeasureData existingMeasureData = tMeasureDataService.getOne(dataQuery);

            // 查询分析数据记录
            QueryWrapper<FMeasureAnalysisData> analysisQuery = new QueryWrapper<>();
            analysisQuery.eq("task_id", taskId)
                    .eq("sample_key", sampleId)
                    .eq("analysis_number", serialNumber)
                    .eq("analysis_type", "5");
            FMeasureAnalysisData existingAnalysisData = tMeasureAnalysisDataService.getOne(analysisQuery);

            // ========== 重新测量模式：清空原有数据，重新开始 ==========
            if (isRemeasurement && context != null && !context.isDataClearedForRemeasure()) {
                log.info("重新测量模式: sampleId={}, serialNumber={}, 将清空原有数据并重新写入", sampleId, serialNumber);

                if (existingMeasureData != null) {
                    // 清空并重置测量数据
                    List<String> newResultList = new ArrayList<>();
                    newResultList.add(pointResult);
                    existingMeasureData.setOriginalCalculationData(JSON.toJSONString(newResultList));

                    List<ChangeParam> newOriginalList = new ArrayList<>();
                    newOriginalList.add(originalData);
                    existingMeasureData.setInstrumentData(JSON.toJSONString(newOriginalList));
                    existingMeasureData.setMeasurementStartTime(null);
                    existingMeasureData.setMeasurementEndTime(null);
                    existingMeasureData.setIsSave("0");
                    tMeasureDataService.updateById(existingMeasureData);
                    log.info("重新测量：已重置测量数据记录, measureDataId={}", existingMeasureData.getId());
                } else {
                    // 如果测量数据不存在，创建新的
                    FMeasureData newMeasureData = new FMeasureData();
                    newMeasureData.setSerialNumber(serialNumber);
                    newMeasureData.setTaskId(taskId);
                    newMeasureData.setSampleKey(sampleId);

                    List<String> newResultList = new ArrayList<>();
                    newResultList.add(pointResult);
                    newMeasureData.setOriginalCalculationData(JSON.toJSONString(newResultList));

                    List<ChangeParam> newOriginalList = new ArrayList<>();
                    newOriginalList.add(originalData);
                    newMeasureData.setInstrumentData(JSON.toJSONString(newOriginalList));

                    newMeasureData.setIsDelete("0");
                    newMeasureData.setIsSave("0");
                    tMeasureDataService.save(newMeasureData);
                    existingMeasureData = newMeasureData;
                    log.info("重新测量：创建新的测量数据记录, measureDataId={}", newMeasureData.getId());
                }

                if (existingAnalysisData != null) {
                    // 清空并重置分析数据
                    List<String> newResultList = new ArrayList<>();
                    newResultList.add(pointResult);
                    existingAnalysisData.setAnalysisResult(JSON.toJSONString(newResultList));

                    List<ChangeParam> newOriginalList = new ArrayList<>();
                    newOriginalList.add(originalData);
                    existingAnalysisData.setAnalysisParam(JSON.toJSONString(newOriginalList));
                    existingAnalysisData.setPeak(null);
                    existingAnalysisData.setGrain(null);
                    existingAnalysisData.setIsSave("0");
                    tMeasureAnalysisDataService.updateById(existingAnalysisData);
                    log.info("重新测量：已重置分析数据记录, analysisDataId={}", existingAnalysisData.getId());
                } else {
                    // 如果分析数据不存在，创建新的
                    FMeasureSample sample = tMeasureSampleService.getById(sampleId);
                    String sampleName = sample != null ? sample.getSampleName() : "样品";

                    FMeasureAnalysisData newAnalysisData = new FMeasureAnalysisData();
                    newAnalysisData.setAnalysisNumber(serialNumber);
                    newAnalysisData.setAnalysisName(sampleName + "-" + serialNumber);
                    newAnalysisData.setTaskId(taskId);
                    newAnalysisData.setSampleKey(sampleId);
                    newAnalysisData.setMeasureDataId(existingMeasureData != null ? existingMeasureData.getId() : null);

                    List<String> newResultList = new ArrayList<>();
                    newResultList.add(pointResult);
                    newAnalysisData.setAnalysisResult(JSON.toJSONString(newResultList));

                    List<ChangeParam> newOriginalList = new ArrayList<>();
                    newOriginalList.add(originalData);
                    newAnalysisData.setAnalysisParam(JSON.toJSONString(newOriginalList));

                    newAnalysisData.setAnalysisType("5");
                    newAnalysisData.setColor(ColorUtil.getRandomColor());
                    newAnalysisData.setIsDelete("0");
                    newAnalysisData.setIsSave("0");
                    tMeasureAnalysisDataService.save(newAnalysisData);

                    // 创建图片记录
                    FMeasureAnalysisImage analysisImage = new FMeasureAnalysisImage();
                    analysisImage.setAnalysisId(newAnalysisData.getId());
                    tMeasureAnalysisImageService.save(analysisImage);
                    log.info("重新测量：创建新的分析数据记录, analysisDataId={}", newAnalysisData.getId());
                }

                // 标记数据已清空，后续点正常追加
                if (context != null) {
                    context.dataClearedForRemeasure.set(true);
                }
                return;
            }

            // ========== 首次测量模式：创建新记录 ==========
            if (existingMeasureData == null) {
                // 第一次创建，新建记录
                FMeasureData measureData = new FMeasureData();
                measureData.setSerialNumber(serialNumber);
                measureData.setTaskId(taskId);
                measureData.setSampleKey(sampleId);

                List<String> resultList = new ArrayList<>();
                resultList.add(pointResult);
                measureData.setOriginalCalculationData(JSON.toJSONString(resultList));

                List<ChangeParam> originalList = new ArrayList<>();
                originalList.add(originalData);
                measureData.setInstrumentData(JSON.toJSONString(originalList));

                measureData.setIsDelete("0");
                measureData.setIsSave("0");
                tMeasureDataService.save(measureData);

                // 获取样品名称
                FMeasureSample sample = tMeasureSampleService.getById(sampleId);
                String sampleName = sample != null ? sample.getSampleName() : "样品";

                // 创建分析数据记录
                FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
                analysisData.setAnalysisNumber(serialNumber);
                analysisData.setAnalysisName(sampleName + "-" + serialNumber);
                analysisData.setTaskId(taskId);
                analysisData.setSampleKey(sampleId);
                analysisData.setMeasureDataId(measureData.getId());
                analysisData.setAnalysisParam(JSON.toJSONString(originalList));
                analysisData.setAnalysisResult(JSON.toJSONString(resultList));
                analysisData.setAnalysisType("5");
                analysisData.setColor(ColorUtil.getRandomColor());
                analysisData.setIsDelete("0");
                analysisData.setIsSave("0");
                tMeasureAnalysisDataService.save(analysisData);

                // 创建图片记录
                FMeasureAnalysisImage analysisImage = new FMeasureAnalysisImage();
                analysisImage.setAnalysisId(analysisData.getId());
                tMeasureAnalysisImageService.save(analysisImage);

                log.info("时间扫描首次创建数据点: sampleId={}, serialNumber={}", sampleId, serialNumber);
                return;
            }

            // ========== 追加模式（同一测量的后续数据点） ==========
            if (existingMeasureData != null) {
                // 更新测量数据 - 追加新点
                String existingDataStr = existingMeasureData.getOriginalCalculationData();
                List<String> existingData = new ArrayList<>();
                if (existingDataStr != null && !"[]".equals(existingDataStr) && !existingDataStr.isEmpty()) {
                    existingData = JSON.parseArray(existingDataStr, String.class);
                }
                existingData.add(pointResult);
                existingMeasureData.setOriginalCalculationData(JSON.toJSONString(existingData));

                // 更新仪器数据
                String existingInstrumentStr = existingMeasureData.getInstrumentData();
                List<ChangeParam> existingInstrument = new ArrayList<>();
                if (existingInstrumentStr != null && !"[]".equals(existingInstrumentStr) && !existingInstrumentStr.isEmpty()) {
                    existingInstrument = JSON.parseArray(existingInstrumentStr, ChangeParam.class);
                }
                existingInstrument.add(originalData);
                existingMeasureData.setInstrumentData(JSON.toJSONString(existingInstrument));

                tMeasureDataService.updateById(existingMeasureData);
            }

            // 更新分析数据 - 追加新点
            if (existingAnalysisData != null) {
                String existingResultStr = existingAnalysisData.getAnalysisResult();
                List<String> existingResult = new ArrayList<>();
                if (existingResultStr != null && !"[]".equals(existingResultStr) && !existingResultStr.isEmpty()) {
                    existingResult = JSON.parseArray(existingResultStr, String.class);
                }
                existingResult.add(pointResult);
                existingAnalysisData.setAnalysisResult(JSON.toJSONString(existingResult));

                String existingParamStr = existingAnalysisData.getAnalysisParam();
                List<ChangeParam> existingParam = new ArrayList<>();
                if (existingParamStr != null && !"[]".equals(existingParamStr) && !existingParamStr.isEmpty()) {
                    existingParam = JSON.parseArray(existingParamStr, ChangeParam.class);
                }
                existingParam.add(originalData);
                existingAnalysisData.setAnalysisParam(JSON.toJSONString(existingParam));

                tMeasureAnalysisDataService.updateById(existingAnalysisData);
            }

            log.debug("时间扫描追加数据点: sampleId={}, serialNumber={}, 当前总点数={}",
                    sampleId, serialNumber,
                    existingMeasureData != null ?
                            JSON.parseArray(existingMeasureData.getOriginalCalculationData(), String.class).size() : 0);

        } catch (Exception e) {
            log.error("处理时间扫描单点数据存储异常", e);
        }
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


            // 寻峰寻谷

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(analysisBody, headers);
            String peakUrl = pythonPathConstant.getPeakSearching();
            String valleyUrl = pythonPathConstant.getValleySearching();

            List<FPeakValleyMarking> peaks = new ArrayList<>();
            List<FPeakValleyMarking> valleys = new ArrayList<>();

            try {
                analysisBody.put("x_data", context.xData.stream().map(BigDecimal::doubleValue).collect(Collectors.toList()));
                analysisBody.put("y_data", context.yData.stream().map(BigDecimal::doubleValue).collect(Collectors.toList()));

                double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
                analysisBody.put("height", height);
                analysisBody.put("distance", confVO.getPeakInterval());
                // 添加日志输出（调试用）
                log.info("调用寻峰接口: " + peakUrl);
                log.info("调用寻谷接口: " + valleyUrl);
                Map<String, Object> peakResult = restTemplate.postForEntity(peakUrl, request, Map.class).getBody();
                List<Double> peakX = (List<Double>) peakResult.get("peak_x");
                List<String> peakY = (List<String>) peakResult.get("peak_y");
                for (int i = 0; i < peakX.size(); i++) {
                    FPeakValleyMarking peak = new FPeakValleyMarking();
                    peak.setX(peakX.get(i).toString());
                    peak.setY(peakY.get(i));
                    peak.setType("波峰");
                    peaks.add(peak);
                }
            } catch (Exception e) {
                log.error("调用寻峰算法失败: {}", e.getMessage(), e);
            }

            try {
                analysisBody.clear();
                analysisBody.put("x_data", context.xData.stream().map(BigDecimal::doubleValue).collect(Collectors.toList()));
                analysisBody.put("y_data", context.yData.stream().map(BigDecimal::doubleValue).collect(Collectors.toList()));

                double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
                analysisBody.put("height", height);
                analysisBody.put("distance", confVO.getPeakInterval());
                Map<String, Object> valleyResult = restTemplate.postForEntity(valleyUrl, request, Map.class).getBody();
                List<Double> valleyX = (List<Double>) valleyResult.get("valley_x");
                List<String> valleyY = (List<String>) valleyResult.get("valley_y");
                for (int i = 0; i < valleyX.size(); i++) {
                    FPeakValleyMarking valley = new FPeakValleyMarking();
                    valley.setX(valleyX.get(i).toString());
                    valley.setY(valleyY.get(i));
                    valley.setType("波谷");
                    valleys.add(valley);
                }
            }catch (Exception e) {
                log.error("调用寻谷算法失败: {}", e.getMessage(), e);
                // 失败时不处理，继续执行保存原始数据
            }

            // 保存测量数据
            FMeasureData measureData = tMeasureDataService.getOne(new QueryWrapper<FMeasureData>().eq("task_id", context.task.getId()).eq("sample_key", sampleId).last("limit 1"));
            if (measureData == null) {
                measureData = new FMeasureData();
            }
            measureData.setSerialNumber(1);
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

            tMeasureDataService.updateById(measureData);

            // 保存分析数据
            FMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(new QueryWrapper<FMeasureAnalysisData>().eq("task_id", context.task.getId()).eq("sample_key", sampleId).last("limit 1"));
            if (analysisData == null) {
                analysisData = new FMeasureAnalysisData();
            }
            analysisData.setAnalysisNumber(1);

            FMeasureSample sample = tMeasureSampleService.getById(sampleId);
            analysisData.setAnalysisName(sample != null ? sample.getSampleName() + "(" + analysisData.getAnalysisNumber() + ")" : "未知样品");
            analysisData.setTaskId(context.task.getId());
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(measureData.getId());
            analysisData.setAnalysisParam(JSON.toJSONString(context.originalData));
            analysisData.setAnalysisResult(JSON.toJSONString(resultData));
            analysisData.setAnalysisType("5");
            if (peaks!=null){
                analysisData.setPeak(com.alibaba.fastjson2.JSON.toJSONString(peaks));
            }
            if (valleys!=null){
                analysisData.setGrain(com.alibaba.fastjson2.JSON.toJSONString(valleys));
            }
            analysisData.setColor(ColorUtil.getRandomColor());
            analysisData.setIsDelete("0");
            analysisData.setIsSave("1");

            tMeasureAnalysisDataService.updateById(analysisData);

            // 保存分析图片
            FMeasureAnalysisImage tMeasureAnalysisImage = tMeasureAnalysisImageService.getOne(new QueryWrapper<FMeasureAnalysisImage>().eq("analysis_id", analysisData.getId()));
            if (tMeasureAnalysisImage == null) {
                tMeasureAnalysisImage = new FMeasureAnalysisImage();
            }
            tMeasureAnalysisImage.setAnalysisId(analysisData.getId());
            tMeasureAnalysisImageService.updateById(tMeasureAnalysisImage);

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

            // 从上下文中移除
            contexts.remove(sessionId);
            log.info("时间扫描上下文已移除: {}", sessionId);
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
        Constants.measureStopStatus.clear();
    }

    private void sendMessage(String deviceSerialNumber, JSONObject object) {

        WebSocketResponseVO vo = new WebSocketResponseVO();
        vo.setOperation(WebSocketOperationEnum.ConciseTimeMeasureOne.getOperation());
        vo.setData(object);

        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }

    private void sendError(String deviceSerialNumber, String message) {
        webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.ConciseTimeMeasureOne.getOperation(), message);
        infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
        safeCleanup(deviceSerialNumber, true);
    }
}