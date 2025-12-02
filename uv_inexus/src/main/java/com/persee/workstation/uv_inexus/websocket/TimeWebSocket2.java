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
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumParameter;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
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
import java.util.stream.Collectors;

/**
 * WebSocket处理器类，继承自TextWebSocketHandler
 * 用于处理WebSocket连接的建立、消息接收和连接关闭等事件
 */
@Component
public class TimeWebSocket2 extends TextWebSocketHandler {

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();


    @Autowired
    private TMeasureTaskService tMeasureTaskService;

    @Autowired
    private TimeSweepService timeSweepService;

    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private TMeasureSampleService  tMeasureSampleService;

    @Autowired
    private PythonPathConstant pythonPathConstant;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private TMeasureDataService tMeasureDataService;

    @Autowired
    private TMeasureAnalysisImageService tMeasureAnalysisImageService;

    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    private TMeasureTaskAttachmentService tMeasureTaskAttachmentService;


    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 当WebSocket连接建立后调用此方法
     * @param session 当前WebSocket会话，包含连接信息
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 打印客户端连接信息，包括会话ID和连接时间
        System.out.println("客户端进入: " + session.getId() + " 时间: " + LocalDateTime.now());

        if (Constants.endTime != null||Constants.startTime != null) {
            sendAndClose(session, "当前测试未结束,无法开启新的测试");
            return;
        }

        // 记录当前连接
        sessions.put(session.getId(), session);

