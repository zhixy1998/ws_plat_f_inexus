
package com.persee.workstation.uv_inexus.websocket.concise;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.model.R;
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
import java.util.stream.Collectors;

@Component
public class ConciseSpectrumWebSocket extends TextWebSocketHandler {

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();


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
        // 打印客户端连接信息，包括会话ID和连接时间
        System.out.println("客户端进入: " + session.getId() + " 时间: " + LocalDateTime.now());

        // 将当前会话添加到sessions集合中
        sessions.put(session.getId(), session);

        if (Constants.endTime != null || Constants.startTime != null) {
            sendAndClose(session, "当前测量未结束,无法开启新的测试");
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
        FMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task.getSampleKey() == null || task.getSampleKey().equals("")) {
            sendAndClose(session, "请先创建样品");
            return;
        }

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
        if (newSampleData.size() == 0 && sampleData.size() != 0){
            sendAndClose(session, "您的样品已测完,请重新添加样品");
        }

        // 根据序列号查询对应的附件配置表信息
        QueryWrapper<FSamplePoolConfigPO> wrapper = new QueryWrapper<>();
        wrapper.eq("device_serial_number", deviceSerialNumber);
        FSamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);
        if (configPO == null) {
            sendAndClose(session, "没有找到对应的附件配置信息");
        }

        String maxLocation= null;
        if (configPO.getIsAutoChangePool()) {
            List<String> sampleIds = newSampleData.stream().map(obj -> obj.getSpecimenKey()).collect(Collectors.toCollection(ArrayList::new));
            QueryWrapper<FMeasureSample> eq = new QueryWrapper<>();
            eq.in("id", sampleIds);
            List<FMeasureSample> list = tMeasureSampleService.list(eq);
            maxLocation = list.stream().map(FMeasureSample::getSampleLocation).max(String::compareTo).orElse(null);
            if (maxLocation == null|| maxLocation.isEmpty()) {
                sendAndClose(session, "样品位置出错");
            }
        }else{
            maxLocation = "";
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
        String finalMaxLocation = maxLocation;
        Thread taskThread = new Thread(() -> {

            try {

                if (!repeatMode) {
                    // 不重复
                    for (int i = 0; i < newSampleData.size(); i++) {
                        // 预留: 设置样品池位置
                        FMeasureSample sample = tMeasureSampleService.getById(newSampleData.get(i).getSpecimenKey());
                        if (configPO.getIsAutoChangePool()){
                            // 根据样品的key查询样品
                            if (sample == null){
                                sendAndClose(session, "样品不存在");
                            }
                            R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
                            if (!stringR.getSuccess()) {
                                sendAndClose(session, "设置样品池出错");
                                return;
                            }
                        }

                        doOneMeasurementSerial(session, command, spectrumParameter, task, taskId, newSampleData.get(i).getSpecimenKey(), deviceSerialNumber, ip, 1);
                        newSampleData.get(i).setIsTested("1");
                        if ((i + 1) != newSampleData.size()||!sample.getSampleLocation().equals(finalMaxLocation)||configPO.getIsAutoChangePool().equals(true)) {
                            JSONObject payload = new JSONObject();
                            payload.put("result", "finish");
                            session.sendMessage(new TextMessage(payload.toJSONString()));
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
                        // 根据样品的key查询样品
                        FMeasureSample sample = tMeasureSampleService.getById(newSampleData.get(i).getSpecimenKey());
                        if (sample == null){
                            sendAndClose(session, "样品不存在");
                        }
                        R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
                        if (!stringR.getSuccess()) {
                            sendAndClose(session, "设置样品池出错");
                            return;
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
                        if (sample.getSampleLocation().equals(finalMaxLocation)){
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
        // 回显
        session.sendMessage(new TextMessage("服务端已收到: " + message.getPayload()));
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
        } catch (IOException e) {
            handleTransportError(session, e);
            Thread.currentThread().interrupt();
            return;
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

        spectrumService.setUpParametersPort(task.getMethodKey(), deviceSerialNumber);

        // 发送测量命令
        devMessageUtil.getMeasureData(command, ip);

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
                while (!Constants.measurd_data.isEmpty()) {
                    calcOnePoint(spectrumParameter, baselineCopy, x, y);
                    pushCount = pushDataIfNeeded(session, x, y, pushCount, count, sampleId);
                }
                break;
            }

            while (!Constants.measurd_data.isEmpty()) {
                calcOnePoint(spectrumParameter, baselineCopy, x, y);
                pushCount = pushDataIfNeeded(session, x, y, pushCount, count, sampleId);
            }

            try { Thread.sleep(10); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        // 推送剩余数据
        pushRemainingData(session, x, y, pushCount, count, sampleId);

        // 返回快照 + x/y
        return new MeasurementResult(
                x,
                y,
                new ArrayList<>(Constants.instrumentData),
                new ArrayList<>(Constants.originalCalculationData)
        );
    }



    // 推送逻辑：每 10 个点推送一次
    private int pushDataIfNeeded(WebSocketSession session, List<Double> x, List<Double> y, int pushCount, Integer count, String sampleId) throws IOException {
//        if (x.size() >= (pushCount + 1) * 10) {
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
    private void pushRemainingData(WebSocketSession session, List<Double> x, List<Double> y, int pushCount, Integer count, String sampleId) throws IOException {
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
            payload.put("sampleId", sampleId);
            payload.put("count", count);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload.toJSONString()));
            }
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
                                                 List<String> instrumentSnapshot,
                                                 List<String> originalCalcSnapshot) {

        if (instrumentSnapshot == null || originalCalcSnapshot == null) {
            sendAndClose(session, "测量数据为空，无法保存");
            return;
        }

        // 1. 处理峰谷数据（假设你原来调用了 Python 接口）
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
            sendAndClose(session, "未找到该样品");
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
            measureData.setOriginalCalculationData(JSON.toJSONString(listData));
            measureData.setMeasurementStartTime(Constants.startTime);
            measureData.setMeasurementEndTime(Constants.endTime);
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
            analysisData.setAnalysisResult(JSON.toJSONString(listData));
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
                measureData.setOriginalCalculationData(JSON.toJSONString(listData));
                measureData.setMeasurementStartTime(Constants.startTime);
                measureData.setMeasurementEndTime(Constants.endTime);
                measureData.setIsSave("1");
                traceDataService.updateById(measureData);

                analysisData.setAnalysisParam(JSON.toJSONString(instrumentSnapshot));
                analysisData.setAnalysisResult(JSON.toJSONString(listData));
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
                measureData.setOriginalCalculationData(JSON.toJSONString(listData));
                measureData.setMeasurementStartTime(Constants.startTime);
                measureData.setMeasurementEndTime(Constants.endTime);
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
                analysis.setAnalysisResult(JSON.toJSONString(listData));
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

}