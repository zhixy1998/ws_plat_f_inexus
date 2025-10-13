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

    private static final BlockingQueue<String> otherQueue = new LinkedBlockingQueue<>();

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

        // 启动测量循环线程
        dataWebSocket.startMeasureLoop(ip);

        // 启动推送循环线程（只启动一次，避免重复开线程）
            new Thread(this::pushLoop, "PushLoopThread").start();
            new Thread(this::pushTimeLoop, "PushTimeLoopThread").start();
            new Thread(this::pushWlLoop, "PushWlLoopThread").start();
            new Thread(this::pushPosLoop, "PushPosLoopThread").start();
            new Thread(this::pushBandwidthLoop, "PushBandwidthLoopThread").start();


//            new Thread(this::pushotherLoop, "PushOtherLoopThread").start();

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

    /**
     * 循环从队列取数据并推送（独立线程）
     */
    private void pushLoop() {
        while (Constants.Running) {
            try {
                String raw = queue.take(); // 阻塞取数据

                String[] split = raw.split(",");
                double x = Double.parseDouble(split[0]);
                double y = Double.parseDouble(split[1]);


                if (y == 0.0) {
                    System.out.println("跳过数据: x=0, y=0");
                    continue;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("x", x);
                data.put("y", y);

                String json = mapper.writeValueAsString(data);
                messagingTemplate.convertAndSend("/topic/measure", json);
                System.out.println("推送数据: " + json);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
    推送响应时间/换灯波长/样品池位置的线程
    * */
//    private void pushotherLoop() {
//        while (Constants.Running) {
//            try {
//                // 哪个队列有数据就取哪个
//                String time = timeQueue.poll();
//                String wl = wlQueue.poll();
//                String pos = posQueue.poll();
//
//                if (time != null) latestTime = time;
//                if (wl != null) latestWl = wl;
//                if (pos != null) latestPos = pos;
//
//                // 如果有至少一个更新，就组装 JSON 推送
//                if (time != null || wl != null || pos != null) {
//                    Map<String, Object> data = new HashMap<>();
//                    data.put("time", latestTime);
//                    data.put("ChangeLampWl", latestWl);
//                    data.put("Pos", latestPos);
//
//                    String json = mapper.writeValueAsString(data);
//                    messagingTemplate.convertAndSend("/topic/measureData", json);
//                    System.out.println("推送数据: " + json);
//                }
//
//                Thread.sleep(50); // 控制频率，避免空转
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

    /*
    推送响应时间线程
    * */
    private void pushTimeLoop() {
        while (Constants.Running) {
            try {
                String rawtime = timeQueue.take(); // 阻塞取数据

                Map<String, Object> data = new HashMap<>();
                data.put("time", rawtime);

                String json = mapper.writeValueAsString(data);
                messagingTemplate.convertAndSend("/topic/time", json);
                System.out.println("推送数据: " + json);

            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
    推送换灯波长的线程
    * */
    private void pushWlLoop() {
        while (Constants.Running) {
            try {
                String rawwl = wlQueue.take(); // 阻塞取数据

                Map<String, Object> data = new HashMap<>();
                data.put("ChangeLampWl", rawwl);

                String json = mapper.writeValueAsString(data);
                messagingTemplate.convertAndSend("/topic/ChangeLampWl", json);
                System.out.println("推送数据: " + json);

            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

     /*
    推送样品池位置的线程
    * */

    private void pushPosLoop() {
        while (Constants.Running) {
            try{
                String rawpos = posQueue.take();

                Map<String, Object> data = new HashMap<>();
                data.put("Pos", rawpos);

                String json = mapper.writeValueAsString(data);
                messagingTemplate.convertAndSend("/topic/Pos", json);
                System.out.println("推送数据: " + json);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void pushBandwidthLoop() {
        while (Constants.Running) {
            try {
                String rawBandwidth = BandwidthQueue.take(); // 阻塞取数据

                Map<String, Object> data = new HashMap<>();
                data.put("Bandwidth", rawBandwidth);

                String json = mapper.writeValueAsString(data);
                messagingTemplate.convertAndSend("/topic/Bandwidth", json);
                System.out.println("推送数据: " + json);

            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

