/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.MeasureData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;




@Component
public class MeasureWebSocketHandler implements WebSocketHandler {

    @Autowired
    DeviceInfoService infoService;

    @Autowired
    MeasureData dataWebSocket;
    // 保存所有的 WebSocket 连接
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 用来缓存待推送的数据（生产者-消费者模型）
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();


    // 启动推送线程池
    private final ExecutorService pushExecutor = Executors.newSingleThreadExecutor();

    private final AtomicBoolean pushLoopStarted = new AtomicBoolean(false);

    public MeasureWebSocketHandler() {
        // 启动一个线程专门消费队列里的数据并推送到前端
//        new Thread(this::pushLoop).start();
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

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        System.out.println("前端连接成功: " + session.getId());

        // 确保只启动一次推送线程
//        if (pushLoopStarted.compareAndSet(false, true)) {
//            new Thread(this::pushLoop, "PushLoop-Thread").start();
//            System.out.println("推送线程已启动");
//        }
        new Thread(() -> pushLoop(session), "PushLoop-" + session.getId()).start();


        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String deviceSerialNumber= getQueryParam(query, "deviceSerialNumber");

        if (deviceSerialNumber == null) {
            sendAndClose(session, "缺少 deviceSerialNumber 参数");
            return;
        }
        else{
            String ip = infoService.getIp(deviceSerialNumber);
            if (ip == null) {
                sendAndClose(session, "设备未连接");
                return;
            }
            else {
                dataWebSocket.startMeasureLoop(ip);
                System.out.println("startMeasureLoop 启动，ip="+ip);
            }
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        System.out.println("收到前端消息: " + message.getPayload());
        // 可以解析前端指令，比如 "stop" 来结束测量
        if ("stop".equalsIgnoreCase((String) message.getPayload())){
            Constants.Running = false;
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.out.println("连接异常: " + exception.getMessage());
        sessions.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        System.out.println("连接关闭: " + session.getId());
        sessions.remove(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 外部（采集线程）调用这个方法，把数据放进队列
     */
    public static void addData(String rawData) {
        System.out.println("addData: " + rawData);
        try {
            queue.put(rawData); // 阻塞放入
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 循环从队列取数据并推送
     */
    private void pushLoop(WebSocketSession session) {
//        while (Constants.Running)
        while (session.isOpen())
        {
            System.out.println("pushLoop 启动, Constants.Running=" + Constants.Running);
            try {
                String raw = queue.take(); // 没数据就阻塞等待

                if (raw.equals(Constants.EndData)) {
                    // 推送结束标志
                    Map<String, Object> finishMsg = new HashMap<>();
                    finishMsg.put("result", "finish");

                    String json = JSON.toJSONString(finishMsg);
                    broadcast(json);
                    System.out.println("推送结束标志: " + json);
                    continue;
                }

                String[] split = raw.split(",");
                double x = Double.parseDouble(split[0]);
                double y = Double.parseDouble(split[1]);

                if (y == 0.0) {
                    System.out.println("跳过数据: x=0, y=0");
                    continue;
                }

                // 封装数据
                Map<String, Object> data = new HashMap<>();
                data.put("x", x);
                data.put("y", y);
                String json = JSON.toJSONString(data);
                broadcast(json);
                System.out.println("推送数据: " + json);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 向所有已连接的前端推送消息
     */
    private void broadcast(String json) throws IOException {
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }

    private void sendAndClose(WebSocketSession session, String msg) {
        try {
            session.sendMessage(new TextMessage(msg));
            session.close();
        } catch (IOException ignored) {
        }
    }
}
