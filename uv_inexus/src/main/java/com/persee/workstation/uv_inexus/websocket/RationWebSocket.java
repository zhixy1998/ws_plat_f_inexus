package com.persee.workstation.uv_inexus.websocket;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricParameter;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureData;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureDataService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureTaskService;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

@Component
@Slf4j
public class RationWebSocket extends TextWebSocketHandler {
    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 假设的设备数据缓存队列
    private static final Map<String, List<String>> deviceDataCache = new ConcurrentHashMap<>();

    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private TMeasureDataService tMeasureDataService;

    @Autowired
    DeviceInfoService infoService;

    @Value("1")
    private String right;

    @Value("0")
    private String error;


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
        if (sampleId != null) {
            //单样品重测
            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getSampleKey, sampleId)
                    .eq(TMeasureData::getTaskId, taskId);
            List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
            if (list != null || !list.isEmpty()) {
                //认为是重测，覆盖数据
                log.info("有数据，重测");
            }

            sendAndClose(session, "缺少 sampleId 参数");
            return;
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);

        if (task == null || !StringUtils.hasText(task.getSampleKey())) {
            sendAndClose(session, "任务或样品不存在");
            return;
        }

        TMeasureMethod byId = tMeasureMethodService.getById(task.getMethodKey());
        PhotometricParameter parameter = JSON.parseObject(byId.getMeasurementParamInfo(), PhotometricParameter.class);

        if (parameter.getWaveLengths() == null || parameter.getWaveLengths().isEmpty()) {
            log.info("缺少波长， 无法测量");
            sendAndClose(session, "缺少波长， 无法测量");
            return;
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            if (specimen.getDeleteType().equals(right) || specimen.getIsTested().equals(right)) {
                System.out.println("删除的样品,不需要测量");
                continue;
            }

            List<String> waveLengths = parameter.getWaveLengths();
            List<Command> list = new ArrayList<>();
            list.add(new Command("SetPhotoMode", new Object[]{1}));
            list.add(new Command("SetResponseTime", new Object[]{parameter.getResponseTime()}));

            for (String waveLength : waveLengths) {
                list.add(new Command("SetWl", new Object[]{waveLength}));
                list.add(new Command("Measure", new Object[]{}));

                String ip = infoService.getIp(task.getDeviceSerialNumber());
//                R<Map<String, String>> res = devMessageUtil.photoMetricMeasurement(list, ip);
//                if (!res.getSuccess()) {
//                    log.info("测量出错");
//                    return;
//                }
//                Map<String, String> data = res.getData();
//                log.info(data.get("wave"));
//                log.info(data.get("value"));
//                log.info(data.get("machineValue"));
//
//                TMeasureData measureData = new TMeasureData();
//                measureData.setTaskId(taskId);
//                measureData.setSampleKey(specimen.getSpecimenKey());


            }







        }


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
