package com.persee.workstation.uv_inexus.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.MeasureData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Controller
public class MeasureController {

    private String deviceSerialNumber;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Autowired
    DeviceInfoService infoService; // 你现有的 service

    @Autowired
    MeasureData dataWebSocket; // 包含 startMeasureLoop

    private volatile String latestTime;
    private volatile String latestWl;
    private volatile String latestPos;

    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    private static final BlockingQueue<String> timeQueue = new LinkedBlockingQueue<>();

    private static final BlockingQueue<String> wlQueue = new LinkedBlockingQueue<>();

    private static final BlockingQueue<String> posQueue = new LinkedBlockingQueue<>();

    private static final BlockingQueue<String> BandwidthQueue = new LinkedBlockingQueue<>();

    public static final BlockingQueue<Map<String, Object>> LampPosQueue = new LinkedBlockingQueue<>();

    private static final BlockingQueue<String> SampleTypeQueue = new LinkedBlockingQueue<>();

    private final ObjectMapper mapper = new ObjectMapper();


    private static String flag = "false";
    /**
     * 前端调用：stompClient.send("/app/measure/start", {}, JSON.stringify({"deviceSerialNumber":"123"}))
     */
    @MessageMapping("/measure/start")
    public void startMeasure(String deviceSerialNumber) {
        if (deviceSerialNumber == null || deviceSerialNumber.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/measure",
                    "缺少 deviceSerialNumber 参数");
            return;
        }

        String ip = infoService.getIp(deviceSerialNumber);
        if (ip == null) {
            messagingTemplate.convertAndSend("/topic/measure",
                    "设备未连接: " + deviceSerialNumber);
            return;
        }

        // 启动发送接收线程
        dataWebSocket.startMeasureLoop(ip);

        // 启动推送循环线程（只启动一次，避免重复开线程）
//        new Thread(this::pushLoop, "PushLoopThread").start();
//        new Thread(this::pushTimeLoop, "PushTimeLoopThread").start();
//        new Thread(this::pushWlLoop, "PushWlLoopThread").start();
//        new Thread(this::pushPosLoop, "PushPosLoopThread").start();
//        new Thread(this::pushBandwidthLoop, "PushBandwidthLoopThread").start();
//        new Thread(this::pushLampPosLoop, "PushLampPosThread").start();
//        new Thread(this::pushSampleTypeLoop, "PushSampleTypeLoopThread").start();

        new Thread(this::pushUnifiedLoop, "PushUnifiedLoopThread").start();
        new Thread(this::pushLampPosLoop, "PushLampPosLoopThread").start();



        // 主动推送通知给前端
        messagingTemplate.convertAndSend("/topic/measure",
                "开始测量: " + deviceSerialNumber + ", ip=" + ip);
    }

    @MessageMapping("/measure/stop")
    public void stopMeasure() {
        Constants.Running = false;
        messagingTemplate.convertAndSend("/topic/measure", "停止测量");
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

    public static void addTimeData(String rawData) {
        System.out.println("addTimeData: " + rawData);
        try {
            timeQueue.put(rawData); // 阻塞放入
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void addWlData(String rawData) {
        System.out.println("addWlData: " + rawData);
        try {
            wlQueue.put(rawData); // 阻塞放入
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void addPosData(String rawData) {
        System.out.println("addPosData: " + rawData);
        try {
            posQueue.put(rawData); // 阻塞放入
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void addBandwidthData(String rawData) {
        System.out.println("addBandwidthData: " + rawData);
        try {
            BandwidthQueue.put(rawData); // 阻塞放入
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

//    public static void addLampPosData(String rawData) {
//        System.out.println("addLampPosData: " + rawData);
//        try {
//            LampPosQueue.put(rawData); // 阻塞放入
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }

    public static void addLampPosData(Map<String, Object> rawData) {
        System.out.println("addLampPosData: " + rawData);
        try {
//            // 解析字符串 -> Map
//            ObjectMapper mapper = new ObjectMapper();
//            Map<String, Object> lampData = mapper.readValue(rawData, Map.class);

            // 放入 Map
            LampPosQueue.put(rawData); // 阻塞放入
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("addLampPosData 转换失败: " + rawData);
        }
    }


    public static void addSampleTypeData(String rawData) {
        System.out.println("addSampleTypeData: " + rawData);
        try {
            SampleTypeQueue.put(rawData); // 阻塞放入
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    /**
     * 推送除 LampPos 外的所有通用数据
     */
    private void pushUnifiedLoop() {
        while (Constants.Running) {
            try {
                // 1. 时间
                if (!timeQueue.isEmpty()) {
                    String rawtime = timeQueue.poll();
                    if (rawtime != null) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("time", rawtime);
                        messagingTemplate.convertAndSend("/topic/time", mapper.writeValueAsString(data));
                        System.out.println("推送时间: " + rawtime);
                    }
                }

                // 2. 波长
                if (!wlQueue.isEmpty()) {
                    String rawwl = wlQueue.poll();
                    if (rawwl != null) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("ChangeLampWl", rawwl);
                        messagingTemplate.convertAndSend("/topic/ChangeLampWl", mapper.writeValueAsString(data));
                        System.out.println("推送波长: " + rawwl);
                    }
                }

                // 3. 样品池位置
                if (!posQueue.isEmpty()) {
                    String rawpos = posQueue.poll();
                    if (rawpos != null) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("Pos", rawpos);
                        messagingTemplate.convertAndSend("/topic/Pos", mapper.writeValueAsString(data));
                        System.out.println("推送位置: " + rawpos);
                    }
                }

                // 4. 带宽
                if (!BandwidthQueue.isEmpty()) {
                    String rawbw = BandwidthQueue.poll();
                    if (rawbw != null) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("Bandwidth", rawbw);
                        messagingTemplate.convertAndSend("/topic/Bandwidth", mapper.writeValueAsString(data));
                        System.out.println("推送带宽: " + rawbw);
                    }
                }

                // 5. 样品类型
                if (!SampleTypeQueue.isEmpty()) {
                    String sampleType = SampleTypeQueue.poll();
                    if (sampleType != null) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("sampletyple", sampleType);
                        messagingTemplate.convertAndSend("/topic/sampletyple", mapper.writeValueAsString(data));
                        System.out.println("推送样品类型: " + sampleType);
                    }
                }

                Thread.sleep(50); // 控制循环频率

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 推送灯位状态（LampPos）
     */
    private void pushLampPosLoop() {
        while (Constants.Running) {
            try {
                Map<String, Object> lampData = LampPosQueue.take(); // 阻塞取 Map
                String json = mapper.writeValueAsString(lampData);
                messagingTemplate.convertAndSend("/topic/LampPos", json);
                System.out.println("推送灯状态数据: " + json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



}
