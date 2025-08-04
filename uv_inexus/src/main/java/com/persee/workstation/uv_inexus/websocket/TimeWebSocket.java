package com.persee.workstation.uv_inexus.websocket;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureData;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureDataService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureTaskService;
import com.persee.workstation.uv_inexus.service.measurementService.TimeSweepService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class TimeWebSocket extends TextWebSocketHandler {
    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 假设的设备数据缓存队列
    private static final Map<String, List<String>> deviceDataCache = new ConcurrentHashMap<>();

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
    private DevMessageUtil devMessageUtil;

    @Autowired
    private RestTemplate restTemplate;


    /**
     * 前端连接成功 WebSocket 时触发
     * @param session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (Constants.endTime != null||Constants.startTime != null) {
            sendAndClose(session, "当前测试未结束,无法开启新的测试");
            return;
        }

        // 记录当前连接
        sessions.put(session.getId(), session);

        // 解析参数
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String sampleId= getQueryParam(query, "sampleId");
        String deviceSerialNumber= getQueryParam(query, "deviceSerialNumber");
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
        if (task.getSampleKey()==null||task.getSampleKey().equals("")) {
            sendAndClose(session, "请先创建样品");
            return;
        }
        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 将方法参数提取出来
        TimeSweepParamter timeSweepParameter = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);
        startTimedTask(session,timeSweepParameter,deviceSerialNumber,sampleId,task); // 启动推送任务
    }


    /**
     * 启动定时任务，每隔 1 秒推送数据给前端
     * @param session
     */
    private void startTimedTask(WebSocketSession session,TimeSweepParamter param,String deviceSerialNumber,String sampleId,TMeasureTask task) {
        // 创建一个单线程的定时任务执行器
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Constants.startTime = LocalDateTime.now();
        final long startTime = System.currentTimeMillis();  // 记录任务开始的时间
        final double intervalTime = Double.parseDouble(param.getIntervalTime());
        final int[] counter = {0}; // 记录执行次数
        ChangeParam baselineData = JSON.parseObject(task.getZeroParam(), ChangeParam.class);
        List<BigDecimal> x_data = new ArrayList<>();
        List<BigDecimal> y_data = new ArrayList<>();
        List<ChangeParam> originalData = new ArrayList<>();

        // 扫描的总个数
        Integer totalTime = Integer.valueOf(param.getTotalNumber());
        // 时间间隔
        Integer interval = Integer.valueOf(param.getIntervalTime());
        // 扫描的单位
        String scanUnit = param.getScanUnit();
        // 每隔 1 秒执行一次任务
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (counter[0] >= totalTime || !session.isOpen()) {
                    scheduler.shutdown(); // 停止任务
                    Constants.endTime = LocalDateTime.now();
                    saveData(session,originalData,x_data,y_data,sampleId,task,deviceSerialNumber); // 保存数据
                    return;
                }

                // 计算当前执行的时间差（x 值）
                long currentTime = System.currentTimeMillis();
                double elapsedTime = (currentTime - startTime) / 1000.0;  // 毫秒转为秒

                // 更新 x 值
                BigDecimal xValue  = BigDecimal.valueOf(Math.floor(elapsedTime));

                // 开始扫描
                String ip = deviceInfoService.getIp(deviceSerialNumber);
                Command command = new Command(SetParamEnum.Measure.getComId(),new Object[]{});
                ChangeParam changeParam = devMessageUtil.timeSweepGetMeasureData(command, ip);
                if (changeParam == null) {
                    scheduler.shutdown();
                    sendAndClose(session,"扫描失败");
                }
                originalData.add(changeParam);
                // 计算当前 y 值
                if (param.getSpectralBandwidth().equals("1")){
                    String s = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
                    String y = DataCountUtil.changeTPercentOrAbs2(null, s);
                    x_data.add(xValue);
                    y_data.add(new BigDecimal(y));
                    JSONObject point = new JSONObject();
                    point.put("x", xValue);
                    point.put("y", y);
                    session.sendMessage(new TextMessage(point.toJSONString()));
                }else if (param.getSpectralBandwidth().equals("2")){
                    String s = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
                    String y = DataCountUtil.changeTPercentOrAbs2(s,null);
                    x_data.add(xValue);
                    y_data.add(new BigDecimal(y));
                    JSONObject point = new JSONObject();
                    point.put("x", xValue);
                    point.put("y", y);
                    session.sendMessage(new TextMessage(point.toJSONString()));
                }else if (param.getSpectralBandwidth().equals("3")){
                    Double sData = Double.valueOf(changeParam.getS());
                    String y = String.valueOf((sData / (4095 * 32)) * 100);
                    x_data.add(xValue);
                    y_data.add(new BigDecimal(y));
                    JSONObject point = new JSONObject();
                    point.put("x", xValue);
                    point.put("y", y);
                    session.sendMessage(new TextMessage(point.toJSONString()));
                }else if (param.getSpectralBandwidth().equals("4")){
                    Double rData = Double.valueOf(changeParam.getR());
                    String y = String.valueOf((rData / (4095 * 32)) * 100);
                    x_data.add(xValue);
                    y_data.add(new BigDecimal(y));
                    JSONObject point = new JSONObject();
                    point.put("x", xValue);
                    point.put("y", y);
                    session.sendMessage(new TextMessage(point.toJSONString()));
                }else{
                    session.close();
                }
                counter[0]++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, interval, scanUnit.equals("0")?TimeUnit.MINUTES:TimeUnit.SECONDS); // 每隔 1 秒执行一次,参数分别是 延迟时间、执行间隔、执行间隔单位
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
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request, Map.class).getBody();
        List<Double> xDataFeng = (List<Double>) resultMap.get("peak_x");
        List<Double> xDataGu = (List<Double>) resultMap1.get("valley_x");
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
        tMeasureData.setTaskId(task.getId());
        tMeasureData.setSampleKey(sampleId);
        tMeasureData.setInstrumentData(JSON.toJSONString(originalData));
        tMeasureData.setOriginalCalculationData(JSON.toJSONString(listData));
        tMeasureData.setMeasurementStartTime(Constants.startTime);
        tMeasureData.setMeasurementEndTime(Constants.endTime);
        tMeasureData.setPeak(JSON.toJSONString(xDataFeng));
        tMeasureData.setGrain(JSON.toJSONString(xDataGu));
        tMeasureData.setIsDelete("0");
        boolean save = tMeasureDataService.save(tMeasureData);
        if (!save) {
            sendAndClose(session, "保存测量数据失败");
            return;
        }
        List<TaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen taskSpecimen : specimen) {
            if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                taskSpecimen.setIsTested("1");
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
        JSONObject payload = new JSONObject();
        payload.put("result", "finish");
        // 将公共的变量中的数据清除
        sendAndClose(session, String.valueOf(payload));
    }

    /**
     * 收到前端消息时触发
     * @param session
     * @param message
     * @throws IOException
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String msg = message.getPayload();
        System.out.println("收到前端消息: " + msg);

        // 示例：如果前端发来"save"，就保存当前缓存
        if ("save".equalsIgnoreCase(msg)) {
            String deviceId = getQueryParam(session.getUri().getQuery(), "device_id");
            if (deviceId != null && deviceDataCache.containsKey(deviceId)) {
                List<String> toSave = new ArrayList<>(deviceDataCache.get(deviceId));
                System.out.println("保存数据[" + deviceId + "]: " + toSave.size() + "条");
                // TODO: 持久化保存到数据库
                deviceDataCache.get(deviceId).clear();
            }
        }
    }

    /**
     * 前端连接关闭时触发
     * @param session
     * @param status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        System.out.println("连接关闭: " + session.getId());
    }

    /**
     * 连接异常时触发
     * @param session
     * @param exception
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
        System.out.println("连接异常: " + exception.getMessage());
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
        } catch (IOException ignored) {
            sessions.remove(session.getId());
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
