package com.persee.workstation.uv_inexus.websocket.concise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTimeSweepParamter;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Component
public class TimingWebSocket extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TimingWebSocket.class);
    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Future<?> future;



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
    private FSamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    private FMeasureSampleService tMeasureSampleService;

    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private RestTemplate restTemplate;

    // WebSocket 会话对象
    private WebSocketSession currentSession;

    // 当WebSocket连接建立成功时，会调用此方法
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        // 将耗时逻辑提交到线程池
         future = executor.submit(() -> {
            try {
                log.info("WebSocket连接已建立: " + session.getId());
                sessions.put(session.getId(), session);
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
                FMeasureTask task = tMeasureTaskService.getById(taskId);
                // 根据方法id查询方法数据
                FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
                // 将方法参数提取出来
                FTimeSweepParamter timeSweepParameter = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);

                startTimedTask(session, timeSweepParameter, deviceSerialNumber, task,sampleId);
            } catch (Exception e) {
                e.printStackTrace();
                sendAndClose(session, "执行异常: " + e.getMessage());
            }
        });
    }


    // 开始定时任务
    private void startTimedTask(WebSocketSession session,
                                FTimeSweepParamter param,
                                String deviceSerialNumber,
                                FMeasureTask task,
                                String sampleId) {
        // 获取任务的校零参数
        final ChangeParam baselineData = (task.getZeroParam() != null && !task.getZeroParam().equals(""))
                ? JSON.parseObject(task.getZeroParam(), ChangeParam.class)
                : null;


        String ip = deviceInfoService.getIp(deviceSerialNumber);
        // 根据序列号查询对应的附件配置表信息
        QueryWrapper<FSamplePoolConfigPO> wrapper = new QueryWrapper<>();
        wrapper.eq("device_serial_number", deviceSerialNumber);
        FSamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);
        if (configPO == null) {
            sendAndClose(session, "没有找到对应的附件配置信息");
        }
        String maxLocation = "";
        if (configPO.getIsAutoChangePool()){
            FMeasureSample sample = tMeasureSampleService.getById(sampleId);
            maxLocation = sample.getSampleLocation();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();


        boolean poolStatus = false;
        if (configPO.getIsAutoChangePool()){
            List<FSamplePoolStatus> poolStatuses = JSON.parseArray(configPO.getPoolInfo(), FSamplePoolStatus.class);
            for (FSamplePoolStatus status : poolStatuses) {
                if (status.getPoolStatus()){
                    poolStatus = true;
                    break;
                }
            }
        }


        ///  设置样品池位置
        FMeasureSample sample = tMeasureSampleService.getById(sampleId);
        if (configPO.getIsAutoChangePool()){
            if (sample == null){
                sendAndClose(session, "请先创建样品");
                return;
            }

            if (poolStatus){
                R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
                if (!stringR.getSuccess()) {
                    sendAndClose(session, "设置出错");
                    return;
                }
                JSONObject object = new JSONObject();
                object.put("currentPoolNum",sample.getSampleLocation());
                try {
                    session.sendMessage(new TextMessage(object.toJSONString()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // 计算并执行定时任务
        int totalTasks = Integer.parseInt(param.getTotalNumber());  // 执行的总次数
        CountDownLatch latch = new CountDownLatch(totalTasks);  // 每个样品的定时任务数作为latch的计数

        executeTimedTaskForSample(session, param, baselineData, sampleId, ip, latch,task,deviceSerialNumber);

        try {
            latch.await();  // 等待当前样品的所有任务完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        sendAndClose(session, "样品任务执行完毕");

    }

    // 执行单个样品的定时任务
    private void executeTimedTaskForSample(WebSocketSession session,
                                           FTimeSweepParamter param,
                                           ChangeParam baselineData,
                                           String sampleId,
                                           String ip,
                                           CountDownLatch latch,
                                           FMeasureTask task,
                                           String deviceSerialNumber) {
        int interval = Integer.parseInt(param.getIntervalTime()); // 间隔时间（秒）

        // 根据总时长和间隔计算总任务次数
        int totalTasks = Integer.parseInt(param.getTotalNumber());  // 执行的总次数

        long currentStartTime = System.currentTimeMillis();  // 当前任务开始时间

        // 记录数据
        List<BigDecimal> x_data = new ArrayList<>();
        List<BigDecimal> y_data = new ArrayList<>();
        List<ChangeParam> originalData = new ArrayList<>();
        for (int i = 0; i < totalTasks; i++) {
            // 计算任务执行的时间点
            final long nextExecutionTime = currentStartTime + (i * interval * 1000L);

            int finalI = i;
            scheduler.schedule(() -> {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        return;  // 如果线程被中断，直接退出
                    }
                    // 计算延迟时间，确保任务精确执行
                    long delay = nextExecutionTime - System.currentTimeMillis();
                    if (delay > 0) {
                        Thread.sleep(delay);  // 等待下一个执行时间点
                    }

                    // 执行测量任务
                    BigDecimal xValue = BigDecimal.valueOf((System.currentTimeMillis() - currentStartTime) / 1000.0);
                    Command command = new Command(SetParamEnum.Measure.getComId(), new Object[]{});
                    ChangeParam changeParam = devMessageUtil.timeSweepGetMeasureData(command, ip);

                    if (changeParam == null) {
                        // 使用shutdown()方法结束任务
                        scheduler.shutdown();
                        // 发送错误信息并关闭会话
                        sendAndClose(session, "扫描失败");
                        latch.countDown(); // 完成当前任务
                        return;
                    }


                    originalData.add(changeParam);

                    // 计算并推送 Y 值（光度模式等相关计算）
                    calculateAndPushYValue(param, baselineData, changeParam, xValue, x_data, y_data, session, sampleId);

                    if (finalI == totalTasks - 1) {
                        // 最后一次测量完成，结束任务
                        Constants.endTime = LocalDateTime.now();
                        saveData(session, originalData, x_data, y_data, sampleId, task, deviceSerialNumber); // 保存数据
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                    sendAndClose(session, "任务执行出现错误");
                } finally {
                    latch.countDown(); // 每个定时任务完成后，计数器减一
                }
            }, nextExecutionTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);  // 精确调度任务到指定时间
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
     * 计算并推送 Y 值（光度模式等相关计算）
     *
     * @param param
     * @param baselineData
     * @param changeParam
     * @param xValue
     * @param x_data
     * @param y_data
     * @param session
     * @param sampleId
     * @throws IOException
     */
    private void calculateAndPushYValue(
            FTimeSweepParamter param,
            ChangeParam baselineData,
            ChangeParam changeParam,
            BigDecimal xValue,
            List<BigDecimal> x_data,
            List<BigDecimal> y_data,
            WebSocketSession session,
            String sampleId) throws IOException {

        if (param.getLuminosity().equals("1")) {
            String s = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
            String y = DataCountUtil.changeTPercentOrAbs2(null,s);
            x_data.add(xValue);
            y_data.add(new BigDecimal(y));
            JSONObject payload = new JSONObject();
            JSONArray points = new JSONArray();
            JSONObject point = new JSONObject();
            point.put("x", xValue);
            point.put("y", y);
            points.add(point);
            payload.put("data", points);
            payload.put("sampleId", sampleId);
            session.sendMessage(new TextMessage(payload.toJSONString()));
        } else if (param.getLuminosity().equals("2")) {
            String s = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
            x_data.add(xValue);
            y_data.add(new BigDecimal(s));
            JSONObject payload = new JSONObject();
            JSONArray points = new JSONArray();
            JSONObject point = new JSONObject();
            point.put("x", xValue);
            point.put("y", s);
            points.add(point);
            payload.put("data", points);
            payload.put("sampleId", sampleId);
            session.sendMessage(new TextMessage(payload.toJSONString()));
        } else if (param.getLuminosity().equals("3")) {
            Double sData = Double.valueOf(changeParam.getS());
            String y = String.valueOf((sData / (4095 * 32)) * 100);
            x_data.add(xValue);
            y_data.add(new BigDecimal(y));
            JSONObject payload = new JSONObject();
            JSONArray points = new JSONArray();
            JSONObject point = new JSONObject();
            point.put("x", xValue);
            point.put("y", y);
            points.add(point);
            payload.put("data", points);
            payload.put("sampleId", sampleId);
            session.sendMessage(new TextMessage(payload.toJSONString()));
        } else if (param.getLuminosity().equals("4")) {
            Double rData = Double.valueOf(changeParam.getR());
            String y = String.valueOf((rData / (4095 * 32)) * 100);
            x_data.add(xValue);
            y_data.add(new BigDecimal(y));
            JSONObject payload = new JSONObject();
            JSONArray points = new JSONArray();
            JSONObject point = new JSONObject();
            point.put("x", xValue);
            point.put("y", y);
            points.add(point);
            payload.put("data", points);
            payload.put("sampleId", sampleId);
            session.sendMessage(new TextMessage(payload.toJSONString()));
        } else {
            sendAndClose(session, "光度模式错误");
            session.close();
        }
    }


    /**
     * 存储数据
     *
     * @param session
     * @param originalData
     * @param x_data
     * @param y_data
     * @param sampleId
     * @param task
     * @throws IOException
     */
    private void saveData(WebSocketSession session,
                          List<ChangeParam> originalData,
                          List<BigDecimal> x_data,
                          List<BigDecimal> y_data,
                          String sampleId,
                          FMeasureTask task,
                          String deviceSerialNumber) {
        boolean dataStatus = false;
        // 先对数据进行寻峰寻谷
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("x_data", x_data);
        body.put("y_data", y_data);
        body.put("height", 0.5);
        body.put("distance", 1);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request, Map.class).getBody();
        List<Double> xDataFeng = (List<Double>) resultMap.get("peak_x");
        List<String> yDataFeng = (List<String>) resultMap.get("peak_y");
        List<Double> xDataGu = (List<Double>) resultMap1.get("valley_x");
        List<String> yDataGu = (List<String>) resultMap1.get("valley_y");

        // 波峰和波谷的标记
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
        // 查询是否已存在此样品的测量数据
        FMeasureData tMeasureData = tMeasureDataService.getOne(
                new QueryWrapper<FMeasureData>()
                        .eq("task_id", task.getId())
                        .eq("sample_key", sampleId)
                        .orderByDesc("serial_number")
                        .last("LIMIT 1")
        );

        List<String> listData = new ArrayList<>();
        for (int i = 0; i < x_data.size(); i++) {
            listData.add(x_data.get(i) + "," + y_data.get(i));
        }
        // 如果数据存在，则更新数据
        if (tMeasureData == null) {
            // 如果数据不存在，则设置为新数据
            tMeasureData = new FMeasureData();
            tMeasureData.setSerialNumber(1);
            // 设置测量数据
            tMeasureData.setTaskId(task.getId());
            tMeasureData.setSampleKey(sampleId);
            tMeasureData.setInstrumentData(JSON.toJSONString(originalData));
            tMeasureData.setOriginalCalculationData(JSON.toJSONString(listData));
            tMeasureData.setMeasurementStartTime(Constants.startTime);
            tMeasureData.setMeasurementEndTime(Constants.endTime);
            tMeasureData.setIsSave("1");
            tMeasureData.setIsDelete("0");
        }else{
            dataStatus = true;
            tMeasureData.setInstrumentData(JSON.toJSONString(originalData));
            tMeasureData.setOriginalCalculationData(JSON.toJSONString(listData));
            tMeasureData.setMeasurementStartTime(Constants.startTime);
            tMeasureData.setMeasurementEndTime(Constants.endTime);
        }






        // 保存或更新测量数据
        boolean save = tMeasureDataService.saveOrUpdate(tMeasureData);
        if (!save) {
            sendAndClose(session, "保存测量数据失败");
            return;
        }


        // 如果数据存在，则更新分析数据
        if (dataStatus) {
            // 根据测量id和任务id查询分析数据
            QueryWrapper<FMeasureAnalysisData> wrapper = new QueryWrapper<>();
            wrapper.eq("measure_data_id", tMeasureData.getId());
            wrapper.eq("task_id", task.getId());
            wrapper.eq("is_delete", "0");
            wrapper.last("limit 1");
            FMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(wrapper);
            if (analysisData == null) {
                sendAndClose(session, "查询分析数据失败");
                return;
            }
            analysisData.setAnalysisParam(JSON.toJSONString(originalData));
            analysisData.setAnalysisResult(JSON.toJSONString(listData));
            analysisData.setPeak(JSON.toJSONString(peakValleys));
            analysisData.setGrain(JSON.toJSONString(markings));
            // 保存或更新分析数据
            boolean save1 = tMeasureAnalysisDataService.saveOrUpdate(analysisData);
            if (!save1) {
                sendAndClose(session, "保存分析数据失败");
                return;
            }
        } else {
            FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
            // 如果数据不存在，则设置为新分析
            analysisData.setAnalysisNumber(1);
            // 根据样品id查询样品数据
            FMeasureSample sample = tMeasureSampleService.getById(sampleId);
            analysisData.setAnalysisName(sample.getSampleName() + "(" + analysisData.getAnalysisNumber() + ")");
            analysisData.setTaskId(task.getId());
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(tMeasureData.getId());
            analysisData.setAnalysisParam(JSON.toJSONString(originalData));
            analysisData.setAnalysisResult(JSON.toJSONString(listData));
            analysisData.setAnalysisType("5");
            analysisData.setPeak(com.alibaba.fastjson2.JSON.toJSONString(peakValleys));
            analysisData.setGrain(com.alibaba.fastjson2.JSON.toJSONString(markings));
            analysisData.setColor("#000000");
            analysisData.setIsSave("1");
            analysisData.setIsDelete("0");
            // 保存或更新分析数据
            boolean save1 = tMeasureAnalysisDataService.saveOrUpdate(analysisData);
            if (!save1) {
                sendAndClose(session, "保存分析数据失败");
                return;
            }

            FMeasureAnalysisImage analysisImage = new FMeasureAnalysisImage();
            analysisImage.setAnalysisId(analysisData.getId());
            boolean save2 = tMeasureAnalysisImageService.save(analysisImage);
            if (!save2) {
                sendAndClose(session, "新增分析数据图片数据失败");
            }
        }





// 更新样品的测试状态
        List<FTaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        for (FTaskSpecimen taskSpecimen : specimen) {
            if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                taskSpecimen.setIsTested("1");
                break;
            }
        }

// 更新任务数据
        task.setSampleKey(JSON.toJSONString(specimen));
        task.setDeviceSerialNumber(deviceSerialNumber);
        boolean update = tMeasureTaskService.updateById(task);
        if (!update) {
            sendAndClose(session, "更新任务失败");
            return;
        }

        System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        JSONObject payload = new JSONObject();
        payload.put("result", "finish");

// 将公共的变量中的数据清除
        sendAndClose(session, String.valueOf(payload));
    }

    /**
     * 全局数据清空
     */
    private void exceptionHandling() {
        Constants.startTime = null;
        Constants.endTime = null;
    }


    // 当WebSocket接收到消息时，会调用此方法
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        // 处理客户端发来的消息
        log.info("收到客户端消息: " + message.getPayload());
    }

    // 当WebSocket连接关闭时，会调用此方法
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        log.info("WebSocket连接已关闭: " + session.getId());
        this.currentSession = null;  // 清理会话

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();  // 强制停止所有任务
            log.info("定时任务已强制停止");
        }
        // 取消正在执行的任务
        if (future != null && !future.isDone()) {
            future.cancel(true);
            log.info("定时任务已强制停止");
        }

    }


    /**
     * 发送消息并关闭连接
     * @param session
     * @param msg
     */
    private void sendAndClose(WebSocketSession session, String msg) {
        try {
            session.sendMessage(new TextMessage(msg));
            exceptionHandling();
            session.close();
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }
}