        // 解析参数
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String deviceSerialNumber= getQueryParam(query, "deviceSerialNumber");
        if (taskId == null) {
            sendAndClose(session, "缺少 taskId 参数");
            return;
        }
        if (deviceSerialNumber == null) {
            sendAndClose(session, "缺少 deviceSerialNumber 参数");
            return;
        }

        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task.getSampleKey()==null|| task.getSampleKey().isEmpty()) {
            sendAndClose(session, "请先创建样品");
            return;
        }
        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 将方法参数提取出来
        TimeSweepParamter timeSweepParameter = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);

        Thread taskThread = new Thread(() -> {
            startTimedTask(session,timeSweepParameter,deviceSerialNumber,task); // 启动推送任务
        });

        taskThreads.put(session.getId(), taskThread);
        taskThread.start();

    }

    /**
     * 启动定时任务，每隔 1 秒推送数据给前端
     * @param session
     */
    private void startTimedTask(WebSocketSession session,TimeSweepParamter param,String deviceSerialNumber,TMeasureTask task) {



        // 扫描的总个数
        Integer totalTime = Integer.valueOf(param.getTotalNumber())+1;
        // 时间间隔
        Integer interval = Integer.valueOf(param.getIntervalTime());
        // 扫描的单位
        TimeUnit timeUnit = param.getScanUnit().equals("0") ? TimeUnit.MINUTES : TimeUnit.SECONDS;

        // 获取任务的校零参数
        final ChangeParam baselineData = (task.getZeroParam() != null && !task.getZeroParam().equals(""))
                ? JSON.parseObject(task.getZeroParam(), ChangeParam.class)
                : null;
        // 获取任务样品参数
        List<TaskSpecimen> specimenList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<TaskSpecimen> newSampleData = new ArrayList<>();
        for (TaskSpecimen taskSpecimen : specimenList) {
            // 样品未测量并且未删除
            if ("0".equals(taskSpecimen.getDeleteType()) && "0".equals(taskSpecimen.getIsTested())) {
                newSampleData.add(taskSpecimen);
            }
        }

        if (newSampleData.isEmpty() && !specimenList.isEmpty()){
            sendAndClose(session, "您的样品已测完,请重新添加样品");
        }

        List<String> sampleIds = newSampleData.stream().map(obj -> obj.getSpecimenKey()).collect(Collectors.toCollection(ArrayList::new));
        QueryWrapper<TMeasureSample> eq = new QueryWrapper<>();
        eq.in("id", sampleIds);
        List<TMeasureSample> list = tMeasureSampleService.list(eq);
        String maxLocation = list.stream().map(TMeasureSample::getSampleLocation).max(String::compareTo).orElse(null);


        if (maxLocation == null) {
            sendAndClose(session, "样品位置出错");
        }

        // 根据多联池模式进行校验
        if (param.getMultiPoolMode().equals("0")){
            // 单池重复连续采样
            for (TaskSpecimen specimen : newSampleData) {
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

                CountDownLatch latch = new CountDownLatch(1); // 用来等定时任务结束
                final int[] counter = {0};
                final long startTime = System.currentTimeMillis();
                final long intervalTime = Long.parseLong(param.getIntervalTime());

                List<BigDecimal> x_data = new ArrayList<>();
                List<BigDecimal> y_data = new ArrayList<>();
                List<ChangeParam> originalData = new ArrayList<>();

                scheduler.scheduleAtFixedRate(() -> {
                    if (counter[0] >= totalTime || !session.isOpen()) {
                        scheduler.shutdown();
                        latch.countDown(); // 通知主线程：这个 specimen 完成了
                        Constants.endTime = LocalDateTime.now();
                        saveData(session,originalData,x_data,y_data,specimen.getSpecimenKey(),task,deviceSerialNumber); // 保存数据
                        return;
                    }

                    BigDecimal xValue = BigDecimal.valueOf(
                            Math.floor((System.currentTimeMillis() - startTime) / 1000.0)
                    );

                    String ip = deviceInfoService.getIp(deviceSerialNumber);
                    Command command = new Command(SetParamEnum.Measure.getComId(), new Object[]{});
                    ChangeParam changeParam = devMessageUtil.timeSweepGetMeasureData(command, ip);

                    if (changeParam == null) {
                        scheduler.shutdown();
                        sendAndClose(session, "扫描失败");
                        latch.countDown();
                        return;
                    }
                    originalData.add(changeParam);

                    try {
                        calculateAndPushYValue(param, baselineData, changeParam, xValue, x_data, y_data, session,specimen.getSpecimenKey());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    counter[0]++;
                }, 0, intervalTime, timeUnit);

                try {
                    latch.await(); // ⬅️ 等待当前 specimen 的定时器跑完
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                TMeasureSample byId = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (byId.getSampleLocation().equals(maxLocation)) {
                    break;
                }
            }

        }else if (param.getMultiPoolMode().equals("1")){
            // 多池重复交替采样

            sendAndClose(session, "暂不支持多池重复交替采样");

        }else{
            sendAndClose(session, "多联池模式参数错误");
            return;
        }
        JSONObject payload = new JSONObject();
        payload.put("result", "finish");
        // 将公共的变量中的数据清除
        sendAndClose(session, String.valueOf(payload));
    }


    private void calculateAndPushYValue(
            TimeSweepParamter param,
            ChangeParam baselineData,
            ChangeParam changeParam,
            BigDecimal xValue,
            List<BigDecimal> x_data,
            List<BigDecimal> y_data,
            WebSocketSession session,
            String sampleId) throws IOException {

        if (param.getLuminosity().equals("1")) {
            String s = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
            String y = DataCountUtil.changeTPercentOrAbs2(null, s);
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
            String y = DataCountUtil.changeTPercentOrAbs2(s, null);
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
                          TMeasureTask task,
                          String deviceSerialNumber) {
        // 先对数据进行寻峰寻谷
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("x_data", x_data);
        body.put("y_data", y_data);
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
        // 根据任务id和样品id去测量数据表中进行查询,根据序列号进行从大到小的排序,并读取最大的那一条
        // 如果发现测量数据表中有数据则将最大的序列号拿出来加1并放入新建的测量数据对象中
        TMeasureData data = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>().eq("task_id", task.getId()).eq("sample_key", sampleId).orderByDesc("serial_number").last("LIMIT 1"));
        if (data != null) {
            tMeasureData.setSerialNumber(data.getSerialNumber() + 1);
        } else {
            tMeasureData.setSerialNumber(1);
        }
        List<String> listData = new ArrayList<>();
        for (int i = 0; i < x_data.size(); i++) {
            listData.add(x_data.get(i) + "," + y_data.get(i));
        }
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
        tMeasureData.setTaskId(task.getId());
        tMeasureData.setSampleKey(sampleId);
        tMeasureData.setInstrumentData(JSON.toJSONString(originalData));
        tMeasureData.setOriginalCalculationData(JSON.toJSONString(listData));
        tMeasureData.setMeasurementStartTime(Constants.startTime);
        tMeasureData.setMeasurementEndTime(Constants.endTime);
        tMeasureData.setIsDelete("0");
        boolean save = tMeasureDataService.save(tMeasureData);
        if (!save) {
            sendAndClose(session, "保存测量数据失败");
            return;
        }

        TMeasureAnalysisData analysisData = new TMeasureAnalysisData();
        TMeasureAnalysisData analysisDataLast = tMeasureAnalysisDataService.getOne(
                new QueryWrapper<TMeasureAnalysisData>()
                        .eq("task_id", task.getId())
                        .eq("sample_key", sampleId)
                        .orderByDesc("analysis_number")
                        .last("LIMIT 1")
        );
        analysisData.setAnalysisNumber(analysisDataLast != null ? analysisDataLast.getAnalysisNumber() + 1 : 1);
        // 根据样品id查询样品数据
        TMeasureSample sample = tMeasureSampleService.getById(sampleId);
        analysisData.setAnalysisName(sample.getSampleName()+"("+analysisData.getAnalysisNumber()+")");
        analysisData.setTaskId(task.getId());
        analysisData.setSampleKey(sampleId);
        analysisData.setMeasureDataId(tMeasureData.getId());
        analysisData.setAnalysisParam(JSON.toJSONString(originalData));
        analysisData.setAnalysisResult(JSON.toJSONString(listData));
        analysisData.setAnalysisType("5");
        analysisData.setPeak(com.alibaba.fastjson2.JSON.toJSONString(peakValleys));
        analysisData.setGrain(com.alibaba.fastjson2.JSON.toJSONString(markings));
        analysisData.setColor("#000000");
        analysisData.setIsDelete("0");
        boolean save1 = tMeasureAnalysisDataService.save(analysisData);
        if (!save1) {
            sendAndClose(session, "保存分析数据失败");
            return;
        }
        TMeasureAnalysisImage tMeasureAnalysisImage = new TMeasureAnalysisImage();
        tMeasureAnalysisImage.setAnalysisId(analysisData.getId());
        tMeasureAnalysisImageService.save(tMeasureAnalysisImage);
        // 创建任务附属表
        TMeasureTaskAttachment tMeasureTaskAttachment = new TMeasureTaskAttachment();
        tMeasureTaskAttachment.setTaskId(task.getId());
        tMeasureTaskAttachmentService.save(tMeasureTaskAttachment);
        List<TaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen taskSpecimen : specimen) {
            if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                taskSpecimen.setIsTested("1");
                break;
            }
        }
        task.setSampleKey(JSON.toJSONString(specimen));
        task.setDeviceSerialNumber(deviceSerialNumber);
        boolean update = tMeasureTaskService.updateById(task);
        if (!update) {
            sendAndClose(session, "更新任务失败");
            return;
        }
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        exceptionHandling();

    }


    /**
     * 计算光度模式的Abs/T%
     * @param spectrumParameter
     * @param baselineCopy
     * @param x
     * @param y
     */
    private void calcOnePoint(SpectrumParameter spectrumParameter,
                              List<ChangeParam> baselineCopy,
                              List<Double> x,
                              List<Double> y) {
        if (spectrumParameter.getLuminosity().equals("1")) {
            String s = DataCountUtil.countTPercent(baselineCopy.get(0), Constants.measurd_data.get(0), 1.0);
            String yy = DataCountUtil.changeTPercentOrAbs2(null, s);
            Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
            x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
            y.add(Double.parseDouble(yy));
        } else if (spectrumParameter.getLuminosity().equals("2")) {
            String s = DataCountUtil.countTPercent(baselineCopy.get(0), Constants.measurd_data.get(0), 1.0);
            String yy = DataCountUtil.changeTPercentOrAbs2(s, null);
            Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
            x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
            y.add(Double.parseDouble(yy));
        } else if (spectrumParameter.getLuminosity().equals("3")) {
            Double sData = Double.valueOf(Constants.measurd_data.get(0).getS());
            String yy = String.valueOf((sData / (4095 * 32)) * 100);
            Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
            x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
            y.add(Double.parseDouble(yy));
        } else if (spectrumParameter.getLuminosity().equals("4")) {
            Double rData = Double.valueOf(Constants.measurd_data.get(0).getR());
            String yy = String.valueOf((rData / (4095 * 32)) * 100);
            Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
            x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
            y.add(Double.parseDouble(yy));
        }
        Constants.measurd_data.remove(0);
        baselineCopy.remove(0);
    }

    /**
     * 处理接收到的文本消息
     * @param session 当前WebSocket会话
     * @param message 接收到的文本消息对象
     * @throws Exception 可能抛出的异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) throws Exception {
        // 打印接收到的消息内容，包括发送方会话ID和消息内容
        System.out.println("收到消息: " + session.getId() + " -> " + message.getPayload());
        // 回显
        session.sendMessage(new org.springframework.web.socket.TextMessage("服务端已收到: " + message.getPayload()));
    }

    /**
     * 重写afterConnectionClosed方法，用于处理WebSocket连接关闭后的操作
     * @param session WebSocket会话对象，包含连接的相关信息
     * @param status 关闭状态，包含连接关闭的原因和代码
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 打印客户端退出信息，包括会话ID、退出时间和关闭原因
        System.out.println("客户端退出: " + session.getId() + " 时间: " + LocalDateTime.now() + " 原因: " + status);
        Thread taskThread = taskThreads.remove(session.getId());
        if (taskThread != null) {
            taskThread.interrupt(); // 直接打断子线程
        }
        sessions.remove(session.getId());
        exceptionHandling(); // 清空公共数据
    }


    /**
     * 获取查询参数
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
