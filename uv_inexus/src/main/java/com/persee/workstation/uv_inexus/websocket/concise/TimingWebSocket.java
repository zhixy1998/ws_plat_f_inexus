
/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
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
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
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
public class TimingWebSocket extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(TimingWebSocket.class);

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();

    private String ip;
    private String deviceSerialNumber;
    private String analysisId = null;
    private String measureId = null;
    private boolean occupancyStatus = false;
    private boolean measureStatus = false;
    private boolean isFirstSave = false;  // 标记是否为第一次存储数据
    private List<Double> Xdata = new ArrayList<>();
    private List<Double> Ydata = new ArrayList<>();


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

    /**
     * 当WebSocket连接建立后调用此方法
     *
     * @param session 当前WebSocket会话，包含连接信息
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        occupancyStatus = false;
        measureStatus = false;
        isFirstSave = true;
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

        Integer repetitionTime = Integer.valueOf(spectrumParameter.getRepetitionTime());
        Integer timeInterval = Integer.valueOf(spectrumParameter.getTimeInterval());
        boolean repeatMode = "单池重复".equalsIgnoreCase(spectrumParameter.getRepetitionPattern());
        int repeatCount = repetitionTime != null ? repetitionTime : 1;
        long repeatInterval = timeInterval != null ? timeInterval : 0;
        ip = deviceInfoService.getIp(deviceSerialNumber);
        Command command = new Command();
        command.setCmdId(SetParamEnum.Scan.getComId());
        command.setParams(new Object[]{});
        // 往前端推送数据的线程
        String finalMaxLocation = maxLocation;
        boolean finalPoolStatus = poolStatus;
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

                        doOneMeasurementSerial(session, command, spectrumParameter, task, taskId, newSampleData.get(i).getSpecimenKey(), deviceSerialNumber, ip, 1);
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
                            }
                            R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
                            if (!stringR.getSuccess()) {
                                sendAndClose(session, "设置样品池出错");
                                return;
                            }
                        }


                        for (int r = 0; r < repetitionTime; r++) {
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
                            doOneMeasurementSerial(session, command, spectrumParameter, task, taskId, newSampleData.get(i).getSpecimenKey(), deviceSerialNumber, ip, r + 1);

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
            // 如果线程正在被使用
            if (occupancyStatus){
                // 释放线程
                deviceInfoService.releaseDevice(deviceSerialNumber);
            }
            if (measureStatus){
                try {
                    // 强制停止基线/测量的线程
                    Constants.sendMeasureThread.interrupt();
                    R<String> stop = devMessageUtil.stop(buildCommand(SetParamEnum.Stop, new Object[]{}), ip);
                    if (stop.getSuccess()){
                        log.info("停止成功");
                    }else {
                        log.info("停止失败");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            Thread thread = taskThreads.get(session.getId());
            // 强制停止线程
            thread.interrupt();
            taskThreads.remove(session.getId());
            session.sendMessage(new TextMessage("停止成功"));
            sessions.remove(session.getId());
            session.close();
        }
    }

    /**
     * 重写afterConnectionClosed方法，用于处理WebSocket连接关闭后的操作
     *
     * @param session WebSocket会话对象，包含连接的相关信息
     * @param status  关闭状态，包含连接关闭的原因和代码
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 打印客户端退出信息，包括会话ID、退出时间和关闭原因
        System.out.println("客户端退出: " + session.getId() + " 时间: " + LocalDateTime.now() + " 原因: " + status);
        if (Constants.sendMeasureThread != null){
            Constants.sendMeasureThread.interrupt();
        }
        // 释放设备
        deviceInfoService.releaseDevice(deviceSerialNumber);
        Thread taskThread = taskThreads.remove(session.getId());
        if (taskThread != null) {
            taskThread.interrupt(); // 直接打断子线程
        }
        sessions.remove(session.getId());
        exceptionHandling(); // 清空公共数据
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
            deviceInfoService.releaseDevice(deviceSerialNumber);
        } catch (IOException e) {
            handleTransportError(session, e);
            Thread.currentThread().interrupt();
            return;
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


    // doOneMeasurementSerial 改造
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

        } finally {
            // 出锁前先生成快照
            if (result == null) {
                result = new MeasurementResult(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        new ArrayList<>(Constants.instrumentData),
                        new ArrayList<>(Constants.originalCalculationData)
                );
            }
            Constants.instrumentData.clear();
            Constants.measurd_data.clear();
            Constants.originalCalculationData.clear();
            Constants.startTime = null;
            Constants.endTime = null;
            Constants.MEASURE_LOCK.unlock();
        }

        // 出锁后保存
        saveOneMeasurementWithSnapshots(task, taskId, sampleId, deviceSerialNumber,
                result.x, result.y, session, count,
                result.instrumentSnapshot, result.originalCalcSnapshot);
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

        // 发送测量命令
        devMessageUtil.getMeasureData(command, ip);
        measureStatus=true;

        List<ChangeParam> baselineCopy = (task.getZeroParam() != null && !task.getZeroParam().isEmpty())
                ? JSON.parseArray(task.getZeroParam(), ChangeParam.class)
                : null;

        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();

        int pushCount = 0;

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
                pushCount = pushDataIfNeeded(session, x, y, pushCount, count, task , sampleId, spectrumParameter.getLuminosity(),spectrumParameter.getLadderCorrection());
            }

            try { Thread.sleep(10); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        // 推送剩余数据
        pushRemainingData(session, x, y, pushCount, count, task , sampleId, spectrumParameter.getLuminosity(),spectrumParameter.getLadderCorrection());

        // 返回快照 + x/y
        return new MeasurementResult(
                x,
                y,
                new ArrayList<>(Constants.instrumentData),
                new ArrayList<>(Constants.originalCalculationData)
        );
    }



    // 推送逻辑：每 10 个点推送一次
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
            JSONArray dataArray = new JSONArray();
            List<Double> x_data = new ArrayList<>();
            List<Double> y_data = new ArrayList<>();
            for (int i = pushCount * 10; i < x.size(); i++) {
                x_data.add(x.get(i));
                y_data.add(y.get(i));
            }
            processAndSaveData(task, x_data, y_data, luminosity,ladderCorrection,sampleId);
            for (int i = 0; i < 10; i++) {
                JSONObject point = new JSONObject();
                point.put("x", x_data.get(i));
                point.put("y", y_data.get(i));
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

    // 推送剩余数据（最后不足10条的情况）
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
            for (int i = startIndex; i < x.size(); i++) {
                x_data.add(x.get(i));
                y_data.add(y.get(i));
            }
            processAndSaveData(task, x_data, y_data, luminosity,ladderCorrection,sampleId);
            for (int i = 0; i < surplusLength; i++) {
                JSONObject point = new JSONObject();
                point.put("x", x_data.get(i));
                point.put("y", y_data.get(i));
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
    public void processAndSaveData(FMeasureTask task,
                                   List<Double> x,
                                   List<Double> y,
                                   String luminosityMode,
                                   boolean ladderCorrection,
                                   String sampleId) {
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        Integer threshold = null;
        if (ladderCorrection){
            /// 预留是否打开积分球的门限系数的校验
            if (true){
                // 判断光度模式
                if ("1".equals(luminosityMode)) {
                    // Abs的台阶消除数值
                    threshold = Integer.valueOf(confVO.getAbs());
                }
                if ("2".equals(luminosityMode)){
                    // 透过率的台阶消除数值
                    threshold = Integer.valueOf(confVO.getTransmittance());
                }
            }else{
                /// 积分球的门限系数
                threshold = Integer.valueOf(confVO.getIntegratingSphere());
            }
            // 这里调用 Python 的台阶消除算法，返回处理后的 y 值
            List<Double> processedY = callStepEliminationPython(x, y, threshold);
            y = processedY;  // 更新 y 数组
        }
        // 保存结果
        saveResultToDatabase(task, x, y,sampleId);
    }

    /**
     * 调用 Python 服务处理台阶消除
     * @param x
     * @param y
     * @param threshold
     * @return
     */
    private List<Double> callStepEliminationPython(List<Double> x, List<Double> y, Integer threshold) {
        // 这里调用 Python 服务处理数据，消除台阶
        // 假设 Python 服务返回的结果是处理后的 y 数组
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("x_data", x);  // 传入 x 数组
        requestBody.put("y_data", y);  // 传入 y 数组
        requestBody.put("k", threshold);  // 传入门限系数

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // 调用 Python API，假设路径是 "/api/eliminateStep"
        ResponseEntity<Map> response = restTemplate.postForEntity(PythonPathConstant.REMOVE_STEP_OR_SPIKES, request, Map.class);

        // 从响应中获取处理后的 y 值
        List<Double> processedY = (List<Double>) response.getBody().get("processed_y");
        return processedY;
    }


    /**
     * 保存结果到数据库
     * @param task
     * @param x
     * @param y
     * @param sampleId
     */
    private void saveResultToDatabase(FMeasureTask task,
                                      List<Double> x,
                                      List<Double> y,
                                      String sampleId) {
        // 根据 task 的信息和样品 ID 查找数据库中是否已有数据
        FMeasureData existingData = tMeasureDataService.getOne(new QueryWrapper<FMeasureData>()
                .eq("task_id", task.getId())
                .eq("sample_key", sampleId)
                .orderByDesc("serial_number")
                .last("LIMIT 1"));

        FMeasureData measureData = new FMeasureData();
        measureData.setTaskId(task.getId());
        measureData.setSampleKey(sampleId);
        measureData.setInstrumentData(JSON.toJSONString(x));  // 存储 x 数据
        measureData.setOriginalCalculationData(JSON.toJSONString(y));  // 存储处理后的 y 数据
        measureData.setIsDelete("0");
        measureData.setIsSave("1");

        if (existingData != null && isFirstSave) {
            // 如果是第一次存储，直接更新现有记录而不删除
            measureData.setSerialNumber(existingData.getSerialNumber());  // 保持原有序列号
            tMeasureDataService.updateById(measureData);  // 更新现有记录

            // 更新标记，后续就不再覆盖数据
            isFirstSave = false;
        } else if (existingData != null) {
            // 如果已有数据并且不是第一次存储，则追加数据
            String existingOriginalData = existingData.getOriginalCalculationData();
            List<String> existingYData = JSON.parseArray(existingOriginalData, String.class);  // 解析为 List<String>

            // 将每个 x 和 y 值以逗号为分隔符进行合并，然后进行追加
            for (int i = 0; i < x.size(); i++) {
                String combinedData = x.get(i) + "," + y.get(i);
                existingYData.add(combinedData);
            }

            // 更新 measureData 的 originalCalculationData 为合并后的数据
            measureData.setOriginalCalculationData(JSON.toJSONString(existingYData));  // 更新为追加后的数据
            // 保存数据到 FMeasureData 表
            tMeasureDataService.updateById(existingData);  // 更新现有数据
        } else {
            // 如果没有数据，则创建新记录
            measureData.setSerialNumber(1);  // 新记录序列号为 1
            tMeasureDataService.save(measureData);  // 保存新记录
        }

        // 更新 FMeasureAnalysisData 表，确保分析数据表同步
        FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
        analysisData.setTaskId(task.getId());
        analysisData.setSampleKey(sampleId);
        analysisData.setMeasureDataId(measureData.getId());  // 设置关联的测量数据 ID
        analysisData.setAnalysisParam(JSON.toJSONString(Constants.instrumentData));
        analysisData.setAnalysisResult(JSON.toJSONString(measureData.getOriginalCalculationData()));  // 分析结果是处理后的 y 数据
        analysisData.setColor("#000000"); // 设置颜色
        analysisData.setAnalysisType("5"); // 设置分析类型
        analysisData.setIsDelete("0");
        analysisData.setIsSave("0");

        // 查找是否已存在分析数据
        FMeasureAnalysisData existingAnalysisData = tMeasureAnalysisDataService.getOne(new QueryWrapper<FMeasureAnalysisData>()
                .eq("task_id", task.getId())
                .eq("sample_key", sampleId)
                .orderByDesc("analysis_number")
                .last("LIMIT 1"));

        if (existingAnalysisData != null) {
            // 如果已有分析数据，则进行数据更新
            existingAnalysisData.setAnalysisResult(JSON.toJSONString(measureData.getOriginalCalculationData()));  // 更新分析结果
            tMeasureAnalysisDataService.updateById(existingAnalysisData);  // 更新分析数据
            analysisId = existingAnalysisData.getId();  // 更新 analysisId
        } else {
            // 如果没有分析数据，则创建新记录
            analysisData.setAnalysisNumber(1);  // 新记录序列号为 1
            tMeasureAnalysisDataService.save(analysisData);  // 保存新记录
            analysisId = analysisData.getId();  // 保存分析数据的 ID
        }

        // 检查是否有对应的分析数据图片，如果没有就新增
        FMeasureAnalysisImage existingImage = tMeasureAnalysisImageService.getOne(new QueryWrapper<FMeasureAnalysisImage>()
                .eq("analysis_id", analysisId));  // 根据 analysis_id 查找是否已经存在

        if (existingImage == null) {
            // 如果没有分析图片记录，则新增一条记录
            FMeasureAnalysisImage analysisImage = new FMeasureAnalysisImage();
            analysisImage.setAnalysisId(analysisId);  // 设置关联的分析数据 ID
            tMeasureAnalysisImageService.save(analysisImage);  // 保存分析图片记录
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
        if (baselineCopy != null && !baselineCopy.isEmpty()) {
            if (spectrumParameter.getLuminosity().equals("1")) {
                String s = DataCountUtil.countTPercent(baselineCopy.get(0), Constants.measurd_data.get(0), 1.0);
                String yy = DataCountUtil.changeTPercentOrAbs2(null,s);
                Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("2")) {
                String s = DataCountUtil.countTPercent(baselineCopy.get(0), Constants.measurd_data.get(0), 1.0);
                Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + s);
                x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
                y.add(Double.parseDouble(s));
            } else if (spectrumParameter.getLuminosity().equals("3")) {
                Double sData = Double.valueOf(Constants.measurd_data.get(0).getS());
                System.out.println("----sData:" + sData);
                String yy = String.valueOf((sData / 8388607) * 100);
                Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("4")) {
                Double rData = Double.valueOf(Constants.measurd_data.get(0).getR());
                System.out.println("----rData:" + rData);
                String yy = String.valueOf((rData / 8388607) * 100);
                Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            }
            Constants.measurd_data.remove(0);
            baselineCopy.remove(0);
        } else {
            if (spectrumParameter.getLuminosity().equals("1")) {
                String s = DataCountUtil.countTPercent(null, Constants.measurd_data.get(0), 1.0);
                String yy = DataCountUtil.changeTPercentOrAbs2(null, s);
                Constants.originalCalculationData.add(Constants.measurd_data.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(Constants.measurd_data.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("2")) {
                String s = DataCountUtil.countTPercent(null, Constants.measurd_data.get(0), 1.0);
                String yy = DataCountUtil.changeTPercentOrAbs2(s, null);
                Constants.originalCalculationData.add(Constants.measurd_data.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(Constants.measurd_data.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("3")) {
                Double sData = Double.valueOf(Constants.measurd_data.get(0).getS());
                String yy = String.valueOf((sData / 8388607) * 100);
                Constants.originalCalculationData.add(Constants.measurd_data.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(Constants.measurd_data.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("4")) {
                Double rData = Double.valueOf(Constants.measurd_data.get(0).getR());
                String yy = String.valueOf((rData / 8388607) * 100);
                Constants.originalCalculationData.add(Constants.measurd_data.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(Constants.measurd_data.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            }
            Constants.measurd_data.remove(0);
        }

    }

    /**
     * 保存一次测量数据
     *
     * @param task
     * @param taskId
     * @param sampleId
     * @param deviceSerialNumber
     * @param x
     * @param y
     * @param session
     * @param number
     * @param instrumentSnapshot
     * @param originalCalcSnapshot
     */

    private void saveOneMeasurementWithSnapshots(FMeasureTask task,
                                                 String taskId,
                                                 String sampleId,
                                                 String deviceSerialNumber,
                                                 List<Double> x,
                                                 List<Double> y,
                                                 WebSocketSession session,
                                                 Integer number,
                                                 List<String> instrumentSnapshot,
                                                 List<String> originalCalcSnapshot) {

        measureStatus = false;
        if (instrumentSnapshot == null || originalCalcSnapshot == null) {
            sendAndClose(session, "测量数据为空，无法保存");
            return;
        }

        // 1. 使用全局变量 Xdata 和 Ydata 进行峰谷数据计算
        List<Double> Xdata = this.Xdata;  // 假设这是全局变量
        List<Double> Ydata = this.Ydata;  // 假设这是全局变量

        // 2. 处理峰谷数据，使用 Python 接口进行计算
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("x_data", Xdata);
        body.put("y_data", Ydata);
        body.put("height", 0.5);
        body.put("distance", 1);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> peakResult = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
        Map<String, Object> valleyResult = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request, Map.class).getBody();

        List<Double> peakX = (List<Double>) peakResult.get("peak_x");
        List<String> peakY = (List<String>) peakResult.get("peak_y");
        List<Double> valleyX = (List<Double>) valleyResult.get("valley_x");
        List<String> valleyY = (List<String>) valleyResult.get("valley_y");

        List<FPeakValleyMarking> peakValleys = new ArrayList<>();
        List<FPeakValleyMarking> valleys = new ArrayList<>();

        // 创建波峰和波谷标记
        for (int i = 0; i < peakX.size(); i++) {
            FPeakValleyMarking marking = new FPeakValleyMarking();
            marking.setX(peakX.get(i).toString());
            marking.setY(peakY.get(i));
            marking.setType("波峰");
            peakValleys.add(marking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            FPeakValleyMarking marking = new FPeakValleyMarking();
            marking.setX(valleyX.get(i).toString());
            marking.setY(valleyY.get(i));
            marking.setType("波谷");
            valleys.add(marking);
        }

        // 3. 保存测量数据
        List<String> listData = new ArrayList<>(originalCalcSnapshot); // 使用快照

        // 查找任务样品数据
        List<FTaskSpecimen> sampleData = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        FTaskSpecimen taskSpecimen = null;
        for (FTaskSpecimen data : sampleData) {
            if (data.getSpecimenKey().equals(sampleId)) {
                taskSpecimen = data;
                break;
            }
        }
        if (taskSpecimen == null) {
            sendAndClose(session, "未找到该样品");
            return;
        }

        // 获取样品、方法和光谱参数信息
        FMeasureSample sample = tMeasureSampleService.getById(taskSpecimen.getSpecimenKey());
        FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

        if ("不重复".equals(spectrumParameter.getRepetitionPattern())) {
            // 处理普通模式，检查是否已有数据
            FMeasureData existingData = tMeasureDataService.getOne(new QueryWrapper<FMeasureData>()
                    .eq("task_id", taskId)
                    .eq("sample_key", sampleId)
                    .orderByDesc("serial_number")
                    .last("LIMIT 1"));

            if (existingData != null) {
                // 如果已有数据，则仅更新该数据
                existingData.setInstrumentData(JSON.toJSONString(instrumentSnapshot));
                existingData.setOriginalCalculationData(JSON.toJSONString(listData));
                existingData.setMeasurementStartTime(Constants.startTime);
                existingData.setMeasurementEndTime(Constants.endTime);
                existingData.setIsSave("1");
                tMeasureDataService.updateById(existingData);

                // 更新分析数据
                FMeasureAnalysisData existingAnalysisData = tMeasureAnalysisDataService.getOne(new QueryWrapper<FMeasureAnalysisData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", sampleId)
                        .eq("analysis_number", number));
                if (existingAnalysisData != null) {
                    existingAnalysisData.setAnalysisParam(JSON.toJSONString(spectrumParameter));
                    existingAnalysisData.setAnalysisResult(JSON.toJSONString(listData));
                    existingAnalysisData.setPeak(JSON.toJSONString(peakValleys));
                    existingAnalysisData.setGrain(JSON.toJSONString(valleys));
                    tMeasureAnalysisDataService.updateById(existingAnalysisData);

                    // 更新公共变量 analysisId 和 measureId
                    this.analysisId = existingAnalysisData.getId();
                    this.measureId = existingData.getId();
                }
            } else {
                // 如果没有数据，则新建记录
                FMeasureData measureData = new FMeasureData();
                measureData.setSerialNumber(1);  // 新记录序列号为 1
                measureData.setTaskId(taskId);
                measureData.setSampleKey(sampleId);
                measureData.setInstrumentData(JSON.toJSONString(instrumentSnapshot));
                measureData.setOriginalCalculationData(JSON.toJSONString(listData));
                measureData.setMeasurementStartTime(Constants.startTime);
                measureData.setMeasurementEndTime(Constants.endTime);
                measureData.setIsDelete("0");
                measureData.setIsSave("1");
                tMeasureDataService.save(measureData);

                // 保存分析数据
                FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
                analysisData.setAnalysisNumber(1);
                analysisData.setAnalysisName(sample.getSampleName() + "(1)");
                analysisData.setTaskId(taskId);
                analysisData.setSampleKey(sampleId);
                analysisData.setMeasureDataId(measureData.getId());
                analysisData.setAnalysisParam(JSON.toJSONString(instrumentSnapshot));
                analysisData.setAnalysisResult(JSON.toJSONString(listData));
                analysisData.setAnalysisType("5");
                analysisData.setPeak(JSON.toJSONString(peakValleys));
                analysisData.setGrain(JSON.toJSONString(valleys));
                analysisData.setColor("#000000");
                analysisData.setIsDelete("0");
                analysisData.setIsSave("1");
                tMeasureAnalysisDataService.save(analysisData);

                // 保存分析数据图片
                FMeasureAnalysisImage analysisImage = new FMeasureAnalysisImage();
                analysisImage.setAnalysisId(analysisData.getId());
                tMeasureAnalysisImageService.save(analysisImage);

                // 更新公共变量 analysisId 和 measureId
                this.analysisId = analysisData.getId();
                this.measureId = measureData.getId();
            }
        } else {
            // 处理重复模式
            // 重复模式时，检查是否已经有对应的分析数据，更新或新增
            FMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(
                    new QueryWrapper<FMeasureAnalysisData>()
                            .eq("task_id", taskId)
                            .eq("sample_key", sampleId)
                            .eq("analysis_number", number));

            if (analysisData != null && analysisData.getAnalysisResult() == null) {
                // 更新已有分析数据
                FMeasureData measureData = tMeasureDataService.getOne(new QueryWrapper<FMeasureData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", sampleId)
                        .eq("serial_number", number));
                measureData.setInstrumentData(JSON.toJSONString(instrumentSnapshot));
                measureData.setOriginalCalculationData(JSON.toJSONString(listData));
                measureData.setMeasurementStartTime(Constants.startTime);
                measureData.setMeasurementEndTime(Constants.endTime);
                measureData.setIsSave("1");
                tMeasureDataService.updateById(measureData);

                analysisData.setAnalysisResult(JSON.toJSONString(listData));
                analysisData.setPeak(JSON.toJSONString(peakValleys));
                analysisData.setGrain(JSON.toJSONString(valleys));
                analysisData.setColor("#000000");
                analysisData.setIsSave("1");
                tMeasureAnalysisDataService.updateById(analysisData);

                // 更新公共变量 analysisId 和 measureId
                this.analysisId = analysisData.getId();
                this.measureId = measureData.getId();
            } else {
                // 新增分析数据
                FMeasureData measureData = new FMeasureData();
                measureData.setSerialNumber(number);  // 设置序列号
                measureData.setTaskId(taskId);
                measureData.setSampleKey(sampleId);
                measureData.setInstrumentData(JSON.toJSONString(instrumentSnapshot));
                measureData.setOriginalCalculationData(JSON.toJSONString(listData));
                measureData.setMeasurementStartTime(Constants.startTime);
                measureData.setMeasurementEndTime(Constants.endTime);
                measureData.setIsDelete("0");
                measureData.setIsSave("1");
                tMeasureDataService.save(measureData);

                FMeasureAnalysisData newAnalysisData = new FMeasureAnalysisData();
                newAnalysisData.setAnalysisNumber(number);
                newAnalysisData.setAnalysisName(sample.getSampleName() + "-" + number);
                newAnalysisData.setTaskId(taskId);
                newAnalysisData.setSampleKey(sampleId);
                newAnalysisData.setMeasureDataId(measureData.getId());
                newAnalysisData.setAnalysisParam(JSON.toJSONString(instrumentSnapshot));
                newAnalysisData.setAnalysisResult(JSON.toJSONString(listData));
                newAnalysisData.setAnalysisType("5");
                newAnalysisData.setPeak(JSON.toJSONString(peakValleys));
                newAnalysisData.setGrain(JSON.toJSONString(valleys));
                newAnalysisData.setColor("#000000");
                newAnalysisData.setIsDelete("0");
                newAnalysisData.setIsSave("1");
                tMeasureAnalysisDataService.save(newAnalysisData);

                // 保存分析数据图片
                FMeasureAnalysisImage analysisImage = new FMeasureAnalysisImage();
                analysisImage.setAnalysisId(newAnalysisData.getId());
                tMeasureAnalysisImageService.save(analysisImage);

                // 更新公共变量 analysisId 和 measureId
                this.analysisId = newAnalysisData.getId();
                this.measureId = measureData.getId();
            }
        }

        // 更新任务状态
        for (FTaskSpecimen taskSample : sampleData) {
            if (taskSample.getSpecimenKey().equals(sampleId)) {
                taskSample.setIsTested("1");
            }
        }
        task.setSampleKey(JSON.toJSONString(sampleData));
        task.setDeviceSerialNumber(deviceSerialNumber);
        tMeasureTaskService.updateById(task);
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
        // 清除Xdata和Ydata的数据
        Xdata.clear();
        Ydata.clear();
        isFirstSave = false;
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
        List<String> instrumentSnapshot;
        List<String> originalCalcSnapshot;

        MeasurementResult(List<Double> x, List<Double> y,
                          List<String> instrumentSnapshot,
                          List<String> originalCalcSnapshot) {
            this.x = x;
            this.y = y;
            this.instrumentSnapshot = instrumentSnapshot;
            this.originalCalcSnapshot = originalCalcSnapshot;
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