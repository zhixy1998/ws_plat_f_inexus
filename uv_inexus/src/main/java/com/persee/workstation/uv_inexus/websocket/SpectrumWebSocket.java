package com.persee.workstation.uv_inexus.websocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumParameter;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureData;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.measurementService.SpectrumService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureDataService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureTaskService;
import com.persee.workstation.uv_inexus.service.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class SpectrumWebSocket extends TextWebSocketHandler {

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

    /**
     * 前端连接成功 WebSocket 时触发
     * @param session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (Constants.endTime != null||Constants.startTime != null) {
            try {
                session.close();
                exceptionHandling();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            sendAndClose(session, "当前测试未结束,无法开启新的测试");
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
        // 设置参数
        Result result = spectrumService.setUpParametersPort(task.getMethodKey(),deviceSerialNumber);
        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 将方法参数提取出来
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        if (result.getCode() != 200) {
            sendAndClose(session, result.getMessage());
        }
        if (result.getCode() == 200){
            // 开始获取数据
            new Thread(() -> {
                String ip = deviceInfoService.getIp(deviceSerialNumber);
                Command command = new Command();
                command.setCmdId(SetParamEnum.Scan.getComId());
                command.setParams(new Object[]{});
                devMessageUtil.getMeasureData(command,ip);
            }).start();

            // 往前端推送数据的线程
            new Thread(() -> {
                try {
                    // 如果公共变量中没有数据的话就让他睡眠50毫秒
                    if (Constants.instrumentData.size() == 0) {
                        Thread.sleep(50);
                    }
                    Boolean flag = true;
                    while (flag) {
                        for (String option : Constants.originalCalculationData) {
                            if (option.equals(Constants.EndData)){
                                flag = false;
                            }
                        }
                        int size = Constants.originalCalculationData.size();
                        JSONArray dataArray = new JSONArray();
                        JSONObject payload = new JSONObject();
                        // 如果size为0则跳过本次循环
                        if (size == 0) {
                            continue;
                        }
                        List<Double> x_data = new ArrayList<>();
                        List<Double> y_data = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            if (!Constants.originalCalculationData.isEmpty()
                                    && Constants.originalCalculationData.get(0).equals(Constants.EndData)) {
                                break;
                            }
                            String[] split = Constants.originalCalculationData.get(0).split(",");
                            x_data.add(Double.parseDouble(split[0]));
                            y_data.add(Double.parseDouble(split[1]));
                            System.err.println("++++++++"+Constants.originalCalculationData.get(0));
                            Constants.originalCalculationData.remove(0);
                        }
                        Map<String, Object> resultMap = new HashMap<>();
                        if (spectrumParameter.getLadderCorrection().equals("1")){
                            // 每次传过来的数据进行一次门限消除
                            SystemAlgorithmConfVO systemAlgorithmConfVO = systemAlgorithmConfService.queryAlgorithmConf();
                            if (systemAlgorithmConfVO == null){
                                sendAndClose(session, result.getMessage());
                            }
                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.APPLICATION_JSON);
                            Map<String, Object> body = new HashMap<>();
                            body.put("x_data", x_data);
                            body.put("y_data", y_data);
                            body.put("k", systemAlgorithmConfVO.getIntegratingSphere());
                            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                            resultMap=restTemplate.postForEntity(PythonPathConstant.REMOVE_STEP_OR_SPIKES, request, Map.class).getBody();
                        }
                        List<String> yData = new ArrayList<>();
                        if (resultMap != null){
                            yData=(List<String>) resultMap.get("smoothed_data");
                        }
                        // 传送给前端消除门限后的值
                        for (int j = 0; j < size; j++) {
                            if (!Constants.originalCalculationData.isEmpty()
                                    && Constants.originalCalculationData.get(0).equals(Constants.EndData)) {
                                break;
                            }
                            JSONObject point = new JSONObject();
                            point.put("x", x_data.get(j));
                            if (spectrumParameter.getLadderCorrection().equals("1")){
                                String format = String.format("%.2f", Double.valueOf(yData.get(j)));
                                point.put("y", format);
                            }else{
                                String format = String.format("%.2f", Double.valueOf(y_data.get(j)));
                                point.put("y", format);
                            }
                            dataArray.add(point);
                        }
                        payload.put("data", dataArray);
                        // 构建数据点JSON
                        if (!flag) {
                            payload.put("result", "finish");
                        }
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(payload.toJSONString()));
                        }
                        TimeUnit.MILLISECONDS.sleep(100);
                    }
                    sessions.remove(session.getId());
                    session.close();
                } catch (Exception e) {
                    exceptionHandling();
                    sessions.remove(session.getId());
                    handleTransportError(session, e);
                }
            }).start();

            // 创建线程循环如果公共变量为空则进行一秒的睡眠
            new Thread(() -> {
                while (Constants.endTime == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        exceptionHandling();
                        handleTransportError(session, e);
                    }
                }
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, Object> body = new HashMap<>();
                body.put("x_data", Constants.x_data);
                body.put("y_data", Constants.y_data);
                body.put("height", 0.5);
                body.put("distance", 1);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
                Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request, Map.class).getBody();
                List<Double> xDataFeng = (List<Double>) resultMap.get("peak_x");
                List<Double> xDataGu = (List<Double>) resultMap1.get("valley_x");
                // 进行存储操作
                // 创建一个测量数据对象
                TMeasureData measureData = new TMeasureData();
                // 根据任务id和样品id去测量数据表中进行查询,根据序列号进行从大到小的排序,并读取最大的那一条
                // 如果发现测量数据表中有数据则将最大的序列号拿出来加1并放入新建的测量数据对象中
                TMeasureData data = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("sample_key", sampleId).orderByDesc("serial_number").last("LIMIT 1"));
                if (data != null) {
                    measureData.setSerialNumber(data.getSerialNumber() + 1);
                }else{
                    measureData.setSerialNumber(1);
                }
                measureData.setTaskId(taskId);
                measureData.setSampleKey(sampleId);
                measureData.setInstrumentData(JSON.toJSONString(Constants.instrumentData));
                measureData.setOriginalCalculationData(JSON.toJSONString(Constants.field3Values));
                measureData.setMeasurementStartTime(Constants.startTime);
                measureData.setMeasurementEndTime(Constants.endTime);
                measureData.setPeak(JSON.toJSONString(xDataFeng));
                measureData.setGrain(JSON.toJSONString(xDataGu));
                measureData.setIsDelete("0");
                boolean save = traceDataService.save(measureData);
                if (!save){
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
                boolean update = tMeasureTaskService.updateById(task);
                if (!update) {
                    sendAndClose(session,"更新任务失败");
                    return;
                }
                System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                // 将公共的变量中的数据清除
                exceptionHandling();
            }).start();
        }
        sessions.remove(session.getId());
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
        exceptionHandling();
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
            Constants.originalCalculationData.clear();
            sessions.remove(session.getId());
            session.close();
        } catch (IOException ignored) {
            exceptionHandling();
            sessions.remove(session.getId());
        }
    }

    private void exceptionHandling(){
        Constants.x_data.clear();
        Constants.y_data.clear();
        Constants.field3Values.clear();
        Constants.instrumentData.clear();
        Constants.originalCalculationData.clear();
        Constants.startTime = null;
        Constants.endTime = null;
    }


}
