package com.persee.workstation.uv_inexus.websocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumParameter;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.PeakValleyMarking;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

@Component
public class SpectrumWebSocket2 extends TextWebSocketHandler {

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 假设的设备数据缓存队列
    private static final Map<String, List<String>> deviceDataCache = new ConcurrentHashMap<>();


    @Autowired
    private SpectrumService spectrumService;
    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private DevMessageUtil devMessageUtil;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private TMeasureDataService traceDataService;
    @Autowired
    private TMeasureMethodService tMeasureMethodService;
    @Autowired
    private SystemAlgorithmConfService systemAlgorithmConfService;
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Autowired
    private TMeasureDataService tMeasureDataService;
    @Autowired
    private TMeasureSampleService tMeasureSampleService;
    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;


    /**
     * 当WebSocket连接建立后调用此方法
     * @param session 当前WebSocket会话，包含连接信息
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 打印客户端连接信息，包括会话ID和连接时间
        System.out.println("客户端进入: " + session.getId() + " 时间: " + LocalDateTime.now());

        // 将当前会话添加到sessions集合中
        sessions.put(session.getId(), session);

        if (Constants.endTime != null||Constants.startTime != null) {
            sendAndClose(session, "当前测试未结束,无法开启新的测试");
            return;
        }

        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");

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
        if (task.getSampleKey()==null||task.getSampleKey().equals("")) {
            sendAndClose(session, "请先创建样品");
            return;
        }

        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 将方法参数提取出来
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        // 将任务中的Rsd取出来
        List<ChangeParam> baselineData = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
        // 将任务中的样品数据取出来
        List<TaskSpecimen> sampleData = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<TaskSpecimen> newSampleData = new ArrayList<>();
        for (TaskSpecimen taskSpecimen : sampleData) {
            // 样品未测量并且未删除
            if ("0".equals(taskSpecimen.getDeleteType()) && "0".equals(taskSpecimen.getIsTested())) {
                newSampleData.add(taskSpecimen);
            }
        }

        Integer repetitionTime = Integer.valueOf(spectrumParameter.getRepetitionTime());
        Integer timeInterval = Integer.valueOf(spectrumParameter.getTimeInterval());
        boolean repeatMode = "单池重复".equalsIgnoreCase(spectrumParameter.getRepetitionPattern());
        int repeatCount = repetitionTime != null ? repetitionTime : 1;
        long repeatInterval = timeInterval != null ? timeInterval : 0;
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        Command command = new Command();
        command.setCmdId(SetParamEnum.Scan.getComId());
        command.setParams(new Object[]{});
        // 往前端推送数据的线程
        new Thread(() -> {

            try {

                if (!repeatMode) {
                    for (int i = 0; i < newSampleData.size(); i++) {
                        // 预留: 设置样品池位置




                        doOneMeasurement(session, command, spectrumParameter, task, taskId, newSampleData.get(i).getSpecimenKey(), deviceSerialNumber,ip);
                        newSampleData.get(i).setIsTested("1");
                        if ((i+1)!= newSampleData.size()) {
                            JSONObject payload = new JSONObject();
                            payload.put("result", "finish");
                            session.sendMessage(new TextMessage(payload.toJSONString()));
                        }else{
                            // 全部完成后再告诉前端 finish
                            JSONObject payload = new JSONObject();
                            payload.put("result", "finish");
                            sendAndClose(session, String.valueOf(payload));
                        }
                    }
                }else{

                }

            }catch (Exception e) {
                System.err.println("异常!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+e);
                handleTransportError(session, e);
            }

        }).start();

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
    }


    /**
     * 连接异常时触发
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
        }
        exceptionHandling();
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


    // 每次测量
    private void doOneMeasurement(WebSocketSession session,
                                  Command command,
                                  SpectrumParameter spectrumParameter,
                                  TMeasureTask task,
                                  String taskId,
                                  String sampleId,
                                  String deviceSerialNumber,
                                  String  ip) throws IOException {

        // 1. 发送测量命令
        devMessageUtil.getMeasureData(command, ip);

        List<ChangeParam> baselineCopy = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();

        // 2. 采集数据，实时推送（每 10 条推一次）
        int pushCount = 0;
        while (true) {
            List<String> snapshot = new ArrayList<>(Constants.instrumentData);
            if (!snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData)) {
                while (!baselineCopy.isEmpty() && !Constants.measurd_data.isEmpty()) {
                    calcOnePoint(spectrumParameter, baselineCopy, x, y);
                    pushCount = pushDataIfNeeded(session, x, y, pushCount);
                }
                break;
            }
            while (!baselineCopy.isEmpty() && !Constants.measurd_data.isEmpty()) {
                calcOnePoint(spectrumParameter, baselineCopy, x, y);
                pushCount = pushDataIfNeeded(session, x, y, pushCount);
            }
            try { sleep(10); } catch (InterruptedException e) { handleTransportError(session, e); }
        }

        // 3. 推送最后不足10条的数据
        pushRemainingData(session, x, y, pushCount);

        // 4. 保存一次完整测量
        saveOneMeasurement(task, taskId, sampleId, deviceSerialNumber, x, y);

        // 5. 清理缓存
        Constants.instrumentData.clear();
        Constants.measurd_data.clear();
        Constants.originalCalculationData.clear();
        Constants.endTime = null;
    }


    // 推送逻辑：每 10 个点推送一次
    private int pushDataIfNeeded(WebSocketSession session, List<Double> x, List<Double> y, int pushCount) throws IOException {
        if (x.size() >= (pushCount + 1) * 10) {
            JSONArray dataArray = new JSONArray();
            for (int i = pushCount * 10; i < x.size(); i++) {
                JSONObject point = new JSONObject();
                point.put("x", x.get(i));
                point.put("y", y.get(i));
                dataArray.add(point);
            }
            JSONObject payload = new JSONObject();
            payload.put("data", dataArray);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload.toJSONString()));
            }
            pushCount++;
        }
        return pushCount;
    }

    // 推送剩余数据（最后不足10条的情况）
    private void pushRemainingData(WebSocketSession session, List<Double> x, List<Double> y, int pushCount) throws IOException {
        int startIndex = pushCount * 10;
        if (startIndex < x.size()) {
            JSONArray dataArray = new JSONArray();
            for (int i = startIndex; i < x.size(); i++) {
                JSONObject point = new JSONObject();
                point.put("x", x.get(i));
                point.put("y", y.get(i));
                dataArray.add(point);
            }
            JSONObject payload = new JSONObject();
            payload.put("data", dataArray);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload.toJSONString()));
            }
        }
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

    // 保存一次测量结果
    private void saveOneMeasurement(TMeasureTask task,
                                    String taskId,
                                    String sampleId,
                                    String deviceSerialNumber,
                                    List<Double> x,
                                    List<Double> y) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("x_data", x);
        body.put("y_data", y);
        body.put("height", 0.5);
        body.put("distance", 1);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request, Map.class).getBody();
        List<Double> xDataFeng = (List<Double>) resultMap.get("peak_x");
        List<String> yDataFeng = (List<String>) resultMap.get("peak_y");
        List<Double> xDataGu = (List<Double>) resultMap1.get("valley_x");
        List<String> yDataGu = (List<String>) resultMap1.get("valley_y");
        // 进行存储操作
        // 根据任务id和样品id去测量数据表中进行查询,根据序列号进行从大到小的排序,并读取最大的那一条
        // 如果发现测量数据表中有数据则将最大的序列号拿出来加1并放入新建的测量数据对象中
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


        List<String> listData = new ArrayList<>();
        for (int i = 0; i < x.size(); i++) {
            listData.add(x.get(i) + "," + y.get(i));
        }

        // 根据样品id查询样品数据
        TMeasureSample sample = tMeasureSampleService.getById(sampleId);

        TMeasureData measureData = new TMeasureData();
        TMeasureData lastData = tMeasureDataService.getOne(
                new QueryWrapper<TMeasureData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", sampleId)
                        .orderByDesc("serial_number")
                        .last("LIMIT 1")
        );
        measureData.setSerialNumber(lastData != null ? lastData.getSerialNumber() + 1 : 1);
        measureData.setTaskId(taskId);
        measureData.setSampleKey(sampleId);
        measureData.setInstrumentData(JSON.toJSONString(Constants.instrumentData));
        measureData.setOriginalCalculationData(JSON.toJSONString(listData));
        measureData.setMeasurementStartTime(Constants.startTime);
        measureData.setMeasurementEndTime(Constants.endTime);
        measureData.setIsDelete("0");
        traceDataService.save(measureData);

        TMeasureAnalysisData analysisData = new TMeasureAnalysisData();
        TMeasureAnalysisData analysisDataLast = tMeasureAnalysisDataService.getOne(
                new QueryWrapper<TMeasureAnalysisData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", sampleId)
                        .orderByDesc("analysis_number")
                        .last("LIMIT 1")
        );
        analysisData.setAnalysisNumber(analysisDataLast != null ? analysisDataLast.getAnalysisNumber() + 1 : 1);
        analysisData.setAnalysisName(sample.getSampleName()+"("+analysisData.getAnalysisNumber()+")");
        analysisData.setTaskId(taskId);
        analysisData.setSampleKey(sampleId);
        analysisData.setMeasureDataId(measureData.getId());
        analysisData.setAnalysisParam(JSON.toJSONString(Constants.instrumentData));
        analysisData.setAnalysisResult(JSON.toJSONString(listData));
        analysisData.setAnalysisType("5");
        analysisData.setPeak(JSON.toJSONString(peakValleys));
        analysisData.setGrain(JSON.toJSONString(markings));
        analysisData.setColor("#000000");
        analysisData.setIsDelete("0");
        tMeasureAnalysisDataService.save(analysisData);


        // 更新任务状态
        List<TaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen taskSpecimen : specimen) {
            if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                taskSpecimen.setIsTested("1");
            }
        }
        task.setSampleKey(JSON.toJSONString(specimen));
        task.setDeviceSerialNumber(deviceSerialNumber);
        tMeasureTaskService.updateById(task);
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
            handleTransportError(session, e);
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
    }


}
