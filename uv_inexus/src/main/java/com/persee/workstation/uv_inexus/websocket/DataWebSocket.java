package com.persee.workstation.uv_inexus.websocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.userManageDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.userManagePo.UserConfigPO;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.util.DeviceMessageUtils;
import com.persee.workstation.uv_inexus.util.SpectrumScan;
import com.persee.workstation.uv_inexus.vo.ScanParamInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ServerEndpoint("/com/persee/workstation/uv-inexus/experimenttest/sys/spectrumScanning/ws-data")
@Component
public class DataWebSocket {

    // 获取SpectrumScan实例
    private SpectrumScan spectrumScan = BeanUtils.getBean(SpectrumScan.class);
    private TMeasureProjectService projectService = BeanUtils.getBean(TMeasureProjectService.class);
    private UserConfigDAO userConfigDAO = BeanUtils.getBean(UserConfigDAO.class);
    private TMeasureTaskService taskService = BeanUtils.getBean(TMeasureTaskService.class);
    private TMeasureDataService dataService = BeanUtils.getBean(TMeasureDataService.class);
    private DeviceMessageUtils messageUtils =  BeanUtils.getBean(DeviceMessageUtils.class);
    private TMeasureMethodService methodService = BeanUtils.getBean(TMeasureMethodService.class);
    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(DataWebSocket.class);
    // 单线程执行器，用于执行数据推送任务
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    // 存储所有WebSocket连接的Session
    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String taskResultId = null;

    @OnOpen
    public void onOpen(Session session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("新WebSocket连接: {}", sessionId);


        String query = session.getQueryString();
        String methodId = null;
        String taskId = null;
        String projectId = null;
        String deviceSerialNumber=null;
        for (String param : query.split("&")) {
            String[] split = param.split("=");
            if (split.length == 2) {
                switch (split[0]) {
                    case "method_id":
                        methodId = split[1];
                        break;
                    case "task_id":
                        taskId = split[1];
                        break;
                    case "project_id":
                        projectId = split[1];
                        break;
                    case "device_serial_number":
                        deviceSerialNumber = split[1];
                        break;
                }
            }
        }
        // 判断deviceSerialNumber是否为空
        if(!StringUtils.hasText(deviceSerialNumber)){
            try {
                session.getBasicRemote().sendText("device_serial_number为空");
                session.close();
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // 开始推送数据
        startDataPush(sessionId, methodId, taskId, projectId,deviceSerialNumber);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到客户端消息: {} from {}", message, session.getId());
        // 可处理客户端请求（如暂停/继续推送）
    }

    @OnClose
    public void onClose(Session session) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("WebSocket连接关闭: {}", sessionId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        String sessionId = session.getId();
        log.error("WebSocket错误 {}: {}", sessionId, error.getMessage());
        sessions.remove(sessionId);
    }

    // 逐点推送数据
    private void startDataPush(String sessionId, String methodId, String taskId, String projectId,String deviceSerialNumber) {
        // 创建测量数据对象
        TMeasureData tMeasureData = new TMeasureData();
        Constants.field1Values.clear();
        TMeasureMethod method = methodService.getById(methodId);
        String measurementParamInfo = method.getMeasurementParamInfo();
        ObjectMapper objectMapper = new ObjectMapper();
        ScanParamInfo ScanParamInfo=new ScanParamInfo();
        try {
            ScanParamInfo = objectMapper.readValue(measurementParamInfo, ScanParamInfo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // 调用仪器 TODO 差前端传过来的小数位数参数
        spectrumScan.scanStart(deviceSerialNumber,ScanParamInfo);

        // 新建一个线程用于获取数据
        new Thread(() -> {
            try {
                while (Constants.field1Values.size() == 0) {
                    log.info(String.valueOf(Constants.field1Values.size()));
                    Thread.sleep(50);
                }
                Session session = sessions.get(sessionId);
                int i = 0;
                boolean last = false;
                while (!last) {
                    //判断session为null或者session未打开，则停止推送
                    if (session == null || !session.isOpen()) {
                        break; // 连接已关闭，停止推送
                    }
                    int size = Constants.field1Values.size();
                    JSONArray dataArray = new JSONArray();
                    JSONObject payload = new JSONObject();
                    if (size == 0) {
                        continue;
                    }
                    if (Constants.field1Values.get(size - 1).equals(Constants.EndData)) {
                        size = size - 1;
                        last = true;
                    }
                    for (int j = 0; j < size; j++) {
                        JSONObject point = new JSONObject();
                        String[] split = Constants.field1Values.get(0).split(",");
                        Constants.field3Values.add(Constants.field1Values.get(0));
                        System.err.println("++++++++" + Constants.field1Values.get(0));
                        point.put("x", split[0]);
                        point.put("y", split[1]);
                        dataArray.add(point);
                        Constants.field1Values.remove(0);
                    }
                    payload.put("data", dataArray);
                    // 构建数据点JSON
                    if (last) {
                        payload.put("result", "finish");
                        payload.put("task_id", taskResultId);
                    }
                    // 推送数据点
                    session.getBasicRemote().sendText(payload.toJSONString());
                    // 模拟数据推送速度（500ms/点）
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                session.close();
            } catch (IOException | InterruptedException e) {
                log.error("数据推送异常: {}", e.getMessage());
            }
        }).start();

        // 等待测量结束,然后对数据对象进行创建
        new Thread(() -> {
            while (Constants.endTime == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (taskId == null) {
                TMeasureProject project = projectService.getById(projectId);
                TMeasureTask tMeasureTask = new TMeasureTask();
                UserConfigPO userConfigPO = userConfigDAO.selectOne(Wrappers.lambdaQuery(UserConfigPO.class).eq(UserConfigPO::getUserName, project.getCreatorKey()));
                // 任务+随机生成的六位数
                tMeasureTask.setTaskName("任务" + new Random().nextInt(100000));
                tMeasureTask.setMethodKey(methodId);
                tMeasureTask.setCreateTime(LocalDateTime.now());
                tMeasureTask.setStartTime(Constants.startTime);
                tMeasureTask.setProjectItemKey(project.getId());
                tMeasureTask.setMethodKey(methodId);
                tMeasureTask.setUserId(userConfigPO.getId());
                tMeasureTask.setDeviceSerialNumber(deviceSerialNumber);
                taskService.save(tMeasureTask);
                tMeasureData.setMeasurementStartTime(Constants.startTime);
                tMeasureData.setMeasurementEndTime(Constants.endTime);
                // 将measurementReturned.getOriginalData()中的数据转换为json
                try {
                    String s = objectMapper.writeValueAsString(Constants.field2Values);
                    tMeasureData.setInstrumentData(s);
                    String s1 = objectMapper.writeValueAsString(Constants.field3Values);
                    tMeasureData.setOriginalCalculationData(s1);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                tMeasureData.setTaskId(tMeasureTask.getId());
                taskResultId = tMeasureTask.getId();
                dataService.save(tMeasureData);
            } else {
                tMeasureData.setMeasurementStartTime(Constants.startTime);
                tMeasureData.setMeasurementEndTime(Constants.endTime);
                // 将measurementReturned.getOriginalData()中的数据转换为json
                try {
                    String s = objectMapper.writeValueAsString(Constants.field2Values);
                    tMeasureData.setInstrumentData(s);
                    String s1 = objectMapper.writeValueAsString(Constants.field2Values);
                    tMeasureData.setOriginalCalculationData(s1);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                tMeasureData.setTaskId(taskId);
                dataService.save(tMeasureData);
                taskResultId = taskId;
            }
            System.err.println(tMeasureData);
        }).start();
    }
}