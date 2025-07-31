package com.persee.workstation.uv_inexus.websocket;


import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureTaskService;
import com.persee.workstation.uv_inexus.service.measurementService.TimeSweepService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private DevMessageUtil devMessageUtil;


    /**
     * 前端连接成功 WebSocket 时触发
     * @param session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (Constants.endTime != null||Constants.startTime != null) {
            try {
                session.close();
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
        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 将方法参数提取出来
        TimeSweepParamter timeSweepParameter = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);
        startTimedTask(session,timeSweepParameter,deviceSerialNumber); // 启动推送任务
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
            sessions.remove(session.getId());
            session.close();
        } catch (IOException ignored) {
            sessions.remove(session.getId());
        }
    }

    /**
     * 启动定时任务，每隔 1 秒推送数据给前端
     * @param session
     */
    private void startTimedTask(WebSocketSession session,TimeSweepParamter param,String deviceSerialNumber) {
        // 创建一个单线程的定时任务执行器
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Constants.startTime = LocalDateTime.now();
        final long startTime = System.currentTimeMillis();  // 记录任务开始的时间
        final double intervalTime = Double.parseDouble(param.getIntervalTime());
        final int[] counter = {0}; // 记录执行次数
        ChangeParam baselineData = param.getBaselineData();

        List<Double> x_data = new ArrayList<>();
        List<Double> y_data = new ArrayList<>();
        List<String> measureData = new ArrayList<>();
        List<String> originalData = new ArrayList<>();

        // 扫描的总时长
        Integer totalTime = Integer.valueOf(param.getScanTime());
        // 时间间隔
        Integer interval = Integer.valueOf(param.getIntervalTime());
        // 扫描的单位
        String scanUnit = param.getScanUnit();
        // 每隔 1 秒执行一次任务
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 如果执行次数大于等于 30 或者会话已经关闭，则停止任务
                if (counter[0] >= totalTime || !session.isOpen()) {
                    scheduler.shutdown(); // 停止任务
                    Constants.endTime = LocalDateTime.now();
                    saveData(measureData); // 保存数据
                    return;
                }

                // 计算当前执行的时间差（x 值）
                long currentTime = System.currentTimeMillis();
                double elapsedTime = (currentTime - startTime) / 1000.0;  // 毫秒转为秒

                // 更新 x 值
                double xValue  = Math.floor(elapsedTime);

                // 开始扫描
                String ip = deviceInfoService.getIp(deviceSerialNumber);
                Command command = new Command(SetParamEnum.Scan.getComId(),new Object[]{});
                ChangeParam changeParam = devMessageUtil.timeSweepGetMeasureData(command, ip);
                if (changeParam == null) {
                    scheduler.shutdown();
                    sendAndClose(session,"扫描失败");
                }
                originalData.add(xValue+ "," + changeParam.getS());
                // 计算当前 y 值
                if (param.getSpectralBandwidth().equals("1")){
                    String s = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
                    String y = DataCountUtil.changeTPercentOrAbs2(null, s);
                    x_data.add(xValue);
                    y_data.add(Double.parseDouble(y));
                    measureData.add( xValue + "," + y);
                    session.sendMessage(new TextMessage(xValue + "," + y));
                }else if (param.getSpectralBandwidth().equals("2")){
                    String s = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
                    String y = DataCountUtil.changeTPercentOrAbs2(null, s);
                    x_data.add(xValue);
                    y_data.add(Double.parseDouble(y));
                    measureData.add( xValue + "," + y);
                    session.sendMessage(new TextMessage(xValue + "," + y));
                }else if (param.getSpectralBandwidth().equals("3")){
                    Double sData = Double.valueOf(changeParam.getS());
                    String y = String.valueOf((sData / (4095 * 32)) * 100);
                    x_data.add(xValue);
                    y_data.add(Double.parseDouble(y));
                    measureData.add( xValue + "," + y);
                    session.sendMessage(new TextMessage(xValue + "," + y));
                }else if (param.getSpectralBandwidth().equals("4")){
                    Double rData = Double.valueOf(changeParam.getR());
                    String y = String.valueOf((rData / (4095 * 32)) * 100);
                    x_data.add(xValue);
                    y_data.add(Double.parseDouble(y));
                    measureData.add( xValue + "," + y);
                    session.sendMessage(new TextMessage(xValue + "," + y));
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
     * @param measureData
     */
    private void saveData(List<String> measureData) {
        // TODO: 实现保存数据的逻辑
        System.out.println("保存数据: " + measureData.size() + "条");
    }

}
