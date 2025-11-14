/*
 * *****************************************************************************************************
 * Module name: **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 * 此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
@Component
public class ConciseSpectrumWebSocket extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ConciseSpectrumWebSocket.class);
    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();
    private String ip;
    private String deviceSerialNumber;
    private boolean occupancyStatus = false;
    private boolean measureStatus = false;
    private Integer digit;
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

    // 用于存储原始Y值（未经格式化）
    private Map<String, List<Double>> originalYValues = new ConcurrentHashMap<>();

    /**
     * 当WebSocket连接建立后调用此方法
     *
     * @param session 当前WebSocket会话，包含连接信息
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        occupancyStatus = false;
        measureStatus = false;
        // 打印客户端连接信息，包括会话ID和连接时间
        System.out.println("客户端进入: " + session.getId() + " 时间: " + LocalDateTime.now());
        // 将当前会话添加到sessions集合中
        sessions.put(session.getId(), session);
        exceptionHandling();
        if (Constants.endTime != null || Constants.startTime != null) {
            sendClose(session, "当前测量未结束,无法开启新的测试");
            return;
        }
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");
        String updateBy = getQueryParam(query, "updateBy");
        digit = Integer.parseInt(getQueryParam(query, "digit"));
        if (taskId == null) {
            sendClose(session, "缺少 taskId 参数");
            return;
        }
        if (deviceSerialNumber == null) {
            sendClose(session, "缺少 deviceSerialNumber 参数");
            return;
        }
        if (updateBy == null){
            sendClose(session, "缺少 updateBy 参数");
            return;
        }
        // 根据任务id查询任务数据
        FMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task.getSampleKey() == null || task.getSampleKey().equals("")) {
            sendClose(session, "请先创建样品");
            return;
        }
        //占用仪器
        R<Boolean> message = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);
        if (!message.getSuccess()) {
            sendClose(session, message.getMessage());
            return;
        }
        occupancyStatus = true;
        // 根据方法id查询方法数据
        FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 将方法参数提取出来
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        // 将任务中的Rsd取出来
        List<ChangeParam> baselineData = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
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
        QueryWrapper<FSamplePoolConfigPO> wrapper = new QueryWrapper<>();
        wrapper.eq("device_serial_number", deviceSerialNumber);
        FSamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);
        if (configPO == null) {
            sendAndClose(session, "没有找到对应的附件配置信息");
            return;
        }
        String maxLocation= "";
        if (configPO.getIsAutoChangePool()){
            List<String> sampleIds = newSampleData.stream().map(obj -> obj.getSpecimenKey()).collect(Collectors.toCollection(ArrayList::new));
            QueryWrapper<FMeasureSample> eq = new QueryWrapper<>();
            eq.in("id", sampleIds);
            List<FMeasureSample> list = tMeasureSampleService.list(eq);
            maxLocation = list.stream().map(FMeasureSample::getSampleLocation).max(String::compareTo).orElse(null);
        }
        /// 设置参数
        Result result = spectrumService.setUpParametersPort(task.getMethodKey(), deviceSerialNumber);
        if (result.getCode() != 200) {
            sendAndClose(session, "设置参数失败");
            return;
        }
        boolean poolStatus = false;
        if (configPO.getIsAutoChangePool()){
            List<FSamplePoolStatus> poolStatuses = com.alibaba.fastjson.JSON.parseArray(configPO.getPoolInfo(), FSamplePoolStatus.class);
            for (FSamplePoolStatus status : poolStatuses) {
                if (status.getPoolStatus()){
                    poolStatus = true;
                    break;
                }
            }
        }
        Integer timeInterval = Integer.valueOf(spectrumParameter.getTimeInterval());
        boolean repeatMode = "单池重复".equalsIgnoreCase(spectrumParameter.getRepetitionPattern());
        ip = deviceInfoService.getIp(deviceSerialNumber);
        Command command = new Command();
        if (spectrumParameter.getScanSpeed().equals("3")){
            command = buildCommand(SetParamEnum.ScanFast,new Object[]{});
        }else{
            command = buildCommand(SetParamEnum.Scan,new Object[]{});
        }
        // 往前端推送数据的线程
        String finalMaxLocation = maxLocation;
        boolean finalPoolStatus = poolStatus;
        Command finalCommand = command;
        Thread taskThread = new Thread(() -> {
            try {
                if (!repeatMode) {
                    // 不重复
                    for (int i = 0; i < newSampleData.size(); i++) {
                        // 预留: 设置样品池位置
                        FMeasureSample sample = tMeasureSampleService.getById(newSampleData.get(i).getSpecimenKey());
                        if (configPO.getIsAutoChangePool()){
                            if (sample == null){
                                sendAndClose(session, "请先创建样品");
                                return;
                            }
                            if (finalPoolStatus){
                                R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
                                if (!stringR.getSuccess()) {
                                    sendAndClose(session, "设置出错");
                                    return;
                                }
                                fSamplePoolConfigService.updateCurrentPoolNum(deviceSerialNumber, Integer.parseInt(sample.getSampleLocation()), updateBy);
                                JSONObject object = new JSONObject();
                                object.put("currentPoolNum",sample.getSampleLocation());
                                try {
                                    session.sendMessage(new TextMessage(object.toJSONString()));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        doOneMeasurementSerial(session, finalCommand, spectrumParameter, task, taskId, newSampleData.get(i).getSpecimenKey(), deviceSerialNumber, ip, 1);
                        newSampleData.get(i).setIsTested("1");
                        if (configPO.getIsAutoChangePool().equals(true)){
                            if ((i + 1) != newSampleData.size()&&!sample.getSampleLocation().equals(finalMaxLocation)) {
                                JSONObject payload = new JSONObject();
                                payload.put("result", "finish");
                                session.sendMessage(new TextMessage(payload.toJSONString()));
                            }else {
                                // 全部完成后再告诉前端 finish
                                JSONObject payload = new JSONObject();
                                payload.put("result", "finish");
                                sendAndClose(session, String.valueOf(payload));
                            }
                        } else {
                            // 全部完成后再告诉前端 finish
                            JSONObject payload = new JSONObject();
                            payload.put("result", "finish");
                            sendAndClose(session, String.valueOf(payload));
                        }
                    }
                } else {
                    // 单池重复
                    for (int i = 0; i < newSampleData.size(); i++) {
                        // 预留: 设置样品池位置
                        FMeasureSample sample = tMeasureSampleService.getById(newSampleData.get(i).getSpecimenKey());
                        if (configPO.getIsAutoChangePool()){
                            if (sample == null){
                                sendAndClose(session, "请先创建样品");
                                return;
                            }
                            if (finalPoolStatus){
                                R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
                                if (!stringR.getSuccess()) {
                                    sendAndClose(session, "设置出错");
                                    return;
                                }
                                fSamplePoolConfigService.updateCurrentPoolNum(deviceSerialNumber, Integer.parseInt(sample.getSampleLocation()), updateBy);
                                JSONObject object = new JSONObject();
                                object.put("currentPoolNum",sample.getSampleLocation());
                                try {
                                    session.sendMessage(new TextMessage(object.toJSONString()));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        // 根据样品的key查询样品
                        if (configPO.getIsAutoChangePool()){
                            if (sample == null){
                                sendAndClose(session, "样品不存在");
                                return;
                            }
                            R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
                            if (!stringR.getSuccess()) {
                                sendAndClose(session, "设置样品池出错");
                                return;
                            }
                        }
                        int repetitionTime= Integer.parseInt(spectrumParameter.getRepetitionTime());
                        int startIndex = 0;
                        // 根据样品id去分析数据表进行查询
                        QueryWrapper<FMeasureAnalysisData> eq = new QueryWrapper<>();
                        eq.eq("sample_key", newSampleData.get(i).getSpecimenKey());
                        List<FMeasureAnalysisData> list = tMeasureAnalysisDataService.list(eq);
                        if (!list.isEmpty()){
                            // 统计分析结果不为空的数量（假设分析结果字段叫 analysisResult，可按你实际字段改）
                            long notNullCount = list.stream()
                                    .filter(data -> data.getAnalysisResult() != null)
                                    .count();
                            startIndex = (int) notNullCount;
                        }
                        for (int r = startIndex; r < repetitionTime; r++) {
                            // 根据样品id,任务id和遍历的r+1查询分析数据
                            QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                            queryWrapper.eq("task_id", taskId);
                            queryWrapper.eq("sample_key", newSampleData.get(i).getSpecimenKey());
                            queryWrapper.eq("analysis_type", "5");
                            queryWrapper.eq("analysis_number", r + 1);
                            FMeasureAnalysisData measureData = tMeasureAnalysisDataService.getOne(queryWrapper);
                            if (measureData != null && measureData.getAnalysisResult() != null) {
                                continue;
                            }
                            if (measureData != null && measureData.getIsDelete().equals("1")) {
                                continue;
                            }
                            doOneMeasurementSerial(session, finalCommand, spectrumParameter, task, taskId, newSampleData.get(i).getSpecimenKey(), deviceSerialNumber, ip, r + 1);
                        }
                        if (!configPO.getIsAutoChangePool().equals(true)||sample.getSampleLocation().equals(finalMaxLocation)){
                            break;
                        }
                    }
                    // 全部完成后再告诉前端 finish
                    JSONObject payload = new JSONObject();
                    payload.put("result", "finish");
                    sendAndClose(session, String.valueOf(payload));
                }
            } catch (Exception e) {
                System.err.println("异常!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + e);
                handleTransportError(session, e);
                Thread.currentThread().interrupt();
                return;
            }
        });
        taskThread.start();
        taskThreads.put(session.getId(), taskThread);
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
        System.out.println("收到消息: " + session.getId() + " -> " + message.getPayload());
        if (message.getPayload().equals("close")){
            Thread thread = taskThreads.get(session.getId());
            if (measureStatus){
                try {
                    R<String> stop = devMessageUtil.stop(buildCommand(SetParamEnum.Stop, new Object[]{}), ip);
                    if (stop.getSuccess()){
                        log.info("停止成功");
                    }else {
                        log.info("停止失败");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    session.sendMessage(new TextMessage("停止成功"));
                    sessions.remove(session.getId());
                    session.close();
                    thread.interrupt();
                    deviceInfoService.releaseDevice(deviceSerialNumber);
                }
            }
            session.sendMessage(new TextMessage("停止成功"));
            sessions.remove(session.getId());
            session.close();
            // 如果线程正在被使用
            if (occupancyStatus){
                // 释放线程
                deviceInfoService.releaseDevice(deviceSerialNumber);
            }
        }
    }
    /**
     * 重写afterConnectionClosed方法，用于处理WebSocket连接关闭后的操作
     *
     * @param session WebSocket会话对象，包含连接的相关信息
     * @param status 关闭状态，包含连接关闭的原因和代码
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 打印客户端退出信息，包括会话ID、退出时间和关闭原因
        System.out.println("客户端退出: " + session.getId() + " 时间: " + LocalDateTime.now() + " 原因: " + status);
        if (Constants.sendMeasureThread != null){
            Constants.sendMeasureThread.interrupt();
        }
        Thread taskThread = taskThreads.remove(session.getId());
        if (taskThread != null) {
            taskThread.interrupt(); // 直接打断子线程
        }
        sessions.remove(session.getId());
        exceptionHandling(); // 清空公共数据
        // 清空原始Y值存储
        originalYValues.remove(session.getId());
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
            handleTransportError(session, e);
            Thread.currentThread().interrupt();
            return;
        } finally {
            deviceInfoService.releaseDevice(deviceSerialNumber);
            // 清空原始Y值存储
            originalYValues.remove(session.getId());
        }
        System.out.println("连接异常: " + exception.getMessage());
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
    private void doOneMeasurementSerial(WebSocketSession session,
                                        Command command,
                                        FSpectrumParameter spectrumParameter,
                                        FMeasureTask task,
                                        String taskId,
                                        String sampleId,
                                        String deviceSerialNumber,
                                        String ip,
                                        Integer count) throws IOException {
        // 1. 抢锁
        try {
            Constants.MEASURE_LOCK.lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        MeasurementResult result = null;
        try {
            // 清空全局缓存
            Constants.instrumentData.clear();
            Constants.measurd_data.clear();
            Constants.originalCalculationData.clear();
            Constants.startTime = LocalDateTime.now();
            Constants.endTime = null;

            // 采集数据 & 返回测量结果
            result = doOneMeasurementCore(session, command, spectrumParameter, task,
                    taskId, sampleId, deviceSerialNumber, ip, count);

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

            // 立即清空缓存，释放锁
            Constants.instrumentData.clear();
            Constants.measurd_data.clear();
            Constants.originalCalculationData.clear();
            Constants.startTime = null;
            Constants.endTime = null;
            Constants.MEASURE_LOCK.unlock();
        }

        // 异步保存数据，不阻塞测量流程
        MeasurementResult finalResult = result;
        new Thread(() -> {
            saveOneMeasurementWithSnapshots(task, taskId, sampleId, deviceSerialNumber,
                    finalResult.x, finalResult.y, session, count, finalResult.startTime, finalResult.endTime,
                    finalResult.instrumentSnapshot, finalResult.originalCalcSnapshot);
        }).start();

        // 立即通知前端测量完成
        JSONObject finishMessage = new JSONObject();
        finishMessage.put("result", "measurement_complete");
        finishMessage.put("sampleId", sampleId);
        finishMessage.put("count", count);

        if (session.isOpen()) {
            session.sendMessage(new TextMessage(finishMessage.toJSONString()));
        }
    }
    // doOneMeasurementCore 改造，返回 MeasurementResult
    private MeasurementResult doOneMeasurementCore(WebSocketSession session,
                                                   Command command,
                                                   FSpectrumParameter spectrumParameter,
                                                   FMeasureTask task,
                                                   String taskId,
                                                   String sampleId,
                                                   String deviceSerialNumber,
                                                   String ip,
                                                   Integer count) throws IOException {
        String luminosity = null;
        switch (spectrumParameter.getLuminosity()){
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
        devMessageUtil.getMeasureData(command, ip);
        Constants.startTime = LocalDateTime.now();
        measureStatus=true;
        List<ChangeParam> baselineCopy = (task.getZeroParam() != null && !task.getZeroParam().isEmpty())
                ? JSON.parseArray(task.getZeroParam(), ChangeParam.class)
                : null;
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        int pushCount = 0;

        // 判断是否为极快扫描模式
        boolean isFastScan = "3".equals(spectrumParameter.getScanSpeed());

        if (isFastScan) {
            // 极快扫描模式：优化版本
            ProcessedData processedData = processAndPushFastScanData(session, spectrumParameter, x, y, count,
                    task, sampleId, luminosity, spectrumParameter.getLadderCorrection());

            // 立即完成测量
            Constants.endTime = LocalDateTime.now();
            measureStatus = false;

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
            // 普通扫描模式：保持原来的逐点推送逻辑
            while (true) {
                if (Thread.currentThread().isInterrupted() || !session.isOpen()) return null;
                // 采集 & 计算一个点
                List<String> snapshot = new ArrayList<>(Constants.instrumentData);
                if (!snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData)) {
                    if (snapshot.get(0).equals(Constants.EndData)){
                        break;
                    }
                    while (!Constants.measurd_data.isEmpty()) {
                        calcOnePoint(spectrumParameter, baselineCopy, x, y);
                        pushCount = pushDataIfNeeded(session, x, y, pushCount, count, task, sampleId, spectrumParameter.getLuminosity(),spectrumParameter.getLadderCorrection());
                    }
                    break;
                }
                while (!Constants.measurd_data.isEmpty()) {
                    calcOnePoint(spectrumParameter, baselineCopy, x, y);
                    pushCount = pushDataIfNeeded(session, x, y, pushCount, count, task, sampleId, spectrumParameter.getLuminosity(),spectrumParameter.getLadderCorrection());
                }
                try { Thread.sleep(10); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            // 推送剩余数据
            pushRemainingData(session, x, y, pushCount, count, task , sampleId, spectrumParameter.getLuminosity(),spectrumParameter.getLadderCorrection());
        }

        Constants.endTime = LocalDateTime.now();
        measureStatus = false;
        // 返回快照 + x/y
        return new MeasurementResult(
                x,
                y,
                Constants.startTime,
                Constants.endTime,
                new ArrayList<>(Constants.instrumentData),
                new ArrayList<>(Constants.originalCalculationData)
        );
    }

    /**
     * 处理极快扫描模式的数据 - 修复版本
     */
    private ProcessedData processAndPushFastScanData(WebSocketSession session,
                                                     FSpectrumParameter spectrumParameter,
                                                     List<Double> x, List<Double> y,
                                                     Integer count, FMeasureTask task,
                                                     String sampleId, String luminosity,
                                                     boolean ladderCorrection) throws IOException {

        log.info("开始极快扫描模式数据处理");

        // 等待数据采集完成 - 修复等待逻辑
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
            // 1. 收到结束标志且数据点数达到预期
            // 2. 数据稳定且达到预期点数
            // 3. 数据稳定且长时间无变化
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
                Thread.sleep(50); // 增加等待时间，减少CPU占用
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
            if (calcOnePointForFastScan(spectrumParameter, baselineCopy, x, y, measurdDataCopy)) {
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
        ProcessedData processedData = repairWavelengthSequence(x, y, startWl, endWl, interval);

        List<Double> processedX = processedData.x;
        List<Double> processedY = processedData.y;

        log.info("波长序列修复完成，修复后点数: " + processedX.size());

        // 如果需要台阶校正
        if (ladderCorrection) {
            log.info("开始台阶校正");
            List<Double> correctedY = processAndSaveData(task, processedX, processedY,
                    luminosity, ladderCorrection, sampleId);
            processedY = correctedY;
            log.info("台阶校正完成");
        }

        // 存储原始Y值（用于后续存储到数据库）
        String key = session.getId() + "_" + sampleId + "_" + count;
        originalYValues.put(key, new ArrayList<>(processedY));

        // 一次性推送所有数据
        pushAllDataAtOnce(session, processedX, processedY, count, sampleId, luminosity);

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
                                            List<ChangeParam> measurdData) {
        if (measurdData.isEmpty()) return false;

        List<LinearSegment> linearSegment = formulaUtil.getLinearSegment(deviceSerialNumber);
        ChangeParam currentData = measurdData.get(0);

        try {
            if (baselineCopy != null && !baselineCopy.isEmpty()) {
                // 有基线校正的情况
                ChangeParam baselineData = baselineCopy.get(0);

                switch (spectrumParameter.getLuminosity()) {
                    case "1": // Abs
                        String s = DataCountUtil.countTPercent(baselineData, currentData, 1.0);
                        String yy = DataCountUtil.changeTPercentOrAbs2(null, s);
                        double parsedWl = Double.parseDouble(currentData.getWl());
                        Double correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);
                        x.add(correctedWl);
                        y.add(Double.parseDouble(yy));
                        break;
                    case "2": // T%
                        s = DataCountUtil.countTPercent(baselineData, currentData, 1.0);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);
                        x.add(correctedWl);
                        y.add(Double.parseDouble(s));
                        break;
                    case "3": // Es
                        Double sData = Double.valueOf(currentData.getS());
                        String esValue = String.valueOf((sData / 8388607) * 100);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);
                        x.add(correctedWl);
                        y.add(Double.parseDouble(esValue));
                        break;
                    case "4": // Er
                        Double rData = Double.valueOf(currentData.getR());
                        String erValue = String.valueOf((rData / 8388607) * 100);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);
                        x.add(correctedWl);
                        y.add(Double.parseDouble(erValue));
                        break;
                }
                baselineCopy.remove(0);
            } else {
                // 无基线校正的情况
                switch (spectrumParameter.getLuminosity()) {
                    case "1": // Abs
                        String s = DataCountUtil.countTPercent(null, currentData, 1.0);
                        String yy = DataCountUtil.changeTPercentOrAbs2(null, s);
                        double parsedWl = Double.parseDouble(currentData.getWl());
                        Double correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);
                        x.add(correctedWl);
                        y.add(Double.parseDouble(yy));
                        break;
                    case "2": // T%
                        s = DataCountUtil.countTPercent(null, currentData, 1.0);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);
                        x.add(correctedWl);
                        y.add(Double.parseDouble(s));
                        break;
                    case "3": // Es
                        Double sData = Double.valueOf(currentData.getS());
                        String esValue = String.valueOf((sData / 8388607) * 100);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);
                        x.add(correctedWl);
                        y.add(Double.parseDouble(esValue));
                        break;
                    case "4": // Er
                        Double rData = Double.valueOf(currentData.getR());
                        String erValue = String.valueOf((rData / 8388607) * 100);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);
                        x.add(correctedWl);
                        y.add(Double.parseDouble(erValue));
                        break;
                }
            }

            measurdData.remove(0);
            return true;
        } catch (Exception e) {
            log.error("计算数据点时发生异常: " + e.getMessage(), e);
            measurdData.remove(0); // 即使出错也移除数据，避免死循环
            return false;
        }
    }

    /**
     * 修复波长序列，确保数据完整性
     */
    private ProcessedData repairWavelengthSequence(List<Double> originalX, List<Double> originalY,
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
            double wavelength = roundToPrecision(originalX.get(i), 2);
            wavelengthMap.put(wavelength, originalY.get(i));
        }

        List<Double> repairedX = new ArrayList<>();
        List<Double> repairedY = new ArrayList<>();

        // 使用线性插值填充缺失的波长点
        int matchedCount = 0;
        int interpolatedCount = 0;

        for (Double expectedWl : expectedWavelengths) {
            if (wavelengthMap.containsKey(expectedWl)) {
                // 直接使用已有的数据点
                repairedX.add(expectedWl);
                repairedY.add(wavelengthMap.get(expectedWl));
                matchedCount++;
            } else {
                // 线性插值填充缺失点
                Double interpolatedValue = linearInterpolate(expectedWl, wavelengthMap, isDescending);
                if (interpolatedValue != null) {
                    repairedX.add(expectedWl);
                    repairedY.add(interpolatedValue);
                    interpolatedCount++;
                }
            }
        }

        log.info("波长序列修复完成: 匹配点=" + matchedCount + ", 插值点=" + interpolatedCount + ", 总数=" + repairedX.size());
        log.info("修复后的波长范围: " + (repairedX.isEmpty() ? "无数据" : repairedX.get(0) + " - " + repairedX.get(repairedX.size() - 1)));

        return new ProcessedData(repairedX, repairedY);
    }

    /**
     * 生成完整的波长序列
     */
    private List<Double> generateCompleteWavelengthSequence(double startWl, double endWl,
                                                            double interval, boolean isDescending) {
        List<Double> wavelengths = new ArrayList<>();

        if (isDescending) {
            for (double wl = startWl; wl >= endWl; wl -= interval) {
                wavelengths.add(roundToPrecision(wl, 2));
            }
        } else {
            for (double wl = startWl; wl <= endWl; wl += interval) {
                wavelengths.add(roundToPrecision(wl, 2));
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
     * 一次性推送所有数据（推送时格式化Y值）- 修正版本
     * 使用修正后的规整化波长值
     */
    private void pushAllDataAtOnce(WebSocketSession session,
                                   List<Double> x, List<Double> y,
                                   Integer count, String sampleId,
                                   String luminosity) throws IOException {
        if (x.isEmpty() || y.isEmpty()) {
            log.warn("没有数据可推送，x大小: " + x.size() + ", y大小: " + y.size());
            return;
        }

        JSONArray dataArray = new JSONArray();

        for (int i = 0; i < x.size(); i++) {
            JSONObject point = new JSONObject();

            // 使用修正后的规整化波长值
            double wavelength = x.get(i);
            point.put("x", wavelength);

            // 推送时：根据指定位数格式化Y值
            Double value = DataCountUtil.formatValue(digit, y.get(i));
            String valueY = DataCountUtil.addZero(value, digit);
            point.put("y", valueY);

            point.put("xType", "nm");

            // 光度模式
            String luminosityType = luminosity.equals("1") ? "Abs" :
                    luminosity.equals("2") ? "T%" :
                            luminosity.equals("3") ? "Es" : "Er";
            point.put("yType", luminosityType);

            dataArray.add(point);
        }

        JSONObject payload = new JSONObject();
        payload.put("data", dataArray);
        payload.put("sampleId", sampleId);
        payload.put("count", count);
        payload.put("scanMode", "fast");

        // 调试信息
        log.info("推送数据信息:");
        log.info("数据点数: " + dataArray.size());
        if (!x.isEmpty()) {
            log.info("修正后的波长范围: " + x.get(0) + " - " + x.get(x.size() - 1));
            if (x.size() > 1) {
                log.info("修正后的波长间隔: " + Math.abs(x.get(1) - x.get(0)));
            }
        }

        if (session.isOpen()) {
            session.sendMessage(new TextMessage(payload.toJSONString()));
            log.info("数据推送完成");
        } else {
            log.warn("会话已关闭，无法推送数据");
        }
    }

    // 推送逻辑：每 10 个点推送一次（推送时格式化Y值）
    private int pushDataIfNeeded(WebSocketSession session,
                                 List<Double> x,
                                 List<Double> y,
                                 int pushCount,
                                 Integer count,
                                 FMeasureTask task,
                                 String sampleId,
                                 String luminosity,
                                 boolean ladderCorrection) throws IOException {
        if (x.size() >= (pushCount + 1) * 10) {
            List<Double> x_data = new ArrayList<>();
            List<Double> y_data = new ArrayList<>();
            for (int i = pushCount * 10; i < x.size(); i++) {
                x_data.add(x.get(i));
                y_data.add(y.get(i));
            }

            // 如果需要台阶校正，处理数据
            if (ladderCorrection){
                List<Double> correctedY = processAndSaveData(task, x_data, y_data, luminosity, ladderCorrection, sampleId);
                // 更新y值
                for (int i = pushCount * 10, j = 0; i < x.size(); i++, j++) {
                    y.set(i, correctedY.get(j));
                }
            }

            // 存储原始Y值（用于后续存储到数据库）
            String key = session.getId() + "_" + sampleId + "_" + count;
            if (!originalYValues.containsKey(key)) {
                originalYValues.put(key, new ArrayList<>(y));
            } else {
                // 更新已存在的原始Y值
                List<Double> existingY = originalYValues.get(key);
                for (int i = pushCount * 10, j = 0; i < x.size(); i++, j++) {
                    if (i < existingY.size()) {
                        existingY.set(i, y.get(i));
                    } else {
                        existingY.add(y.get(i));
                    }
                }
            }

            JSONArray dataArray = new JSONArray();
            for (int i = pushCount * 10; i < x.size(); i++) {
                JSONObject point = new JSONObject();
                point.put("x", x.get(i));

                // 推送时：根据指定位数格式化Y值
                Double value = DataCountUtil.formatValue(digit, y.get(i));
                String valueY = DataCountUtil.addZero(value, digit);
                point.put("y", valueY);

                point.put("xType", "nm");
                // 光度模式是1.Abs 2.T% 3.Es 4.Er
                String luminosityType = luminosity.equals("1")?"Abs":luminosity.equals("2")?"T%":luminosity.equals("3")?"Es":"Er";
                point.put("yType", luminosityType);
                dataArray.add(point);
            }
            JSONObject payload = new JSONObject();
            payload.put("data", dataArray);
            payload.put("sampleId", sampleId);
            payload.put("count", count);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload.toJSONString()));
            }
            pushCount++;
        }
        return pushCount;
    }

    // 推送剩余数据（最后不足10条的情况）（推送时格式化Y值）
    private void pushRemainingData(WebSocketSession session,
                                   List<Double> x,
                                   List<Double> y,
                                   int pushCount,
                                   Integer count,
                                   FMeasureTask task,
                                   String sampleId,
                                   String luminosity,
                                   boolean ladderCorrection) throws IOException {
        int startIndex = pushCount * 10;
        int surplusLength = x.size() - startIndex;
        if (startIndex < x.size()) {
            JSONArray dataArray = new JSONArray();
            List<Double> x_data = new ArrayList<>();
            List<Double> y_data = new ArrayList<>();
            // 收集需要处理的数据段
            for (int i = startIndex; i < x.size(); i++) {
                x_data.add(x.get(i));
                y_data.add(y.get(i));
            }

            // 如果需要台阶校正
            if (ladderCorrection) {
                List<Double> correctedY = processAndSaveData(task, x_data, y_data, luminosity, ladderCorrection, sampleId);
                // 更新y值
                for (int i = 0; i < correctedY.size(); i++) {
                    y.set(startIndex + i, correctedY.get(i));
                }
            }

            // 存储原始Y值（用于后续存储到数据库）
            String key = session.getId() + "_" + sampleId + "_" + count;
            if (!originalYValues.containsKey(key)) {
                originalYValues.put(key, new ArrayList<>(y));
            } else {
                // 更新已存在的原始Y值
                List<Double> existingY = originalYValues.get(key);
                for (int i = startIndex, j = 0; i < x.size(); i++, j++) {
                    if (i < existingY.size()) {
                        existingY.set(i, y.get(i));
                    } else {
                        existingY.add(y.get(i));
                    }
                }
            }

            // 构建推送数据
            for (int i = 0; i < surplusLength; i++) {
                JSONObject point = new JSONObject();
                point.put("x", x.get(startIndex + i));

                // 推送时：根据指定位数格式化Y值
                Double value = DataCountUtil.formatValue(digit, y.get(startIndex + i));
                String valueY = DataCountUtil.addZero(value, digit);
                point.put("y", valueY);

                point.put("xType", "nm");
                // 光度模式是1.Abs 2.T% 3.Es 4.Er
                String luminosityType = luminosity.equals("1") ? "Abs" : luminosity.equals("2") ? "T%" : luminosity.equals("3") ? "Es" : "Er";
                point.put("yType", luminosityType);
                dataArray.add(point);
            }
            JSONObject payload = new JSONObject();
            payload.put("data", dataArray);
            payload.put("sampleId", sampleId);
            payload.put("count", count);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload.toJSONString()));
            }
        }
    }

    /**
     * 处理数据并保存
     * @param task
     * @param x
     * @param y
     * @param luminosityMode
     */
    public List<Double> processAndSaveData(FMeasureTask task,
                                           List<Double> x,
                                           List<Double> y,
                                           String luminosityMode,
                                           boolean ladderCorrection,
                                           String sampleId) {
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        System.err.println("+++++++++++++++++++++++++++++++++++进入推送存储逻辑");

        // 如果不需要台阶校正，直接返回原始数据
        if (!ladderCorrection) {
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
            log.warn("门限系数为null，使用默认值100");
        }

        System.err.println("+++++++++++++++++++++++++++++++++++台阶消除门限系数: " + threshold);

        // 这里调用 Python 的台阶消除算法，返回处理后的 y 值
        List<Double> processedY = callStepEliminationPython(x, y, threshold);
        return processedY;
    }

    /**
     * 调用 Python 服务处理台阶消除
     * @param x
     * @param y
     * @param threshold
     * @return
     */
    private List<Double> callStepEliminationPython(List<Double> x, List<Double> y, Integer threshold) {
        System.err.println("+++++++++++++++++++++++++++++++++++进入台阶消除逻辑");

        // 确保threshold不为null
        if (threshold == null) {
            threshold = 100;
            log.warn("调用Python服务时门限系数为null，使用默认值100");
        }

        try {
            // 这里调用 Python 服务处理数据，消除台阶
            // 假设 Python 服务返回的结果是处理后的 y 数组
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("x_data", x); // 传入 x 数组
            requestBody.put("y_data", y); // 传入 y 数组
            requestBody.put("k", threshold); // 传入门限系数，确保不为null

            System.err.println("+++++++++++++++++++++++++++++++++++调用Python服务参数: " + requestBody);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 调用 Python API
            ResponseEntity<Map> response = restTemplate.postForEntity(PythonPathConstant.REMOVE_STEP_OR_SPIKES, request, Map.class);

            // 从响应中获取处理后的 y 值
            List<Double> processedY = (List<Double>) response.getBody().get("smoothed_data");

            System.err.println("+++++++++++++++++++++++++++++++++++Python服务返回数据长度: " + (processedY != null ? processedY.size() : "null"));

            return processedY;
        } catch (Exception e) {
            log.error("调用Python台阶消除服务失败，返回原始数据", e);
            // 如果调用失败，返回原始数据
            return y;
        }
    }

    /**
     * 计算光度模式的Abs/T%
     *
     * @param spectrumParameter
     * @param baselineCopy
     * @param x
     * @param y
     */
    private void calcOnePoint(FSpectrumParameter spectrumParameter,
                              List<ChangeParam> baselineCopy,
                              List<Double> x,
                              List<Double> y) {
        List<LinearSegment> linearSegment = formulaUtil.getLinearSegment(deviceSerialNumber);
        if (baselineCopy != null && !baselineCopy.isEmpty()) {
            if (spectrumParameter.getLuminosity().equals("1")) {
                String s = DataCountUtil.countTPercent(baselineCopy.get(0), Constants.measurd_data.get(0), 1.0);
                String yy = DataCountUtil.changeTPercentOrAbs2(null,s);
                Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);
                Constants.originalCalculationData.add(v + "," + yy);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("2")) {
                String s = DataCountUtil.countTPercent(baselineCopy.get(0), Constants.measurd_data.get(0), 1.0);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);
                Constants.originalCalculationData.add(v + "," + s);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(s));
            } else if (spectrumParameter.getLuminosity().equals("3")) {
                Double sData = Double.valueOf(Constants.measurd_data.get(0).getS());
                System.out.println("----sData:" + sData);
                String yy = String.valueOf((sData / 8388607) * 100);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);
                Constants.originalCalculationData.add(v + "," + yy);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("4")) {
                Double rData = Double.valueOf(Constants.measurd_data.get(0).getR());
                System.out.println("----rData:" + rData);
                String yy = String.valueOf((rData / 8388607) * 100);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);
                Constants.originalCalculationData.add(v + "," + yy);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(yy));
            }
            Constants.measurd_data.remove(0);
            baselineCopy.remove(0);
        } else {
            if (spectrumParameter.getLuminosity().equals("1")) {
                String s = DataCountUtil.countTPercent(null, Constants.measurd_data.get(0), 1.0);
                String yy = DataCountUtil.changeTPercentOrAbs2(null, s);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);
                Constants.originalCalculationData.add(v + "," + yy);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("2")) {
                String s = DataCountUtil.countTPercent(null, Constants.measurd_data.get(0), 1.0);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);
                Constants.originalCalculationData.add(v + "," + s);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(s));
            } else if (spectrumParameter.getLuminosity().equals("3")) {
                Double sData = Double.valueOf(Constants.measurd_data.get(0).getS());
                String yy = String.valueOf((sData / 8388607) * 100);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);
                Constants.originalCalculationData.add(v + "," + yy);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("4")) {
                Double rData = Double.valueOf(Constants.measurd_data.get(0).getR());
                String yy = String.valueOf((rData / 8388607) * 100);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);
                Constants.originalCalculationData.add(v + "," + yy);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(yy));
            }
            Constants.measurd_data.remove(0);
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
                                                 WebSocketSession session,
                                                 Integer number,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime,
                                                 List<String> instrumentSnapshot,
                                                 List<String> originalCalcSnapshot) {
        measureStatus=false;
        if (instrumentSnapshot == null || originalCalcSnapshot == null) {
            log.warn("测量数据为空，无法保存");
            return;
        }

        try {
            SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
            // 1. 处理峰谷数据（假设你原来调用了 Python 接口）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();

            // 使用原始Y值进行峰谷分析（而不是格式化后的值）
            String key = session.getId() + "_" + sampleId + "_" + number;
            List<Double> originalY = originalYValues.get(key);
            if (originalY == null) {
                // 如果没有存储原始Y值，使用当前Y值（这些是原始计算值）
                originalY = y;
            }

            body.put("x_data", x);
            body.put("y_data", originalY); // 使用原始Y值进行峰谷分析
            double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
            body.put("height", height);
            body.put("distance", confVO.getPeakInterval());
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
            Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request, Map.class).getBody();
            List<Double> xDataFeng = (List<Double>) resultMap.get("peak_x");
            List<String> yDataFeng = (List<String>) resultMap.get("peak_y");
            List<Double> xDataGu = (List<Double>) resultMap1.get("valley_x");
            List<String> yDataGu = (List<String>) resultMap1.get("valley_y");
            List<FPeakValleyMarking> peakValleys = new ArrayList<>();
            List<FPeakValleyMarking> markings = new ArrayList<>();
            for (int i = 0; i < xDataFeng.size(); i++) {
                FPeakValleyMarking marking = new FPeakValleyMarking();
                marking.setX(xDataFeng.get(i).toString());
                marking.setY(yDataFeng.get(i));
                marking.setType("波峰");
                peakValleys.add(marking);
            }
            for (int i = 0; i < xDataGu.size(); i++) {
                FPeakValleyMarking marking = new FPeakValleyMarking();
                marking.setX(xDataGu.get(i).toString());
                marking.setY(yDataGu.get(i));
                marking.setType("波谷");
                markings.add(marking);
            }

            // 构建结果字符串，存储时使用原始Y值（不进行位数格式化）
            List<String> result = new ArrayList<>();
            for (int i = 0; i < x.size(); i++) {
                // 存储时：使用原始Y值（不进行位数格式化，保留完整精度）
                double originalYValue = originalY != null && i < originalY.size() ? originalY.get(i) : y.get(i);
                // 直接使用Double.toString()保持完整精度，或者使用科学计数法避免精度损失
                result.add(x.get(i) + "," + originalYValue);
            }

            // 2. 保存测量数据
            List<String> listData = new ArrayList<>(originalCalcSnapshot); // 使用快照
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
            if ("不重复".equals(spectrumParameter.getRepetitionPattern())) {
                // 普通模式
                FMeasureData measureData = new FMeasureData();
                measureData.setSerialNumber(1);
                measureData.setTaskId(taskId);
                measureData.setSampleKey(sampleId);
                measureData.setInstrumentData(JSON.toJSONString(instrumentSnapshot)); // 使用快照
                measureData.setOriginalCalculationData(JSON.toJSONString(result));
                measureData.setMeasurementStartTime(startTime);
                measureData.setMeasurementEndTime(endTime);
                measureData.setIsDelete("0");
                measureData.setIsSave("1");
                traceDataService.save(measureData);
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
                tMeasureAnalysisDataService.save(analysisData);
                FMeasureAnalysisImage tMeasureAnalysisImage = new FMeasureAnalysisImage();
                tMeasureAnalysisImage.setAnalysisId(analysisData.getId());
                tMeasureAnalysisImageService.save(tMeasureAnalysisImage);
            } else {
                // 重复模式
                FMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(
                        new QueryWrapper<FMeasureAnalysisData>()
                                .eq("task_id", taskId)
                                .eq("sample_key", sampleId)
                                .eq("analysis_number", number)
                );
                if (analysisData != null && analysisData.getAnalysisResult() == null) {
                    // 更新已有分析数据
                    FMeasureData measureData = tMeasureDataService.getOne(
                            new QueryWrapper<FMeasureData>()
                                    .eq("task_id", taskId)
                                    .eq("sample_key", sampleId)
                                    .eq("serial_number", number)
                    );
                    measureData.setInstrumentData(JSON.toJSONString(instrumentSnapshot));
                    measureData.setOriginalCalculationData(JSON.toJSONString(result));
                    measureData.setMeasurementStartTime(startTime);
                    measureData.setMeasurementEndTime(endTime);
                    measureData.setIsSave("1");
                    traceDataService.updateById(measureData);
                    analysisData.setAnalysisParam(JSON.toJSONString(instrumentSnapshot));
                    analysisData.setAnalysisResult(JSON.toJSONString(result));
                    analysisData.setPeak(JSON.toJSONString(peakValleys));
                    analysisData.setGrain(JSON.toJSONString(markings));
                    analysisData.setColor("#000000");
                    analysisData.setIsSave("1");
                    tMeasureAnalysisDataService.updateById(analysisData);
                } else {
                    // 新增分析数据
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
                    traceDataService.save(measureData);
                    FMeasureAnalysisData analysis = new FMeasureAnalysisData();
                    analysis.setAnalysisNumber(number);
                    analysis.setAnalysisName(sample.getSampleName() + "-" + number);
                    analysis.setTaskId(taskId);
                    analysis.setSampleKey(sampleId);
                    analysis.setMeasureDataId(measureData.getId());
                    analysis.setAnalysisParam(JSON.toJSONString(instrumentSnapshot));
                    analysis.setAnalysisResult(JSON.toJSONString(result));
                    analysis.setAnalysisType("5");
                    analysis.setPeak(JSON.toJSONString(peakValleys));
                    analysis.setGrain(JSON.toJSONString(markings));
                    analysis.setColor("#000000");
                    analysis.setIsDelete("0");
                    analysis.setIsSave("1");
                    tMeasureAnalysisDataService.save(analysis);
                    FMeasureAnalysisImage tMeasureAnalysisImage = new FMeasureAnalysisImage();
                    tMeasureAnalysisImage.setAnalysisId(analysis.getId());
                    tMeasureAnalysisImageService.save(tMeasureAnalysisImage);
                }
            }
            /// 更新任务状态
            List<FTaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            // 如果不是重复模式的情况直接更新任务状态
            if(spectrumParameter.getRepetitionPattern().equals("不重复")){
                for (FTaskSpecimen taskSample : specimen) {
                    if (taskSample.getSpecimenKey().equals(sampleId)) {
                        taskSample.setIsTested("1");
                    }
                }
                task.setSampleKey(JSON.toJSONString(specimen));
                task.setDeviceSerialNumber(deviceSerialNumber);
                boolean update = tMeasureTaskService.updateById(task);
                if (!update) {
                    log.error("更新任务失败: " + taskId);
                }
            }else{
                // 如果是重复模式的情况，需要判断是否全部测完
                // 根据重复次数遍历
                boolean success = false;
                Integer repetitionCount = Integer.valueOf(spectrumParameter.getRepetitionTime());
                for (int i = 0; i < repetitionCount; i++) {
                    // 根据任务id,样品id和编号查询分析数据
                    QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("task_id", taskId);
                    queryWrapper.eq("sample_key", sampleId);
                    queryWrapper.eq("analysis_number", i+1);
                    queryWrapper.eq("analysis_type", "5");
                    queryWrapper.last("limit 1");
                    FMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(queryWrapper);
                    // 判断数据是否不为空或者不为null
                    if(analysisData==null||analysisData.getAnalysisResult()==null){
                        success=true;
                        break;
                    }
                }
                if (success){
                    // 不更新任务状态
                }else{
                    for (FTaskSpecimen taskSample : specimen) {
                        if (taskSample.getSpecimenKey().equals(sampleId)) {
                            taskSample.setIsTested("1");
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

            log.info("数据保存完成: 样品=" + sampleId + ", 计数=" + number);
        } catch (Exception e) {
            log.error("保存测量数据时发生异常", e);
        } finally {
            // 清理存储的原始Y值
            String key = session.getId() + "_" + sampleId + "_" + number;
            originalYValues.remove(key);
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
        } catch (IOException e) {
            handleTransportError(session, e);
            Thread.currentThread().interrupt();
            return;
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
            // 释放线程
            deviceInfoService.releaseDevice(deviceSerialNumber);
            session.sendMessage(new TextMessage(msg));
            sessions.remove(session.getId());
            session.close();
        } catch (IOException e) {
            handleTransportError(session, e);
            Thread.currentThread().interrupt();
            return;
        }
    }
    /**
     * 全局数据清空
     */
    private void exceptionHandling() {
        Constants.x_data.clear();
        Constants.y_data.clear();
        Constants.field3Values.clear();
        Constants.instrumentData.clear();
        Constants.originalCalculationData.clear();
        Constants.startTime = null;
        Constants.endTime = null;
        Constants.sendMeasureThread = null;
    }
    // 返回测量结果的类
    class MeasurementResult {
        List<Double> x;
        List<Double> y;
        LocalDateTime startTime;
        LocalDateTime endTime;
        List<String> instrumentSnapshot;
        List<String> originalCalcSnapshot;
        MeasurementResult(List<Double> x,
                          List<Double> y,
                          LocalDateTime startTime,
                          LocalDateTime endTime,
                          List<String> instrumentSnapshot,
                          List<String> originalCalcSnapshot) {
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

    /**
     * 构建命令
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
}