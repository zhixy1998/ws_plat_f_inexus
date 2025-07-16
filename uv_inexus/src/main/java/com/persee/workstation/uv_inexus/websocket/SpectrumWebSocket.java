package com.persee.workstation.uv_inexus.websocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureData;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.service.measurementService.SpectrumService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureTaskService;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /**
     * 前端连接成功 WebSocket 时触发
     * @param session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 记录当前连接
        sessions.put(session.getId(), session);

        // 解析参数
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String sampleId= getQueryParam(query, "sampleId");

        if (taskId == null) {
            sendAndClose(session, "缺少 taskId 参数");
            return;
        }
        if (sampleId == null) {
            sendAndClose(session, "缺少 sampleId 参数");
            return;
        }
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        Result result = spectrumService.setUpParametersPort(task.getMethodKey());
        if (result.getCode() != 200) {
            sendAndClose(session, result.getMessage());
        }
        // 开始获取数据
        Command command = new Command();
        command.setCmdId(SetParamEnum.Scan.getComId());
        command.setParams(new Object[]{});
        devMessageUtil.getMeasureData(command);

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
                    for (int j = 0; j < size; j++) {
                        JSONObject point = new JSONObject();
                        String[] split = Constants.originalCalculationData.get(0).split(",");
                        Constants.field3Values.add(Constants.originalCalculationData.get(0));
                        point.put("x", split[0]);
                        point.put("y", split[1]);
                        dataArray.add(point);
                        Constants.originalCalculationData.remove(0);
                    }
                    payload.put("data", dataArray);
                    // 构建数据点JSON
                    if (!flag) {
                        payload.put("result", "finish");
                    }
                    session.sendMessage(new TextMessage(payload.toJSONString()));
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                session.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // 将公共的变量中的原始数据暂存变量清楚
            Constants.originalCalculationData.clear();
        }).start();

        // 创建线程循环如果公共变量为空则进行一秒的睡眠
        new Thread(() -> {
            while (Constants.endTime == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            // 进行存储操作
            // 创建一个测量数据对象
            TMeasureData measureData = new TMeasureData();
            measureData.setTaskId(taskId);
            measureData.setSampleKey(sampleId);
            measureData.setInstrumentData(JSON.toJSONString(Constants.instrumentData));
            measureData.setOriginalCalculationData(JSON.toJSONString(Constants.field3Values));
            measureData.setMeasurementStartTime(Constants.startTime);
            measureData.setMeasurementEndTime(Constants.endTime);

        }).start();

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
            session.close();
        } catch (IOException ignored) {}
    }
}
