/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.PeakValleyMarking;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class TimeOneMeasureWebSocket extends TextWebSocketHandler {
    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储任务上下文
    private static final Map<String, TimeMeasureContext> contexts = new ConcurrentHashMap<>();

    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();

    @Autowired
    private TMeasureTaskService tMeasureTaskService;

    @Autowired
    private TimeSweepService timeSweepService;

    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private TMeasureDataService tMeasureDataService;

    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    private TMeasureSampleService tMeasureSampleService;

    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PythonPathConstant pythonPathConstant;


    /**
     * 时间测量上下文
     */
    private static class TimeMeasureContext {
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicBoolean dataSaved = new AtomicBoolean(false);
        final CountDownLatch saveLatch = new CountDownLatch(1);

        ScheduledExecutorService scheduler;
        List<BigDecimal> x_data = new ArrayList<>();
        List<BigDecimal> y_data = new ArrayList<>();
        List<ChangeParam> originalData = new ArrayList<>();

        String deviceSerialNumber;
        String ip;
        String currentSampleId;
        TMeasureTask task;

        void stopAllTasks() {
            running.set(false);
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
        }

        boolean waitForDataSave(long timeout, TimeUnit unit) {
            try {
                return saveLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        void markDataSaved() {
            dataSaved.set(true);
            saveLatch.countDown();
        }
    }



    /**
     * 前端连接成功 WebSocket 时触发
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (Constants.endTime != null || Constants.startTime != null) {
            sendAndClose(session, "当前测试未结束,无法开启新的测试");
            return;
        }

        // 记录当前连接
        sessions.put(session.getId(), session);

        // 创建上下文
        TimeMeasureContext context = new TimeMeasureContext();
        contexts.put(session.getId(), context);

        // 解析参数
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String sampleId = getQueryParam(query, "sampleId");
        String deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");

        if (taskId == null) {
            sendAndClose(session, "缺少 taskId 参数");
            return;
        }
        if (sampleId == null) {
            sendAndClose(session, "缺少 sampleId 参数");
            return;
        }
        if (deviceSerialNumber == null) {
            sendAndClose(session, "缺少 deviceSerialNumber 参数");
            return;
        }

        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task.getSampleKey() == null || task.getSampleKey().equals("")) {
            sendAndClose(session, "请先创建样品");
            return;
        }

        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 将方法参数提取出来
        TimeSweepParamter timeSweepParameter = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);

        String ip = deviceInfoService.getIp(deviceSerialNumber);
        context.ip = ip;
        context.deviceSerialNumber = deviceSerialNumber;
        context.currentSampleId = sampleId;
        context.task = task;

        // 设置样品池位置poolNum
        TMeasureSample sample = tMeasureSampleService.getById(sampleId);
        if (sample == null){
            sendAndClose(session, "请先创建样品");
            return;
        }

        R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
        if (!stringR.getSuccess()) {
            log.info("设置出错");
            sendAndClose(session, "设置出错");
            return;
        }

        Thread taskThread = new Thread(() -> {
            startTimedTask(session, timeSweepParameter, context); // 启动推送任务
        });

        taskThreads.put(session.getId(), taskThread);
        taskThread.start();
    }

    /**
     * 启动定时任务，每隔指定间隔推送数据给前端
     */
    private void startTimedTask(WebSocketSession session, TimeSweepParamter param, TimeMeasureContext context) {
        // 创建一个单线程的定时任务执行器
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        context.scheduler = scheduler;

        Constants.startTime = LocalDateTime.now();
        final long startTime = System.currentTimeMillis();  // 记录任务开始的时间
        final double intervalTime = Double.parseDouble(param.getIntervalTime());
        final int[] counter = {0}; // 记录执行次数

        ChangeParam baselineData = JSON.parseObject(context.task.getZeroParam(), ChangeParam.class);

        // 扫描的总个数
        Integer totalTime = Integer.valueOf(param.getTotalNumber()) + 1;
        // 时间间隔
        Integer interval = Integer.valueOf(param.getIntervalTime());

        // 每隔指定间隔执行一次任务
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 检查是否应该停止
                if (counter[0] >= totalTime || !session.isOpen() || !context.running.get()) {
                    scheduler.shutdown(); // 停止任务
                    Constants.endTime = LocalDateTime.now();
                    saveDataSync(session, context); // 同步保存数据
                    return;
                }

                // 计算当前执行的时间差（x 值）
                long currentTime = System.currentTimeMillis();
                double elapsedTime = (currentTime - startTime) / 1000.0;  // 毫秒转为秒
                log.debug("Elapsed time: " + elapsedTime + " seconds");

                // 更新 x 值
                BigDecimal xValue = BigDecimal.valueOf(Math.floor(elapsedTime));

                // 开始扫描
                Command command = new Command(SetParamEnum.Measure.getComId(), new Object[]{});
                ChangeParam changeParam = devMessageUtil.timeSweepGetMeasureData(command, context.ip);

                if (changeParam == null) {
                    log.error("获取测量数据失败");
                    return;
                }

                context.originalData.add(changeParam);

                // 计算当前 y 值
                String yValue = calculateYValue(param, baselineData, changeParam);
                if (yValue != null) {
                    context.x_data.add(xValue);
                    context.y_data.add(new BigDecimal(yValue));
                    pushDataPoint(session, xValue, yValue, context.currentSampleId);
                }

                counter[0]++;
            } catch (Exception e) {
                log.error("定时任务执行异常", e);
            }
        }, 0, interval, TimeUnit.SECONDS);
    }


    private String calculateYValue(TimeSweepParamter param, ChangeParam baselineData, ChangeParam changeParam) {
        try {
            switch (param.getLuminosity()) {
                case "1": // Abs
                    String s = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
                    return DataCountUtil.changeTPercentOrAbs2(null, s);
                case "2": // T%
                    String t = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
                    return DataCountUtil.changeTPercentOrAbs2(t, null);
                case "3": // Es
                    Double sData = Double.valueOf(changeParam.getS());
                    return String.valueOf((sData / (4095 * 32)) * 100);
                case "4": // Er
                    Double rData = Double.valueOf(changeParam.getR());
                    return String.valueOf((rData / (4095 * 32)) * 100);
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("计算Y值异常", e);
            return null;
        }
    }

    private void pushDataPoint(WebSocketSession session, BigDecimal xValue, String yValue, String sampleId) throws IOException {
        JSONObject payload = new JSONObject();
        JSONArray points = new JSONArray();
        JSONObject point = new JSONObject();
        point.put("x", xValue);
        point.put("y", yValue);
        points.add(point);
        payload.put("data", points);
        payload.put("sampleId", sampleId);
        session.sendMessage(new TextMessage(payload.toJSONString()));
    }

    /**
     * 同步保存数据
     */
    private void saveDataSync(WebSocketSession session, TimeMeasureContext context) {
        log.info("开始同步保存测量数据, 样品: {}, 数据点数: {}", context.currentSampleId, context.x_data.size());
        try {
            saveData(session, context);
            context.markDataSaved();
            log.info("同步保存测量数据完成, 样品: {}", context.currentSampleId);
        } catch (Exception e) {
            log.error("同步保存测量数据异常", e);
            context.markDataSaved(); // 即使异常也要标记完成，避免死锁
        }
    }

    /**
     * 存储数据
     */
    private void saveData(WebSocketSession session, TimeMeasureContext context) {
        try {
            if (context.x_data.isEmpty() || context.y_data.isEmpty()) {
                log.warn("没有数据需要保存");
                return;
            }

            // 先对数据进行寻峰寻谷
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("x_data", context.x_data);
            body.put("y_data", context.y_data);
            body.put("height", 0.5);
            body.put("distance", 1);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            Map<String, Object> resultMap = restTemplate.postForEntity(pythonPathConstant.getPeakSearching(), request, Map.class).getBody();
            Map<String, Object> resultMap1 = restTemplate.postForEntity(pythonPathConstant.getValleySearching(), request, Map.class).getBody();

            List<Double> xDataFeng = (List<Double>) resultMap.get("peak_x");
            List<String> yDataFeng = (List<String>) resultMap.get("peak_y");
            List<Double> xDataGu = (List<Double>) resultMap1.get("valley_x");
            List<String> yDataGu = (List<String>) resultMap1.get("valley_y");

            // 创建一个测量数据对象
            TMeasureData tMeasureData = new TMeasureData();
            // 查询是否已存在此样品的测量数据
            TMeasureData existingData = tMeasureDataService.getOne(
                    new QueryWrapper<TMeasureData>()
                            .eq("task_id", context.task.getId())
                            .eq("sample_key", context.currentSampleId)
                            .orderByDesc("serial_number")
                            .last("LIMIT 1")
            );

            // 如果数据存在，则更新数据
            if (existingData != null) {
                tMeasureData.setId(existingData.getId());
                tMeasureData.setSerialNumber(existingData.getSerialNumber() + 1);  // 序列号递增
            } else {
                // 如果数据不存在，则设置为新数据
                tMeasureData.setSerialNumber(1);
            }

            List<String> listData = new ArrayList<>();
            for (int i = 0; i < context.x_data.size(); i++) {
                listData.add(context.x_data.get(i) + "," + context.y_data.get(i));
            }

            // 波峰和波谷的标记
            List<PeakValleyMarking> peakValleys = new ArrayList<>();
            List<PeakValleyMarking> markings = new ArrayList<>();
            for (int i = 0; i < xDataFeng.size(); i++) {
                PeakValleyMarking marking = new PeakValleyMarking();
                marking.setX(xDataFeng.get(i).toString());
                marking.setY(yDataFeng.get(i));
                marking.setType("波峰");
                peakValleys.add(marking);
            }
            for (int i = 0; i < xDataGu.size(); i++) {
                PeakValleyMarking marking = new PeakValleyMarking();
                marking.setX(xDataGu.get(i).toString());
                marking.setY(yDataGu.get(i));
                marking.setType("波谷");
                markings.add(marking);
            }

            // 设置测量数据
            tMeasureData.setTaskId(context.task.getId());
            tMeasureData.setSampleKey(context.currentSampleId);
            tMeasureData.setInstrumentData(JSON.toJSONString(context.originalData));
            tMeasureData.setOriginalCalculationData(JSON.toJSONString(listData));
            tMeasureData.setMeasurementStartTime(Constants.startTime);
            tMeasureData.setMeasurementEndTime(Constants.endTime);
            tMeasureData.setIsDelete("0");

            // 保存或更新测量数据
            boolean save = tMeasureDataService.saveOrUpdate(tMeasureData);
            if (!save) {
                log.error("保存测量数据失败");
                return;
            }

            // 创建分析数据对象
            TMeasureAnalysisData analysisData = new TMeasureAnalysisData();

            // 查询是否已存在此样品的分析数据
            TMeasureAnalysisData existingAnalysisData = tMeasureAnalysisDataService.getOne(
                    new QueryWrapper<TMeasureAnalysisData>()
                            .eq("task_id", context.task.getId())
                            .eq("sample_key", context.currentSampleId)
                            .orderByDesc("analysis_number")
                            .last("LIMIT 1")
            );

            // 如果数据存在，则更新分析数据
            if (existingAnalysisData != null) {
                analysisData.setAnalysisNumber(existingAnalysisData.getAnalysisNumber() + 1);  // 分析号递增
            } else {
                // 如果数据不存在，则设置为新分析
                analysisData.setAnalysisNumber(1);
            }

            // 根据样品id查询样品数据
            TMeasureSample sample = tMeasureSampleService.getById(context.currentSampleId);
            analysisData.setAnalysisName(sample.getSampleName() + "(" + analysisData.getAnalysisNumber() + ")");
            analysisData.setTaskId(context.task.getId());
            analysisData.setSampleKey(context.currentSampleId);
            analysisData.setMeasureDataId(tMeasureData.getId());
            analysisData.setAnalysisParam(JSON.toJSONString(context.originalData));
            analysisData.setAnalysisResult(JSON.toJSONString(listData));
            analysisData.setAnalysisType("5");
            analysisData.setPeak(com.alibaba.fastjson2.JSON.toJSONString(peakValleys));
            analysisData.setGrain(com.alibaba.fastjson2.JSON.toJSONString(markings));
            analysisData.setColor("#000000");
            analysisData.setIsDelete("0");

            // 保存或更新分析数据
            boolean save1 = tMeasureAnalysisDataService.saveOrUpdate(analysisData);
            if (!save1) {
                log.error("保存分析数据失败");
                return;
            }

            // 更新样品的测试状态
            List<TaskSpecimen> specimen = JSON.parseArray(context.task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen taskSpecimen : specimen) {
                if (taskSpecimen.getSpecimenKey().equals(context.currentSampleId)) {
                    taskSpecimen.setIsTested("1");
                    break;
                }
            }

            // 更新任务数据
            context.task.setSampleKey(JSON.toJSONString(specimen));
            context.task.setDeviceSerialNumber(context.deviceSerialNumber);
            boolean update = tMeasureTaskService.updateById(context.task);
            if (!update) {
                log.error("更新任务失败");
                return;
            }

            log.info("数据保存完成: 样品={}, 点数={}", context.currentSampleId, context.x_data.size());

            JSONObject payload = new JSONObject();
            payload.put("result", "finish");
            sendMessageSafe(session, String.valueOf(payload));

        } catch (Exception e) {
            log.error("保存数据异常", e);
            throw new RuntimeException("数据保存失败", e);
        }
    }

    /**
     * 收到前端消息时触发
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String msg = message.getPayload();
        log.info("收到前端消息: " + msg);

        if ("close".equalsIgnoreCase(msg)) {
            handleStopRequest(session.getId());
        }
    }


    /**
     * 处理停止请求
     */
    private void handleStopRequest(String sessionId) {
        log.info("处理时间单扫停止请求: {}", sessionId);

        TimeMeasureContext context = contexts.get(sessionId);
        WebSocketSession session = sessions.get(sessionId);

        if (context == null || session == null) {
            return;
        }

        try {
            // 停止任务
            context.stopAllTasks();

            // 发送设备停止命令
            if (context.ip != null) {
                try {
                    Command stopCmd = new Command(SetParamEnum.Stop.getComId(), new Object[]{});
                    devMessageUtil.stop(stopCmd, context.ip);
                } catch (Exception e) {
                    log.error("发送设备停止命令异常", e);
                }
            }

            // 设置结束时间
            Constants.endTime = LocalDateTime.now();

            // 保存已采集的数据
            if (!context.x_data.isEmpty() && context.currentSampleId != null) {
                log.info("停止时保存已采集数据, 点数: {}", context.x_data.size());
                saveDataSync(session, context);
            } else {
                context.markDataSaved();
            }

            // 等待数据保存完成
            log.info("等待数据保存完成...");
            boolean saved = context.waitForDataSave(30, TimeUnit.SECONDS);
            if (saved) {
                log.info("数据保存完成");
            } else {
                log.warn("数据保存超时");
            }

        } catch (Exception e) {
            log.error("处理停止请求异常", e);
        } finally {
            safeCleanup(sessionId, true);
        }
    }

    /**
     * 前端连接关闭时触发
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        log.info("时间单扫连接关闭: {}, 原因: {}", sessionId, status);
        handleConnectionClosed(sessionId);
    }


    /**
     * 连接异常时触发
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = session.getId();
        log.error("时间单扫传输错误: {}", sessionId, exception);
        handleConnectionClosed(sessionId);
    }


    /**
     * 处理连接关闭
     */
    private void handleConnectionClosed(String sessionId) {
        TimeMeasureContext context = contexts.get(sessionId);
        WebSocketSession session = sessions.get(sessionId);

        if (context == null) {
            safeCleanup(sessionId, false);
            return;
        }

        try {
            // 停止任务
            context.stopAllTasks();

            // 设置结束时间
            Constants.endTime = LocalDateTime.now();

            // 尝试保存数据
            if (!context.x_data.isEmpty() && context.currentSampleId != null) {
                log.info("连接关闭时保存已采集数据, 点数: {}", context.x_data.size());
                saveDataSync(session, context);
            } else {
                context.markDataSaved();
            }

            // 等待数据保存完成
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
            safeCleanup(sessionId, true);
        }
    }




    /**
     * 安全清理
     */
    private void safeCleanup(String sessionId, boolean releaseDevice) {
        log.info("开始安全清理时间单扫会话: {}", sessionId);

        TimeMeasureContext context = contexts.get(sessionId);
        if (context != null) {
            // 确保任务已停止
            context.stopAllTasks();

            // 等待数据保存完成
            if (!context.dataSaved.get()) {
                log.info("等待最终数据保存...");
                context.waitForDataSave(5, TimeUnit.SECONDS);
            }

            // 释放设备
            if (releaseDevice && context.deviceSerialNumber != null) {
                try {
                    deviceInfoService.releaseDevice(context.deviceSerialNumber);
                    log.info("释放时间单扫设备: {}", context.deviceSerialNumber);
                } catch (Exception e) {
                    log.error("释放设备异常", e);
                }
            }

            contexts.remove(sessionId);
        }

        // 移除任务线程
        Thread taskThread = taskThreads.remove(sessionId);
        if (taskThread != null) {
            // 不再直接中断线程，因为已经通过上下文停止了
            log.info("移除任务线程: {}", sessionId);
        }

        // 关闭WebSocket会话
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.close();
                log.info("WebSocket连接已关闭: {}", sessionId);
            } catch (IOException e) {
                log.debug("关闭WebSocket会话异常: {}", sessionId);
            }
        }

        exceptionHandling();
        log.info("时间单扫会话安全清理完成: {}", sessionId);
    }

    private void sendMessageSafe(WebSocketSession session, String message) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            log.debug("发送消息失败: {}", session.getId());
        }
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

    /**
     * 发送消息并关闭连接
     *
     * @param session
     * @param msg
     */
    private void sendAndClose(WebSocketSession session, String msg) {
        try {
            session.sendMessage(new TextMessage(msg));
            exceptionHandling();
            sessions.remove(session.getId());
            session.close();
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }


    /**
     * 全局数据清空
     */
    private void exceptionHandling() {
        Constants.startTime = null;
        Constants.endTime = null;
    }

}
