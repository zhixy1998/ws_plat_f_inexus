package com.persee.workstation.uv_inexus.websocket.concise;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureMethodService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskAttachmentService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.websocket.MyWebSocketHandler;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * WebSocket处理器类，继承自TextWebSocketHandler
 * 用于处理WebSocket连接的建立、消息接收和连接关闭等事件
 */
@Component
public class ConciseBaseLineWebSocket extends TextWebSocketHandler {
    // 创建日志记录器，用于记录类中的日志信息
    private static final Logger log = LoggerFactory.getLogger(MyWebSocketHandler.class);

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储正在执行的任务
    private final Map<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();




    @Autowired
    private SpectrumConciseService spectrumService;

    @Autowired
    private FMeasureTaskService tMeasureTaskService;

    @Autowired
    private FMeasureTaskAttachmentService tMeasureTaskAttachmentService;

    @Autowired
    private FMeasureMethodService tMeasureMethodService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private SystemUserManageDAO systemUserManageDAO;

    /**
     * 当WebSocket连接建立后调用此方法
     * @param session 当前WebSocket会话，包含连接信息
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("客户端进入: " + session.getId() + " 时间: " + LocalDateTime.now());
        sessions.put(session.getId(), session);

        // 将耗时逻辑提交到线程池
        Future<?> future = executor.submit(() -> {
            try {
                runBusinessLogic(session);
            } catch (InterruptedException e) {
                // 线程被中断，立即停止
                System.out.println("任务被中断: " + session.getId());
                sendAndClose(session, "操作已取消，连接关闭");
            } catch (Exception e) {
                e.printStackTrace();
                sendAndClose(session, "执行异常: " + e.getMessage());
            }
        });
    }

    /**
     * 处理接收到的文本消息
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
     * @param session WebSocket会话对象，包含连接的相关信息
     * @param status 关闭状态，包含连接关闭的原因和代码
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            System.out.println("客户端退出: " + session.getId() + " 时间: " + LocalDateTime.now() + " 原因: " + status);
        sessions.remove(session.getId());

        // 中断正在执行的任务
        Future<?> future = runningTasks.remove(session.getId());
        if (future != null) {
            future.cancel(true);
        }

        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    private void runBusinessLogic(WebSocketSession session) throws InterruptedException {
        // 检查线程中断
        checkInterrupted(session);

        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String methodId = getQueryParam(query, "methodId");
        String deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");

        if (methodId == null || methodId.equals("")) {
            sendAndClose(session, "方法编号不能为空");
            return;
        }
        if (deviceSerialNumber == null || deviceSerialNumber.equals("")) {
            sendAndClose(session, "设备编号不能为空");
            return;
        }

        checkInterrupted(session);
        Result result = spectrumService.setUpParametersPort(methodId, deviceSerialNumber);
        if (result.getCode() != 200) {
            sendAndClose(session, "设置参数失败");
            return;
        }

        checkInterrupted(session);
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        Command command = buildCommand(SetParamEnum.BaseLine, new Object[]{});
        R<List<ChangeParam>> listR = devMessageUtil.spectrumScanGetBaseline(command, ip);
        if (!listR.getSuccess()) {
            sendAndClose(session, listR.getMessage());
            return;
        }

        checkInterrupted(session);
        FMeasureTask task;
        if (taskId != null && !taskId.equals("")) {
            task = tMeasureTaskService.getById(taskId);
        } else {
            task = new FMeasureTask();
            FMeasureMethod method = tMeasureMethodService.getById(methodId);
            SystemUserManagePO user = systemUserManageDAO
                    .selectOne(new QueryWrapper<>(SystemUserManagePO.class).eq("user_name", method.getMethodCreator()));
            task.setUserId(user.getId());

            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            String collect = new Random().ints(8, 0, chars.length())
                    .mapToObj(i -> String.valueOf(chars.charAt(i)))
                    .collect(Collectors.joining());

            task.setTaskName(method.getMeasurementType() + "task_" + collect);
            task.setCreateTime(LocalDateTime.now());
            task.setMethodKey(methodId);
            task.setMeasureType(method.getMeasurementType());
            task.setProjectItemKey(method.getProjectItemKey());

            task.setDeviceSerialNumber(deviceSerialNumber);
            boolean save1 = tMeasureTaskService.save(task);
            if (!save1) {
                sendAndClose(session, "保存失败");
                return;
            }

            FMeasureTaskAttachment taskAttachment = new FMeasureTaskAttachment();
            taskAttachment.setTaskId(task.getId());
            boolean save = tMeasureTaskAttachmentService.save(taskAttachment);
            if (!save) {
                sendAndClose(session, "保存失败");
                return;
            }
        }

        checkInterrupted(session);
        task.setZeroParam(JSON.toJSONString(listR.getData()));
        boolean update = tMeasureTaskService.updateById(task);
        if (!update) {
            sendAndClose(session, "保存失败");
            return;
        }

        sendAndClose(session, "保存成功");
    }

    // 中断检测
    private void checkInterrupted(WebSocketSession session) throws InterruptedException {
        if (Thread.currentThread().isInterrupted() || !session.isOpen()) {
            throw new InterruptedException("任务被中断或连接关闭");
        }
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

    /**
     * 构建命令
     * @param cmdEnum
     * @param value
     * @return
     */
    private Command buildCommand(SetParamEnum cmdEnum, Object value) {
        Command cmd = new Command();
        cmd.setCmdId(cmdEnum.getComId());
        cmd.setParams(new Object[]{value});
        return cmd;
    }
}
