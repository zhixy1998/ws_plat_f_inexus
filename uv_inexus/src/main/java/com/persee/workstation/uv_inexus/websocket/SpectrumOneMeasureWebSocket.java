/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumParameter;
import com.persee.workstation.uv_inexus.model.R;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

@Component
public class SpectrumOneMeasureWebSocket extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SpectrumOneMeasureWebSocket.class);

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();

    private String deviceSerialNumber;
    private boolean measureStatus = false;

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
    @Autowired
    private TMeasureAnalysisImageService tMeasureAnalysisImageService;
    @Autowired
    private PythonPathConstant pythonPathConstant;

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
            sendAndClose(session, "当前测量未结束,无法开启新的测试");
            return;
        }

        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String sampleId = getQueryParam(query, "sampleId");
        String analysisNumber = getQueryParam(query, "analysisNumber");
        deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");

        if (taskId == null) {
            sendAndClose(session, "缺少 taskId 参数");
            return;
        }

        if (sampleId == null) {
            sendAndClose(session, "缺少 sampleId 参数");
            return;
        }

        if (analysisNumber == null) {
            sendAndClose(session, "缺少 analysisNumber 参数");
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

        // 占用仪器
        R<Boolean> message = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);
        if (!message.getSuccess()) {
            sendAndClose(session, message.getMessage());
            return;
        }

        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 将方法参数提取出来
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);

        String ip = deviceInfoService.getIp(deviceSerialNumber);

        ///  设置样品池位置poolNum
        // 根据样品id找到样品数据
        TMeasureSample sample = tMeasureSampleService.getById(sampleId); // 注意：这里应该是sampleId而不是task.getSampleKey()
        if (sample == null){
            sendAndClose(session, "请先创建样品");
            return;
        }

        R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
        if (!stringR.getSuccess()) {
            sendAndClose(session, "设置出错");
            return;
        }

        Command command = new Command();
        command.setCmdId(SetParamEnum.Scan.getComId());
        command.setParams(new Object[]{});

        // 往前端推送数据的线程
        Thread taskThread = new Thread(() -> {
            try {
                System.out.println("analysisNumber=" + analysisNumber);
                Integer number = Integer.valueOf(analysisNumber);
                doOneMeasurement(session, command, spectrumParameter, task, taskId, sampleId, deviceSerialNumber, ip, number);
                // 全部完成后再告诉前端 finish
                JSONObject payload = new JSONObject();
                payload.put("result", "finish");
                sendAndClose(session, String.valueOf(payload));

            } catch (Exception e) {
                System.err.println("异常!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + e);
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
     * @param session 当前WebSocket会话
     * @param message 接收到的文本消息对象
     * @throws Exception 可能抛出的异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) throws Exception {
        // 打印接收到的消息内容，包括发送方会话ID和消息内容
        System.out.println("收到消息: " + session.getId() + " -> " + message.getPayload());

        if (message.getPayload().equals("close")){
            // 优先释放测量线程
            releaseMeasureThread();

            Thread thread = taskThreads.get(session.getId());
            if (measureStatus){
                try {
                    Command stopCommand = new Command();
                    stopCommand.setCmdId(SetParamEnum.Stop.getComId());
                    stopCommand.setParams(new Object[]{});
                    String ip = deviceInfoService.getIp(deviceSerialNumber);
                    R<String> stop = devMessageUtil.stop(stopCommand, ip);
                    if (stop.getSuccess()){
                        log.info("停止成功");
                    }else {
                        log.info("停止失败");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
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
                deviceInfoService.releaseDevice(deviceSerialNumber);
            }
        } else {
            // 回显
            session.sendMessage(new org.springframework.web.socket.TextMessage("服务端已收到: " + message.getPayload()));
        }
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

        // 5. 最后释放设备
        deviceInfoService.releaseDevice(deviceSerialNumber);
    }

    /**
     * 连接异常时触发
     * @param session
     * @param exception
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
            log.error("关闭会话时发生异常", e);
        } finally {
            // 最后释放设备
            deviceInfoService.releaseDevice(deviceSerialNumber);
            exceptionHandling();
        }
        System.out.println("连接异常: " + exception.getMessage());
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
                                  String  ip,
                                  Integer number) throws IOException {

        // 1. 发送测量命令
        devMessageUtil.getMeasureData(command, ip);
        Constants.startTime = LocalDateTime.now();
        measureStatus = true;

        List<ChangeParam> baselineCopy = new ArrayList<>();
        if (task.getZeroParam()!=null&&!task.getZeroParam().isEmpty()){
            baselineCopy = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
        }else{
            baselineCopy = null;
        }
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();

        // 2. 采集数据，实时推送（每 10 条推一次）
        int pushCount = 0;
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("线程被中断，终止任务");
                return;
            }
            List<String> snapshot = new ArrayList<>(Constants.instrumentData);
            if (!snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData)) {
                while (!Constants.measurd_data.isEmpty()) {
                    calcOnePoint(spectrumParameter, baselineCopy, x, y);
                    pushCount = pushDataIfNeeded(session, x, y, pushCount,sampleId);
                }
                break;
            }
            while (!Constants.measurd_data.isEmpty()) {
                calcOnePoint(spectrumParameter, baselineCopy, x, y);
                pushCount = pushDataIfNeeded(session, x, y, pushCount,sampleId);
            }
            try { sleep(10); } catch (InterruptedException e) {
                handleTransportError(session, e);
                Thread.currentThread().interrupt();
                return;
            }
        }

        // 3. 推送最后不足10条的数据
        pushRemainingData(session, x, y, pushCount,sampleId);

        // 4. 保存一次完整测量
        saveOneMeasurement(task, taskId, sampleId, deviceSerialNumber, x, y,session,number);

        // 5. 清理缓存
        Constants.instrumentData.clear();
        Constants.measurd_data.clear();
        Constants.originalCalculationData.clear();
        Constants.startTime = null;
        Constants.endTime = null;
        measureStatus = false;
    }

    // 推送逻辑：每 10 个点推送一次
    private int pushDataIfNeeded(WebSocketSession session, List<Double> x, List<Double> y, int pushCount,String sampleId) throws IOException {
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
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload.toJSONString()));
            }
            pushCount++;
        }
        return pushCount;
    }

    // 推送剩余数据（最后不足10条的情况）
    private void pushRemainingData(WebSocketSession session, List<Double> x, List<Double> y, int pushCount,String sampleId) throws IOException {
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
        if (baselineCopy != null && !baselineCopy.isEmpty()){
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
        }else{
            if (spectrumParameter.getLuminosity().equals("1")) {
                String s = DataCountUtil.countTPercent(null, Constants.measurd_data.get(0), 1.0);
                String yy = DataCountUtil.changeTPercentOrAbs2(null, s);
                Constants.originalCalculationData.add(Constants.measurd_data.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(Constants.measurd_data.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            }
            else if (spectrumParameter.getLuminosity().equals("2")) {
                String s = DataCountUtil.countTPercent(null, Constants.measurd_data.get(0), 1.0);
                String yy = DataCountUtil.changeTPercentOrAbs2(s, null);
                Constants.originalCalculationData.add(Constants.measurd_data.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(Constants.measurd_data.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("3")) {
                Double sData = Double.valueOf(Constants.measurd_data.get(0).getS());
                String yy = String.valueOf((sData / (4095 * 32)) * 100);
                Constants.originalCalculationData.add(Constants.measurd_data.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(Constants.measurd_data.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("4")) {
                Double rData = Double.valueOf(Constants.measurd_data.get(0).getR());
                String yy = String.valueOf((rData / (4095 * 32)) * 100);
                Constants.originalCalculationData.add(Constants.measurd_data.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(Constants.measurd_data.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            }
            Constants.measurd_data.remove(0);
        }
    }

    // 保存一次测量结果 - 修复重测覆盖问题
    private void saveOneMeasurement(TMeasureTask task,
                                    String taskId,
                                    String sampleId,
                                    String deviceSerialNumber,
                                    List<Double> x,
                                    List<Double> y,
                                    WebSocketSession session,
                                    Integer number) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("x_data", x);
            body.put("y_data", y);
            body.put("height", 0.5);
            body.put("distance", 1);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            Map<String, Object> resultMap = restTemplate.postForEntity(pythonPathConstant.getPeakSearching(), request, Map.class).getBody();
            Map<String, Object> resultMap1 = restTemplate.postForEntity(pythonPathConstant.getValleySearching(), request, Map.class).getBody();
            List<Double> xDataFeng = (List<Double>) resultMap.get("peak_x");
            List<String> yDataFeng = (List<String>) resultMap.get("peak_y");
            List<Double> xDataGu = (List<Double>) resultMap1.get("valley_x");
            List<String> yDataGu = (List<String>) resultMap1.get("valley_y");

            // 进行存储操作
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

            // 将任务中的样品数据取出来
            List<TaskSpecimen> sampleData = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            TaskSpecimen taskSpecimen = null;
            for (TaskSpecimen data : sampleData) {
                if (data.getSpecimenKey().equals(sampleId)){
                    taskSpecimen=data;
                    break;
                }
            }
            if (taskSpecimen==null){
                sendAndClose( session, "未找到该样品");
                return;
            }

            // 根据样品的key查询该样品的参数
            TMeasureSample sample = tMeasureSampleService.getById(sampleId);

            // 根据任务中的方法id查询方法参数
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);

            // 检查是否已经存在测量数据（重测情况）
            QueryWrapper<TMeasureAnalysisData> analysisQuery = new QueryWrapper<>();
            analysisQuery.eq("task_id", taskId)
                    .eq("sample_key", sampleId)
                    .eq("analysis_number", number)
                    .eq("analysis_type", "5");
            TMeasureAnalysisData existingAnalysisData = tMeasureAnalysisDataService.getOne(analysisQuery);

            if (existingAnalysisData != null) {
                // 重测情况：更新现有数据
                log.info("重测情况，更新现有数据 - 任务ID: {}, 样品ID: {}, 编号: {}", taskId, sampleId, number);

                // 更新测量数据
                TMeasureData existingMeasureData = tMeasureDataService.getById(existingAnalysisData.getMeasureDataId());
                if (existingMeasureData != null) {
                    existingMeasureData.setInstrumentData(JSON.toJSONString(Constants.instrumentData));
                    existingMeasureData.setOriginalCalculationData(JSON.toJSONString(listData));
                    existingMeasureData.setMeasurementStartTime(Constants.startTime);
                    existingMeasureData.setMeasurementEndTime(Constants.endTime);
                    boolean updateMeasure = traceDataService.updateById(existingMeasureData);
                    if (!updateMeasure) {
                        log.error("更新测量数据失败");
                    }
                }

                // 更新分析数据
                existingAnalysisData.setAnalysisParam(JSON.toJSONString(Constants.instrumentData));
                existingAnalysisData.setAnalysisResult(JSON.toJSONString(listData));
                existingAnalysisData.setPeak(JSON.toJSONString(peakValleys));
                existingAnalysisData.setGrain(JSON.toJSONString(markings));
                existingAnalysisData.setColor("#000000");
                boolean updateAnalysis = tMeasureAnalysisDataService.updateById(existingAnalysisData);
                if (!updateAnalysis) {
                    log.error("更新分析数据失败");
                }

                log.info("重测数据更新完成");

            } else {
                // 新建测量情况
                log.info("新建测量情况 - 任务ID: {}, 样品ID: {}, 编号: {}", taskId, sampleId, number);

                if (spectrumParameter.getRepetitionPattern().equals("不重复")){
                    TMeasureData measureData = new TMeasureData();
                    measureData.setSerialNumber(1);
                    measureData.setTaskId(taskId);
                    measureData.setSampleKey(sampleId);
                    measureData.setInstrumentData(JSON.toJSONString(Constants.instrumentData));
                    measureData.setOriginalCalculationData(JSON.toJSONString(listData));
                    measureData.setMeasurementStartTime(Constants.startTime);
                    measureData.setMeasurementEndTime(Constants.endTime);
                    measureData.setIsDelete("0");
                    boolean saveMeasure = traceDataService.save(measureData);
                    if (!saveMeasure) {
                        sendAndClose(session, "测量数据保存失败");
                        return;
                    }

                    TMeasureAnalysisData analysisData = new TMeasureAnalysisData();
                    analysisData.setAnalysisNumber(1);
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
                    boolean saveAnalysis = tMeasureAnalysisDataService.save(analysisData);
                    if (!saveAnalysis) {
                        sendAndClose(session, "分析数据保存失败");
                        return;
                    }

                    TMeasureAnalysisImage tMeasureAnalysisImage = new TMeasureAnalysisImage();
                    tMeasureAnalysisImage.setAnalysisId(analysisData.getId());
                    boolean saveImage = tMeasureAnalysisImageService.save(tMeasureAnalysisImage);
                    if (!saveImage) {
                        sendAndClose(session, "分析图像保存失败");
                        return;
                    }
                } else {
                    // 重复模式
                    TMeasureData measureData = new TMeasureData();
                    measureData.setSerialNumber(number);
                    measureData.setTaskId(taskId);
                    measureData.setSampleKey(sampleId);
                    measureData.setInstrumentData(JSON.toJSONString(Constants.instrumentData));
                    measureData.setOriginalCalculationData(JSON.toJSONString(listData));
                    measureData.setMeasurementStartTime(Constants.startTime);
                    measureData.setMeasurementEndTime(Constants.endTime);
                    measureData.setIsDelete("0");
                    boolean saveMeasure = traceDataService.save(measureData);
                    if (!saveMeasure) {
                        sendAndClose(session, "测量数据保存失败");
                        return;
                    }

                    TMeasureAnalysisData analysisData = new TMeasureAnalysisData();
                    analysisData.setAnalysisNumber(number);
                    analysisData.setAnalysisName(sample.getSampleName()+"-"+number);
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
                    boolean saveAnalysis = tMeasureAnalysisDataService.save(analysisData);
                    if (!saveAnalysis) {
                        sendAndClose(session, "分析数据保存失败");
                        return;
                    }

                    TMeasureAnalysisImage tMeasureAnalysisImage = new TMeasureAnalysisImage();
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

        } catch (Exception e) {
            log.error("保存测量数据时发生异常", e);
            sendAndClose(session, "数据保存失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(TMeasureTask task, String taskId, String sampleId,
                                  String deviceSerialNumber, SpectrumParameter spectrumParameter,
                                  WebSocketSession session) {
        try {
            List<TaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

            if (spectrumParameter.getRepetitionPattern().equals("不重复")) {
                // 不重复模式：直接标记为已测试
                for (TaskSpecimen taskSample : specimen) {
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
                    QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("task_id", taskId)
                            .eq("sample_key", sampleId)
                            .eq("analysis_number", i)
                            .eq("analysis_type", "5");
                    TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(queryWrapper);

                    if (analysisData == null || analysisData.getAnalysisResult() == null) {
                        allCompleted = false;
                        break;
                    }
                }

                if (allCompleted) {
                    // 所有重复次数都完成了，标记为已测试
                    for (TaskSpecimen taskSample : specimen) {
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
     * 发送消息并关闭连接
     * @param session
     * @param msg
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
            handleTransportError(session, e);
            Thread.currentThread().interrupt();
            return;
        } finally {
            // 最后释放设备
            deviceInfoService.releaseDevice(deviceSerialNumber);
            exceptionHandling();
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
        // 测量线程在releaseMeasureThread方法中单独处理
    }
}