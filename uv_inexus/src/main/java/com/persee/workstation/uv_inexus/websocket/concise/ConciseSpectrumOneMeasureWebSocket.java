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

@Component
public class ConciseSpectrumOneMeasureWebSocket extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ConciseSpectrumOneMeasureWebSocket.class);

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
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        occupancyStatus = false;
        measureStatus = false;

        log.info("客户端进入: " + session.getId() + " 时间: " + LocalDateTime.now());
        sessions.put(session.getId(), session);
        exceptionHandling();

        if (Constants.endTime != null || Constants.startTime != null) {
            sendClose(session, "当前测量未结束,无法开启新的测试");
            return;
        }

        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String sampleId = getQueryParam(query, "sampleId");
        String analysisNumber = getQueryParam(query, "analysisNumber");
        deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");
        String updateBy = getQueryParam(query, "updateBy");
        digit = Integer.parseInt(getQueryParam(query, "digit"));

        if (taskId == null) {
            sendClose(session, "缺少 taskId 参数");
            return;
        }
        if (sampleId == null) {
            sendClose(session, "缺少 sampleId 参数");
            return;
        }
        if (analysisNumber == null) {
            sendClose(session, "缺少 analysisNumber 参数");
            return;
        }
        if (deviceSerialNumber == null) {
            sendClose(session, "缺少 deviceSerialNumber 参数");
            return;
        }
        if (updateBy == null) {
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
        log.info("占用设备成功");
        if (!message.getSuccess()) {
            sendClose(session, message.getMessage());
            return;
        }
        occupancyStatus = true;

        /// 设置参数
        Result result = spectrumService.setUpParametersPort(task.getMethodKey(), deviceSerialNumber);
        if (result.getCode() != 200) {
            sendAndClose(session, "设置参数失败");
            return;
        }

        // 根据方法id查询方法数据
        FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 将方法参数提取出来
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

        ip = deviceInfoService.getIp(deviceSerialNumber);

        // 根据序列号查询对应的附件配置表信息
        QueryWrapper<FSamplePoolConfigPO> wrapper = new QueryWrapper<>();
        wrapper.eq("device_serial_number", deviceSerialNumber);
        FSamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);
        if (configPO == null) {
            sendAndClose(session, "没有找到对应的附件配置信息");
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
                sendAndClose(session, "请先创建样品");
                return;
            }

            if (poolStatus) {
                R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
                if (!stringR.getSuccess()) {
                    sendAndClose(session, "设置出错");
                    return;
                }
                fSamplePoolConfigService.updateCurrentPoolNum(deviceSerialNumber, Integer.parseInt(sample.getSampleLocation()), updateBy);
                JSONObject object = new JSONObject();
                object.put("currentPoolNum", sample.getSampleLocation());
                try {
                    session.sendMessage(new TextMessage(object.toJSONString()));
                } catch (IOException e) {
                    log.error("发送样品池位置信息失败", e);
                }
            }
        }

        Command command = buildCommand(SetParamEnum.Scan, new Object[]{});
        if (spectrumParameter.getScanSpeed().equals("3")) {
            command = buildCommand(SetParamEnum.ScanFast, new Object[]{});
        }

        // 启动测量任务线程
        Command finalCommand = command;
        Thread taskThread = new Thread(() -> {
            try {
                Integer number = Integer.valueOf(analysisNumber);
                doOneMeasurementSerial(session, finalCommand, spectrumParameter, task, taskId, sampleId, deviceSerialNumber, ip, number);

                // 发送完成消息
                JSONObject payload = new JSONObject();
                payload.put("result", "finish");
                sendAndClose(session, payload.toJSONString());

            } catch (Exception e) {
                log.error("单次测量异常", e);
                handleTransportError(session, e);
                Thread.currentThread().interrupt();
                return;
            }
        });

        taskThreads.put(session.getId(), taskThread);
        taskThread.start();
    }

    /**
     * 处理接收到的文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("收到消息: " + session.getId() + " -> " + message.getPayload());

        if (message.getPayload().equals("close")) {
            // 优先释放测量线程
            releaseMeasureThread();

            Thread thread = taskThreads.get(session.getId());
            if (measureStatus) {
                try {
                    R<String> stop = devMessageUtil.stop(buildCommand(SetParamEnum.Stop, new Object[]{}), ip);
                    if (stop.getSuccess()) {
                        log.info("停止成功");
                    } else {
                        log.info("停止失败");
                    }
                } catch (Exception e) {
                    log.error("停止测量异常", e);
                } finally {
                    measureStatus = false;
                }
            }

            // 然后关闭其他线程和会话
            if (thread != null) {
                thread.interrupt();
                taskThreads.remove(session.getId());
            }

            try {
                session.sendMessage(new TextMessage("停止成功"));
                sessions.remove(session.getId());
                session.close();
            } finally {
                // 最后释放设备
                if (occupancyStatus) {
                    deviceInfoService.releaseDevice(deviceSerialNumber);
                    occupancyStatus = false;
                    log.info("释放设备成功");
                }
            }
        }
    }

    /**
     * 重写afterConnectionClosed方法，用于处理WebSocket连接关闭后的操作
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("客户端退出: " + session.getId() + " 时间: " + LocalDateTime.now() + " 原因: " + status);

        // 1. 优先释放测量线程
        releaseMeasureThread();

        // 2. 关闭任务线程
        Thread taskThread = taskThreads.remove(session.getId());
        if (taskThread != null) {
            taskThread.interrupt();
        }

        // 3. 移除会话
        sessions.remove(session.getId());

        // 4. 清空公共数据
        exceptionHandling();

        // 5. 清空原始Y值存储
        originalYValues.remove(session.getId());

        // 6. 最后释放设备
        if (occupancyStatus) {
            deviceInfoService.releaseDevice(deviceSerialNumber);
            occupancyStatus = false;
            log.info("释放设备成功");
        }
    }

    /**
     * 连接异常时触发
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        // 优先释放测量线程
        releaseMeasureThread();

        // 关闭任务线程
        Thread taskThread = taskThreads.remove(session.getId());
        if (taskThread != null) {
            taskThread.interrupt();
        }

        sessions.remove(session.getId());
        try {
            session.close();
        } catch (IOException e) {
            log.error("处理传输错误时发生异常", e);
        } finally {
            // 最后释放设备
            if (occupancyStatus) {
                deviceInfoService.releaseDevice(deviceSerialNumber);
                occupancyStatus = false;
            }
            // 清空原始Y值存储
            originalYValues.remove(session.getId());
        }
        log.error("连接异常: " + exception.getMessage());
    }

    /**
     * 释放测量线程 - 新增方法
     */
    private void releaseMeasureThread() {
        if (Constants.sendMeasureThread != null) {
            try {
                Constants.sendMeasureThread.interrupt();
                Constants.sendMeasureThread = null;
                log.info("测量线程已释放");
            } catch (Exception e) {
                log.error("释放测量线程时发生异常", e);
            }
        }
        // 重置测量状态
        measureStatus = false;
    }

    /**
     * 执行单次测量序列
     */
    private void doOneMeasurementSerial(WebSocketSession session,
                                        Command command,
                                        FSpectrumParameter spectrumParameter,
                                        FMeasureTask task,
                                        String taskId,
                                        String sampleId,
                                        String deviceSerialNumber,
                                        String ip,
                                        Integer number) throws IOException {
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
            result = doOneMeasurementCore(session, command, spectrumParameter, task, taskId, sampleId, deviceSerialNumber, ip, number);
        } finally {
            // 出锁前先生成快照
            if (result == null) {
                result = new MeasurementResult(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Constants.startTime,
                        Constants.endTime,
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
                result.x, result.y, session, number, result.startTime, result.endTime,
                result.instrumentSnapshot, result.originalCalcSnapshot);
    }

    /**
     * 执行单次测量核心逻辑
     */
    private MeasurementResult doOneMeasurementCore(WebSocketSession session,
                                                   Command command,
                                                   FSpectrumParameter spectrumParameter,
                                                   FMeasureTask task,
                                                   String taskId,
                                                   String sampleId,
                                                   String deviceSerialNumber,
                                                   String ip,
                                                   Integer number) throws IOException {

        String luminosity = getLuminosityType(spectrumParameter.getLuminosity());

        // 发送测量命令
        devMessageUtil.getMeasureData(command, ip);
        Constants.startTime = LocalDateTime.now();
        measureStatus = true;

        List<ChangeParam> baselineCopy = (task.getZeroParam() != null && !task.getZeroParam().isEmpty())
                ? JSON.parseArray(task.getZeroParam(), ChangeParam.class)
                : null;

        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        int pushCount = 0;

        // 获取波长参数用于约束
        double startWl = Double.parseDouble(spectrumParameter.getStartWaveLength());
        double endWl = Double.parseDouble(spectrumParameter.getEndWaveLength());
        double interval = Double.parseDouble(spectrumParameter.getWaveLengthInterval());
        boolean isDescending = startWl > endWl;

        // 判断是否为极快扫描模式
        boolean isFastScan = "3".equals(spectrumParameter.getScanSpeed());

        if (isFastScan) {
            // 极快扫描模式：一次性处理所有数据
            ProcessedData processedData = processAndPushFastScanData(session, spectrumParameter, x, y, number,
                    task, sampleId, luminosity, spectrumParameter.getLadderCorrection());
            // 使用处理后的数据
            x = processedData.x;
            y = processedData.y;
        } else {
            // 普通扫描模式：保持原来的逐点推送逻辑
            int currentIndex = 0; // 用于强制修约的索引
            while (true) {
                if (Thread.currentThread().isInterrupted() || !session.isOpen()) return null;

                List<String> snapshot = new ArrayList<>(Constants.instrumentData);
                if (!snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData)) {
                    if (snapshot.get(0).equals(Constants.EndData)) {
                        break;
                    }
                    while (!Constants.measurd_data.isEmpty()) {
                        calcOnePoint(spectrumParameter, baselineCopy, x, y, currentIndex, startWl, endWl, interval, isDescending);
                        currentIndex++;
                        pushCount = pushDataIfNeeded(session, x, y, pushCount, number, task, sampleId,
                                spectrumParameter.getLuminosity(), spectrumParameter.getLadderCorrection());
                    }
                    break;
                }
                while (!Constants.measurd_data.isEmpty()) {
                    calcOnePoint(spectrumParameter, baselineCopy, x, y, currentIndex, startWl, endWl, interval, isDescending);
                    currentIndex++;
                    pushCount = pushDataIfNeeded(session, x, y, pushCount, number, task, sampleId,
                            spectrumParameter.getLuminosity(), spectrumParameter.getLadderCorrection());
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            // 推送剩余数据
            pushRemainingData(session, x, y, pushCount, number, task, sampleId,
                    spectrumParameter.getLuminosity(), spectrumParameter.getLadderCorrection());
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
     * 处理极快扫描模式的数据
     */
    private ProcessedData processAndPushFastScanData(WebSocketSession session,
                                                     FSpectrumParameter spectrumParameter,
                                                     List<Double> x, List<Double> y,
                                                     Integer number, FMeasureTask task,
                                                     String sampleId, String luminosity,
                                                     boolean ladderCorrection) throws IOException {
        // 等待数据采集完成
        while (true) {
            if (Thread.currentThread().isInterrupted() || !session.isOpen())
                return new ProcessedData(x, y);

            List<String> snapshot = new ArrayList<>(Constants.instrumentData);
            if (!snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData)) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new ProcessedData(x, y);
            }
        }

        // 计算所有数据点
        List<ChangeParam> baselineCopy = (task.getZeroParam() != null && !task.getZeroParam().isEmpty())
                ? JSON.parseArray(task.getZeroParam(), ChangeParam.class)
                : null;

        int currentIndex = 0; // 用于强制修约的索引
        while (!Constants.measurd_data.isEmpty()) {
            calcOnePointForFastScan(spectrumParameter, baselineCopy, x, y, currentIndex);
            currentIndex++;
        }

        // 获取起始和结束波长
        double startWl = Double.parseDouble(spectrumParameter.getStartWaveLength());
        double endWl = Double.parseDouble(spectrumParameter.getEndWaveLength());
        double interval = Double.parseDouble(spectrumParameter.getWaveLengthInterval());

        // 处理数据：按照严格间隔修正波长
        ProcessedData processedData = processFastScanWavelengthDataWithConstraint(x, y, startWl, endWl, interval);

        List<Double> processedX = processedData.x;
        List<Double> processedY = processedData.y;

        // 如果需要台阶校正
        if (ladderCorrection) {
            List<Double> correctedY = processAndSaveData(task, processedX, processedY,
                    spectrumParameter.getLuminosity(), ladderCorrection, sampleId);
            processedY = correctedY;
        }

        // 存储原始Y值（用于后续存储到数据库）
        String key = session.getId() + "_" + sampleId + "_" + number;
        originalYValues.put(key, new ArrayList<>(processedY));

        // 一次性推送所有数据
        pushAllDataAtOnce(session, processedX, processedY, number, sampleId, spectrumParameter.getLuminosity());

        return new ProcessedData(processedX, processedY);
    }

    /**
     * 为极快扫描模式计算数据点（带强制修约）
     */
    private void calcOnePointForFastScan(FSpectrumParameter spectrumParameter,
                                         List<ChangeParam> baselineCopy,
                                         List<Double> x, List<Double> y,
                                         int currentIndex) {
        if (Constants.measurd_data.isEmpty()) return;

        List<LinearSegment> linearSegment = formulaUtil.getLinearSegment(deviceSerialNumber);
        ChangeParam currentData = Constants.measurd_data.get(0);

        // 获取波长参数用于约束
        double startWl = Double.parseDouble(spectrumParameter.getStartWaveLength());
        double endWl = Double.parseDouble(spectrumParameter.getEndWaveLength());
        double interval = Double.parseDouble(spectrumParameter.getWaveLengthInterval());
        boolean isDescending = startWl > endWl;

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

                        // 添加波长约束 - 强制修约
                        correctedWl = forceConstrainWavelength(correctedWl, startWl, endWl, interval, isDescending, currentIndex);

                        x.add(correctedWl);
                        y.add(Double.parseDouble(yy));
                        break;
                    case "2": // T%
                        s = DataCountUtil.countTPercent(baselineData, currentData, 1.0);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);

                        // 添加波长约束 - 强制修约
                        correctedWl = forceConstrainWavelength(correctedWl, startWl, endWl, interval, isDescending, currentIndex);

                        x.add(correctedWl);
                        y.add(Double.parseDouble(s));
                        break;
                    case "3": // Es
                        Double sData = Double.valueOf(currentData.getS());
                        String esValue = String.valueOf((sData / 8388607) * 100);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);

                        // 添加波长约束 - 强制修约
                        correctedWl = forceConstrainWavelength(correctedWl, startWl, endWl, interval, isDescending, currentIndex);

                        x.add(correctedWl);
                        y.add(Double.parseDouble(esValue));
                        break;
                    case "4": // Er
                        Double rData = Double.valueOf(currentData.getR());
                        String erValue = String.valueOf((rData / 8388607) * 100);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);

                        // 添加波长约束 - 强制修约
                        correctedWl = forceConstrainWavelength(correctedWl, startWl, endWl, interval, isDescending, currentIndex);

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

                        // 添加波长约束 - 强制修约
                        correctedWl = forceConstrainWavelength(correctedWl, startWl, endWl, interval, isDescending, currentIndex);

                        x.add(correctedWl);
                        y.add(Double.parseDouble(yy));
                        break;
                    case "2": // T%
                        s = DataCountUtil.countTPercent(null, currentData, 1.0);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);

                        // 添加波长约束 - 强制修约
                        correctedWl = forceConstrainWavelength(correctedWl, startWl, endWl, interval, isDescending, currentIndex);

                        x.add(correctedWl);
                        y.add(Double.parseDouble(s));
                        break;
                    case "3": // Es
                        Double sData = Double.valueOf(currentData.getS());
                        String esValue = String.valueOf((sData / 8388607) * 100);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);

                        // 添加波长约束 - 强制修约
                        correctedWl = forceConstrainWavelength(correctedWl, startWl, endWl, interval, isDescending, currentIndex);

                        x.add(correctedWl);
                        y.add(Double.parseDouble(esValue));
                        break;
                    case "4": // Er
                        Double rData = Double.valueOf(currentData.getR());
                        String erValue = String.valueOf((rData / 8388607) * 100);
                        parsedWl = Double.parseDouble(currentData.getWl());
                        correctedWl = formulaUtil.calculateWavelength(parsedWl, linearSegment);

                        // 添加波长约束 - 强制修约
                        correctedWl = forceConstrainWavelength(correctedWl, startWl, endWl, interval, isDescending, currentIndex);

                        x.add(correctedWl);
                        y.add(Double.parseDouble(erValue));
                        break;
                }
            }

            Constants.measurd_data.remove(0);
        } catch (Exception e) {
            log.error("计算数据点时发生异常: " + e.getMessage(), e);
            Constants.measurd_data.remove(0); // 即使出错也移除数据，避免死循环
        }
    }

    /**
     * 强制约束波长值，确保符合设定的波长间隔和范围
     * 无论原始波长是多少，都强制使用期望的波长值
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

        // 记录修约信息（如果原始值与修约值不同）
        if (Math.abs(originalWl - constrainedWl) > 0.001) {
            log.debug("波长强制修约: 原始值=" + originalWl + ", 修约值=" + constrainedWl +
                    ", 期望值=" + expectedWl + ", 索引=" + currentIndex);
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
     * 处理极快扫描的波长数据 - 严格按照设定的间隔生成规整的波长序列（增强约束版本）
     */
    private ProcessedData processFastScanWavelengthDataWithConstraint(List<Double> originalX,
                                                                      List<Double> originalY,
                                                                      double startWl, double endWl,
                                                                      double interval) {
        List<Double> processedX = new ArrayList<>();
        List<Double> processedY = new ArrayList<>();

        // 确定波长顺序（从大到小还是从小到大）
        boolean isDescending = startWl > endWl;

        // 生成严格的预期波长序列
        List<Double> expectedWavelengths = generateStrictExpectedWavelengths(startWl, endWl, interval, isDescending);

        // 创建映射：实际波长 -> Y值列表
        Map<Double, List<Double>> actualWavelengthMap = new HashMap<>();
        for (int i = 0; i < originalX.size(); i++) {
            double actualWavelength = originalX.get(i);
            double yValue = originalY.get(i);

            actualWavelengthMap
                    .computeIfAbsent(actualWavelength, k -> new ArrayList<>())
                    .add(yValue);
        }

        // 为每个预期波长找到最匹配的实际数据
        for (Double expectedWavelength : expectedWavelengths) {
            Double bestMatchWavelength = findBestMatchingWavelength(expectedWavelength,
                    actualWavelengthMap.keySet(), interval);

            if (bestMatchWavelength != null) {
                List<Double> yValues = actualWavelengthMap.get(bestMatchWavelength);
                if (yValues != null && !yValues.isEmpty()) {
                    // 使用第一个Y值
                    processedX.add(expectedWavelength);
                    processedY.add(yValues.get(0));

                    // 从映射中移除已使用的波长，避免重复使用
                    actualWavelengthMap.remove(bestMatchWavelength);
                }
            }
        }

        return new ProcessedData(processedX, processedY);
    }

    /**
     * 生成严格的预期波长序列
     */
    private List<Double> generateStrictExpectedWavelengths(double startWl, double endWl,
                                                           double interval, boolean isDescending) {
        List<Double> expectedWavelengths = new ArrayList<>();

        int decimalPlaces = getDecimalPlaces(interval);

        if (isDescending) {
            // 从大到小
            for (double wl = startWl; wl >= endWl; wl -= interval) {
                expectedWavelengths.add(roundToPrecision(wl, decimalPlaces));
            }
        } else {
            // 从小到大
            for (double wl = startWl; wl <= endWl; wl += interval) {
                expectedWavelengths.add(roundToPrecision(wl, decimalPlaces));
            }
        }

        return expectedWavelengths;
    }

    /**
     * 找到与预期波长最匹配的实际波长
     */
    private Double findBestMatchingWavelength(Double expectedWavelength,
                                              Set<Double> actualWavelengths,
                                              double interval) {
        double tolerance = interval * 0.3;

        Double bestMatch = null;
        double minDifference = Double.MAX_VALUE;

        for (Double actualWavelength : actualWavelengths) {
            double difference = Math.abs(actualWavelength - expectedWavelength);

            if (difference <= tolerance && difference < minDifference) {
                minDifference = difference;
                bestMatch = actualWavelength;
            }
        }

        return bestMatch;
    }

    /**
     * 精确舍入到指定小数位
     */
    private double roundToPrecision(double value, int decimalPlaces) {
        double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }

    /**
     * 计算光度模式的Abs/T%（带强制修约）
     */
    private void calcOnePoint(FSpectrumParameter spectrumParameter,
                              List<ChangeParam> baselineCopy,
                              List<Double> x,
                              List<Double> y,
                              int currentIndex,
                              double startWl,
                              double endWl,
                              double interval,
                              boolean isDescending) {
        List<LinearSegment> linearSegment = formulaUtil.getLinearSegment(deviceSerialNumber);

        if (baselineCopy != null && !baselineCopy.isEmpty()) {
            if (spectrumParameter.getLuminosity().equals("1")) {
                String s = DataCountUtil.countTPercent(baselineCopy.get(0), Constants.measurd_data.get(0), 1.0);
                String yy = DataCountUtil.changeTPercentOrAbs2(null,s);
                Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);

                // 添加波长约束 - 强制修约
                v = forceConstrainWavelength(v, startWl, endWl, interval, isDescending, currentIndex);

                Constants.originalCalculationData.add(v + "," + yy);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("2")) {
                String s = DataCountUtil.countTPercent(baselineCopy.get(0), Constants.measurd_data.get(0), 1.0);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);

                // 添加波长约束 - 强制修约
                v = forceConstrainWavelength(v, startWl, endWl, interval, isDescending, currentIndex);

                Constants.originalCalculationData.add(v + "," + s);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(s));
            } else if (spectrumParameter.getLuminosity().equals("3")) {
                Double sData = Double.valueOf(Constants.measurd_data.get(0).getS());
                String yy = String.valueOf((sData / 8388607) * 100);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);

                // 添加波长约束 - 强制修约
                v = forceConstrainWavelength(v, startWl, endWl, interval, isDescending, currentIndex);

                Constants.originalCalculationData.add(v + "," + yy);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("4")) {
                Double rData = Double.valueOf(Constants.measurd_data.get(0).getR());
                String yy = String.valueOf((rData / 8388607) * 100);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);

                // 添加波长约束 - 强制修约
                v = forceConstrainWavelength(v, startWl, endWl, interval, isDescending, currentIndex);

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

                // 添加波长约束 - 强制修约
                v = forceConstrainWavelength(v, startWl, endWl, interval, isDescending, currentIndex);

                Constants.originalCalculationData.add(v + "," + yy);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("2")) {
                String s = DataCountUtil.countTPercent(null, Constants.measurd_data.get(0), 1.0);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);

                // 添加波长约束 - 强制修约
                v = forceConstrainWavelength(v, startWl, endWl, interval, isDescending, currentIndex);

                Constants.originalCalculationData.add(v + "," + s);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(s));
            } else if (spectrumParameter.getLuminosity().equals("3")) {
                Double sData = Double.valueOf(Constants.measurd_data.get(0).getS());
                String yy = String.valueOf((sData / 8388607) * 100);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);

                // 添加波长约束 - 强制修约
                v = forceConstrainWavelength(v, startWl, endWl, interval, isDescending, currentIndex);

                Constants.originalCalculationData.add(v + "," + yy);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("4")) {
                Double rData = Double.valueOf(Constants.measurd_data.get(0).getR());
                String yy = String.valueOf((rData / 8388607) * 100);
                double parsed = Double.parseDouble(Constants.measurd_data.get(0).getWl());
                Double v = formulaUtil.calculateWavelength(parsed, linearSegment);

                // 添加波长约束 - 强制修约
                v = forceConstrainWavelength(v, startWl, endWl, interval, isDescending, currentIndex);

                Constants.originalCalculationData.add(v + "," + yy);
                x.add(Double.parseDouble(v.toString()));
                y.add(Double.parseDouble(yy));
            }
            Constants.measurd_data.remove(0);
        }
    }

    /**
     * 一次性推送所有数据
     */
    private void pushAllDataAtOnce(WebSocketSession session,
                                   List<Double> x, List<Double> y,
                                   Integer number, String sampleId,
                                   String luminosity) throws IOException {
        JSONArray dataArray = new JSONArray();

        for (int i = 0; i < x.size(); i++) {
            JSONObject point = new JSONObject();
            point.put("x", x.get(i));

            // 推送时：根据指定位数格式化Y值
            Double value = DataCountUtil.formatValue(digit, y.get(i));
            String valueY = DataCountUtil.addZero(value, digit);
            point.put("y", y.get(i));
            point.put("roundOffY",valueY);

            point.put("xType", "nm");
            String luminosityType = getLuminosityType(luminosity);
            point.put("yType", luminosityType);

            dataArray.add(point);
        }

        JSONObject payload = new JSONObject();
        payload.put("data", dataArray);
        payload.put("sampleId", sampleId);
        payload.put("count", number);
        payload.put("scanMode", "fast");

        if (session.isOpen()) {
            session.sendMessage(new TextMessage(payload.toJSONString()));
        }
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
                point.put("y", y.get(i));
                point.put("roundOffY",valueY);

                point.put("xType", "nm");
                String luminosityType = getLuminosityType(luminosity);
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
                point.put("y", y.get(i));
                point.put("roundOffY",valueY);

                point.put("xType", "nm");
                String luminosityType = getLuminosityType(luminosity);
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
     */
    public List<Double> processAndSaveData(FMeasureTask task,
                                           List<Double> x,
                                           List<Double> y,
                                           String luminosityMode,
                                           boolean ladderCorrection,
                                           String sampleId) {
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        log.info("进入台阶校正逻辑");

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

        log.info("台阶消除门限系数: " + threshold);

        // 这里调用 Python 的台阶消除算法，返回处理后的 y 值
        List<Double> processedY = callStepEliminationPython(x, y, threshold);
        return processedY;
    }

    /**
     * 调用 Python 服务处理台阶消除
     */
    private List<Double> callStepEliminationPython(List<Double> x, List<Double> y, Integer threshold) {
        log.info("进入台阶消除逻辑");

        // 确保threshold不为null
        if (threshold == null) {
            threshold = 100;
            log.warn("调用Python服务时门限系数为null，使用默认值100");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("x_data", x);
            requestBody.put("y_data", y);
            requestBody.put("k", threshold);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 调用 Python API
            ResponseEntity<Map> response = restTemplate.postForEntity(PythonPathConstant.REMOVE_STEP_OR_SPIKES, request, Map.class);

            // 从响应中获取处理后的 y 值
            List<Double> processedY = (List<Double>) response.getBody().get("smoothed_data");

            log.info("Python服务返回数据长度: " + (processedY != null ? processedY.size() : "null"));

            return processedY;
        } catch (Exception e) {
            log.error("调用Python台阶消除服务失败，返回原始数据", e);
            // 如果调用失败，返回原始数据
            return y;
        }
    }

    /**
     * 使用快照保存一次测量结果 - 修复重测覆盖问题
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
        measureStatus = false;

        if (instrumentSnapshot == null || originalCalcSnapshot == null) {
            sendAndClose(session, "测量数据为空，无法保存");
            return;
        }

        try {
            SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();

            // 处理峰谷数据
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();

            // 使用原始Y值进行峰谷分析
            String key = session.getId() + "_" + sampleId + "_" + number;
            List<Double> originalY = originalYValues.get(key);
            if (originalY == null) {
                originalY = y;
            }

            body.put("x_data", x);
            body.put("y_data", originalY);
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

            // 构建结果字符串，存储时使用原始Y值
            List<String> result = new ArrayList<>();
            for (int i = 0; i < x.size(); i++) {
                double originalYValue = originalY != null && i < originalY.size() ? originalY.get(i) : y.get(i);
                result.add(x.get(i) + "," + originalYValue);
            }

            // 保存测量数据 - 修复重测覆盖逻辑
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

            FMeasureSample sample = tMeasureSampleService.getById(sampleId);
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
                    existingMeasureData.setInstrumentData(JSON.toJSONString(instrumentSnapshot));
                    existingMeasureData.setOriginalCalculationData(JSON.toJSONString(result));
                    existingMeasureData.setMeasurementStartTime(startTime);
                    existingMeasureData.setMeasurementEndTime(endTime);
                    existingMeasureData.setIsSave("1");
                    boolean updateMeasure = traceDataService.updateById(existingMeasureData);
                    if (!updateMeasure) {
                        log.error("更新测量数据失败");
                        sendAndClose(session, "更新测量数据失败");
                        return;
                    }
                }

                // 更新分析数据
                existingAnalysisData.setAnalysisParam(JSON.toJSONString(instrumentSnapshot));
                existingAnalysisData.setAnalysisResult(JSON.toJSONString(result));
                existingAnalysisData.setPeak(JSON.toJSONString(peakValleys));
                existingAnalysisData.setGrain(JSON.toJSONString(markings));
                existingAnalysisData.setColor("#000000");
                existingAnalysisData.setIsSave("1");
                boolean updateAnalysis = tMeasureAnalysisDataService.updateById(existingAnalysisData);
                if (!updateAnalysis) {
                    log.error("更新分析数据失败");
                    sendAndClose(session, "更新分析数据失败");
                    return;
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
                    measureData.setInstrumentData(JSON.toJSONString(instrumentSnapshot));
                    measureData.setOriginalCalculationData(JSON.toJSONString(result));
                    measureData.setMeasurementStartTime(startTime);
                    measureData.setMeasurementEndTime(endTime);
                    measureData.setIsDelete("0");
                    measureData.setIsSave("1");
                    boolean saveMeasure = traceDataService.save(measureData);
                    if (!saveMeasure) {
                        sendAndClose(session, "测量数据保存失败");
                        return;
                    }

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
                    boolean saveAnalysis = tMeasureAnalysisDataService.save(analysisData);
                    if (!saveAnalysis) {
                        sendAndClose(session, "分析数据保存失败");
                        return;
                    }

                    FMeasureAnalysisImage tMeasureAnalysisImage = new FMeasureAnalysisImage();
                    tMeasureAnalysisImage.setAnalysisId(analysisData.getId());
                    boolean saveImage = tMeasureAnalysisImageService.save(tMeasureAnalysisImage);
                    if (!saveImage) {
                        sendAndClose(session, "分析图像保存失败");
                        return;
                    }
                } else {
                    // 重复模式
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
                    boolean saveMeasure = traceDataService.save(measureData);
                    if (!saveMeasure) {
                        sendAndClose(session, "测量数据保存失败");
                        return;
                    }

                    FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
                    analysisData.setAnalysisNumber(number);
                    analysisData.setAnalysisName(sample.getSampleName() + "-" + number);
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
                    boolean saveAnalysis = tMeasureAnalysisDataService.save(analysisData);
                    if (!saveAnalysis) {
                        sendAndClose(session, "分析数据保存失败");
                        return;
                    }

                    FMeasureAnalysisImage tMeasureAnalysisImage = new FMeasureAnalysisImage();
                    tMeasureAnalysisImage.setAnalysisId(analysisData.getId());
                    boolean saveImage = tMeasureAnalysisImageService.save(tMeasureAnalysisImage);
                    if (!saveImage) {
                        sendAndClose(session, "分析图像保存失败");
                        return;
                    }
                }
            }

            // 更新任务状态
            updateTaskStatus(task, taskId, sampleId, deviceSerialNumber, spectrumParameter, session);

            // 清理存储的原始Y值
            originalYValues.remove(key);

        } catch (Exception e) {
            log.error("保存测量数据时发生异常", e);
            sendAndClose(session, "数据保存失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(FMeasureTask task, String taskId, String sampleId,
                                  String deviceSerialNumber, FSpectrumParameter spectrumParameter,
                                  WebSocketSession session) {
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
     * 获取光度类型
     */
    private String getLuminosityType(String luminosityCode) {
        switch (luminosityCode) {
            case "1": return "Abs";
            case "2": return "T%";
            case "3": return "Es";
            case "4": return "Er";
            default: return "Abs";
        }
    }

    /**
     * 获取查询参数
     */
    private String getQueryParam(String query, String key) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) return pair[1];
        }
        return null;
    }

    /**
     * 发送消息并关闭连接
     */
    private void sendAndClose(WebSocketSession session, String msg) {
        try {
            // 优先释放测量线程
            releaseMeasureThread();

            // 关闭任务线程
            Thread thread = taskThreads.get(session.getId());
            if (thread != null) {
                thread.interrupt();
                taskThreads.remove(session.getId());
            }

            session.sendMessage(new TextMessage(msg));
            sessions.remove(session.getId());
            session.close();
        } catch (IOException e) {
            log.error("发送消息并关闭连接时发生异常", e);
        } finally {
            // 最后释放设备
            if (occupancyStatus) {
                deviceInfoService.releaseDevice(deviceSerialNumber);
                occupancyStatus = false;
                log.info("释放设备成功");
            }
            exceptionHandling();
        }
    }

    /**
     * 发送消息并关闭连接2
     */
    private void sendClose(WebSocketSession session, String msg) {
        try {
            session.sendMessage(new TextMessage(msg));
            sessions.remove(session.getId());
            session.close();
        } catch (IOException e) {
            log.error("发送关闭消息时发生异常", e);
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

    /**
     * 全局数据清空
     */
    private void exceptionHandling() {
        Constants.instrumentData.clear();
        Constants.measurd_data.clear();
        Constants.originalCalculationData.clear();
        Constants.startTime = null;
        Constants.endTime = null;
        // 测量线程在releaseMeasureThread方法中单独处理
    }

    /**
     * 测量结果类
     */
    class MeasurementResult {
        List<Double> x;
        List<Double> y;
        LocalDateTime startTime;
        LocalDateTime endTime;
        List<String> instrumentSnapshot;
        List<String> originalCalcSnapshot;

        MeasurementResult(List<Double> x, List<Double> y,
                          LocalDateTime startTime, LocalDateTime endTime,
                          List<String> instrumentSnapshot, List<String> originalCalcSnapshot) {
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
}