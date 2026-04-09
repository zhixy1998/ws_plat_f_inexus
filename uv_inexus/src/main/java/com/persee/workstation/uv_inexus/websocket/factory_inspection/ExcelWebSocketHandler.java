package com.persee.workstation.uv_inexus.websocket.factory_inspection;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.util.DateUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.component.*;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.controller.guide.instrumentController.DeviceController;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TaskExecutionDetailMapper;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceTypeInfoDAO;
import com.persee.workstation.uv_inexus.dto.factory_inspection.*;
import com.persee.workstation.uv_inexus.enums.T600ComIdEnums;
import com.persee.workstation.uv_inexus.enums.factoryInspection.AalysisMethodEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceCalibrationInfoEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.EnergyLampEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.ScanSpeedEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.exception.BusinessException;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.factory_inspection.*;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceTypeInfoPO;
import com.persee.workstation.uv_inexus.service.factory_inspection.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.*;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.MessageUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.persee.workstation.uv_inexus.constant.Constants.*;
import static com.persee.workstation.uv_inexus.enums.Cache.CacheEnums.DEFAULT_CACHE;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.*;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.DEFAULT_TASK;
import static com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum.abs;
import static com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum.tPercent;
import static com.persee.workstation.uv_inexus.util.deviceUtil.param.ActionCmdEnum.BaseLine;
import static com.persee.workstation.uv_inexus.util.deviceUtil.param.ActionCmdEnum.Scan;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

/**
 * Excel缓存和会话存储，包括会话信息、文件名、会话ID、连接时间
 */
/*会话管理
每个WebSocket连接创建独立的Excel工作簿缓存
通过WorkbookCacheManager管理各会话的工作簿实例
实时编辑
接收前端发送的单元格更新指令(CellUpdDto)
调用TemplateService更新指定单元格数据
自动计算Excel公式并更新工作簿
数据推送
更新完成后将变更的Sheet数据序列化并推送给前端
支持二进制格式传输，提升效率
持久化机制
操作一定次数后自动保存到文件
连接断开时清理会话并持久化最终数据
* */
@Component
@Slf4j
public class ExcelWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private WorkbookCacheManager workbookCacheManager;
    @Autowired
    DeviceInfoService infoService;
    // 会话存储：sessionId -> SessionInfo
    private final Map<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> operationCount = new ConcurrentHashMap<>();
    @Autowired
    private TemplateService templateService;
    @Autowired
    private ExcelCellLocator excelCellLocator;
    @Autowired
    private ExcelCellEditor excelCellEditor;
    @Autowired
    private TaskExecutionDetailService detailService;
    @Autowired
    private CalArtifactsService calArtifactsService;
    @Autowired
    private TaskItemStatusService taskItemStatusService;
    @Autowired
    private CalStdCatsService catsService;
    @Autowired
    private ExcelCellComparator excelCellComparator;
    @Autowired
    private DevMessageUtil devMessageUtil;
    @Autowired
    private MessageUtil messageUtil;
    @Autowired
    private ExcelDataAssetService dataAssetService;
    @Autowired
    private TaskManagementService taskService;
    @Autowired
    private CalStdCatsService calStdCatsService;
    // 使用线程池执行独立的测量任务
    @Autowired
    @Qualifier("measurementTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    @Autowired
    private EhcacheUtil ehcacheUtil;
    //波形图持久化机制
    @Autowired
    private SpectrumDataService spectrumDataService;
    @Autowired
    private DevOperationUtilService devOperationUtilService;
    @Autowired
    private GetMaxDistanceUVTestTool getMaxDistanceUVTestTool;
    @Autowired
    private FindHalfPeakUVTestTool findHalfPeakUVTestTool;
    @Autowired
    private DeviceManageService deviceManageService;
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Autowired
    private PythonPathConstant pythonPathConstant;
    @Autowired
    private DeviceController deviceController;
    @Autowired
    private TaskExecutionDetailMapper taskExecutionDetailMapper;
    @Autowired
    private SystemAlgorithmConfService systemAlgorithmConfService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private StandardMaterialManager materialManager;
    @Autowired
    private FindMaxOffsetUVTestTool findMaxOffsetUVTestTool;
    @Autowired
    private SpectrumResultCalculator calculator;
    //    private volatile Boolean autoSwitchSamplePool = true;
    private final UnknowCountMessageQueue queue = MessageOnTimeDeal.unknowCountMessageQueue;
    //用任务id做主键，任务当前测量的检测项为值，线程安全
    private final Map<String, String> currentMeasureItemMap = new ConcurrentHashMap<>();

    @Autowired
    //任务 id 和仪器型号关联关系 - 使用自定义缓存包装类
    private DeviceTypeInfoDAO deviceTypeInfoDAO;

    // 在类中添加缓存存储
    private final Map<String, List<JSONObject>> waveformCache = new ConcurrentHashMap<>();
    // 在类中添加任务管理Map
    private final Map<String, Future<Boolean>> taskFutures = new ConcurrentHashMap<>();

    // 测量状态管理
    private final Map<String, Boolean> measurementStatusMap = new ConcurrentHashMap<>();
    // 测量任务对应的session  key taskid value session
    private final Map<String, Set<WebSocketSession>> sessionTaskMap = new ConcurrentHashMap<>();

    //给个标志，能停止还是不能停止，用taskId做key
    HashMap<String, Boolean> taskStopFlag = new HashMap<>();
    // 心跳调度器
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newScheduledThreadPool(5, r -> {
                Thread t = new Thread(r, "calibrationCheckWebSocket-Heartbeat-Thread");
                t.setDaemon(true);
                return t;
            });
    // 用于跟踪已注册的 session，避免重复添加
    private final Map<String, Set<String>> registeredSessionIds = new ConcurrentHashMap<>();
    //任务 - 设备绑定关系
    private final Map<String, String> taskDeviceBindingMap = new ConcurrentHashMap<>();
    // 记录每个连接的最后活动时间
    private final Map<String, Long> lastActivityTime = new ConcurrentHashMap<>();
    // 记录心跳任务
    private final Map<String, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();

    // 时间扫描停止的控制
    private static final ConcurrentHashMap<String, Boolean> timeScanStopControl = new ConcurrentHashMap<>();

    private static final long HEARTBEAT_INTERVAL = 30000; // 30秒心跳间隔
    private static final long HEARTBEAT_TIMEOUT = 60000;   // 60秒超时
    //判断是否终止
//    private volatile  halt = false;
    private final Map<String, Boolean> halt = new ConcurrentHashMap<>();
    private List<ChangeParam> zeroList = new ArrayList<>();
    //任务id和仪器型号关联关系

    @Autowired
    //任务 id 和仪器型号关联关系 - 使用自定义缓存包装类
    private  TaskDeviceTypeCache taskAndVersionMap;



// ... existing code ...

    // 在类中添加暂停控制类
    @Data
    public static class TaskPauseControl {
        //初始许可证是0，也就是一调用就会停止
        private final Semaphore semaphore = new Semaphore(0);
        /*
          标识任务是否处于暂停状态
          true: 任务当前处于暂停状态
          false: 任务正常运行
        * */
        private volatile boolean paused = false;

        // 添加线程安全的状态更新方法
        public synchronized void setPaused(boolean paused) {
            this.paused = paused;
        }

        public synchronized boolean isPaused() {
            return this.paused;
        }
    }

    // 在类中添加暂停控制映射
    private final Map<String, TaskPauseControl> pauseControls = new ConcurrentHashMap<>();

    private final Map<String, BlockingQueue<QueuedMessage>> messageQueues = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> queueProcessingFlags = new ConcurrentHashMap<>();

    // 消息队列项
    @Data
    private static class QueuedMessage {
        private final WebSocketSession originalSession;
        private final String taskId;
        private final JSONObject payload;
        private final CountDownLatch latch; // 用于同步等待发送完成

        public QueuedMessage(WebSocketSession originalSession, String taskId, JSONObject payload) {
            this.originalSession = originalSession;
            this.taskId = taskId;
            this.payload = payload;
            this.latch = new CountDownLatch(1);
        }
    }

    // 提供添加会话的方法

    // 在类成员变量处添加一个新的数据结构，用于跟踪已连接的前端地址
    private final Map<String, Set<String>> taskToFrontendAddresses = new ConcurrentHashMap<>();


    public void addSessionToTask(String taskId, WebSocketSession session) {
        String sessionId = session.getId();

        // 获取前端地址（IP + 端口）
        String frontendAddress = getFrontendAddress(session);

        // 检查是否已经注册过
        Set<String> sessionIds = registeredSessionIds.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet());

        synchronized (sessionIds) {
            // 只有未注册的 session 才添加
            if (sessionIds.add(sessionId)) {
                // 检查这个前端地址是否已经存在于该任务中
                Set<String> frontendAddresses = taskToFrontendAddresses.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet());

                synchronized (frontendAddresses) {
                    if (frontendAddresses.contains(frontendAddress)) {
                        log.warn("检测到同一前端重复连接，跳过添加：taskId={}, sessionId={}, 前端地址={}",
                                taskId, sessionId, frontendAddress);
                        // 从 sessionId 集合中移除刚才添加的 ID
                        sessionIds.remove(sessionId);
                        return;
                    }

                    // 添加前端地址
                    frontendAddresses.add(frontendAddress);
                }

                sessionTaskMap.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(session);
                log.info("添加新会话到任务：taskId={}, sessionId={}, 前端地址={}", taskId, sessionId, frontendAddress);
            } else {
                log.debug("会话已存在，跳过添加：taskId={}, sessionId={}", taskId, sessionId);
            }
        }
    }


    private boolean sendToActiveSession(WebSocketSession originalSession, String taskId, JSONObject payload) {
//        log.info("我被调用了" + payload.toString());
        try {
            if (originalSession != null && !isSessionValid(originalSession)) {
                // 【修改】克隆 payload 对象，避免修改传入的对象导致线程安全问题
                JSONObject payloadCopy = (JSONObject) payload.clone();
                payloadCopy.put("taskId", taskId);

                originalSession.sendMessage(new TextMessage(payloadCopy.toJSONString()));
                return true;
            }
            Set<WebSocketSession> sessions = sessionTaskMap.get(taskId);
            if (CollectionUtils.isNotEmpty(sessions)) {
                Iterator<WebSocketSession> iterator = sessions.iterator();
                while (iterator.hasNext()) {
                    WebSocketSession session = iterator.next();
                    if (isSessionValid(session)) {
                        // 【修改】克隆 payload 对象，避免修改传入的对象导致线程安全问题
                        JSONObject payloadCopy = (JSONObject) payload.clone();
                        payloadCopy.put("taskId", taskId);

                        log.info("准备发送消息到 sessionId: {}, 前端地址：{}, 消息内容：{}",
                                session.getId(), getFrontendAddress(session), payloadCopy.toJSONString());

                        session.sendMessage(new TextMessage(payloadCopy.toJSONString()));
                        log.info("消息发送成功到 sessionId: {}", session.getId());
                    } else {
                        // 移除无效 session 时，也要清理对应的前端地址
                        String frontendAddress = getFrontendAddress(session);
                        Set<String> frontendAddresses = taskToFrontendAddresses.get(taskId);
                        if (frontendAddresses != null) {
                            frontendAddresses.remove(frontendAddress);
                        }
                        iterator.remove();
                        log.warn("WebSocket 消息发送失败：taskId={}, 源会话已关闭，前端地址={}", taskId, frontendAddress);
                    }
                }
                log.info("消息已发送到 {} 个 session，sessionId 列表：{}", sessions.size(),
                        sessions.stream().map(WebSocketSession::getId).collect(Collectors.toList()));
            } else {
                log.warn("没有找到活跃的 session: taskId={}", taskId);
            }

            return true;
        } catch (Exception e) {
            log.error("发送消息失败：taskId={}, 错误：{}", taskId, e.getMessage(), e);
            return false;
        }
    }


    /**
     * 获取前端连接的地址标识（IP + 端口）
     */
    private String getFrontendAddress(WebSocketSession session) {
        try {
            java.net.InetSocketAddress remoteAddress = session.getRemoteAddress();
            if (remoteAddress != null) {
                String ip = remoteAddress.getAddress() != null ?
                        remoteAddress.getAddress().getHostAddress() : "unknown";
                int port = remoteAddress.getPort();
                return ip + ":" + port;
            }
        } catch (Exception e) {
            log.warn("获取远程地址失败：{}", e.getMessage());
        }
        // 如果获取不到远程地址，使用 sessionId 作为备用标识
        return "session:" + session.getId();
    }

    // ... existing code ...


    private boolean sendToActiveSession(WebSocketSession originalSession, String taskId, String payload) {
        try {
            HashSet<WebSocketSession> set = new HashSet<>();
            set.add(originalSession);
            Set<WebSocketSession> sessions = sessionTaskMap.getOrDefault(taskId, set);
            if (CollectionUtils.isNotEmpty(sessions)) {
                Iterator<WebSocketSession> iterator = sessions.iterator();
                while (iterator.hasNext()) {
                    WebSocketSession session = iterator.next();
                    if (isSessionValid(session)) {
//                        log.info("消息是：" + payload.toJSONString());
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("taskId", taskId);
                        jsonObject.put("data", payload);
                        session.sendMessage(new TextMessage(jsonObject.toJSONString()));
                        log.info("消息发送给了：" + session.getId());
                    } else {
                        iterator.remove();
                        log.warn("WebSocket消息发送失败: taskId={}, 源会话已关闭", taskId);
                    }
                }

            }
//            // 获取或创建任务的消息队列
//            BlockingQueue<QueuedMessage> queue = messageQueues.computeIfAbsent(taskId, k -> new LinkedBlockingQueue<>());
//
//            // 创建消息并加入队列
//            QueuedMessage queuedMessage = new QueuedMessage(originalSession, taskId, payload);
//            boolean offer = queue.offer(queuedMessage);
//
//            // 启动队列处理器（如果还没有运行）
//            startQueueProcessor(taskId);
//
//            // 等待发送完成（可选，可根据性能需求调整）
//            try {
//                // 设置合理的超时时间，避免永久阻塞
//                boolean completed = queuedMessage.getLatch().await(5, TimeUnit.SECONDS);
//                if (!completed) {
//                    log.warn("WebSocket消息发送超时: taskId={}, 消息类型={}", taskId, payload.get("type"));
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                log.warn("等待消息发送完成时被中断: taskId={}", taskId);
//            }

            return true; // 消息已加入队列，不代表发送成功
        } catch (Exception e) {
            log.error("添加消息到队列失败: taskId={}, 错误: {}", taskId, e.getMessage());
            return false;
        }
    }

    // 实时回显 - 添加连接状态检查和频率控制
    private void realTimeEcho(String taskId, String content) throws BusinessException {
        // 检查任务是否仍在运行
        if (!isTaskMeasuring(taskId)) {
            log.debug("任务 {} 已停止，跳过实时回显", taskId);
            return;
        }

        // 检查是否有活跃连接
        if (!sessionTaskMap.containsKey(taskId)) {
            log.debug("任务 {} 无活跃连接，跳过实时回显", taskId);
            return;
        }

        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("liveEcho", content);

        // 发送到队列
        sendToActiveSession(null, taskId, jsonObject1);
    }

    // 启动队列处理器
//    private void startQueueProcessor(String taskId) {
//        AtomicBoolean processing = queueProcessingFlags.computeIfAbsent(taskId, k -> new AtomicBoolean(false));
//
//        if (processing.compareAndSet(false, true)) {
//            // 启动单独的线程处理队列
//            CompletableFuture.runAsync(() -> processMessageQueue(taskId), taskExecutor);
//        }
//    }

//    // 处理消息队列
//    private void processMessageQueue(String taskId) {
//        try {
//            BlockingQueue<QueuedMessage> queue = messageQueues.get(taskId);
//            if (queue == null) {
//                return;
//            }
//
//            QueuedMessage message;
//            while ((message = queue.poll()) != null) {
//                // 实际发送消息
//                boolean success = sendMessageToSession(message.getOriginalSession(), message.getTaskId(), message.getPayload());
//
//                // 通知等待线程发送完成
//                message.getLatch().countDown();
//
//                if (!success) {
//                    log.warn("消息发送失败: taskId={}, 消息类型={}", message.getTaskId(), message.getPayload().get("type"));
//                }
//
//                // 避免过于频繁的发送，可调节此延时
//                try {
//                    Thread.sleep(10); // 10ms间隔，可根据需要调整
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        } finally {
//            // 重置处理标志
//            AtomicBoolean processing = queueProcessingFlags.get(taskId);
//            if (processing != null) {
//                processing.set(false);
//            }
//
//            // 如果队列仍有消息，重新启动处理器
//            BlockingQueue<QueuedMessage> remainingQueue = messageQueues.get(taskId);
//            if (remainingQueue != null && !remainingQueue.isEmpty()) {
//                startQueueProcessor(taskId);
//            }
//        }
//    }

    // 实际的消息发送逻辑
//    private boolean sendMessageToSession(WebSocketSession originalSession, String taskId, JSONObject payload) {
//        log.debug("准备发送消息到 taskId: {}, 消息内容: {}", taskId, payload.toJSONString());
//
//        try {

    /// /            // 首先尝试使用原始session
    /// /            if (isSessionValid(originalSession)) {
    /// /                try {
    /// /                    originalSession.sendMessage(new TextMessage(payload.toJSONString()));
    /// /                    log.info("通过原始session发送消息成功: taskId={}, 消息类型={}", taskId, payload.get("type"));
    /// /                    return true;
    /// /                } catch (IOException e) {
    /// /                    log.warn("原始session发送失败: {}", e.getMessage());
    /// /                } catch (IllegalStateException e) {
    /// /                    log.warn("原始session状态异常: {}", e.getMessage());
    /// /                }
    /// /            }
//
//            // 尝试使用sessionTaskMap中的session
//            Set<WebSocketSession> sessions = sessionTaskMap.get(taskId);
//            if (sessions != null) {
//                for (WebSocketSession mappedSession : sessions) {
//                    if (isSessionValid(mappedSession)) {
//                        try {
//                            mappedSession.sendMessage(new TextMessage(payload.toJSONString()));
//                            log.info("通过映射session发送消息成功: taskId={}, 消息类型={}", taskId, payload.toJSONString());
//                            return true;
//                        } catch (Exception e) {
//                            log.warn("映射session状态异常: {}", e);
//                            // 如果状态异常，清理该session
//                            // 移除 session
//                            Set<WebSocketSession> sessionSet = sessionTaskMap.get(taskId);
//                            if (sessionSet != null) {
//                                sessionSet.remove(mappedSession);
//                                if (sessionSet.isEmpty()) {
//                                    sessionTaskMap.remove(taskId);
//                                }
//                            }
//                        }
//                    }
//                }
//                log.warn("所有发送方式都失败: taskId={}", taskId);
//            } else {
//                log.warn("没有可用的session: taskId={}", taskId);
//            }
//
//            return false;
//        } catch (Exception e) {
//            log.error("发送消息时发生异常: {}", e.getMessage());
//            return false;
//        }
//    }

    // 获取测量状态的方法
    public boolean isTaskMeasuring(String taskId) {
        return measurementStatusMap.getOrDefault(taskId, false);
    }

    // 修改 setTaskMeasuring 方法
    public void setTaskMeasuring(String taskId, boolean isMeasuring) throws BusinessException {
        if (!isMeasuring) {
            TaskManagement management = taskService.getById(taskId);
            deviceInfoService.updateDeviceCurrentStatus(management.getOperationInstrument(), DEVICE_STATUS_IDLE);
        }
//        if (!isMeasuring) {
//            log.info("修改测量状态: taskId={}, isMeasuring={}", taskId, isMeasuring);
//        }
        Boolean previousStatus = measurementStatusMap.get(taskId);
        measurementStatusMap.put(taskId, isMeasuring);

        // 只有当状态发生改变时才发送消息
        if (previousStatus == null || !previousStatus.equals(isMeasuring)) {
//            sendMeasurementStatusToClient(taskId, isMeasuring);
            JSONObject statusMessage = new JSONObject();
            statusMessage.put("isMeasuring", isMeasuring);
            log.info("发送测量状态更新: taskId={}, isMeasuring={}", taskId, isMeasuring);
            sendToActiveSession(null, taskId, statusMessage);
        }
    }

    public String getCurrentMeasureItem(String taskId) {
        return currentMeasureItemMap.get(taskId);
    }

    public void setCurrentMeasureItem(String taskId, String measureItem) {
        currentMeasureItemMap.put(taskId, measureItem);
    }


    // 会话信息类
    @Data
    private static class SessionInfo {
        private String fileName;
        private String sessionId;
        private long connectTime;
    }

    /**
     * 从会话URL中提取taskId参数
     */
    private String getTaskIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("taskId=")) {
                    return param.substring(7); // "taskId=".length()
                }
            }
        }
        return null;
    }

    private String getUpdateByFromUrl(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null) {
                MultiValueMap<String, String> parameters =
                        UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams();
                List<String> userIdList = parameters.get("updateBy");
                if (userIdList != null && !userIdList.isEmpty()) {
                    return userIdList.get(0);
                }
            }
        } catch (Exception e) {
            log.warn("解析URL参数失败", e);
        }
        return null;
    }


    //这里的文件名应该是对应着使用模板的文件名  也就是说结果模板名和使用模板名一致
    //注意这里说的模板名是模板code值
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String sessionId = session.getId();
            // 从URL参数或握手信息中获取文件名
            String fileName = getFileNameFromSession(session);
            String taskId = getTaskIdFromSession(session);
            String updateBy = getUpdateByFromUrl(session);
            if (StringUtils.isBlank(fileName)) {
//                LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
//                wrapper.eq(TaskManagement::getId, taskId);
                //这里查询出内置任务
                TaskManagement management = taskService.getById(taskId);
                if (management != null) {
                    fileName = management.getResultTemplate();
                }
            }
            session = new ConcurrentWebSocketSessionDecorator(
                    session,
                    10 * 1000, // 发送超时时间（毫秒）
                    1024 * 1024 // 最大缓冲区大小（字节）
            );
//            sessionTaskMap.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(session);
            addSessionToTask(taskId, session);
//            boolean b = workbookCacheManager.acquireEditLock(taskId, updateBy);
            // 检查是否已经获取过编辑锁（checkTaskUsage 时已获取）
//            String currentEditor = workbookCacheManager.getCurrentEditor(taskId);
//            if (currentEditor == null) {
//                log.warn("任务{}没有获取锁，不允许连接（checkTaskUsage 未调用？）", taskId);
//                JSONObject errorMsg = new JSONObject();
//                errorMsg.put("error", "请先检查任务状态");
//                session.sendMessage(new TextMessage(errorMsg.toJSONString()));
//                session.close();
//                return;
//            }

//            if (!currentEditor.equals(updateBy)) {
//                log.warn("任务正在被其他用户编辑，拒绝连接：taskId={}, 当前用户={}, 编辑者={}", taskId, updateBy, currentEditor);
//                JSONObject errorMsg = new JSONObject();
//                errorMsg.put("error", "任务正在被其他用户编辑");
//                errorMsg.put("editor", currentEditor);
//                session.sendMessage(new TextMessage(errorMsg.toJSONString()));
//                session.close();
//                return;
//            }

            log.info("获取编辑锁成功：taskId={}, updateBy={}", taskId, updateBy);
            // 为会话创建专属Workbook
            Workbook workbook = workbookCacheManager.createTaskWorkbook(taskId, fileName);
            log.info("WebSocket连接建立，会话ID: {}, 文件: {}", sessionId, fileName);
            //记录连接时间
            lastActivityTime.put(sessionId, System.currentTimeMillis());
            //这个任务有存活的session
            // 添加 session


            JSONObject statusMessage = new JSONObject();
            statusMessage.put("isMeasuring", isTaskMeasuring(taskId));
            sendToActiveSession(session, taskId, statusMessage);

            // 发送当前检测项信息
            String currentMeasureItem = getCurrentMeasureItem(taskId);
            if (StringUtils.isNotBlank(currentMeasureItem)) {
                ExcelDataAsset asset = dataAssetService.getById(currentMeasureItem);
                if (asset != null) {
                    Map<String, Object> dataMap = asset.getDataMap();
                    Integer repeatCount = MapUtils.getInteger(dataMap, REPEAT_COUNT.getDescription());

                    JSONObject currentItemMessage = new JSONObject();
                    JSONObject currentItemData = new JSONObject();
                    currentItemData.put("itemId", currentMeasureItem);
                    currentItemData.put("checkCount", repeatCount != null ? repeatCount : 1);
                    currentItemMessage.put("currentItem", currentItemData);

                    sendToActiveSession(session, taskId, currentItemMessage);
                    log.info("已发送当前检测项信息，任务ID: {}, 检测项ID: {}", taskId, currentMeasureItem);
                }
            }

            //如果是选择过标准物的任务，则需要修改工作簿的特征值和标准值
            LambdaQueryWrapper<TaskExecutionDetail> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TaskExecutionDetail::getTaskId, taskId);
            //这是修改过标准物的任务
            List<TaskExecutionDetail> list = detailService.list(queryWrapper);
            if (CollectionUtils.isNotEmpty(list)) {
                for (TaskExecutionDetail detail : list) {
                    String standardId = detail.getStandardId();
                    String itemId = detail.getItemId();
                    //看看这个 标准物是哪个检测项的
                    ExcelDataAsset item = dataAssetService.getById(itemId);
                    String data = item.getStructuredData();
                    ObjectMapper mapper = new ObjectMapper();
                    //获取到检测项的所有数据
                    Map<String, Object> dataMap = mapper.readValue(data, new TypeReference<Map<String, Object>>() {
                    });
                    String groupName = MapUtils.getString(dataMap, "检测类别");
                    String itemName = MapUtils.getString(dataMap, "项目名称");
                    //得到标准物质实体，可以进行修改了
                    String characteristicValue = "";
                    String standardValue = "";
                    CalArtifacts artifacts = calArtifactsService.getById(standardId);
                    if (artifacts == null) {
                        CalStdCats cats = catsService.getById(standardId);
                        characteristicValue = cats.getCharacteristicValue();
                        standardValue = cats.getStandardValue();
                    }
                    if (artifacts != null) {
                        characteristicValue = artifacts.getCharacteristicValue();
                        standardValue = artifacts.getStandardValue();
                    }
                    if (StringUtils.isNotBlank(characteristicValue)) {
                        CellUpdDto cellUpdDto = new CellUpdDto();
                        cellUpdDto.setCellValue(characteristicValue);
                        cellUpdDto.setSheetName(groupName);
                        cellUpdDto.setValueType(CHARACTERISTIC.getDescription());
                        cellUpdDto.setProjectName(itemName);
                        updateWorkbookCells(workbook, cellUpdDto);
                    }
                    if (StringUtils.isNotBlank(standardValue)) {
                        CellUpdDto cellUpdDto = new CellUpdDto();
                        cellUpdDto.setCellValue(standardValue);
                        cellUpdDto.setSheetName(groupName);
                        cellUpdDto.setValueType(STANDARD.getDescription());
                        cellUpdDto.setProjectName(itemName);
                        updateWorkbookCells(workbook, cellUpdDto);
                    }

                }
            }
            log.info("开始发送");
            this.pushEntireWorkbook(session, taskId, fileName);
            log.info("结束发送");
//            session.sendMessage(new TextMessage(statusMessage.toString()));
            // 发送缓存的波形图数据给新会话
            sendCachedWaveformData(session, taskId);
            // 启动心跳任务
            startHeartbeatTask(taskId);
//            TaskManagement management = taskService.getById(taskId);
            if (StringUtils.isBlank(String.valueOf(taskAndVersionMap.get(taskId)))) {
                TaskManagement taskManagement = taskService.getById(taskId);
                String s1 = deviceInfoService.queryDeviceTypeBySerialNumber(taskManagement.getOperationInstrument());
                taskAndVersionMap.put(taskId, s1);
            }
            super.afterConnectionEstablished(session);
        } catch (Exception e) {
            log.error("WebSocket连接建立失败", e);
        }
    }

    private void startHeartbeatTask(String taskId) {

        // 取消旧任务
        ScheduledFuture<?> oldTask = heartbeatTasks.get(taskId);
        if (oldTask != null && !oldTask.isDone()) {
            oldTask.cancel(false);
        }

        // 创建新心跳任务
        ScheduledFuture<?> newTask = heartbeatScheduler.scheduleAtFixedRate(
                () -> sendHeartbeat(taskId),
                HEARTBEAT_INTERVAL,
                HEARTBEAT_INTERVAL,
                TimeUnit.MILLISECONDS
        );

        heartbeatTasks.put(taskId, newTask);
    }

    private void sendHeartbeat(String taskId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ping", "heartbeat");
        sendToActiveSession(null, taskId, jsonObject);
    }

    private void pushEntireWorkbook(WebSocketSession session, String taskId, String fileName) throws IOException {
        if (!isSessionValid(session)) {
            log.warn("尝试向无效的session发送数据: taskId={}", taskId);
            return;
        }

        Workbook workbook = workbookCacheManager.getTaskWorkbook(taskId);

        WorkbookDto workbookDto = new WorkbookDto();
        workbookDto.setWorkbookName(fileName);
        workbookDto.setSheets(extractAllSheetsData(workbook));

        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writeValueAsString(workbookDto);
        JSONObject jsonObject = JSONObject.parseObject(jsonData);
        sendToActiveSession(null, taskId, jsonObject);
//        session.sendMessage(new TextMessage(jsonData));
    }


    private List<SheetDto> extractAllSheetsData(Workbook workbook) {
        List<SheetDto> sheets = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            SheetDto sheetDto = extractSheetData(sheet, workbook);
            sheets.add(sheetDto);
        }
        return sheets;
    }


    private String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return "=" + cell.getCellFormula();
            default:
                return "";
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        session = new ConcurrentWebSocketSessionDecorator(
//                session,
//                10 * 1000, // 发送超时时间（毫秒）
//                1024 * 1024 // 最大缓冲区大小（字节）
//        );
        // 更新最后活动时间
        lastActivityTime.put(session.getId(), System.currentTimeMillis());

        String payload = message.getPayload();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(payload);
        String type = root.get("type").asText();
        String updateBy = root.get("updateBy").asText();
        String taskId = root.get("taskId").asText();

        //得到当前任务的仪器类型，方便对之后的操作做界定
        if (StringUtils.isBlank(String.valueOf(taskAndVersionMap.get(taskId)))) {
            TaskManagement taskManagement = taskService.getById(taskId);
            String s1 = deviceInfoService.queryDeviceTypeBySerialNumber(taskManagement.getOperationInstrument());
            taskAndVersionMap.put(taskId, s1);
        }
        if (CLOSE.getCode().equals(type)) {
            try {
                log.info("收到停止指令");
                taskId = root.get("taskId").asText();

                //当前测量项
                String measureItem = getCurrentMeasureItem(taskId);
                ExcelDataAsset asset = dataAssetService.getById(measureItem);
                if (asset != null) {
                    Map<String, Object> dataMap = asset.getDataMap();
                    if (CollectionUtils.isNotEmpty(dataMap)) {
                        //是不是光谱扫描
                        String res = MapUtils.getString(dataMap, SPECTRAL_SCAN_TYPE.getDescription());
                        //扫描个数
                        Integer scanCount = MapUtils.getInteger(dataMap, SCAN_COUNT.getDescription());

                        //如果是光谱扫描
                        if (YES.getDescription().equals(res)) {
                            log.info("光谱扫描>>>>>>>停止");
                            TaskManagement management = taskService.getById(taskId);

                            DevOperationInputParam param = new DevOperationInputParam();
                            param.setDeviceSerialNumber(management.getOperationInstrument());
                            param.setOperationType(OperationTypeEnums.Stop.getType());
                            if (taskStopFlag.get(taskId) != null && taskStopFlag.get(taskId)) {
                                R<OperationResultEntity> stop = devOperationUtilService.operation(param);
                                if (stop.getSuccess()) {
                                    log.info("停止成功");
                                    deviceInfoService.updateDeviceCurrentStatus(management.getOperationInstrument(), DEVICE_STATUS_IDLE);
                                }
                            }
                            // 只停止任务，不关闭连接
                            stopTask(taskId);
//
//                            // 发送停止确认消息，而不是关闭连接
//                            JSONObject confirmMsg = new JSONObject();
//                            confirmMsg.put("isMeasuring", false);
//                            sendToActiveSession(session, taskId, confirmMsg);
                        } else if (scanCount != null) {
                            log.info("时间扫描>>>>>停止");
                            while (true) {
                                if (timeScanStopControl.get(taskId)) {
//                            setTaskMeasuring(taskId, false);
                                    TaskManagement management = taskService.getById(taskId);
//                            if (device.getSuccess()) {
                                    ConcurrentMap<String, Boolean> timeMeasureIsStop = UdpSendUtil.timeMeasureIsStop;
                                    DeviceInfoPO info = infoService.getInfo(management.getOperationInstrument());
                                    Boolean put = timeMeasureIsStop.put(info.getDeviceId(), true);
                                    stopTask(taskId);
                                    // 发送确认消息
                                    JSONObject confirmMsg = new JSONObject();
                                    confirmMsg.put("isMeasuring", false);
                                    sendToActiveSession(session, taskId, confirmMsg);
                                    deviceInfoService.updateDeviceCurrentStatus(management.getOperationInstrument(), DEVICE_STATUS_IDLE);
                                    break;
                                }
                            }
//                            }
                        } else {
                            log.info("光度扫描>>>>>>>>停止");
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("warn", "光度测量任务执行时间较短，无需中途停止！！！");
                            sendToActiveSession(session, taskId, jsonObject);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("停止任务异常", e);
                // 发送错误响应而不是让异常传播
                try {
                    JSONObject errorMsg = new JSONObject();
                    errorMsg.put("error", "停止任务失败: " + e.getMessage());
                    sendToActiveSession(session, taskId, errorMsg);
                } catch (Exception sendError) {
                    log.error("发送错误消息也失败", sendError);
                }
            }
            // 注意：这里不调用 session.close()
        }


        if (SINGLE_ITEM.getCode().equals(type)) {
            log.info("收到单项测试指令");
            taskStopFlag.put(taskId, false);
            TaskManagement management1 = taskService.getById(taskId);
            if (StringUtils.isNotBlank(management1.getOperationInstrument())) {
                DeviceInfoPO info = deviceInfoService.getInfo(management1.getOperationInstrument());
                Integer status = info.getDeviceCurrentStatus();
                if (status != 0) {
                    // 【改造】检查设备是否被其他任务占用
                    String occupiedTaskId = findTaskByDevice(management1.getOperationInstrument());
                    if (StringUtils.isNotBlank(occupiedTaskId) && !occupiedTaskId.equals(taskId)) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("error", "设备正在被其他任务使用中（任务 ID: " + occupiedTaskId + "）！！！");
                        sendToActiveSession(session, taskId, jsonObject);
                        sendToActiveSession(session, taskId, jsonObject);
                        return;
                    }
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", "设备正在使用中！！！");
                    sendToActiveSession(session, taskId, jsonObject);
                    return;
                }
            }
            //判断一下是单样品池还是多样品池

            TaskManagement management = new TaskManagement();
            management.setId(taskId);
            management.setUpdateBy(updateBy);
            management.setStartTime(new Date());
            management.setCompletionStatus(IN_PROGRESS.getCode());
//            management.set
            taskService.updateById(management);
            InspectionRequestDto request = mapper.readValue(payload, InspectionRequestDto.class);
            TaskManagement management2 = taskService.getById(taskId);
            //如果任务没有绑定设备号的时候，绑定第一次遇到的那个设备
            if (management2 != null && StringUtils.isBlank(management2.getOperationInstrument())) {
                //只有检定校准在用，出厂检验任务创建时已经存在设备号
                R<String> device = deviceManageService.getConnectDevice(updateBy);

//                            String deviceSerialNumber = "30-U1602-01-0022";
                String deviceSerialNumber = device.getData();

                LambdaUpdateWrapper<TaskManagement> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(TaskManagement::getId, taskId)  // 根据taskId匹配
                        .set(TaskManagement::getOperationInstrument, deviceSerialNumber); // 设置新序列号

                // 执行更新
                taskService.update(updateWrapper);
            }
            management1 = taskService.getById(taskId);
            if (StringUtils.isNotBlank(management1.getOperationInstrument())) {
                //得到当前任务的仪器类型，方便对之后的操作做界定
                if (StringUtils.isBlank(String.valueOf(taskAndVersionMap.get(taskId)))) {
                    TaskManagement taskManagement = taskService.getById(taskId);
                    String s1 = deviceInfoService.queryDeviceTypeBySerialNumber(taskManagement.getOperationInstrument());
                    taskAndVersionMap.put(taskId, s1);
                }
                deviceInfoService.updateDeviceCurrentStatus(management1.getOperationInstrument(), DEVICE_STATUS_MEASURE);
            }
            handleSingleItemInspection(session, request, taskId, updateBy, false);
        }
        if (AUTO.getCode().equals(type)) {
            log.info("收到自动测试指令");


            TaskManagement management1 = taskService.getById(taskId);

            //如果任务没有绑定设备号的时候，绑定第一次遇到的那个设备
            if (management1 != null && StringUtils.isBlank(management1.getOperationInstrument())) {
                R<String> device = deviceManageService.getConnectDevice(updateBy);
//                            String deviceSerialNumber = "30-U1602-01-0022";
                String deviceSerialNumber = device.getData();

                LambdaUpdateWrapper<TaskManagement> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(TaskManagement::getId, taskId)  // 根据taskId匹配
                        .set(TaskManagement::getOperationInstrument, deviceSerialNumber); // 设置新序列号

                // 执行更新
                taskService.update(updateWrapper);
                // 记录任务 - 设备绑定关系
                taskDeviceBindingMap.put(taskId, deviceSerialNumber);
            }

            if (StringUtils.isNotBlank(management1 != null ? management1.getOperationInstrument() : null)) {
                DeviceInfoPO info = deviceInfoService.getInfo(management1.getOperationInstrument());
                Integer status = info.getDeviceCurrentStatus();
//                if (info.getIsActivated() == 0) {
//                    JSONObject jsonObject = new JSONObject();
//                    jsonObject.put("error", "设备未激活！！！");
//                    sendToActiveSession(session, taskId, jsonObject);
//                }
                if (status != 0) {
                    // 【改造】检查设备是否被其他任务占用
                    String occupiedTaskId = findTaskByDevice(management1.getOperationInstrument());
                    if (StringUtils.isNotBlank(occupiedTaskId) && !occupiedTaskId.equals(taskId)) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("error", "设备正在被其他任务使用中（任务 ID: " + occupiedTaskId + "）！！！");
                        sendToActiveSession(null, taskId, jsonObject);
//                        throw new BusinessException("设备正在被其他任务使用中（任务 ID: " + occupiedTaskId + "）！！！");
                        return;
                    }
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", "设备正在使用中！！！");
                    sendToActiveSession(session, taskId, jsonObject);
//                    throw new BusinessException("设备正在使用中！！！");
                    return;
                }
            } else {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "找不到该任务！！！");
                sendToActiveSession(null, taskId, jsonObject);
//                throw new BusinessException("找不到该任务！！！");
                return;
            }
            DeviceAttachmentTypeVO vo = deviceInfoService.getAttachmentInfo(management1.getOperationInstrument());

            if (vo.getSamplePoolNum() == 1) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "单样品池不支持自动测量！！！");
                sendToActiveSession(null, taskId, jsonObject);
                return;
            }
//            if (vo.getSamplePoolNum() < 1) {
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("error", "未知样品池！！！");
//                sendToActiveSession(null, taskId, jsonObject);
//                return;
//            }
            TaskManagement management = new TaskManagement();
            management.setId(taskId);
            management.setUpdateBy(updateBy);
            management.setStartTime(new Date());
            management.setCompletionStatus(IN_PROGRESS.getCode());
//            management.set
            taskService.updateById(management);
            // 添加这一行：设置停止标志，与单项测量保持一致
            taskStopFlag.put(taskId, false);
            try {
                handleAutoInspection(session, taskId, updateBy);
            } catch (Exception e) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", e.getMessage());
                sendToActiveSession(null, taskId, jsonObject);
                setTaskMeasuring(taskId, false);
            }

        }
        if (SET_STANDARD.getCode().equals(type)) {
            WebSocketMessageDto<StdMaterialSelectionDto> messageWrapper =
                    mapper.readValue(payload, new TypeReference<WebSocketMessageDto<StdMaterialSelectionDto>>() {
                    });
            StdMaterialSelectionDto selectionDto = messageWrapper.getData();
            taskId = selectionDto.getTaskId();
            String groupName = selectionDto.getGroupName();
            String itemName = selectionDto.getItemName();
            String standardId = selectionDto.getStandardId();
            String standardType = selectionDto.getStandardType();
            String itemId = selectionDto.getItemId();
            log.info("收到设置检测项: {} 的标准物指令", itemName);

            //判断任务是否正在被编辑中
            boolean edited = workbookCacheManager.isTaskBeingEditedByOthers(taskId, updateBy);
            if (edited) {
                sendToActiveSession(null, taskId, "任务正在被编辑中");
//                session.sendMessage(new TextMessage("任务正在被编辑中"));
            }
            if (DEFAULT_TASK.getCode().equals(taskId)) {
                LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(TaskManagement::getContentCategory, TEMPLATE_BUILT_IN_TASK.getCode());
                TaskManagement one = taskService.getOne(wrapper);
                taskId = one.getId();
            }
            if (!edited) {

                //如果之前这个任务有设置过这个检测项的标准物，再次设置
                LambdaQueryWrapper<TaskExecutionDetail> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TaskExecutionDetail::getItemId, itemId)
                        .eq(TaskExecutionDetail::getStandardType, standardType)
                        .eq(TaskExecutionDetail::getTaskId, taskId);
                TaskExecutionDetail detail = new TaskExecutionDetail();

                boolean acquired = workbookCacheManager.acquireEditLock(taskId, updateBy);
                log.info("尝试获取编辑锁");

                if (acquired) {
                    log.info("获取编辑锁成功");
                }
                detail.setStandardId(standardId);
                boolean b = detailService.update(detail, queryWrapper);
                if (!b) {
                    //这个任务之前没有设置过标准物
                    detail.setId(IdGeneratorUtil.generateId());
                    detail.setTaskId(taskId);
                    detail.setItemId(itemId);
                    detail.setStandardId(standardId);
                    detail.setStandardType(standardType);
                    detailService.save(detail);
                }
                //如果是测量物质的话
                if (MEASUREMENT.getCode().equals(standardType)) {
                    log.info("正在设置测量标准物");
                    Workbook workbook = workbookCacheManager.getTaskWorkbook(taskId);
                    //得到标准物质实体，可以进行修改了
                    String characteristicValue = "";
                    String standardValue = "";
                    CalArtifacts artifacts = calArtifactsService.getById(standardId);
                    if (artifacts == null) {
                        CalStdCats cats = catsService.getById(standardId);
                        characteristicValue = cats.getCharacteristicValue();
                        standardValue = cats.getStandardValue();
                    }
                    if (artifacts != null) {
                        characteristicValue = artifacts.getCharacteristicValue();
                        standardValue = artifacts.getStandardValue();
                    }
                    if (StringUtils.isNotBlank(characteristicValue)) {
                        CellUpdDto cellUpdDto = new CellUpdDto();
                        cellUpdDto.setCellValue(characteristicValue);
                        cellUpdDto.setSheetName(groupName);
                        cellUpdDto.setValueType(CHARACTERISTIC.getDescription());
                        cellUpdDto.setProjectName(itemName);
                        updateWorkbookCells(workbook, cellUpdDto);
                    }
                    if (StringUtils.isNotBlank(standardValue)) {
                        CellUpdDto cellUpdDto = new CellUpdDto();
                        cellUpdDto.setCellValue(standardValue);
                        cellUpdDto.setSheetName(groupName);
                        cellUpdDto.setValueType(STANDARD.getDescription());
                        cellUpdDto.setProjectName(itemName);
                        updateWorkbookCells(workbook, cellUpdDto);
                    }
                    workbookCacheManager.updateTaskWorkbook(taskId, workbook, updateBy);
                    sendSheetUpdate(session, workbook.getSheet(groupName), taskId);
                }
            }
            log.info("检测项标准物设置指令处理完毕 - 数据更新成功, 任务ID: {}, 检测项: {}", taskId, itemName);
        }
        //这里是查找之前的历史数据
        if (HISTORY_WAVEFORM.getCode().equals(type)) {
            log.info("收到查找历史波形图指令");
            WebSocketMessageDto<StdMaterialSelectionDto> messageWrapper =
                    mapper.readValue(payload, new TypeReference<WebSocketMessageDto<StdMaterialSelectionDto>>() {
                    });
            StdMaterialSelectionDto selectionDto = messageWrapper.getData();
            taskId = selectionDto.getTaskId();
            String itemId = selectionDto.getItemId();

            // 从数据库查询历史数据
            List<MultiLineSpectrumData> historyData = spectrumDataService.findByTaskIdAndItemId(taskId, itemId);
            //只给4个历史波长，多了就不给了，避免超长
            if (historyData != null && historyData.size() > 4) {
                historyData = historyData.subList(0, 4);
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("data", historyData);
            sendToActiveSession(null, taskId, jsonObject);
            String s = JSON.toJSONString(historyData);
//            sendToActiveSession(null, taskId, s);
        }
        // 处理用户操作确认
        if (RESUME.getCode().equals(type)) { // 您定义的恢复命令类型
            resumeTask(taskId);
            JSONObject response = new JSONObject();
            response.put("resumed", true);
//            session.sendMessage(new TextMessage(response.toJSONString()));
            sendToActiveSession(null, taskId, response);
            return;
        }
        super.handleTextMessage(session, message);

    }

    /**
     * 根据设备序列号查找正在使用该设备的任务 ID
     *
     * @param deviceSerialNumber 设备序列号
     * @return 任务 ID，如果没有找到则返回 null
     */
    private String findTaskByDevice(String deviceSerialNumber) {
        if (StringUtils.isBlank(deviceSerialNumber)) {
            return null;
        }

        // 遍历所有测量中的任务，查找使用该设备的任务
        for (Map.Entry<String, Boolean> entry : measurementStatusMap.entrySet()) {
            String taskId = entry.getKey();
            Boolean isMeasuring = entry.getValue();

            if (Boolean.TRUE.equals(isMeasuring)) {
                // 从缓存中获取任务绑定的设备
                String boundDevice = taskDeviceBindingMap.get(taskId);
                if (deviceSerialNumber.equals(boundDevice)) {
                    return taskId;
                }

                // 如果缓存中没有，从数据库查询
                TaskManagement task = taskService.getById(taskId);
                if (task != null && deviceSerialNumber.equals(task.getOperationInstrument())) {
                    // 更新缓存
                    taskDeviceBindingMap.put(taskId, deviceSerialNumber);
                    return taskId;
                }
            }
        }
        return null;
    }

    /**
     * 获取任务绑定的设备序列号
     *
     * @param taskId 任务 ID
     * @return 设备序列号，如果没有绑定则返回 null
     */
    private String getTaskDevice(String taskId) {
        String device = taskDeviceBindingMap.get(taskId);
        if (StringUtils.isBlank(device)) {
            TaskManagement task = taskService.getById(taskId);
            if (task != null) {
                device = task.getOperationInstrument();
                taskDeviceBindingMap.put(taskId, device);
            }
        }
        return device;
    }

    private void handleAutoInspection(WebSocketSession session, String taskId, String updateBy) throws BusinessException {
        InspectionRequestDto request = new InspectionRequestDto();
        TaskManagement management = taskService.getById(taskId);
        ExcelTemplate excelTemplate = templateService.getById(management.getUseTemplate());
        LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExcelDataAsset::getTemplateCode, excelTemplate.getTemplateCode());

        List<ExcelDataAsset> list = dataAssetService.list(wrapper);

        // 替代方案：使用安全的类型转换

        list.sort(Comparator.comparing(
                (ExcelDataAsset asset) -> getSafeStringValue(asset, TEST_CATEGORY.getDescription()),
                Comparator.nullsLast(String::compareTo)
        ).thenComparing(
                (ExcelDataAsset asset) -> getSafeStringValue(asset, PROJECT_NAME.getDescription()),
                Comparator.nullsLast(String::compareTo)
        ));
        //反转数组
//        Collections.reverse(list);


//        list.sort(Comparator.comparing(ExcelDataAsset::getDataMap).thenComparing(TaskCheckItemDto.CheckItem::getItemName));


        TaskManagement taskManagement = taskService.getById(taskId);
        //获取序列号
        String string = taskManagement.getOperationInstrument();
        DeviceInfoPO info = deviceInfoService.getInfo(string);
//        R<String> device = deviceManageService.getConnectDevice(updateBy);

        if (!CONNECT_DEVICE_SUCCESS.equals(info.getConnectStatus())) {
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("error", "请检查当前仪器是否连接");
//            sendToActiveSession(null, taskId, jsonObject);
            throw new BusinessException("请检查当前仪器是否连接！！！");
        }
//        if (!StringUtils.equals(device.getData(), string)) {
////            JSONObject jsonObject = new JSONObject();
////            jsonObject.put("error", "设备已切换,请检查当前仪器与任务是否匹配！！！");
////            sendToActiveSession(null, taskId, jsonObject);
//            throw new BusinessException("设备已切换,请检查当前仪器与任务是否匹配！！！");
//        }
        if (info.getIsActivated() == 0) {
            throw new BusinessException("设备未激活！！！");
        }
        if (info.getIsInited() != 1 && info.getIsInited() != 3) {
            throw new BusinessException("请检查当前仪器是否初始化！！！");
        }
        if (info.getIsInited() == 3) {
            throw new BusinessException("设备正在初始化！！！");
        }

        halt.put(taskId, false);
        //得到所有的检测项
        if (CollectionUtils.isNotEmpty(list)) {
            CompletableFuture.runAsync(() -> {
                for (ExcelDataAsset asset : list) {
                    if (halt.get(taskId)) {
                        try {
                            setTaskMeasuring(taskId, false);
                            break;
                        } catch (BusinessException e) {
                            log.error("设置任务测量状态失败，任务ID: {}", taskId, e);
                            // 可选：向前端发送错误信息
                            JSONObject errorMsg = new JSONObject();
                            errorMsg.put("error", "设置任务测量状态失败: " + e.getMessage());
                            sendToActiveSession(session, taskId, errorMsg);
                        }
                        log.info("任务 {} 已被暂停，停止自动检测流程", taskId);
                    }
                    Map<String, Object> dataMap = asset.getDataMap();
                    if (!CollectionUtils.isEmpty(dataMap)) {
                        //检测类别，也就是sheet页的名称
                        String category = MapUtils.getString(dataMap, TEST_CATEGORY.getDescription());
                        //检测项名称
                        String projectName = MapUtils.getString(dataMap, PROJECT_NAME.getDescription());
                        log.info("自动检测，开始检测项>>>>>>>>>>>>> 检测类别：{} , 检测项名称：{}", category, projectName);

                    }


                    request.setItemId(asset.getId());
                    try {

//                        addSessionToTask(taskId, session);
//                        sessionTaskMap.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(session);
                        if (halt.get(taskId)) {
                            log.info("任务已暂停，请检查任务是否暂停");
                            setTaskMeasuring(taskId, false);
                            return;
                        }
                        deviceInfoService.updateDeviceCurrentStatus(management.getOperationInstrument(), DEVICE_STATUS_MEASURE);

                        this.handleSingleItemInspection(session, request, taskId, updateBy, true);

                        //同一个任务，同一时间只有一个检测项在跑
                        Future<Boolean> future = taskFutures.get(taskId);
                        if (future == null) {
                            log.error("检测项: {},检测失败，进行下一个检测项测量>>>>>>>>>", asset.getId());
                        }
//                    Future<?> future = taskFutures.get(taskId);
                        if (future != null) {
                            try {
                                // 设置合理的超时时间，避免无限等待
                                Boolean b = future.get();
                                JSONObject object = new JSONObject();
                                object.put("isCompleted", b);
                                sendToActiveSession(null, taskId, object);
                                if (b) {
                                    log.info("检测项: {},检测成功，进行下一个检测项测量>>>>>>>>>", asset.getId());
                                } else {
                                    log.error("检测项: {},检测失败，进行下一个检测项测量>>>>>>>>>", asset.getId());
                                }
                                if (halt.get(taskId)) {
                                    setTaskMeasuring(taskId, false);
                                    log.info("任务 {} 已被暂停，停止自动检测流程", taskId);
                                    return;
                                }
                            } catch (Exception e) {
                                log.error("任务执行异常: taskId={}", taskId, e.getCause());
                                log.error("错误信息为{}", String.valueOf(e));
                                continue;
                            }
                        } else {
                            log.error("任务执行失败,future为空: taskId={}", taskId);
                        }

                    } catch (Exception e) {
                        //检测项。。。跳过，进行下一项测量
                        log.error("检测项: {},检测失败，进行下一个检测项测量>>>>>>>>>", asset.getId(), e);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("error", e.getMessage());
                        sendToActiveSession(null, taskId, jsonObject);
                    }
                }
                JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("liveEcho", "自动检测结束");

                // 发送到队列
                sendToActiveSession(null, taskId, jsonObject1);
                try {
                    setTaskMeasuring(taskId, false);
                } catch (BusinessException e) {
                    throw new RuntimeException(e);
                }
            });


        }
        setTaskMeasuring(taskId, false);
    }

    // 添加辅助方法
    private String getSafeStringValue(ExcelDataAsset asset, String key) {
        Object dataMapObj = asset.getDataMap();
        if (dataMapObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) dataMapObj;
            return MapUtils.getString(map, key);
        }
        return null;
    }

    @Data
    public static class MultiLineSpectrumData implements Serializable {
        private static final long serialVersionUID = 1L;
        private String taskId;           // 任务ID
        private String itemId;          // 检测项ID
        //key是时间  value是线的数据
        private Map<String, List<List<JSONObject>>> data = new HashMap<>();

        public void setData(List<List<JSONObject>> lines) {
            data.putIfAbsent(DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"), lines);
        }

        public void setData(Map<String, List<List<JSONObject>>> data) {
            this.data = data;
        }
    }


    /**
     * 暂停任务
     */
    public void pauseTask(String taskId) throws BusinessException {
        TaskPauseControl control = pauseControls.get(taskId);
        if (control != null) {
            control.setPaused(true);
            try {
                control.semaphore.acquire();
            } catch (Exception e) {
                throw new BusinessException("暂停任务失败！" + e);
            }

            log.info("任务 {} 已标记为暂停", taskId);
        }
    }

    /**
     * 恢复任务
     */
    public void resumeTask(String taskId) {
        TaskPauseControl control = pauseControls.get(taskId);
        if (control != null) {
            control.setPaused(false);
            // 释放一个信号量，让任务继续执行
            control.semaphore.release();
            log.info("任务 {} 已恢复", taskId);
        }
    }


    private void handleSingleItemInspection(WebSocketSession session, InspectionRequestDto request, String taskId, String updateBy, Boolean isAutoInspection) throws BusinessException {
        //先做暗电流
        //在做校零
        //再做测量
        String itemId = request.getItemId();
        setCurrentMeasureItem(taskId, itemId);

        ExcelDataAsset dataAsset = dataAssetService.getById(itemId);
        Map<String, Object> dataMap = dataAsset.getDataMap();
        //检测类别，也就是sheet页的名称
        String category = MapUtils.getString(dataMap, TEST_CATEGORY.getDescription());
        //检测项名称
        String projectName = MapUtils.getString(dataMap, PROJECT_NAME.getDescription());

        



        //获取工作簿
        Workbook taskWorkbook = workbookCacheManager.getTaskWorkbook(taskId);

        Sheet sheet = taskWorkbook.getSheet(category);
        try {
            setTaskMeasuring(taskId, true);
            halt.put(taskId, false);

            // 【新增】记录任务 - 设备绑定关系
            TaskManagement management = taskService.getById(taskId);
            if (management != null && StringUtils.isNotBlank(management.getOperationInstrument())) {
                taskDeviceBindingMap.put(taskId, management.getOperationInstrument());
                log.info("任务 {} 绑定设备 {}", taskId, management.getOperationInstrument());
            }

            if (isAutoInspection) {
                //自动测试
                log.info("自动测试，开始测试>>>>>>>>>>>>>>>");
            }
            Future<Boolean> submit = taskExecutor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws BusinessException {

                    try {
                        if (halt.get(taskId)) {
                            return false;
                        }
                        TaskPauseControl control = pauseControls.computeIfAbsent(taskId, k -> new TaskPauseControl());
                        setTaskMeasuring(taskId, true);
                        //这个项的执行结果
                        boolean futureResult = false;
                        //正在测量
//                        setTaskMeasuring(taskId, true);
                        //获取检测项id


                        List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "特征值", projectName);
                        List<String> list = new ArrayList<>();

                        if (positions != null && !positions.isEmpty()) {
                            for (ExcelCellLocator.CellPosition position : positions) {
                                if (StringUtils.isNotBlank(position.getValue())) {
                                    list.add(position.getValue());
                                }
                            }
                        }
                        //重复检测次数
                        Integer repeatCount = MapUtils.getInteger(dataMap, REPEAT_COUNT.getDescription());

                        JSONObject object = new JSONObject();
                        JSONObject object1 = new JSONObject();
                        object1.put("itemId", itemId);
                        object1.put("checkCount", repeatCount);
                        object.put("currentItem", object1);
                        sendToActiveSession(null, taskId, object);
                        ArrayList<List<JSONObject>> lists = new ArrayList<>();
                        log.info("开始进行测量");
                        log.info("list:{}", list.size());
                        if (list != null) {
                            log.info("开始进行多特征值测量");


//                    String deviceSerialNumber = management.getOperationInstrument();
                            //更新任务对应的序列号为当前连接的仪器

//                            TaskManagement management = taskService.getById(taskId);
//                            //如果任务没有绑定设备号的时候，绑定第一次遇到的那个设备
//                            if (management != null && StringUtils.isBlank(management.getOperationInstrument())) {
//                                R<String> device = deviceManageService.getConnectDevice(updateBy);
//
////                            String deviceSerialNumber = "30-U1602-01-0022";
//                                String deviceSerialNumber = device.getData();
//
//                                LambdaUpdateWrapper<TaskManagement> updateWrapper = new LambdaUpdateWrapper<>();
//                                updateWrapper.eq(TaskManagement::getId, taskId)  // 根据taskId匹配
//                                        .set(TaskManagement::getOperationInstrument, deviceSerialNumber); // 设置新序列号
//
//                                // 执行更新
//                                taskService.update(updateWrapper);
//                            }
                            TaskManagement taskManagement = taskService.getById(taskId);
                            //获取序列号
                            String string = taskManagement.getOperationInstrument();

//                            R<String> device = deviceManageService.getConnectDevice(updateBy);
                            DeviceInfoPO info = deviceInfoService.getInfo(string);

                            if (!CONNECT_DEVICE_SUCCESS.equals(info.getConnectStatus())) {
                                throw new BusinessException("请检查当前仪器是否连接！！！");
                            }


//                            if (!StringUtils.equals(device.getData(), string)) {
//                                throw new BusinessException("设备已切换,请检查当前仪器与任务是否匹配！！！");
//                            }
                            if (info.getIsActivated() == 0) {
                                throw new BusinessException("设备未激活！！！");
                            }
                            if (info.getIsInited() != 1 && info.getIsInited() != 3) {
                                throw new BusinessException("请检查当前仪器是否初始化！！！");
                            }
                            if (info.getIsInited() == 3) {
                                throw new BusinessException("设备正在初始化！！！");
                            }
                            //获取具体的样品池信息
                            DeviceAttachmentTypeVO vo = deviceInfoService.getAttachmentInfo(string);
                            Integer samplePoolNum = vo.getSamplePoolNum();
//                            Integer samplePoolNum = 1;
                            if (samplePoolNum <= 0) {
                                log.info("样品池未识别,默认单池");
                                samplePoolNum=1;
//                                throw new BusinessException("样品池未识别");
                            }
                            if (samplePoolNum != 1) {
                                //样品池的检测
                                samplePoolDetection(taskId, itemId);

                            }
                            if (samplePoolNum == 1) {
                                log.info("开始进行样品池的检测");
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("notice", "单样品池，后续请根据提示放入相应物质!!!");
                                sendToActiveSession(null, taskId, jsonObject);
                                // 设置等待状态
                                pauseTask(taskId);
                            }

                            log.info("样品池检测完成");
                            log.info("测量次数：" + repeatCount);

//告诉前端走到了哪个检测项

                            //每个特征值测几次
                            for (Integer d = 0; d < repeatCount; d++) {


                                //光谱扫描
                                //测一条线的数据
//                            List<JSONObject> spectrum = sendBatchDataPoints(session, "spectrum", taskId);
                                R<Map<String, List<JSONObject>>> map = null;
                                TaskManagement management1 = taskService.getById(taskId);
                                //序列号
                                String operationInstrument = management1.getOperationInstrument();
                                Boolean needSetSit = deviceInfoService.isNeedSetSit(operationInstrument);
                                //狭缝，光谱带宽
                                Double narrow = MapUtils.getDouble(dataMap, SPECTRAL_BANDWIDTH.getDescription());

                                log.info("是否为可变狭缝：{}", needSetSit);
                                log.info("开始进行扫描");
                                if (!needSetSit) {
                                    if (Double.compare(narrow, 2.0) != 0) {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("warning", "仪器为固定狭缝，此项跳过");
                                        sendToActiveSession(null, taskId, jsonObject);
                                        setTaskMeasuring(taskId, false);
                                        return futureResult;
                                    }
                                }

                                try {
                                    if (!halt.get(taskId)) {
                                        if (d == 0) {
                                            JSONObject json1 = new JSONObject();
                                            json1.put("itemName", category + "--" + projectName);
                                            sendToActiveSession(null, taskId, json1);


                                            JSONObject jsonObject1 = new JSONObject();
                                            jsonObject1.put("itemId", itemId);
                                            sendToActiveSession(null, taskId, jsonObject1);
                                        }
                                        map = sendBatchDataPoints(session, taskId, itemId, dataMap, updateBy, operationInstrument, list, samplePoolNum, d);
                                    }
                                } catch (Exception e) {
                                    return false;
                                }
                                Map<String, List<JSONObject>> data = Objects.requireNonNull(map).getData();
                                List<JSONObject> line = Objects.requireNonNull(data).get("line");
                                if (CollectionUtils.isNotEmpty(line)) {
                                    lists.add(data.get("line"));
                                }
                                log.info("fjaishfdisdf" + lists.size());
                                Workbook workbook = workbookCacheManager.getTaskWorkbook(taskId);
                                //发送sheet图，也就是绘制一幅波形图就要发送一个测量值给前端
                                CellUpdDto cellUpdDto = new CellUpdDto();
                                Random random = new Random();

                                //把list按照特征值进行排序
//                        Collections.sort(list);
                                if (data.get("res") != null) {
                                    if (CollectionUtils.isNotEmpty(list)) {
                                        //修改工作簿，需要把第一个测量值的列填满，然后第二个，第三个
                                        for (int k = 0; k < list.size(); k++) {
                                            if (halt.get(taskId)) {
                                                return false;

                                            }
                                            cellUpdDto.setPosition(d);
                                            //测量值，后期替换这里的测量值，测量值是经过暗电流矫正，校零，测量得出的
//                                int result = random.nextInt(301) + 300; // 生成300-600的随机整数
                                            List<JSONObject> jsonObjects = data.get("res");

                                            if (CollectionUtils.isNotEmpty(jsonObjects)) {
                                                log.info("得到的测量结果为:{}", jsonObjects);
                                                ArrayList<String> strings = new ArrayList<>();
                                                jsonObjects.forEach(jsonObject -> {
                                                    strings.add(jsonObject.getString("peak"));
                                                });
                                                String s = findClosestNumber(strings, list.get(k));
                                                cellUpdDto.setCellValue(s);
                                            }
                                            cellUpdDto.setSheetName(category);
                                            cellUpdDto.setValueType(MEASUREMENT_VALUE.getDescription());
                                            cellUpdDto.setProjectName(projectName);
                                            //设置他的特征值
                                            cellUpdDto.setFeatureValue(list.get(k));
                                            if (CollectionUtils.isNotEmpty(list)) {
                                                cellUpdDto.setIsMultiple(list.size() > 1);
                                            }
                                            log.info("修改工作簿");
                                            //修改工作簿
                                            updateWorkbookCell(workbook, cellUpdDto);
                                            log.info("修改完成");
                                        }
                                    } else {
                                        cellUpdDto.setPosition(d);
                                        //测量值，后期替换这里的测量值，测量值是经过暗电流矫正，校零，测量得出的
//                                int result = random.nextInt(301) + 300; // 生成300-600的随机整数
                                        List<JSONObject> jsonObjects = data.get("res");

                                        if (CollectionUtils.isNotEmpty(jsonObjects)) {
                                            cellUpdDto.setCellValue(jsonObjects.get(0).getString("peak"));
                                        } else {
                                            return false;
                                        }
                                        cellUpdDto.setSheetName(category);
                                        cellUpdDto.setValueType(MEASUREMENT_VALUE.getDescription());
                                        cellUpdDto.setProjectName(projectName);
                                        log.info("修改工作簿");
                                        //修改工作簿
                                        updateWorkbookCell(workbook, cellUpdDto);
                                        log.info("修改完成");
                                    }

                                    try {
                                        log.info("修改缓存中的工作簿，并异步更新数据库");
                                        //修改缓存中的工作簿
                                        workbookCacheManager.updateTaskWorkbook(taskId, workbook, updateBy);
                                        //发送特定的sheet页
                                        log.info("发送{},sheet页数据", category);
                                        sendSheetUpdate(session, workbook.getSheet(category), taskId);
                                        futureResult = true;
                                        saveAndSendJudgeResult(taskId, itemId, projectName, sheet, MEASUREMENT_STATUS_SUCCESS.getCode());
//                                return futureResult;
                                        //结束测量
//                        setTaskMeasuring(taskId, false);
                                    } catch (Exception e) {
                                        futureResult = false;
                                        log.error("发送sheet页数据失败", e);
                                        return futureResult;
                                    }
                                } else {
//                                    saveAndSendJudgeResult(taskId, itemId, projectName, sheet, MEASUREMENT_STATUS_FAILED.getCode());
                                }

                                //如果这一次执行失败，那就退出，不再测量
                                if (!map.getSuccess()) {
                                    log.info("本次测量失败退出测量");
                                    setTaskMeasuring(taskId, false);
                                    break;
                                } else {
                                    if (repeatCount > 1) {
                                        realTimeEcho(taskId, "第 " + (d + 1) + " 次测量完成");
                                    } else {
                                        realTimeEcho(taskId, "测量完成");
                                    }
                                }
                                if (halt.get(taskId)) {
                                    break;
                                }
                            }
                        }
                        log.info("fdgddd");

                        if (CollectionUtils.isNotEmpty(lists)) {
                            log.info("sdfsddfs" + lists.size());


                            // 修改数据存储部分
                            MultiLineSpectrumData data = new MultiLineSpectrumData();
                            data.setTaskId(taskId);
                            data.setItemId(itemId);
                            data.setData(lists);

                            // 将数据转换为数据库实体并保存
                            SpectrumData spectrumData = new SpectrumData();
                            spectrumData.setId(IdGeneratorUtil.generateIdWithPrefix("spec"));
                            spectrumData.setTaskId(taskId);
                            spectrumData.setItemId(itemId);
                            spectrumData.setSpectrumData(JSON.toJSONString(data.getData())); // 序列化数据
                            spectrumData.setMeasurementTime(new Date());
                            spectrumData.setCreateTime(new Date());

                            spectrumDataService.save(spectrumData);
                            setTaskMeasuring(taskId, false);

                            try {
                                setTaskMeasuring(taskId, false);
//                            webSocketSession.sendMessage(new TextMessage(statusMessage.toJSONString()));
                            } catch (Exception e) {
                                log.error("发送测量状态失败", e);
                            }
                        }

                        if (futureResult) {
                            realTimeEcho(taskId, "测量成功");
                        }
                        return futureResult;
                    } catch (Exception e) {
                        if (!isAutoInspection) {
                            log.error("测量任务执行异常", e);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("error", e.getMessage());
                            sendToActiveSession(null, taskId, jsonObject);
                            setTaskMeasuring(taskId, false);
//                            saveAndSendJudgeResult(taskId, itemId, projectName, sheet, MEASUREMENT_STATUS_FAILED.getCode());
                        }
                        if (isAutoInspection) {
                            log.error("测量任务执行异常", e);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("warn", "检测项：[" + projectName + "] 出现问题:" + e.getMessage() + "跳过");
                            sendToActiveSession(null, taskId, jsonObject);
//                            saveAndSendJudgeResult(taskId, itemId, projectName, sheet, MEASUREMENT_STATUS_FAILED.getCode());
                        }
                        return false;
                    } finally {
                        // 确保清理任务相关资源
                        taskFutures.remove(taskId);
                        pauseControls.remove(taskId);
                    }
                }
            });
            // 保存任务引用，key为taskId
            taskFutures.put(taskId, submit);

        } catch (Exception e) {
            log.error("单项测量处理异常", e);

            setTaskMeasuring(taskId, false);
        }
    }

    //保存并发送判定结果
    public void saveAndSendJudgeResult(String taskId, String itemId, String projectName, Sheet sheet, String measured) {
        String value;
        List<ExcelCellLocator.CellPosition> checkResult = excelCellLocator.findCellPositions(sheet, "判定结果", projectName);
        if (CollectionUtils.isNotEmpty(checkResult)) {
            if (CHECK_RESULT_PASS.getDescription().equals(checkResult.get(0).getValue())) {
                value = CHECK_RESULT_PASS.getCode();
            } else if (CHECK_RESULT_FAIL.getDescription().equals(checkResult.get(0).getValue())) {
                value = CHECK_RESULT_FAIL.getCode();
            } else {
                value = CHECK_RESULT_NOT_EVALUATED.getCode();
            }
        } else {
            value = "0";
        }

        // 异步更新任务检测项状态
        CompletableFuture.runAsync(() -> {
            try {
                LambdaQueryWrapper<TaskItemStatus> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(TaskItemStatus::getTaskId, taskId)
                        .eq(TaskItemStatus::getItemId, itemId);
                TaskItemStatus status = taskItemStatusService.getOne(wrapper);

                if (status != null) {
                    TaskItemStatus updateStatus = new TaskItemStatus();
                    updateStatus.setId(status.getId());
                    updateStatus.setMeasured(measured);
                    updateStatus.setCheckResult(value);
                    boolean b = taskItemStatusService.updateById(updateStatus);
                    if (!b) {
                        log.error("更新任务检测项状态失败，任务ID: {}, 检测项ID: {}", taskId, itemId);
                    }
                } else {
                    log.warn("未找到任务检测项状态记录，任务ID: {}, 检测项ID: {}", taskId, itemId);
                }
            } catch (Exception e) {
                log.error("更新任务检测项状态异常，任务ID: {}, 检测项ID: {}", taskId, itemId, e);
            }
        }, taskExecutor);


        // 发送测量状态消息给前端
        JSONObject measureStateMessage = new JSONObject();
        JSONObject measureStateData = new JSONObject();

        measureStateData.put("itemId", itemId);
        measureStateData.put("measured", measured);
        measureStateData.put("checkResult", value);


        measureStateMessage.put("measureState", measureStateData);

        sendToActiveSession(null, taskId, measureStateMessage);
    }


    //找出最接近的值
    public static String findClosestNumber(List<String> numbers, String targetStr) {
        if (numbers == null || numbers.isEmpty() || targetStr == null) {
            return null;
        }

        try {
            double target = Double.parseDouble(targetStr);
            String closest = null;
            double minDifference = Double.MAX_VALUE;

            for (String numberStr : numbers) {
                try {
                    double value = Double.parseDouble(numberStr);
                    double difference = Math.abs(value - target);

                    if (difference < minDifference) {
                        minDifference = difference;
                        closest = numberStr;
                    }
                } catch (NumberFormatException e) {
                    // 跳过无效数字字符串
                    continue;
                }
            }

            return closest;
        } catch (NumberFormatException e) {
            // 目标字符串无法解析为数字
            return null;
        }
    }

    private void sendBlackBlockNotFoundError(String taskId, String blackBlockDesc) throws BusinessException {
        log.info("样品池中未找到：{}", blackBlockDesc);
        throw new BusinessException("样品池中未找到：" + blackBlockDesc);
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("error", "样品池中未找到：" + blackBlockDesc);
//        sendToActiveSession(null, taskId, jsonObject);
    }

    /**
     * 样品池检测
     */
    private void samplePoolDetection(String taskId, String itemId) throws Exception {

        //关于样品池
        Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(), taskId, String.class, Object.class);
        log.info("1111111111111111>>>>>>>>>>>>");
        List<SetSamplePoolDto> dtoList = new ArrayList<>();
        if (cachedObject instanceof List) {
            // 进行类型检查和转换
            dtoList = (List<SetSamplePoolDto>) cachedObject;
        }
        Map<String, String> standardMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(dtoList)) {
            ExcelDataAsset asset = dataAssetService.getById(itemId);
            Map<String, Object> dataMap = asset.getDataMap();
            String s = MapUtils.getString(dataMap, SPECTRAL_SCAN_TYPE.getDescription());
            //先看一下是不是光谱扫描
            if (YES.getDescription().equals(s)) {
                //光度模式
                String luminosityMode = MapUtils.getString(dataMap, LUMINOSITY_MODE.getDescription());
                //abs和T%就进行基线，否则就不基线
                if (abs.getName().equalsIgnoreCase(luminosityMode) || tPercent.getName().equalsIgnoreCase(luminosityMode)) {
                    for (SetSamplePoolDto dto : dtoList) {
                        TaskCheckItemDto.Standard standard = dto.getStandard();
                        if (org.apache.commons.lang3.StringUtils.contains(standard.getStandardName(), AIR.getDescription())) {
                            materialManager.addBaselineMaterial(taskId, standard.getStandardId());
                            materialManager.setActionPosition(taskId, StandardMaterialManager.MaterialType.BASELINE, dto.getPoolNum());
                            break;
                        } else {
                            String id = standard.getStandardId();
                            if (StringUtils.isNotBlank(id)) {
                                CalArtifacts calArtifacts = calArtifactsService.getById(id);
                                if (calArtifacts != null) {
                                    String categoryId = calArtifacts.getCategoryId();
                                    CalStdCats cats = calStdCatsService.getById(categoryId);
                                    if (org.apache.commons.lang3.StringUtils.contains(cats.getName(), AIR.getDescription())) {
                                        materialManager.addMeasureMaterial(taskId, id);
                                        materialManager.setActionPosition(taskId, StandardMaterialManager.MaterialType.MEASURE, dto.getPoolNum());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.BASELINE) == null) {
                        throw new BusinessException("样品池中，未找到基线所需的空气");
                    }
                }
            }
//                        List<TaskCheckItemDto.Standard> standardList = dtoList.stream()
//                                .map(SetSamplePoolDto::getStandard)
//                                .collect(Collectors.toList());
            standardMap = dtoList.stream()
                    .map(SetSamplePoolDto::getStandard)  // 获取 Standard 对象
                    .filter(Objects::nonNull)            // 过滤 null 值
                    .collect(Collectors.toMap(
                            TaskCheckItemDto.Standard::getStandardId,   // key: standardId
                            TaskCheckItemDto.Standard::getStandardName  // value: standardName
                    ));
        } else {
            throw new BusinessException("样品池为空");
        }

        log.info("222222222222222222>>>>>>>>>>>>");

        List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
        if (detailList != null) {
            //获取当前检测项的所有标准物
            List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(collect)) {
                List<TaskExecutionDetail> measurementlist = collect.stream().filter(detail -> detail.getStandardType().equals(MEASUREMENT.getCode())).collect(Collectors.toList());
                List<TaskExecutionDetail> zerolist = collect.stream().filter(detail -> detail.getStandardType().equals(ZERO.getCode())).collect(Collectors.toList());

                //看一下需不需要暗电流矫正
                ExcelDataAsset asset = dataAssetService.getById(itemId);
                Map<String, Object> dataMap = asset.getDataMap();
                String s = MapUtils.getString(dataMap, LUMINOSITY_MODE.getDescription());
                // 优化后的代码
                WaveLengthEnum anEnum = WaveLengthEnum.getByName(s);
                if ((anEnum == abs || anEnum == tPercent)) {
                    String blackBlockDesc = BLACK_BLOCK.getDescription();

                    if (CollectionUtils.isNotEmpty(dtoList)) {
                        for (int i = 0; i < dtoList.size(); i++) {


                            String id = dtoList.get(i).getStandard().getStandardId();
                            if (id == null) return;

                            // 检查 CalStdCats
                            CalStdCats cats = calStdCatsService.getById(id);
                            if (cats != null && !cats.getName().contains(blackBlockDesc)) {
                                //如果是最后一次循环
                                if (i == dtoList.size() - 1) {
                                    sendBlackBlockNotFoundError(taskId, blackBlockDesc);
                                }
                            }

                            if (cats != null && cats.getName().contains(blackBlockDesc)) {
                                materialManager.addBlackBlockMaterial(taskId, id);
                                materialManager.setActionPosition(taskId, StandardMaterialManager.MaterialType.BLACK_BLOCK, dtoList.get(i).getPoolNum());
                                break;
                            }
                            // 检查 CalArtifacts
                            if (cats == null) {
                                CalArtifacts artifacts = calArtifactsService.getById(id);
                                if (artifacts != null) {
                                    String categoryId = artifacts.getCategoryId();
                                    CalStdCats categoryCats = catsService.getById(categoryId);
                                    if (categoryCats != null && !categoryCats.getName().contains(blackBlockDesc)) {
                                        if (i == dtoList.size() - 1) {
                                            sendBlackBlockNotFoundError(taskId, blackBlockDesc);
                                        }
                                    }
                                    if (categoryCats != null && categoryCats.getName().contains(blackBlockDesc)) {
                                        materialManager.addBlackBlockMaterial(taskId, id);
                                        materialManager.setActionPosition(taskId, StandardMaterialManager.MaterialType.BLACK_BLOCK, dtoList.get(i).getPoolNum());
                                        break;
                                    }
                                } else {
                                    log.warn("找不到ID为 {} 的标准物", id);
                                }
                            }
                        }

                    } else {
                        sendBlackBlockNotFoundError(taskId, blackBlockDesc);
                    }

                }

                log.info("333333333333333333>>>>>>>>>>>>");

                //
                if (!zerolist.isEmpty()) {
                    //校零的标准物
                    TaskExecutionDetail zero = zerolist.get(0);
                    //测量的标准物
                    if (!standardMap.containsKey(zero.getStandardId())) {
                        String standardName = "无标准物";
                        CalStdCats cats = catsService.getById(zero.getStandardId());
                        if (cats != null) {
                            standardName = cats.getName();
                        }
                        if (cats == null) {
                            CalArtifacts artifacts = calArtifactsService.getById(zero.getStandardId());
                            standardName = artifacts.getName();
                        }
                        log.info("样品池中未找到：" + standardName);
//                        JSONObject jsonObject = new JSONObject();
//                        jsonObject.put("error", "样品池中未找到：" + standardName);
//                        sendToActiveSession(null, taskId, jsonObject);
                        throw new BusinessException("样品池中未找到：" + standardName);
                    } else {
                        materialManager.addZeroMaterial(taskId, zero.getStandardId());
                        for (SetSamplePoolDto sample : dtoList) {
                            if (StringUtils.equals(sample.getStandard().getStandardId(), zero.getStandardId())) {
                                materialManager.setActionPosition(taskId, StandardMaterialManager.MaterialType.ZERO, sample.getPoolNum());
                                break;
                            }
                        }
                    }
                }
                log.info("4444444444444444444>>>>>>>>>>>>");

                if (!measurementlist.isEmpty()) {
                    //测量的标准物
                    TaskExecutionDetail measurement = measurementlist.get(0);
                    if (!standardMap.containsKey(measurement.getStandardId())) {
                        String standardName = "无标准物";
                        CalStdCats cats = catsService.getById(measurement.getStandardId());
                        if (cats != null) {
                            standardName = cats.getName();
                        }
                        if (cats == null) {
                            CalArtifacts artifacts = calArtifactsService.getById(measurement.getStandardId());
                            standardName = artifacts.getName();
                        }
                        log.info("样品池中未找到：" + standardName);
//                        JSONObject jsonObject = new JSONObject();
//                        jsonObject.put("error", "样品池中未找到：" + standardName);
//                        sendToActiveSession(null, taskId, jsonObject);
                        throw new BusinessException("样品池中未找到：" + standardName);
                    } else {
                        materialManager.addMeasureMaterial(taskId, measurement.getStandardId());
                        for (SetSamplePoolDto sample : dtoList) {
                            if (StringUtils.equals(sample.getStandard().getStandardId(), measurement.getStandardId())) {
                                materialManager.setActionPosition(taskId, StandardMaterialManager.MaterialType.MEASURE, sample.getPoolNum());
                                break;
                            }
                        }
                    }
                }
                log.info("555555555555555555>>>>>>>>>>>>");


            }
        }
    }

    public List<JSONObject> analysisProcess(List<Double> xList, List<BigDecimal> yList, List<BigDecimal> featurePoints, String methodType) throws BusinessException {
        ArrayList<JSONObject> res = new ArrayList<>();
        AalysisMethodEnum anEnum = AalysisMethodEnum.getByMsg(methodType);
        if (anEnum != null) {
            log.info("分析方法是：{}", anEnum.getMsg());
            switch (anEnum) {
                case FIND_PEAK:
                    // 寻峰找到点
                    SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
                    Map<String, Object> body1 = new HashMap<>();
                    body1.put("x_data", xList);
                    body1.put("y_data", yList);
                    BigDecimal height = new BigDecimal(confVO.getPeakValleyDiff()).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                    body1.put("height", height);
                    body1.put("distance", confVO.getPeakInterval());
                    log.info("寻峰入参：{}", body1);
                    Map<String, Object> resultMap;
                    try {
                        resultMap = callPythonService(
                                pythonPathConstant.getPeakSearching(),
                                body1
                        );
                        log.info("寻峰结果：{}", resultMap);
                        JSONObject jsonObject = new JSONObject();

                        // 【修复】Python 返回的是 List<Double>，需要转换为 List<BigDecimal>
                        List<Double> xPeakDouble = (List<Double>) resultMap.get("peak_x");
                        List<String> yPeak = (List<String>) resultMap.get("peak_y");

                        if (xPeakDouble != null) {
                            for (Double doubleValue : xPeakDouble) {
                                JSONObject peakObject = new JSONObject();
                                // 将 Double 转换为 BigDecimal
                                BigDecimal peakValue = new BigDecimal(doubleValue);
                                peakObject.put("peak", peakValue);
                                res.add(peakObject);
                            }
                        }

                    } catch (Exception e) {
                        log.error("Python服务调用异常：", e);
                        throw new BusinessException("Python服务调用异常");
                    }
                    break;
                case FIND_HALF_PEAK_WIDTH:
                    // 寻峰找到点
                    SystemAlgorithmConfVO confVO1 = systemAlgorithmConfService.queryAlgorithmConf();
                    Map<String, Object> body2 = new HashMap<>();
                    body2.put("x_data", xList);
                    body2.put("y_data", yList);
                    BigDecimal height1 = new BigDecimal(confVO1.getPeakValleyDiff()).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                    body2.put("height", height1);
                    body2.put("distance", confVO1.getPeakInterval());
                    log.info("寻峰入参：{}", body2);
                    Map<String, Object> resultMap1;
                    try {
                        resultMap1 = callPythonService(
                                pythonPathConstant.getPeakSearching(),
                                body2
                        );
                        log.info("寻峰结果：{}", resultMap1);
                        JSONObject jsonObject = new JSONObject();

                        // 【修复】Python 返回的是 List<Double>，需要转换为 List<BigDecimal>
                        List<Double> xPeakDouble = (List<Double>) resultMap1.get("peak_x");

                        // 安全地处理类型转换
                        List<BigDecimal> xPeak = new ArrayList<>();
                        if (xPeakDouble != null) {
                            for (Double doubleValue : xPeakDouble) {
                                xPeak.add(new BigDecimal(doubleValue));
                            }
                        }
                        // 安全地处理类型转换
                        List<BigDecimal> yPeak = null;
                        Object peakYObj = resultMap1.get("peak_y");
                        if (peakYObj instanceof List) {
                            List<?> rawList = (List<?>) peakYObj;
                            yPeak = new ArrayList<>();
                            for (Object obj : rawList) {
                                if (obj instanceof BigDecimal) {
                                    yPeak.add((BigDecimal) obj);
                                } else if (obj instanceof String) {
                                    try {
                                        yPeak.add(new BigDecimal((String) obj));
                                    } catch (NumberFormatException e) {
                                        log.warn("无法解析字符串为BigDecimal: {}", obj);
                                        // 可以选择跳过或使用默认值
                                        continue;
                                    }
                                } else if (obj instanceof Number) {
                                    yPeak.add(new BigDecimal(((Number) obj).doubleValue()));
                                } else {
                                    log.warn("未知的数据类型: {}", obj.getClass().getName());
                                }
                            }
                        } else {
                            log.error("peak_y 不是列表类型: {}", peakYObj != null ? peakYObj.getClass().getName() : "null");
                        }
                        List<BigDecimal> bigDecimals = doubleListToBigDecimalList(xList);
                        List<BigDecimal> halfPeakWidths = calculateHalfPeakWidths(bigDecimals, yList, xPeak, yPeak);

                        if (CollectionUtils.isNotEmpty(halfPeakWidths)) {
                            for (BigDecimal width : halfPeakWidths) {
                                JSONObject jsonObject1 = new JSONObject();
                                jsonObject1.put("peak", width);
                                res.add(jsonObject1);
                            }
                        } else {
                            // 如果无法计算半峰宽，返回0或空值
                            for (int j = 0; j < xPeak.size(); j++) {
                                JSONObject jsonObject2 = new JSONObject();
                                jsonObject2.put("peak", BigDecimal.ZERO);
                                res.add(jsonObject2);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Python服务调用异常：", e);
                        throw new BusinessException("Python服务调用异常");
                    }
                    break;
                case FIND_MAX_OFFSET:
                    List<BigDecimal> bigDecimals = doubleListToBigDecimalList(xList);
                    List<BigDecimal> calcResult = getMaxDistanceUVTestTool.calcResult(bigDecimals, yList, featurePoints);
                    if (CollectionUtils.isNotEmpty(calcResult)) {
                        for (BigDecimal aBigDecimal : calcResult) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("peak", aBigDecimal);
                            log.info("最大偏移量结果111：{}", jsonObject);
                            res.add(jsonObject);
                        }
                    }
                    break;
                case FIND_MAX_MIN_DIFFERENCE:
                    List<BigDecimal> bigDecimalList = doubleListToBigDecimalList(xList);

                    List<BigDecimal> doubles = findMaxOffsetUVTestTool.calcResult(bigDecimalList, yList, featurePoints);
                    if (CollectionUtils.isNotEmpty(doubles)) {
                        for (BigDecimal aBigDecimal : doubles) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("peak", aBigDecimal);
                            res.add(jsonObject);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        if (CollectionUtils.isNotEmpty(res)) {
            for (JSONObject re : res) {
                BigDecimal peak = (BigDecimal) re.get("peak");
                if (peak != null) {
                    // 使用BigDecimal进行科学修约到两位小数
//                    peak = peak.setScale(2, RoundingMode.HALF_UP);
                    re.put("peak", peak.toString());
                }
            }
        }

        return res;
    }


    //写一个方法用来把double的list入参转化为bigdecimal的list出参
    private List<BigDecimal> doubleListToBigDecimalList(List<Double> doubleList) {
        List<BigDecimal> bigDecimalList = new ArrayList<>();
        for (Double d : doubleList) {
            bigDecimalList.add(new BigDecimal(d));
        }
        return bigDecimalList;
    }


    private List<BigDecimal> calculateHalfPeakWidths(List<BigDecimal> xData, List<BigDecimal> yData,
                                                     List<BigDecimal> peakX, List<?> peakYRaw) {
        // 将 peakYRaw 转换为 BigDecimal 列表
        List<BigDecimal> peakY = new ArrayList<>();
        for (Object obj : peakYRaw) {
            if (obj instanceof Number) {
                peakY.add(new BigDecimal(((Number) obj).doubleValue()));
            } else if (obj instanceof String) {
                try {
                    peakY.add(new BigDecimal((String) obj));
                } catch (NumberFormatException e) {
                    log.warn("无法解析字符串为 BigDecimal: {}", obj);
                    peakY.add(BigDecimal.ZERO); // 默认值
                }
            } else {
                log.warn("未知的数据类型: {}", obj.getClass().getName());
                peakY.add(BigDecimal.ZERO); // 默认值
            }
        }

        List<BigDecimal> fwhmList = new ArrayList<>();

        for (int i = 0; i < peakX.size(); i++) {
            Object xObj = peakX.get(i);
            Object yObj = peakY.get(i);

            BigDecimal peakXVal = convertToBigDecimal(xObj, "peakX");
            BigDecimal peakYVal = convertToBigDecimal(yObj, "peakY");


            // 计算半峰高
            BigDecimal halfHeight = peakYVal.divide(BigDecimal.valueOf(2.0), 10, RoundingMode.HALF_UP);

            // 找到峰值点在数据中的索引
            int peakIndex = findNearestIndex(xData, peakXVal);

            if (peakIndex != -1) {
                // 向左寻找半峰高点
                int leftIndex = findLeftHalfMaximum(xData, yData, peakIndex, halfHeight);
                // 向右寻找半峰高点
                int rightIndex = findRightHalfMaximum(xData, yData, peakIndex, halfHeight);

                if (leftIndex != -1 && rightIndex != -1) {
                    // 使用绝对值确保结果为正
                    BigDecimal fwhm = xData.get(rightIndex).subtract(xData.get(leftIndex)).abs();
                    fwhmList.add(fwhm);
                } else {
                    fwhmList.add(BigDecimal.ZERO);
                }
            } else {
                fwhmList.add(BigDecimal.ZERO);
            }
        }

        return fwhmList;
    }

    private BigDecimal convertToBigDecimal(Object obj, String fieldName) {
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        } else if (obj instanceof Number) {
            return new BigDecimal(obj.toString()).setScale(10, RoundingMode.HALF_UP);
        } else {
            log.error("{} 中遇到非数值类型: {}", fieldName, obj == null ? "null" : obj.getClass());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 找到最接近目标值的索引
     */
    private int findNearestIndex(List<BigDecimal> xData, BigDecimal target) {
        int nearestIndex = -1;
        BigDecimal minDiff = new BigDecimal(Double.MAX_VALUE); // 使用 BigDecimal 替代 double

        for (int i = 0; i < xData.size(); i++) {
            // 使用 BigDecimal 的 subtract 和 abs 方法进行计算
            BigDecimal diff = xData.get(i).subtract(target).abs();

            // 比较 BigDecimal 类型的差值
            if (diff.compareTo(minDiff) < 0) {
                minDiff = diff;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    }


    /**
     * 向左寻找半峰高点
     */
    private int findLeftHalfMaximum(List<BigDecimal> xData, List<BigDecimal> yData, int peakIndex, BigDecimal halfHeight) {
        // 从峰值点向左搜索
        for (int i = peakIndex; i >= 0; i--) {
            if (yData.get(i).compareTo(halfHeight) <= 0) {
                // 找到最接近半峰高的点
                if (i + 1 < yData.size() && yData.get(i + 1).compareTo(halfHeight) >= 0) {
                    // 线性插值找到精确的半峰高点
                    return interpolateHalfMaximumIndex(xData, yData, i, i + 1, halfHeight);
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * 向右寻找半峰高点
     */
    private int findRightHalfMaximum(List<BigDecimal> xData, List<BigDecimal> yData, int peakIndex, BigDecimal halfHeight) {
        // 从峰值点向右搜索
        for (int i = peakIndex; i < yData.size(); i++) {
            if (yData.get(i).compareTo(halfHeight) <= 0) {
                // 找到最接近半峰高的点
                if (i - 1 >= 0 && yData.get(i - 1).compareTo(halfHeight) >= 0) {
                    // 线性插值找到精确的半峰高点
                    return interpolateHalfMaximumIndex(xData, yData, i - 1, i, halfHeight);
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * 线性插值找到精确的半峰高点索引
     */
    private int interpolateHalfMaximumIndex(List<BigDecimal> xData, List<BigDecimal> yData,
                                            int index1, int index2, BigDecimal targetY) {
        // 使用线性插值计算更精确的半峰高点
        BigDecimal x1 = xData.get(index1);
        BigDecimal y1 = yData.get(index1);
        BigDecimal x2 = xData.get(index2);
        BigDecimal y2 = yData.get(index2);

        BigDecimal yDiff = y2.subtract(y1);
        if (yDiff.abs().compareTo(new BigDecimal("1e-10")) < 0) {
            // 避免除零错误
            return y1.compareTo(targetY) >= 0 ? index1 : index2;
        }

        // 线性插值公式：x = x1 + (targetY - y1) * (x2 - x1) / (y2 - y1)
        BigDecimal interpolatedX = x1.add(targetY.subtract(y1).multiply(x2.subtract(x1)).divide(yDiff, 10, RoundingMode.HALF_UP));

        // 找到最接近插值点的索引
        int nearestIndex = index1;
        BigDecimal minDiff = xData.get(index1).subtract(interpolatedX).abs();

        BigDecimal diffAtIndex2 = xData.get(index2).subtract(interpolatedX).abs();
        if (diffAtIndex2.compareTo(minDiff) < 0) {
            nearestIndex = index2;
        }

        return nearestIndex;
    }

    private List<Double> convertToDoubleList(List<?> rawList) {
        if (rawList == null) return new ArrayList<>();

        List<Double> result = new ArrayList<>();
        for (Object obj : rawList) {
            if (obj instanceof Double) {
                result.add((Double) obj);
            } else if (obj instanceof String) {
                try {
                    result.add(Double.parseDouble((String) obj));
                } catch (NumberFormatException e) {
                    log.warn("无法转换为double: {}", obj);
                    result.add(0.0); // 或者跳过该元素
                }
            } else if (obj instanceof Number) {
                result.add(((Number) obj).doubleValue());
            } else {
                log.warn("未知的数据类型: {}", obj);
                result.add(0.0);
            }
        }
        return result;
    }

    /**
     * 光谱扫描
     *
     * @param
     * @param taskId
     * @param dataMap
     * @param deviceSerialNumber
     * @param stringList
     * @param itemId
     * @param d
     * @return
     * @throws Exception
     */

    //光谱扫描
    public R<Map<String, List<JSONObject>>> spectrumScan(String taskId, Map<String, Object> dataMap, String deviceSerialNumber, List<String> stringList, boolean isMultiProductPool, String itemId, Integer d) throws BusinessException {

        //图的原始list
        ArrayList<JSONObject> list = new ArrayList<>();
        //数值的list
        List<JSONObject> res = new ArrayList<>();
        //x轴list
        ArrayList<Double> xList = new ArrayList<>();
        //y轴list liveEcho
        ArrayList<BigDecimal> yList = new ArrayList<>();
        HashMap<String, List<JSONObject>> map = new HashMap<>();

        //光度模式
        String luminosityMode = MapUtils.getString(dataMap, LUMINOSITY_MODE.getDescription());
        //分析方法
        String methodType = MapUtils.getString(dataMap, METHOD_TYPE.getDescription());
        //获取扫描速度
        String scanSpeed = MapUtils.getString(dataMap, SCAN_SPEED.getDescription());
        //获取起始波长
        Double startWavelength = MapUtils.getDouble(dataMap, START_WAVELENGTH.getDescription());
        //获取终止波长
        Double endWavelength = MapUtils.getDouble(dataMap, END_WAVELENGTH.getDescription());
        //扫描间隔
        Double scanInterval = MapUtils.getDouble(dataMap, SCAN_INTERVAL.getDescription());
        //增益
        Integer gain = MapUtils.getInteger(dataMap, GAIN.getDescription());
        //狭缝，光谱带宽
        Double narrow = MapUtils.getDouble(dataMap, SPECTRAL_BANDWIDTH.getDescription());
        //起始和终止分别多一个间隔
        startWavelength = startWavelength + scanInterval;
        endWavelength = endWavelength - scanInterval;
        //灯状态，灯位置
        Integer lampStatus = null;
        try {
            lampStatus = Integer.valueOf(Objects.requireNonNull(EnergyLampEnums.getIdentityByName(MapUtils.getString(dataMap, LAMP_STATUS.getDescription()))));
        } catch (Exception e) {
            log.error("无效的灯状态");
        }
        boolean b = false;
        WaveLengthEnum anEnum = WaveLengthEnum.getByName(luminosityMode);
        //如果是第一次进来的话
        if (d == 0) {
            if ((anEnum == abs || anEnum == tPercent)) {
                if (org.apache.commons.lang3.StringUtils.equalsIgnoreCase(String.valueOf(taskAndVersionMap.get(taskId)), "T800")) {

                    DevOperationInputParam devType = new DevOperationInputParam();
                    devType.setDeviceSerialNumber(deviceSerialNumber);
                    SetParam devSetParam = new SetParam();
                    //设置起始波长
                    devSetParam.setStartWl(startWavelength);
                    //设置终止波长
                    devSetParam.setEndWl(endWavelength);
                    //设置扫描间隔
                    devSetParam.setScanWlInterval(1.0);
                    devType.setParam(devSetParam);
                    devType.setOperationType(OperationTypeEnums.SetParam.getType());
                    R<OperationResultEntity> opera =
                            devOperationUtilService.operation(devType);
                }
//        log.info("开始暗电流矫正>>>>>>>>>>>>>>>>>>>>");
                //切换样品池到黑挡块
                //看看是否为多样品池
                if (isMultiProductPool) {
                    if (halt.get(taskId)) {
                        log.info("任务被暂停1");
                        map.put("line", list);
                        return R.fail(map);
                    }
                    this.realTimeEcho(taskId, "光谱-切换样品池到黑挡块");

                    log.info("切换样品池到黑挡块-光谱>>>>>>>>>>>>>>>>>>>>");
                    DevOperationInputParam operationInputParamDark = new DevOperationInputParam();
                    operationInputParamDark.setDeviceSerialNumber(deviceSerialNumber);
                    operationInputParamDark.setOperationType(OperationTypeEnums.SetParam.getType());
                    SetParam sampleParamDark = new SetParam();
                    Integer positionDark = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.BLACK_BLOCK);

                    //设置样品池位置
                    sampleParamDark.setCellPos(positionDark);
                    operationInputParamDark.setParam(sampleParamDark);

                    R<OperationResultEntity> operationDark1 = devOperationUtilService.operation(operationInputParamDark);
                    if (operationDark1.getSuccess()) {
                        log.info("切换样品池到黑挡块成功-光谱>>>>>>>>>>>>>>>>");
                        this.realTimeEcho(taskId, "光谱-切换样品池到黑挡块成功");
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("currentPoolNum", positionDark);
                        sendToActiveSession(null, taskId, jsonObject1);
                    }
                    if (!operationDark1.getSuccess()) {
                        setTaskMeasuring(taskId, false);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("error", operationDark1.getMessage());
                        sendToActiveSession(null, taskId, jsonObject);
                        return R.fail(map);
//                        throw new BusinessException(operationDark1.getMessage());
                    }
                } else {
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("notice","请放入黑挡块");
//                sendToActiveSession(null,taskId,jsonObject);
                    if (halt.get(taskId)) {
                        map.put("line", list);
                        return R.fail(map);
                    }
                    promptUserToPlaceMaterial(taskId, "黑挡块");
                    if (halt.get(taskId)) {
                        map.put("line", list);
                        return R.fail(map);
                    }
                }
                this.realTimeEcho(taskId, "光谱-开始暗电流矫正");
                if (halt.get(taskId)) {
                    map.put("line", list);
                    return R.fail(map);
                }
                log.info("开始暗电流矫正-光谱>>>>>>>>>>>>>>>>>>>>");
                DevOperationInputParam darkParam = new DevOperationInputParam();
                SetParam setParam1 = new SetParam();
                darkParam.setDeviceSerialNumber(deviceSerialNumber);
//        darkParam.setParam(setParam1);
                darkParam.setCalibrationType(DeviceCalibrationInfoEnum.Dark.getType());
                darkParam.setOperationType(OperationTypeEnums.Calibration.getType());
//        darkParam.setUpdateBy();
                R<OperationResultEntity> operationDark =
                        devOperationUtilService.operation(darkParam);
                b = operationDark.getSuccess();
                if (b) {
                    log.info("暗电流矫正结果-光谱：{}", operationDark);
                    this.realTimeEcho(taskId, "光谱-暗电流矫正结束");
                } else {
//                this.realTimeEcho(taskId, "光谱-"+operationDark.getMessage());
                    setTaskMeasuring(taskId, false);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", operationDark.getMessage());
                    sendToActiveSession(null, taskId, jsonObject);
                    return R.fail(map);
//                    throw new BusinessException(operationDark.getMessage());
                }

            }

            //构建测量前置设置参数入参
            DevOperationInputParam inputParam = new DevOperationInputParam();
            inputParam.setDeviceSerialNumber(deviceSerialNumber);
            SetParam setParam = new SetParam();
            //先搞一波假数据，后续增加正式数据
            String code = WaveLengthEnum.getCode(luminosityMode);
            //设置光度模式
            setParam.setPhotoMode(Integer.valueOf(Objects.requireNonNull(code)));
            //设置扫描速度
            String speedCode = ScanSpeedEnum.getCodeByName(scanSpeed);
            String s = taskAndVersionMap.get(taskId);
            Map<String, Object> devType = new HashMap<>();
            devType.put("dev_type", s);
            List<DeviceTypeInfoPO> pos = deviceTypeInfoDAO.selectByMap(devType);
            if (CollectionUtils.isNotEmpty(pos)){
                DeviceTypeInfoPO deviceTypeInfoPO = pos.get(0);
                com.alibaba.fastjson.JSONObject speedJson = deviceTypeInfoPO.getScanSpeed();
                if (speedJson != null && !speedJson.isEmpty()) {
                    // 遍历 JSON 对象，找到值为 "medium" 的 key
                    for (String key : speedJson.keySet()) {
                        String speedValue = speedJson.getString(key);
                        if (speedCode.equals(speedValue)) {
                            // 找到了 medium 对应的 key（如 "1"）
                            setParam.setScanSpeed(Integer.parseInt(key));

                            log.info("设备类型 {} 的扫描速度对应的值为：{}", devType, setParam.getScanSpeed());
                            break;
                        }
                    }
                }
            }
            //设置起始波长
            setParam.setStartWl(startWavelength);
            //换灯波长
//        setParam.setChangeLampWl(361.0);
            //设置终止波长
            setParam.setEndWl(endWavelength);
            //设置扫描间隔
            setParam.setScanWlInterval(scanInterval);
            //增益
            setParam.setGain(gain);
            //狭缝  光谱带宽
            setParam.setSlit1(narrow);
            //灯状态 灯位置
            setParam.setLampPos(lampStatus);
            //这里放起始波长
            setParam.setSetWl(startWavelength);

//                    inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
            try {
                inputParam.setParam(setParam);
                inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
                log.info("入参：》》》》》》》" + inputParam);
                if (halt.get(taskId)) {
                    log.info("任务被暂停3");
                    map.put("line", list);
                    return R.fail(map);
                }
                this.realTimeEcho(taskId, "光谱-开始设置入参");
                R<OperationResultEntity> opera =
                        devOperationUtilService.operation(inputParam);

//            resultParam = deviceController.setParam(inputParam);
                b = opera.getSuccess();
                if (!b) {
                    log.warn("设备测量失败-光谱: {}, 错误: {}", deviceSerialNumber, opera.getMessage());
                    setTaskMeasuring(taskId, false);
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("error", "设置参数失败-光谱" + opera.getMessage());
//                sendToActiveSession(null, taskId, jsonObject);
//                    throw new BusinessException(opera.getMessage());
                    return R.fail(map);
                } else {
                    this.realTimeEcho(taskId, "光谱-设置入参成功");
                }
            } catch (Exception e) {
                log.error("设置参数失败-光谱：{}", e.getMessage());
//                throw new BusinessException("设置参数失败-光谱" + e.getMessage());
                return R.fail(map);
            }


            //光度模式是abs,T%就基线,Es,Er就不用基线
            if (setParam.getPhotoMode() == 1 || setParam.getPhotoMode() == 2) {
                //构建基线入参
                if (b) {
                    String msg = "";
                    if (isMultiProductPool) {
                        log.info("切换样品池到基线位置-光谱>>>>>>>>>>>>>>>>>>>>");
                        DevOperationInputParam operationBaseline = new DevOperationInputParam();
                        operationBaseline.setDeviceSerialNumber(deviceSerialNumber);
                        operationBaseline.setOperationType(OperationTypeEnums.SetParam.getType());
                        SetParam sampleParamDark = new SetParam();
                        Integer positionDark = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.BASELINE);
                        //设置样品池位置
                        sampleParamDark.setCellPos(positionDark);
                        operationBaseline.setParam(sampleParamDark);
                        if (halt.get(taskId)) {
                            map.put("line", list);
                            return R.fail(map);
                        }
                        R<OperationResultEntity> operationDark1 = devOperationUtilService.operation(operationBaseline);
                        b = operationDark1.getSuccess();
                        if (!b) {
                            log.warn("设备测量失败-基线-切换样品池: {}, 错误: {}", deviceSerialNumber, operationDark1.getMessage());
                            setTaskMeasuring(taskId, false);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("error", "切换样品池失败-基线" + operationDark1.getMessage());
                            sendToActiveSession(null, taskId, jsonObject);
                            return R.fail(map);
                        }
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("currentPoolNum", positionDark);
                        sendToActiveSession(null, taskId, jsonObject1);
                        DevOperationInputParam inputParam1 = new DevOperationInputParam();
                        inputParam1.setDeviceSerialNumber(deviceSerialNumber);
                        inputParam1.setAction(BaseLine);

                        inputParam1.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());
                        this.realTimeEcho(taskId, "光谱-开始基线");
                        if (halt.get(taskId)) {
                            map.put("line", list);
                            return R.fail(map);
                        }
                        R<OperationResultEntity> operation =
                                devOperationUtilService.operation(inputParam1);
                        b = operation.getSuccess();
                        msg = operation.getMessage();
                    } else {
                        if (halt.get(taskId)) {
                            map.put("line", list);
                            return R.fail(map);
                        }

                        promptUserToPlaceMaterial(taskId, "空气");
                        if (halt.get(taskId)) {
                            map.put("line", list);
                            return R.fail(map);
                        }
//                Thread.sleep(5000);
                        DevOperationInputParam inputParam1 = new DevOperationInputParam();
                        inputParam1.setDeviceSerialNumber(deviceSerialNumber);
                        inputParam1.setAction(BaseLine);

                        inputParam1.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());
                        this.realTimeEcho(taskId, "光谱-开始基线");
                        if (halt.get(taskId)) {
                            map.put("line", list);
                            return R.fail(map);
                        }
                        R<OperationResultEntity> operation =
                                devOperationUtilService.operation(inputParam1);
                        b = operation.getSuccess();
                        msg = operation.getMessage();
                    }
                    if (!b) {
                        log.warn("设备基线失败-光谱: {}, 错误: {}", deviceSerialNumber, msg);
                        setTaskMeasuring(taskId, false);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("error", "基线失败-光谱" + msg);
                        sendToActiveSession(null, taskId, jsonObject);
                        return R.fail(map);
//                        throw new BusinessException(msg);

                    }

//            R<OperationResultEntity> action = deviceManageService.Action(param);
                    while (true) {
                        taskStopFlag.put(taskId, true);
//                        if (this.halt.get(taskId)) {
//
//                            setTaskMeasuring(taskId, false);
////                            JSONObject jsonObject = new JSONObject();
////                            jsonObject.put("error", "停止");
////                            sendToActiveSession(null, taskId, jsonObject);
//                            realTimeEcho(taskId, "停止-光谱");
//                            map.put("line", list);
////                            throw new BusinessException("任务被中断-光谱");
//                            return map;
//                        }
                        try {
                            Object o = queue.pollMessage(deviceSerialNumber, BaseLine.getCmdId());
                            if (o instanceof SpectrumPoint || o == null) {
                                SpectrumPoint point = (SpectrumPoint) o;
                                if (point != null) {
                                    log.info("基线结果：{}", point);
                                    this.realTimeEcho(taskId, "光谱-基线: " + point.getWavelength());
                                }
                            } else if (o instanceof EndMarker) {
                                EndMarker marker = (EndMarker) o;
                                if (marker.getReason() == EndMarker.Reason.UN_NORMAL) {
                                    log.error("基线数据获取错误：{}", o);
                                    setTaskMeasuring(taskId, false);
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("error", "基线数据获取错误" + o);
                                    sendToActiveSession(null, taskId, jsonObject);
//                                    throw new BusinessException("基线数据获取错误" + o);
                                    return R.fail(map);
                                }
                                if (marker.getReason() == EndMarker.Reason.NORMAL) {
                                    log.info("基线-拿到正常结束标识");
                                    this.realTimeEcho(taskId, "光谱-基线成功");
                                    break;
                                }
                                if (marker.getReason() == EndMarker.Reason.NORMAL_STOP) {
                                    this.realTimeEcho(taskId, "光谱-基线停止");
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            log.error("基线数据获取错误：", e);
                        }
                    }
                } else {
                    log.warn("暗电流矫正失败-光谱: {}", deviceSerialNumber);
                    setTaskMeasuring(taskId, false);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", "暗电流矫正失败-光谱");
                    sendToActiveSession(null, taskId, jsonObject);
//                    throw new BusinessException("任务被中断-光谱");
                    return R.fail(map);
                }
            }
            if (halt.get(taskId)) {
                log.info("线程中断");
                setTaskMeasuring(taskId, false);
                realTimeEcho(taskId, "停止-光谱");
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("error", "停止");
//                sendToActiveSession(null, taskId, jsonObject);
//                throw new BusinessException("任务被中断-光谱");
                return R.fail(map);
            }
        } else {
            b = true;
        }
        if (b) {

            if (isMultiProductPool) {
                if (d == 0) {
                    //切换样品池到扫描物质
                    this.realTimeEcho(taskId, "光谱-切换样品池到扫描物质");

                    log.info("切换样品池到扫描物质-光谱>>>>>>>>>>>>>>>>>>>>");
                    DevOperationInputParam operationInputParam = new DevOperationInputParam();
                    operationInputParam.setDeviceSerialNumber(deviceSerialNumber);
                    operationInputParam.setOperationType(OperationTypeEnums.SetParam.getType());
                    SetParam sampleParam = new SetParam();
                    Integer position = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.MEASURE);
                    //设置样品池位置
                    sampleParam.setCellPos(position);
                    operationInputParam.setParam(sampleParam);
                    if (halt.get(taskId)) {
                        return R.fail(map);
                    }
                    R<OperationResultEntity> operation1 = devOperationUtilService.operation(operationInputParam);
                    if (operation1.getSuccess()) {
                        this.realTimeEcho(taskId, "光谱-切换样品池到测量物质成功");
                        log.info("切换样品池到测量物质成功-光谱>>>>>>>>>>>>>>>>");
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("currentPoolNum", position);
                        sendToActiveSession(null, taskId, jsonObject1);
                    }
                    b = operation1.getSuccess();
                    if (!b) {
                        log.error("切换样品池到扫描物质失败-光谱");
                        setTaskMeasuring(taskId, false);
//                    throw new BusinessException(operation1.getMessage());
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("error", "切换样品池到扫描物质失败-光谱");
                        sendToActiveSession(null, taskId, jsonObject);
                        return R.fail(map);
                    }
                }
            } else {
                b = true;
                List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
                //获取当前检测项的所有标准物
                List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(collect)) {
                    String standardName = "";
                    for (TaskExecutionDetail detail : collect) {
                        if (detail.getStandardType().equals(MEASUREMENT.getCode())) {

                            CalStdCats cats = calStdCatsService.getById(detail.getStandardId());
                            if (cats == null) {
                                CalArtifacts calArtifacts = calArtifactsService.getById(detail.getStandardId());
                                if (calArtifacts != null) {
                                    standardName = calArtifacts.getName();
                                }
                            } else {
                                standardName = cats.getName();
                            }
                        }
                    }
                    if (halt.get(taskId)) {
                        map.put("line", list);
                        return R.fail(map);
                    }
                    promptUserToPlaceMaterial(taskId, standardName);
                    if (halt.get(taskId)) {
                        map.put("line", list);
                        return R.fail(map);
                    }
                } else {
                    log.warn("当前检测项没有标准物");
                }
            }
            if (b) {
                this.realTimeEcho(taskId, "光谱-开始光谱扫描");

                //光谱扫描入参
                DevOperationInputParam param1 = new DevOperationInputParam();
                param1.setDeviceSerialNumber(deviceSerialNumber);
                param1.setAction(Scan);
//        param1.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());

                param1.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());
                if (halt.get(taskId)) {
                    return R.fail(map);
                }
                R<OperationResultEntity> operation =
                        devOperationUtilService.operation(param1);
                log.info("光谱扫描方法返回：{}", operation);
                if (!operation.getSuccess()) {
                    log.warn("光谱扫描失败: {}, 错误: {}", deviceSerialNumber, operation.getMessage());
                    setTaskMeasuring(taskId, false);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", "光谱扫描失败" + operation.getMessage());
                    sendToActiveSession(null, taskId, jsonObject);
//                    throw new BusinessException("光谱扫描失败-光谱");
                    return R.fail(map);
                }
                int i = 0;
                JSONObject countJsonObject = getJsonObject(startWavelength, endWavelength, scanInterval);
                sendToActiveSession(null, taskId, countJsonObject);
                while (true) {
                    try {
                        taskStopFlag.put(taskId, true);
//                        if (halt.get(taskId)) {
//                            log.info("任务被暂停5");
//
//                            log.info("线程中断");
//                            setTaskMeasuring(taskId, false);
////                            throw new InterruptedException("任务被中断");
//                            map.put("line", list);
//                            return map;
//                        }
                        Object o = queue.pollMessage(deviceSerialNumber, Scan.getCmdId());
//                        JSONObject jsonObject = new JSONObject();
//                        jsonObject.put("xCount", );
//                        sendToActiveSession(null,taskId,jsonObject);

//                        log.info("光谱扫描总点数：{}, 起始波长：{}, 终止波长：{}, 扫描间隔：{}",
//                                totalPoints, startWavelength, endWavelength, scanInterval);

                        if (o instanceof SpectrumPoint || o == null) {
                            SpectrumPoint p = (SpectrumPoint) o;
                            if (p != null) {
                                i++;
                                // 获取或创建波形图数据缓存
                                List<JSONObject> cachedDataPoints = waveformCache.computeIfAbsent(taskId, k -> new ArrayList<>());


                                getPointsByLuminosityMode(luminosityMode, p, xList, yList, taskId, i, cachedDataPoints, list, dataMap);

                                log.info("扫描结果：{}", p);
                            }
                        } else if (o instanceof EndMarker) {
                            EndMarker endMarker = (EndMarker) o;
                            if (endMarker.getReason().equals(EndMarker.Reason.UN_NORMAL)) {
                                setTaskMeasuring(taskId, false);
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("error", "任务异常-光谱");
                                sendToActiveSession(null, taskId, jsonObject);
//                                throw new BusinessException("任务被中断-光谱");
                                map.put("line", list);
                                return R.fail(map);
                            }
                            if (endMarker.getReason().equals(EndMarker.Reason.NORMAL)) {
                                break;
                            }
                            if (endMarker.getReason().equals(EndMarker.Reason.TIMEOUT)) {
                                setTaskMeasuring(taskId, false);
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("error", "任务超时-光谱");
                                sendToActiveSession(null, taskId, jsonObject);
//                                throw new BusinessException("任务超时-光谱");
                                map.put("line", list);
                                return R.fail(map);
                            }
                            if (endMarker.getReason() == EndMarker.Reason.NORMAL_STOP) {
                                this.realTimeEcho(taskId, "光谱-扫描停止");
                                map.put("line", list);
                                return R.fail(map);
                            }
                        } else {
                            log.warn("遇到未知的值，报错返回");
                            setTaskMeasuring(taskId, false);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("error", "任务失败-光谱");
                            sendToActiveSession(null, taskId, jsonObject);
//                            throw new BusinessException("遇到未知的值");
                            map.put("line", list);
                            return R.fail(map);
                        }
                    } catch (Exception e) {
                        log.error("光谱扫描任务执行失败了：", e);
                        setTaskMeasuring(taskId, false);
//                        throw new BusinessException("任务执行失败：" + e.getMessage());
                        map.put("line", list);
                        return R.fail(map);
                    }
                }
                log.info("fhaosdhgfoipadshgfohsadfghpasdhgpasudhgiupas");
            }
//            if (!b) {
//                log.warn("扫描物质切换失败-光谱: {}", deviceSerialNumber);
//                setTaskMeasuring(taskId, false);
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("error", "扫描物质切换失败-光谱");
//                sendToActiveSession(null, taskId, jsonObject);
//                throw new BusinessException("扫描物质切换失败-光谱");
//            }
        } else {
            log.warn("光谱扫描失败-光谱: {}", deviceSerialNumber);
            setTaskMeasuring(taskId, false);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", "光谱扫描失败-光谱");
            sendToActiveSession(null, taskId, jsonObject);
            return R.fail(map);
//            stopTask(taskId);
        }
        List<BigDecimal> bigDecimalList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(stringList)) {
            for (String str : stringList) {
                if (str != null) {
                    try {
                        bigDecimalList.add(new BigDecimal(str));
                    } catch (NumberFormatException e) {
                        log.warn("无法解析为数字: {}", str);
                    }
                }
            }
        }
        try {
            if (halt.get(taskId)) {
                map.put("line", list);
                return R.fail(map);
            }
            res = analysisProcess(xList, yList, bigDecimalList, methodType);
        } catch (Exception e) {
            log.error("任务执行失败：", e);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", e.getMessage());
            sendToActiveSession(null, taskId, jsonObject);
            map.put("line", list);
            return R.fail(map);
        }
        if (halt.get(taskId)) {
            log.info("任务停止");
            if (CollectionUtils.isNotEmpty(res)) {
                for (JSONObject re : res) {
                    String s = String.valueOf(re.get("peak"));
                    String s1 = DataCountUtil.formatValueWithBigDecimal(5, s);
                    re.put("peak", s1);
                }
            }
//            throw new BusinessException("任务被中断");
            map.put("line", list);
            map.put("res", res);
            return R.fail(map);
        }
        if (CollectionUtils.isNotEmpty(res)) {
            for (JSONObject re : res) {
                String s = String.valueOf(re.get("peak"));
                String s1 = DataCountUtil.formatValueWithBigDecimal(5, s);
                re.put("peak", s1);
            }
        }
        map.put("line", list);
        map.put("res", res);
        log.info("最大偏移量结果222：{}", res);

        return R.ok(map);
    }

    private static JSONObject getJsonObject(Double startWavelength, Double endWavelength, Double scanInterval) {
        HashMap<String, String> map1 = new HashMap<>();
        map1.put("type", "spectrum");
        map1.put("startWavelength", startWavelength.toString());
        map1.put("endWavelength", endWavelength.toString());
        map1.put("scanInterval", scanInterval.toString());
//                        int totalPoints = calculateSpectrumPoints(startWavelength, endWavelength, scanInterval);
        JSONObject countJsonObject = new JSONObject();
        countJsonObject.put("horizontalAxis", map1);
        return countJsonObject;
    }

    /**
     * 计算光谱扫描的总点数
     * 公式：总点数 = |终止波长 - 起始波长| / 扫描间隔 + 1
     *
     * @param startWavelength 起始波长
     * @param endWavelength 终止波长
     * @param scanInterval 扫描间隔
     * @return 总点数
     */
    private int calculateSpectrumPoints(Double startWavelength, Double endWavelength, Double scanInterval) {
        if (startWavelength == null || endWavelength == null || scanInterval == null || scanInterval <= 0) {
            log.warn("波长或扫描间隔参数无效：startWavelength={}, endWavelength={}, scanInterval={}",
                    startWavelength, endWavelength, scanInterval);
            return 0;
        }

        // 计算波长范围的绝对值，确保起始和终止波长大小顺序不影响结果
        double wavelengthRange = Math.abs(endWavelength - startWavelength);

        // 计算点数：波长范围 / 扫描间隔 + 1
        // 例如：起始 200nm，终止 800nm，间隔 1nm，则点数 = |800-200|/1 + 1 = 601 个点
        // 例如：起始 800nm，终止 200nm，间隔 1nm，则点数 = |200-800|/1 + 1 = 601 个点
        int totalPoints = (int) Math.round(wavelengthRange / scanInterval) + 1;

        return totalPoints;
    }
    /**
     * 提示用户放入物质并等待确认
     */
    private void promptUserToPlaceMaterial(String taskId, String stdMaterialName) throws BusinessException {
        // 发送提示信息
        JSONObject noticeMsg = new JSONObject();
        if (AIR.getDescription().equals(stdMaterialName)) {
            noticeMsg.put("notice", "请保持样品池为空！");
        } else {
            noticeMsg.put("notice", "请放入 " + stdMaterialName);
        }
        sendToActiveSession(null, taskId, noticeMsg);
        // 设置等待状态
        pauseTask(taskId);
    }

    //根据不同的光度模式得到点
    private void getPointsByLuminosityMode(String luminosityMode, SpectrumPoint p, List<Double> xList, List<BigDecimal> yList, String taskId, Integer i, List<JSONObject> cachedDataPoints, List<JSONObject> list, Map<String, Object> dataMap) throws BusinessException {
// 将时间戳转换为相对于第一个点的秒数
//        long aaTimestamp = waveformCache.getOrDefault(taskId, new ArrayList<>()).stream()
//                .findFirst()
//                .map(obj -> obj.getLong("timestamp"))
//                .orElse(p.getTimestamp());
//        double bbSeconds = (p.getTimestamp() - aaTimestamp) / 1000.0;
//        log.info("bbSeconds：{}", bbSeconds);
//        log.info("aaTimestamp：{}", aaTimestamp);
//        log.info("p.getTimestamp()：{}", p.getTimestamp());
        //获取枚举，看看是哪个
        WaveLengthEnum anEnum = WaveLengthEnum.getByName(luminosityMode);
        log.info("光度模式：{}", anEnum.getName());
//获取起始波长
        Double startWavelength = MapUtils.getDouble(dataMap, START_WAVELENGTH.getDescription());
        //获取终止波长
        Double endWavelength = MapUtils.getDouble(dataMap, END_WAVELENGTH.getDescription());
        //扫描间隔
        Double scanInterval = MapUtils.getDouble(dataMap, SCAN_INTERVAL.getDescription());
        Double scanCount = MapUtils.getDouble(dataMap, SCAN_COUNT.getDescription());
        if (anEnum == abs) {
            if (ObjectUtils.isNotEmpty(p)) {
                JSONObject payload = new JSONObject();
                JSONArray points = new JSONArray();
                JSONObject point = new JSONObject();

                if (p.getTimestamp() != null) {

                    // 将时间戳转换为相对于第一个点的秒数
                    long firstTimestamp = waveformCache.getOrDefault(taskId, new ArrayList<>()).stream()
                            .findFirst()
                            .map(obj -> obj.getLong("timestamp"))
                            .orElse(p.getTimestamp());
                    double relativeTimeInSeconds = Math.round((p.getTimestamp() - firstTimestamp) / 1000.0 * 10) / 10.0;
                    payload.put("timestamp", p.getTimestamp());
                    payload.put("totalPoints", scanCount);
                    point.put("yType", "Abs");
                    point.put("xType", "秒(s)");
                    double wavelength = p.getAbsResult().getWavelength();
                    xList.add(relativeTimeInSeconds);
                    BigDecimal bigDecimal = roundToDecimalPlaces(new BigDecimal(p.getAbsResult().getAbs()), 4);
                    point.put("x", relativeTimeInSeconds);
                    yList.add(new BigDecimal(p.getAbsResult().getAbs()));
                    point.put("y", p.getAbsResult().getAbs());

                    this.realTimeEcho(taskId, "时间扫描-正在时间扫描: " + wavelength + "nm " + bigDecimal + " Abs");

                } else {
                    payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);
                    point.put("xType", "波长(nm)");
                    point.put("yType", "abs");
                    double wavelength = p.getAbsResult().getWavelength();
                    point.put("x", wavelength);
                    xList.add(wavelength);
                    BigDecimal aDouble = roundToDecimalPlaces(new BigDecimal(p.getAbsResult().getAbs()), 4);
                    point.put("y", p.getAbsResult().getAbs());
                    yList.add(new BigDecimal(p.getAbsResult().getAbs()));
                    this.realTimeEcho(taskId, "光谱-正在光谱扫描: " + wavelength + "nm " + aDouble + " Abs");

                }
//                    point.put("roundOffY", valueY);
//                    point.put("nm", waveLength);


                points.add(point);
                payload.put("data", points);
                payload.put("sampleId", "");
                payload.put("taskId", taskId);
                payload.put("pointIndex", i);
//                payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);


                // 将数据点添加到缓存
                cachedDataPoints.add(payload);

                // 尝试发送到活跃session
                sendToActiveSession(null, taskId, payload);
                list.add(payload);
            }

        }
        if (anEnum == tPercent) {
            if (ObjectUtils.isNotEmpty(p)) {
                JSONObject payload = new JSONObject();
                JSONArray points = new JSONArray();
                JSONObject point = new JSONObject();
                if (p.getTimestamp() != null) {
                    // 将时间戳转换为相对于第一个点的秒数
                    long firstTimestamp = waveformCache.getOrDefault(taskId, new ArrayList<>()).stream()
                            .findFirst()
                            .map(obj -> obj.getLong("timestamp"))
                            .orElse(p.getTimestamp());
                    double relativeTimeInSeconds = Math.round((p.getTimestamp() - firstTimestamp) / 1000.0 * 10) / 10.0;
                    payload.put("timestamp", p.getTimestamp());
                    payload.put("totalPoints", scanCount);
                    point.put("yType", "T%");
                    point.put("xType", "秒(s)");
                    double wavelength = p.getTResult().getWavelength();
                    xList.add(relativeTimeInSeconds);
                    BigDecimal bigDecimal = roundToDecimalPlaces(new BigDecimal(p.getTResult().getTPercent()), 4);
                    point.put("x", relativeTimeInSeconds);
                    yList.add(new BigDecimal(p.getTResult().getTPercent()));
                    point.put("y", p.getTResult().getTPercent());

//
//
//                    payload.put("timestamp", p.getTimestamp());
//                    payload.put("totalPoints", scanCount);
//                    point.put("yType", "波长(nm)");
//                    point.put("xType", "T%");
//                    double wavelength = p.getTResult().getWavelength();
//                    point.put("y", wavelength);
//                    xList.add(wavelength);
//                    Double aDouble = new Double(p.getTResult().getTPercent());
//                    point.put("x", aDouble);
//                    yList.add(aDouble);
                    this.realTimeEcho(taskId, "时间扫描-正在时间扫描: " + wavelength + "nm " + bigDecimal + " T%");

                } else {
                    payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);
                    point.put("xType", "波长(nm)");
                    point.put("yType", "T%");
                    double wavelength = p.getTResult().getWavelength();
                    point.put("x", wavelength);
                    xList.add(wavelength);
                    BigDecimal aDouble = roundToDecimalPlaces(new BigDecimal(p.getTResult().getTPercent()), 4);
                    point.put("y", p.getTResult().getTPercent());
                    yList.add(new BigDecimal(p.getTResult().getTPercent()));
                    this.realTimeEcho(taskId, "光谱-正在光谱扫描: " + wavelength + "nm " + aDouble + " T%");

                }
                points.add(point);
                payload.put("data", points);
                payload.put("sampleId", "");
                payload.put("taskId", taskId);
                payload.put("pointIndex", i);


                // 将数据点添加到缓存
                cachedDataPoints.add(payload);

                // 尝试发送到活跃session
                sendToActiveSession(null, taskId, payload);
                list.add(payload);
            }

        }
        if (anEnum == WaveLengthEnum.es) {
            if (ObjectUtils.isNotEmpty(p)) {
                JSONObject payload = new JSONObject();
                JSONArray points = new JSONArray();
                JSONObject point = new JSONObject();
                if (p.getTimestamp() != null) {
                    // 将时间戳转换为相对于第一个点的秒数
                    long firstTimestamp = waveformCache.getOrDefault(taskId, new ArrayList<>()).stream()
                            .findFirst()
                            .map(obj -> obj.getLong("timestamp"))
                            .orElse(p.getTimestamp());
                    double relativeTimeInSeconds = Math.round((p.getTimestamp() - firstTimestamp) / 1000.0 * 10) / 10.0;
                    payload.put("timestamp", p.getTimestamp());
                    payload.put("totalPoints", scanCount);
                    point.put("yType", "Es");
                    point.put("xType", "秒(s)");
//                    double wavelength = p.getTResult().getWavelength();
                    xList.add(relativeTimeInSeconds);
                    BigDecimal aDouble = roundToDecimalPlaces(new BigDecimal(p.getEsResult().getEs()), 4);
                    point.put("x", relativeTimeInSeconds);
                    yList.add(new BigDecimal(p.getEsResult().getEs()));
                    point.put("y", p.getEsResult().getEs());
                    double wavelength = p.getEsResult().getWavelength();
//
//                    payload.put("timestamp", p.getTimestamp());
//                    payload.put("totalPoints", scanCount);
//                    point.put("yType", "波长(nm)");
//                    point.put("xType", "ES");
//                    double wavelength = p.getEsResult().getWavelength();
//                    point.put("y", wavelength);
//                    xList.add(wavelength);
//                    Double aDouble = new Double(p.getEsResult().getEs());
//                    point.put("x", aDouble);
//                    yList.add(aDouble);
                    this.realTimeEcho(taskId, "时间扫描-正在时间扫描: " + wavelength + "nm " + aDouble + " Es");

                } else {
                    payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);
                    point.put("xType", "波长(nm)");
                    point.put("yType", "ES");
                    double wavelength = p.getEsResult().getWavelength();
                    point.put("x", wavelength);
                    xList.add(wavelength);
                    BigDecimal aDouble = roundToDecimalPlaces(new BigDecimal(p.getEsResult().getEs()), 4);
                    point.put("y", p.getEsResult().getEs());
                    yList.add(new BigDecimal(p.getEsResult().getEs()));
                    this.realTimeEcho(taskId, "光谱-正在光谱扫描: " + wavelength + "nm " + aDouble + " Es");

                }
                points.add(point);
                payload.put("data", points);
                payload.put("sampleId", "");
                payload.put("taskId", taskId);
                payload.put("pointIndex", i);
//                payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);

                // 将数据点添加到缓存
                cachedDataPoints.add(payload);

                // 尝试发送到活跃session
                sendToActiveSession(null, taskId, payload);
                list.add(payload);
            }

        }
        if (anEnum == WaveLengthEnum.er) {
            if (ObjectUtils.isNotEmpty(p)) {
                JSONObject payload = new JSONObject();
                JSONArray points = new JSONArray();
                JSONObject point = new JSONObject();
                if (p.getTimestamp() != null) {
                    // 将时间戳转换为相对于第一个点的秒数
                    long firstTimestamp = waveformCache.getOrDefault(taskId, new ArrayList<>()).stream()
                            .findFirst()
                            .map(obj -> obj.getLong("timestamp"))
                            .orElse(p.getTimestamp());
                    double relativeTimeInSeconds = Math.round((p.getTimestamp() - firstTimestamp) / 1000.0 * 10) / 10.0;
                    payload.put("timestamp", p.getTimestamp());
                    payload.put("totalPoints", scanCount);
                    point.put("yType", "Er");
                    point.put("xType", "秒(s)");
//                    double wavelength = p.getTResult().getWavelength();
                    xList.add(relativeTimeInSeconds);
                    BigDecimal aDouble = roundToDecimalPlaces(new BigDecimal(p.getErResult().getEr()), 4);
                    point.put("x", relativeTimeInSeconds);
                    yList.add(new BigDecimal(p.getErResult().getEr()));
                    point.put("y", p.getErResult().getEr());
                    double wavelength = p.getErResult().getWavelength();
//
//                    payload.put("timestamp", p.getTimestamp());
//                    payload.put("totalPoints", scanCount);
//                    point.put("yType", "波长(nm)");
//                    point.put("xType", "ER");
//                    double wavelength = p.getErResult().getWavelength();
//                    point.put("y", wavelength);
//                    xList.add(wavelength);
//                    Double aDouble = new Double(p.getErResult().getEr());
//                    point.put("x", aDouble);
//                    yList.add(aDouble);
                    this.realTimeEcho(taskId, "时间扫描-正在时间扫描: " + wavelength + "nm " + aDouble + " Er");

                } else {
                    payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);
                    point.put("xType", "波长(nm)");
                    point.put("yType", "ER");
                    double wavelength = p.getErResult().getWavelength();
                    point.put("x", wavelength);
                    xList.add(wavelength);
                    BigDecimal aDouble = roundToDecimalPlaces(new BigDecimal(p.getErResult().getEr()), 4);
                    point.put("y", p.getErResult().getEr());
                    yList.add(new BigDecimal(p.getErResult().getEr()));
                    this.realTimeEcho(taskId, "光谱-正在光谱扫描: " + wavelength + "nm " + aDouble + " Er");

                }
                points.add(point);
                payload.put("data", points);
                payload.put("sampleId", "");
                payload.put("taskId", taskId);
                payload.put("pointIndex", i);
//                payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);

                // 将数据点添加到缓存
                cachedDataPoints.add(payload);

                // 尝试发送到活跃session
                sendToActiveSession(null, taskId, payload);
                list.add(payload);
            }

        }
        List<JSONObject> put = waveformCache.put(taskId, cachedDataPoints);
    }

    /**
     * 对给定的BigDecimal数值进行四舍五入，保留指定的小数位数
     *
     * @param value 需要处理的BigDecimal数值
     * @param scale 保留的小数位数
     * @return 四舍五入后的BigDecimal结果
     */
    private BigDecimal roundToDecimalPlaces(BigDecimal value, Integer scale) {
        if (value == null || scale == null) {
            return value;
        }

        try {
            return value.setScale(scale, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("数值四舍五入处理失败: value={}, scale={}", value, scale, e);
            return value;
        }
    }

    /**
     * 调用Python服务
     *
     * @param url
     * @param requestBody
     * @return
     */
    private Map<String, Object> callPythonService(String url, Map<String, Object> requestBody) throws BusinessException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 使用 Future 实现可中断的异步调用
            java.util.concurrent.Future<ResponseEntity<Map>> future = taskExecutor.submit(() ->
                    restTemplate.postForEntity(url, request, Map.class)
            );

            // 设置超时时间（例如 30 秒）
            ResponseEntity<Map> response;
            try {
                response = future.get(30, java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new BusinessException("算法服务调用超时");
            } catch (java.util.concurrent.ExecutionException e) {
                throw new BusinessException("算法服务调用失败: " + e.getCause().getMessage());
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new BusinessException("算法服务调用被中断");
            }

            Map<String, Object> responseBody = response.getBody();

            // 检查响应体是否为空
            if (responseBody == null) {
                throw new BusinessException(YamlMessageSource.getMessageStatic("00000032")); // "算法服务返回异常"
            }

            Integer code = (Integer) responseBody.get("code");
            if (code == null || code != 200) {
                String message = (String) responseBody.get("message");
                String errorMsg = org.springframework.util.StringUtils.hasText(message) ? message : "未知错误";
                throw new BusinessException(errorMsg);
            }

            return responseBody;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("算法服务异常", e);
            throw new BusinessException("算法服务异常,请重启服务");
        }
    }



    // 发送缓存的波形图数据
    private void sendCachedWaveformData(WebSocketSession session, String taskId) {
        List<JSONObject> cachedDataPoints = waveformCache.get(taskId);
        if (cachedDataPoints != null && !cachedDataPoints.isEmpty()) {
            log.info("发送缓存的波形图数据给新会话，共 {} 个数据点", cachedDataPoints.size());

            for (JSONObject payload : cachedDataPoints) {
                sendToActiveSession(null, taskId, payload);
            }
        }
    }


    // 修改 sendBatchDataPoints 方法
    private R<Map<String, List<JSONObject>>> sendBatchDataPoints(WebSocketSession originalSession, String taskId, String itemId, Map<String, Object> dataMap, String updateBy, String deviceSerialNumber, List<String> list, Integer samplePoolNum, Integer d) throws BusinessException {
        boolean b = samplePoolNum > 1;
        //是不是光谱扫描
        String res = MapUtils.getString(dataMap, SPECTRAL_SCAN_TYPE.getDescription());
        //光度模式
        String luminosityMode = MapUtils.getString(dataMap, LUMINOSITY_MODE.getDescription());
        //扫描个数
        Integer scanCount = MapUtils.getInteger(dataMap, SCAN_COUNT.getDescription());
        R<Map<String, List<JSONObject>>> map = new R<>();

        try {
            //如果是光谱扫描
            if (YES.getDescription().equals(res)) {
                log.info("光谱扫描>>>>>>>");

                
                map = spectrumScan(taskId, dataMap, deviceSerialNumber, list, b, itemId, d);
            } else if (scanCount != null) {
                log.info("时间扫描>>>>>");
                map = timeScan(taskId, dataMap, deviceSerialNumber, scanCount, luminosityMode, b, itemId, d);
            } else {
                log.info("光度扫描>>>>>>>>");
                map = intensityScan(taskId, dataMap, deviceSerialNumber, b, itemId, d);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("fsdgfasdgfsdgdsgdegtrg");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", e.getMessage());
            this.sendToActiveSession(null, taskId, jsonObject);
            this.realTimeEcho(taskId, e.getMessage());
            setTaskMeasuring(taskId, false);
            throw new BusinessException(e.getMessage());
        }
        /*每一次的测量结果我都要按照任务-检测项id-测量日期这样的给存储起来，因为用户要看，如果用户*/
//        WebSocketSession session = sessionTaskMap.get(taskId);
        JSONObject statusMessage = new JSONObject();
        statusMessage.put("finished", true);
        sendToActiveSession(null, taskId, statusMessage);
//        if (isSessionValid(session)) {
//            try {
//                session.sendMessage(new TextMessage(statusMessage.toJSONString()));
//            } catch (IOException e) {
//                log.warn("发送失败: ", e);
//            }
//        }
        if (map.getSuccess()) {
            List<JSONObject> jsonObjects = map.getData().get("line");
            if (CollectionUtils.isNotEmpty(jsonObjects)) {
                jsonObjects.add(statusMessage);
                Map<String, List<JSONObject>> data = map.getData();
                data.put("line", jsonObjects);
                map.setData(data);
            }
        }
//        if (CollectionUtils.isEmpty(jsonObjects)) {
//            map.put("line", null);
//        }
        // 波形图完成时清理缓存（可选）
        waveformCache.remove(taskId);

        return map;
    }

    /**
     * 使用高精度计时器实现完美准确的时间扫描
     * 改进：边测量边推送数据，让前端时间显示更准确
     */
    private R<Map<String, List<JSONObject>>> timeScan(String taskId, Map<String, Object> dataMap, String deviceSerialNumber, Integer scanCount, String luminosityMode, boolean isMultiProductPool, String itemId, Integer d) throws BusinessException {
        Map<String, List<JSONObject>> map = new HashMap<>();
        // 线数据列表
        ArrayList<JSONObject> line = new ArrayList<>();
        // 结果数据列表
        ArrayList<JSONObject> res = new ArrayList<>();
        // X轴数据列表
        ArrayList<Double> xList = new ArrayList<>();
        // Y轴数据列表
        ArrayList<BigDecimal> yList = new ArrayList<>();

        // 增益
        Integer gain = MapUtils.getInteger(dataMap, GAIN.getDescription());
        // 狭缝，光谱带宽
        Double narrow = MapUtils.getDouble(dataMap, SPECTRAL_BANDWIDTH.getDescription());
        // 响应时间
        Double responseTime = MapUtils.getDouble(dataMap, RESPONSE_TIME.getDescription());
        // 特征值
        String characteristic = MapUtils.getString(dataMap, CHARACTERISTIC.getDescription());
        //时间间隔
        Double timeInterval = MapUtils.getDouble(dataMap, TIME_INTERVAL.getDescription());
        //总时长
        Double totalTime = timeInterval * (scanCount + 1);

        // 设置参数
        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
        SetParam param = new SetParam();

        // 光度模式
        Integer photoMode = null;
        try {
            photoMode = Integer.valueOf(Objects.requireNonNull(WaveLengthEnum.getCode(luminosityMode)));
        } catch (Exception e) {
            log.warn("时间扫描-光度模式为空！");
        }

        // 灯状态，灯位置
        Integer lampStatus = null;
        try {
            lampStatus = Integer.valueOf(Objects.requireNonNull(EnergyLampEnums.getIdentityByName(MapUtils.getString(dataMap, LAMP_STATUS.getDescription()))));
        } catch (Exception e) {
            log.error("时间扫描-无效的灯状态");
        }

        param.setPhotoMode(photoMode);
        param.setResponseTime(responseTime);
        param.setGain(gain);
        param.setLampPos(lampStatus);
        param.setSlit1(narrow);
        inputParam.setParam(param);
        R<OperationResultEntity> operationDark3 = new R<>();
        WaveLengthEnum anEnum = WaveLengthEnum.getByName(luminosityMode);
        boolean isOk;
        String errorMsg = "";
        timeScanStopControl.put(taskId, true);
        if (d == 0) {
            if ((anEnum == abs || anEnum == tPercent)) {

                if (isMultiProductPool) {
                    realTimeEcho(taskId, "时间扫描-切换样品池到暗电流物质");

                    //切换样品池到黑挡块
                    log.info("时间扫描-切换样品池到黑挡块>>>>>>>>>>>>>>>>>>>>");
                    DevOperationInputParam operationInputParamDark = new DevOperationInputParam();
                    operationInputParamDark.setDeviceSerialNumber(deviceSerialNumber);
                    operationInputParamDark.setOperationType(OperationTypeEnums.SetParam.getType());
                    SetParam sampleParamDark = new SetParam();
                    Integer positionDark = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.BLACK_BLOCK);
                    //设置样品池位置
                    sampleParamDark.setCellPos(positionDark);
                    operationInputParamDark.setParam(sampleParamDark);
                    if (halt.get(taskId)) {
                        log.info("时间扫描-任务被暂停");
                        return R.fail(map);
                    }
                    operationDark3 = devOperationUtilService.operation(operationInputParamDark);
                    if (operationDark3.getSuccess()) {
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("currentPoolNum", positionDark);
                        sendToActiveSession(null, taskId, jsonObject1);
                    }
                } else {
                    operationDark3.setSuccess(true);
                    if (halt.get(taskId)) {
                        map.put("line", line);
                        return R.fail(map);
                    }
                    promptUserToPlaceMaterial(taskId, "黑挡块");
                    if (halt.get(taskId)) {
                        map.put("line", line);
                        return R.fail(map);
                    }
                }
                if (operationDark3.getSuccess()) {
                    realTimeEcho(taskId, "时间扫描-切换样品池到暗电流物质成功");

                    log.info("时间扫描-切换样品池到黑挡块成功>>>>>>>>>>>>>>>>");
                } else {
                    log.warn("时间扫描-切换样品池到黑挡块失败！");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", operationDark3.getMessage());
                    sendToActiveSession(null, taskId, jsonObject);
                    setTaskMeasuring(taskId, false);
//                    stopTask(taskId);
                    map.put("line", line);
                    return R.fail(map);
                }

                if (org.apache.commons.lang3.StringUtils.equalsIgnoreCase(String.valueOf(taskAndVersionMap.get(taskId)), "T800")) {
                    String[] parts = characteristic.split("[,，]");

                    // 解析特征波长并找出最大最小值
                    List<Double> wavelengths = new ArrayList<>();
                    for (String part : parts) {
                        try {
                            double wl = Double.parseDouble(part.trim());
                            wavelengths.add(wl);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析波长值：{}", part);
                        }
                    }

                    if (!wavelengths.isEmpty()) {
                        double minWavelength = Collections.min(wavelengths);
                        double maxWavelength = Collections.max(wavelengths);

                        // 最大值加 2，最小值减 2
                        double startWavelength = minWavelength - 2.0;
                        double endWavelength = maxWavelength + 2.0;

                        log.info("计算得到的起始波长：{}, 终止波长：{}", startWavelength, endWavelength);

                        DevOperationInputParam devType = new DevOperationInputParam();
                        devType.setDeviceSerialNumber(deviceSerialNumber);
                        SetParam setParam = new SetParam();
                        //设置起始波长
                        setParam.setStartWl(startWavelength);
                        //设置终止波长
                        setParam.setEndWl(endWavelength);
                        //设置扫描间隔
                        setParam.setScanWlInterval(1.0);
                        devType.setParam(setParam);
                        devType.setOperationType(OperationTypeEnums.SetParam.getType());
                        R<OperationResultEntity> opera =
                                devOperationUtilService.operation(devType);
                    } else {
                        log.error("未找到有效的波长值");
                    }
                }
                realTimeEcho(taskId, "时间扫描-开始暗电流矫正");

                //到这里就不能停止了，得等到做完暗电流停止
                timeScanStopControl.put(taskId, false);
                log.info("时间扫描-开始暗电流矫正>>>>>>>>>>>>>>>>>>>>");
                DevOperationInputParam darkParam = new DevOperationInputParam();
                SetParam setParam1 = new SetParam();
                darkParam.setDeviceSerialNumber(deviceSerialNumber);
//        darkParam.setParam(setParam1);
                darkParam.setCalibrationType(DeviceCalibrationInfoEnum.Dark.getType());
                darkParam.setOperationType(OperationTypeEnums.Calibration.getType());
//        darkParam.setUpdateBy();
                if (halt.get(taskId)) {
                    return R.fail(map);
                }
                R<OperationResultEntity> operationDark =
                        devOperationUtilService.operation(darkParam);
                timeScanStopControl.put(taskId, true);
                log.info("时间扫描-暗电流矫正结果：{}", operationDark);
//            operationDark.setSuccess(false);
                if (operationDark.getSuccess()) {
                    isOk = true;
                    realTimeEcho(taskId, "时间扫描-暗电流矫正成功");
                } else {
//                isOk=false;
                    log.warn("时间扫描-暗电流矫正出错！");
                    setTaskMeasuring(taskId, false);
//            stopTask(taskId);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", operationDark.getMessage());
                    errorMsg = operationDark.getMessage();
                    boolean b = sendToActiveSession(null, taskId, jsonObject);
                    return R.fail(map);
                }
            } else {
                isOk = true;
            }

//            if (halt.get(taskId)) {
//                return R.fail(map);
//            }
            realTimeEcho(taskId, "时间扫描-开始设置参数");
            // 设置参数
            R<OperationResultEntity> operation = devOperationUtilService.operation(inputParam);
            if (!operation.getSuccess()) {
                log.warn("时间扫描-参数设置出错！");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", operation.getMessage());
                sendToActiveSession(null, taskId, jsonObject);
                setTaskMeasuring(taskId, false);
//            stopTask(taskId);
                return R.fail(map);
            } else {
                realTimeEcho(taskId, "时间扫描-参数设置成功");
            }


            // 解析特征波长
            String[] parts = characteristic.split("[,，]");
            List<String> waveLength = Arrays.stream(parts)
                    .filter(part -> part != null && !part.trim().isEmpty())
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(waveLength)) {
                inputParam.setWavelength(waveLength);
            }

            if (photoMode != null) {
                if (photoMode == 1 || photoMode == 2) {
                    if (halt.get(taskId)) {
                        log.warn("任务被取消！");
                        setTaskMeasuring(taskId, false);
                        return R.fail(map);
                    }


                }
            }
            R<OperationResultEntity> operation4 = new R<>();
            if (isMultiProductPool) {
                realTimeEcho(taskId, "时间扫描-切换样品池到校零物质");

                //切换样品池到校零物质
                log.info("时间扫描-切换样品池到校零物质>>>>>>>>>>>>>>>>>>>>");
                DevOperationInputParam operationInputParam = new DevOperationInputParam();
                operationInputParam.setDeviceSerialNumber(deviceSerialNumber);
                operationInputParam.setOperationType(OperationTypeEnums.SetParam.getType());
                SetParam sampleParam = new SetParam();
                Integer position = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.ZERO);
                //设置样品池位置
                sampleParam.setCellPos(position);
                operationInputParam.setParam(sampleParam);
                if (halt.get(taskId)) {
                    return R.fail(map);

                }
                operation4 = devOperationUtilService.operation(operationInputParam);
                if (operation4.getSuccess()) {
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("currentPoolNum", position);
                    sendToActiveSession(null, taskId, jsonObject1);
                }
            } else {
                operation4.setSuccess(true);
                List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
                //获取当前检测项的所有标准物
                List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(collect)) {
                    String standardName = "";
                    for (TaskExecutionDetail detail : collect) {
                        if (detail.getStandardType().equals(ZERO.getCode())) {

                            CalStdCats cats = calStdCatsService.getById(detail.getStandardId());
                            if (cats == null) {
                                CalArtifacts calArtifacts = calArtifactsService.getById(detail.getStandardId());
                                standardName = calArtifacts.getName();
                            } else {
                                standardName = cats.getName();
                            }
                        }
                    }
                    if (halt.get(taskId)) {
                        map.put("line", line);
                        return R.fail(map);
                    }
                    promptUserToPlaceMaterial(taskId, standardName);
                    if (halt.get(taskId)) {
                        map.put("line", line);
                        return R.fail(map);
                    }
                } else {
                    log.warn("当前检测项没有标准物");
                }
            }
            if (operation4.getSuccess()) {
                realTimeEcho(taskId, "时间扫描-切换样品池到校零物质成功");

                log.info("时间扫描-切换样品池到校零物质成功>>>>>>>>>>>>>>>>");
            } else {
                log.warn("时间扫描-切换样品池到校零物质失败！");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", operation4.getMessage());
                sendToActiveSession(null, taskId, jsonObject);
                setTaskMeasuring(taskId, false);
//            stopTask(taskId);
                return R.fail(map);
            }
            // 执行校零操作
            realTimeEcho(taskId, "时间扫描-执行校零操作");

            inputParam.setOperationType(OperationTypeEnums.Zero.getType());
            if (halt.get(taskId)) {
                return R.fail(map);
            }
            R<OperationResultEntity> zero = devOperationUtilService.operation(inputParam);
            if (!zero.getSuccess()) {
                log.warn("时间扫描-校零失败！");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", zero.getMessage());
                sendToActiveSession(null, taskId, jsonObject);
                setTaskMeasuring(taskId, false);
//            stopTask(taskId);
                return R.fail(map);

            } else {
                realTimeEcho(taskId, "时间扫描-校零操作成功");
            }

            List<ChangeParam> zeroList = zero.getData().getZeroResult();
            if (CollectionUtils.isEmpty(zeroList)) {
                log.warn("时间扫描-校零结果为空！");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "时间扫描-校零结果为空！");
                sendToActiveSession(null, taskId, jsonObject);
                setTaskMeasuring(taskId, false);
//            stopTask(taskId);
                return R.fail(map);
            }
        }

        // 使用定时任务实现边测量边推送
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TimeScan-" + taskId);
            t.setDaemon(true);
            return t;
        });

        R<OperationResultEntity> operation5 = new R<>();
        if (isMultiProductPool) {
            if (d == 0) {
                //切换样品池到测量物质
                realTimeEcho(taskId, "时间扫描-切换样品池到测量物质");

                log.info("时间扫描-切换样品池到测量物质>>>>>>>>>>>>>>>>>>>>");
                DevOperationInputParam operationInputParam1 = new DevOperationInputParam();
                operationInputParam1.setDeviceSerialNumber(deviceSerialNumber);
                operationInputParam1.setOperationType(OperationTypeEnums.SetParam.getType());
                SetParam sampleParam1 = new SetParam();
                Integer position1 = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.MEASURE);
                //设置样品池位置
                sampleParam1.setCellPos(position1);
                operationInputParam1.setParam(sampleParam1);
                if (halt.get(taskId)) {
                    return R.fail(map);
                }
                operation5 = devOperationUtilService.operation(operationInputParam1);
                if (operation5.getSuccess()) {
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("currentPoolNum", position1);
                    sendToActiveSession(null, taskId, jsonObject1);
                }
            }
        } else {
            operation5.setSuccess(true);
            List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
            //获取当前检测项的所有标准物
            List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(collect)) {
                String standardName = "";
                for (TaskExecutionDetail detail : collect) {
                    if (detail.getStandardType().equals(MEASUREMENT.getCode())) {

                        CalStdCats cats = calStdCatsService.getById(detail.getStandardId());
                        if (cats == null) {
                            CalArtifacts calArtifacts = calArtifactsService.getById(detail.getStandardId());
                            standardName = calArtifacts.getName();
                        } else {
                            standardName = cats.getName();
                        }
                    }
                }
                if (halt.get(taskId)) {
                    map.put("line", line);
                    return R.fail(map);
                }
                promptUserToPlaceMaterial(taskId, standardName);
                if (halt.get(taskId)) {
                    map.put("line", line);
                    return R.fail(map);
                }
            } else {
                log.warn("当前检测项没有标准物");
            }
        }

        if (operation5.getSuccess()) {
            realTimeEcho(taskId, "时间扫描-切换样品池到测量物质成功");

            log.info("时间扫描-切换样品池到测量物质成功>>>>>>>>>>>>>>>>");
        } else {
            log.warn("时间扫描-切换样品池到测量物质失败！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", "时间扫描-切换样品池到测量物质失败！");
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
//            stopTask(taskId);
            return R.fail(map);
        }

        realTimeEcho(taskId, "时间扫描-开始扫描");

        DevOperationInputParam param1 = new DevOperationInputParam();
        // 执行单次测量
        param1.setOperationType(OperationTypeEnums.TimeMeasure.getType());
        param1.setDeviceSerialNumber(deviceSerialNumber);
        ArrayList<String> strings = new ArrayList<>();
        strings.add(characteristic);
        param1.setIntervalSeconds(timeInterval);
        param1.setTotalTime(totalTime);
        if (halt.get(taskId)) {
            return R.fail(map);
        }

        R<OperationResultEntity> measure = devOperationUtilService.operation(param1);
        int i = 0;
        waveformCache.clear();

        // 在循环开始前发送总点数给前端（时间扫描）
//        int totalPoints = calculateTimeScanPoints(totalTime, timeInterval);
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("xCount", totalPoints);
//        sendToActiveSession(null, taskId, jsonObject);
//        log.info("时间扫描总点数：{}, 总时间：{}秒，时间间隔：{}秒", totalPoints, totalTime, timeInterval);
//

        HashMap<String, String> map1 = new HashMap<>();
        map1.put("type", "timeScan");
        map1.put("totalTime", totalTime.toString());
        map1.put("timeInterval", timeInterval.toString());
        JSONObject horizontalAxis = new JSONObject();
        horizontalAxis.put("horizontalAxis", map1);
        sendToActiveSession(null, taskId, horizontalAxis);

        while (true) {
            try {
                taskStopFlag.put(taskId, true);
//                if (halt.get(taskId)) {
//                    return R.fail(map);
//                }
                Object o = queue.pollMessage(deviceSerialNumber, T600ComIdEnums.Measure.getComId());
                if (o instanceof SpectrumPoint || o == null) {
                    if (o != null) {
                        i++;
                        SpectrumPoint p = (SpectrumPoint) o;

                        // 获取或创建波形图数据缓存
                        List<JSONObject> cachedDataPoints = waveformCache.computeIfAbsent(taskId, k -> new ArrayList<>());


                        getPointsByLuminosityMode(luminosityMode, p, xList, yList, taskId, i, cachedDataPoints, line, dataMap);
                        log.info("时间扫描结果：{}", p);
                    }
                } else {
                    log.info("收到了结束相关标志!!!");
                    if (o instanceof EndMarker) {
                        log.info("找到结束标志了，看看是什么结束标志！！！");
                        EndMarker marker = (EndMarker) o;
                        if (marker.getReason() == EndMarker.Reason.NORMAL) {
                            log.info("i={}, total_time={}", i, totalTime);
                            if (i < totalTime) {
                                log.info("时间扫描未成完，继续接收数据");
                                continue;
                            }
                            log.info("时间扫描正常结束标志");
                            log.info("设备 {} 的时间扫描正常结束", deviceSerialNumber);
                            break;
                        } else if (marker.getReason() == EndMarker.Reason.TIME_STOP) {
                            setTaskMeasuring(taskId, false);
                            realTimeEcho(taskId, "停止-时间扫描");
//                            JSONObject jsonObject = new JSONObject();
//                            jsonObject.put("error", "时间扫描-超时");
//                            sendToActiveSession(null, taskId, jsonObject);
                            map.put("line", line);
                            log.info("时间扫描停止标志");
                            return R.fail(map);
                        } else if (marker.getReason() == EndMarker.Reason.UN_NORMAL) {
                            log.info("时间扫描停止标志");
                            setTaskMeasuring(taskId, false);
                            map.put("line", line);
                            return R.fail(map);
                        }
                    } else {
                        log.error("队列中出现未知类" + o);
                    }

                }

            } catch (Exception e) {
                log.error("错误111：", e);
                setTaskMeasuring(taskId, false);
                map.put("line", line);
                return R.fail(map);
            }
        }
        if (halt.get(taskId)) {
            map.put("line", line);
            return R.fail(map);
        }
        // 最终分析结果
        try {
            if (!xList.isEmpty() && !yList.isEmpty()) {
                List<BigDecimal> bigDecimals = doubleListToBigDecimalList(xList);
                List<BigDecimal> analysisResult = findMaxOffsetUVTestTool.calcResult(bigDecimals, yList, null);
                if (CollectionUtils.isNotEmpty(analysisResult)) {
                    for (BigDecimal value : analysisResult) {
                        JSONObject resultObj = new JSONObject();
                        resultObj.put("peak", value.toString());
                        res.add(resultObj);
                    }
                }
            }
        } catch (
                Exception e) {
            log.warn("时间测量分析出错！", e);
            JSONObject errorObj = new JSONObject();
            errorObj.put("error", "时间测量分析出错！");
            sendToActiveSession(null, taskId, errorObj);
            setTaskMeasuring(taskId, false);
            map.put("res", res);
            map.put("line", line);
            return R.fail(map);
        }

        map.put("line", line);
        map.put("res", res);
        return R.ok(map);
    }


    /**
     * 计算时间扫描的总点数
     * 公式：总点数 = 总时间 / 时间间隔
     *
     * @param totalTime 总时间（秒）
     * @param timeInterval 时间间隔（秒）
     * @return 总点数
     */
    private int calculateTimeScanPoints(Double totalTime, Double timeInterval) {
        if (totalTime == null || timeInterval == null || timeInterval <= 0) {
            log.warn("时间参数无效：totalTime={}, timeInterval={}", totalTime, timeInterval);
            return 0;
        }

        // 计算点数：总时间 / 时间间隔
        // 例如：总时间 60 秒，间隔 1 秒，则点数 = 60/1 = 60 个点
        int totalPoints = (int) Math.round(totalTime / timeInterval);

        return totalPoints;
    }

    private R<Map<String, List<JSONObject>>> intensityScan(String taskId, Map<String, Object> dataMap, String deviceSerialNumber, boolean isMultiProductPool, String itemId, Integer d) throws BusinessException {
        //光度模式
        String luminosityMode = MapUtils.getString(dataMap, LUMINOSITY_MODE.getDescription());
        //增益
        Integer gain = MapUtils.getInteger(dataMap, GAIN.getDescription());
        //狭缝，光谱带宽
        Double narrow = MapUtils.getDouble(dataMap, SPECTRAL_BANDWIDTH.getDescription());
        //响应时间
        Double responseTime = MapUtils.getDouble(dataMap, RESPONSE_TIME.getDescription());
        //特征值
        String characteristic = MapUtils.getString(dataMap, CHARACTERISTIC.getDescription());
        //灯状态，灯位置
        Integer lampStatus = null;
        try {
            lampStatus = Integer.valueOf(Objects.requireNonNull(EnergyLampEnums.getIdentityByName(MapUtils.getString(dataMap, LAMP_STATUS.getDescription()))));
        } catch (Exception e) {
            log.warn("灯位置为空！");
        }
        Map<String, List<JSONObject>> map = new HashMap<>();
        map.put("line", null);
        ArrayList<JSONObject> res = new ArrayList<>();
        Integer photoMode = null;
        try {
            photoMode = Integer.valueOf(Objects.requireNonNull(WaveLengthEnum.getCode(luminosityMode)));
        } catch (Exception e) {
            log.warn("光度模式为空！");
        }
        R<OperationResultEntity> operation3 = new R<>();
        if (d == 0) {
            WaveLengthEnum anEnum = WaveLengthEnum.getByName(luminosityMode);
            if ((anEnum == abs || anEnum == tPercent)) {

                if (isMultiProductPool) {
                    realTimeEcho(taskId, "切换样品池到黑挡块");
                    //切换样品池到黑挡块
                    log.info("切换样品池到黑挡块>>>>>>>>>>>>>>>>>>>>");
                    DevOperationInputParam operationInputParam = new DevOperationInputParam();
                    operationInputParam.setDeviceSerialNumber(deviceSerialNumber);
                    operationInputParam.setOperationType(OperationTypeEnums.SetParam.getType());
                    SetParam sampleParam = new SetParam();
                    Integer position = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.BLACK_BLOCK);
                    //设置样品池位置
                    sampleParam.setCellPos(position);
                    operationInputParam.setParam(sampleParam);

                    operation3 = devOperationUtilService.operation(operationInputParam);
                    if (operation3.getSuccess()) {
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("currentPoolNum", position);
                        sendToActiveSession(null, taskId, jsonObject1);
                        realTimeEcho(taskId, "切换样品池到黑挡块成功");
                    }
                } else {
                    operation3.setSuccess(true);
                    if (halt.get(taskId)) {
                        return R.fail(map);
                    }
                    promptUserToPlaceMaterial(taskId, "黑挡块");
                    if (halt.get(taskId)) {
                        return R.fail(map);
                    }
                }
                if (operation3.getSuccess()) {
                    log.info("切换样品池到黑挡块成功>>>>>>>>>>>>>>>>");
                } else {
                    log.warn("切换样品池到黑挡块失败！");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", operation3.getMessage());
                    sendToActiveSession(null, taskId, jsonObject);
                    setTaskMeasuring(taskId, false);
//            stopTask(taskId);
                    realTimeEcho(taskId, "切换样品池到黑挡块失败");
                    return R.fail(map);
                }

                if (org.apache.commons.lang3.StringUtils.equalsIgnoreCase(String.valueOf(taskAndVersionMap.get(taskId)), "T800")) {
                    String[] parts = characteristic.split("[,，]");

                    // 解析特征波长并找出最大最小值
                    List<Double> wavelengths = new ArrayList<>();
                    for (String part : parts) {
                        try {
                            double wl = Double.parseDouble(part.trim());
                            wavelengths.add(wl);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析波长值：{}", part);
                        }
                    }

                    if (!wavelengths.isEmpty()) {
                        double minWavelength = Collections.min(wavelengths);
                        double maxWavelength = Collections.max(wavelengths);

                        // 最大值加 2，最小值减 2
                        double startWavelength = minWavelength - 2.0;
                        double endWavelength = maxWavelength + 2.0;

                        log.info("计算得到的起始波长：{}, 终止波长：{}", startWavelength, endWavelength);

                        DevOperationInputParam devType = new DevOperationInputParam();
                        devType.setDeviceSerialNumber(deviceSerialNumber);
                        SetParam setParam = new SetParam();
                        //设置起始波长
                        setParam.setStartWl(startWavelength);
                        //设置终止波长
                        setParam.setEndWl(endWavelength);
                        //设置扫描间隔
                        setParam.setScanWlInterval(1.0);
                        devType.setParam(setParam);
                        devType.setOperationType(OperationTypeEnums.SetParam.getType());
                        R<OperationResultEntity> opera =
                                devOperationUtilService.operation(devType);
                    } else {
                        log.error("未找到有效的波长值");
                    }
                }





                log.info("开始暗电流矫正>>>>>>>>>>>>>>>>>>>>");
                realTimeEcho(taskId, "开始暗电流矫正");
                DevOperationInputParam darkParam = new DevOperationInputParam();
//        SetParam setParam1 = new SetParam();
                darkParam.setDeviceSerialNumber(deviceSerialNumber);
//        darkParam.setParam(setParam1);
                darkParam.setCalibrationType(DeviceCalibrationInfoEnum.Dark.getType());
                darkParam.setOperationType(OperationTypeEnums.Calibration.getType());
//        darkParam.setUpdateBy();
                R<OperationResultEntity> operationDark =
                        devOperationUtilService.operation(darkParam);
                log.info("暗电流矫正结果：{}", operationDark);
                if (!operationDark.getSuccess()) {
                    log.warn("暗电流矫正出错！");
                    realTimeEcho(taskId, "暗电流矫正出错：" + operationDark.getMessage());
                    setTaskMeasuring(taskId, false);
//                stopTask(taskId);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", operationDark.getMessage());
                    boolean b = sendToActiveSession(null, taskId, jsonObject);
                    return R.fail(map);
                }
                realTimeEcho(taskId, "暗电流矫正成功");
            }
            log.info("开始设置参数>>>>>>>>>>>>>>>>>>>>");
            DevOperationInputParam inputParam = new DevOperationInputParam();
            inputParam.setDeviceSerialNumber(deviceSerialNumber);
            inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
            SetParam param = new SetParam();

            param.setPhotoMode(photoMode);
            param.setResponseTime(responseTime);

            param.setGain(gain);

            param.setLampPos(lampStatus);

            param.setSlit1(narrow);
            inputParam.setParam(param);

            realTimeEcho(taskId, "开始设置参数");
            log.info("开始设置参数  " + inputParam.toString());
            //设置参数
            R<OperationResultEntity> operation = devOperationUtilService.operation(inputParam);
            if (!operation.getSuccess()) {
                log.warn("参数设置出错！");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", operation.getMessage());
                sendToActiveSession(null, taskId, jsonObject);
                setTaskMeasuring(taskId, false);
                return R.fail(map);
            }

            realTimeEcho(taskId, "设置参数成功");

            R<OperationResultEntity> operation5 = new R<>();
            if (isMultiProductPool) {
                realTimeEcho(taskId, "切换样品池到校零物质");
                log.info("切换样品池到校零物质>>>>>>>>>>>>>>>>>>>>");
                DevOperationInputParam devOperationInputParam = new DevOperationInputParam();
                devOperationInputParam.setDeviceSerialNumber(deviceSerialNumber);
                devOperationInputParam.setOperationType(OperationTypeEnums.SetParam.getType());
                SetParam setParam = new SetParam();
                Integer actionPosition = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.ZERO);
                //设置样品池位置
                setParam.setCellPos(actionPosition);
                devOperationInputParam.setParam(setParam);

                operation5 = devOperationUtilService.operation(devOperationInputParam);
                if (operation5.getSuccess()) {
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("currentPoolNum", actionPosition);
                    sendToActiveSession(null, taskId, jsonObject1);
                }
            } else {
                operation5.setSuccess(true);
                List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
                //获取当前检测项的所有标准物
                List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(collect)) {
                    String standardName = "";
                    for (TaskExecutionDetail detail : collect) {
                        if (detail.getStandardType().equals(ZERO.getCode())) {

                            CalStdCats cats = calStdCatsService.getById(detail.getStandardId());
                            if (cats == null) {
                                CalArtifacts calArtifacts = calArtifactsService.getById(detail.getStandardId());
                                standardName = calArtifacts.getName();
                            } else {
                                standardName = cats.getName();
                            }
                        }
                    }
                    if (halt.get(taskId)) {
                        return R.fail(map);
                    }
                    promptUserToPlaceMaterial(taskId, standardName);
                    if (halt.get(taskId)) {
                        return R.fail(map);
                    }
                } else {
                    log.warn("当前检测项没有标准物");
                }
            }
            if (operation5.getSuccess()) {
                realTimeEcho(taskId, "切换样品池到校零物质");
                log.info("切换样品池到校零物质成功>>>>>>>>>>>>>>>>");
            } else {
                log.warn("切换样品池到校零物质失败！");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "切换样品池到校零物质失败！");
                sendToActiveSession(null, taskId, jsonObject);
                setTaskMeasuring(taskId, false);
                return R.fail(map);
            }

            String[] parts = characteristic.split("[,，]");
            List<String> waveLength = Arrays.stream(parts)
                    .filter(part -> part != null && !part.trim().isEmpty())
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(waveLength)) {
                inputParam.setWavelength(waveLength);
            }
            inputParam.setOperationType(OperationTypeEnums.Zero.getType());
            realTimeEcho(taskId, "开始校零");
            log.info("校零入参：" + inputParam);
            //校零
            R<OperationResultEntity> zero = devOperationUtilService.operation(inputParam);
            if (zero.getSuccess()) {
                realTimeEcho(taskId, "校零成功");
                this.zeroList = zero.getData().getZeroResult();
            } else {
                return R.fail(map);
            }
        }

        R<OperationResultEntity> operation6 = new R<>();
        if (isMultiProductPool) {
            realTimeEcho(taskId, "切换样品池到测量物质");

            log.info("切换样品池到测量物质>>>>>>>>>>>>>>>>>>>>");
            DevOperationInputParam param2 = new DevOperationInputParam();
            param2.setDeviceSerialNumber(deviceSerialNumber);
            param2.setOperationType(OperationTypeEnums.SetParam.getType());
            SetParam setParam3 = new SetParam();
            Integer actionPosition1 = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.MEASURE);
            //设置样品池位置
            setParam3.setCellPos(actionPosition1);
            param2.setParam(setParam3);

            operation6 = devOperationUtilService.operation(param2);
            if (operation6.getSuccess()) {
                JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("currentPoolNum", actionPosition1);
                sendToActiveSession(null, taskId, jsonObject1);
            }
        } else {
            operation6.setSuccess(true);
            List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
            //获取当前检测项的所有标准物
            List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(collect)) {
                String standardName = "";
                for (TaskExecutionDetail detail : collect) {
                    if (detail.getStandardType().equals(MEASUREMENT.getCode())) {

                        CalStdCats cats = calStdCatsService.getById(detail.getStandardId());
                        if (cats == null) {
                            CalArtifacts calArtifacts = calArtifactsService.getById(detail.getStandardId());
                            standardName = calArtifacts.getName();
                        } else {
                            standardName = cats.getName();
                        }
                    }
                }
                if (halt.get(taskId)) {
                    return R.fail(map);
                }
                promptUserToPlaceMaterial(taskId, standardName);
                if (halt.get(taskId)) {
                    return R.fail(map);
                }
            } else {
                log.warn("当前检测项没有标准物");
            }
        }
        if (operation6.getSuccess()) {
            realTimeEcho(taskId, "切换样品池到测量物质成功");
            log.info("切换样品池到测量物质成功>>>>>>>>>>>>>>>>");
        } else {
            log.warn("切换样品池到测量物质失败！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", "切换样品池到测量物质失败！");
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            return R.fail(map);
        }

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.Measure.getType());
        String[] parts = characteristic.split("[,，]");
        List<String> waveLength = Arrays.stream(parts)
                .filter(part -> part != null && !part.trim().isEmpty())
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(waveLength)) {
            inputParam.setWavelength(waveLength);
        }
        LocalDateTime startTime = LocalDateTime.now();
//        realTimeEcho(taskId, "开始测量");
        R<OperationResultEntity> measure = devOperationUtilService.operation(inputParam);
        if (!measure.getSuccess()) {
            log.warn("测量出错！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", measure.getMessage());
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            return R.fail(map);
        }
        log.info("光度测量完成：" + measure);
        // ... existing code ...

        if (photoMode != null) {
            List<ChangeParam> measureList = measure.getData().getOriginalMeasureResult();

            // 替换pairByWavelength的实现，使用Map直接查找避免配对依赖
            Map<String, ChangeParam> zeroMap = new HashMap<>();
            if (zeroList != null) {
                for (ChangeParam zeroParam : zeroList) {
                    zeroMap.put(zeroParam.getWl(), zeroParam);
                }
            }

            Map<String, ChangeParam> measureMap = new HashMap<>();
            if (measureList != null) {
                for (ChangeParam measureParam : measureList) {
                    measureMap.put(measureParam.getWl(), measureParam);
                }
            }

            // 获取所有唯一的波长
            Set<String> allWavelengths = new HashSet<>();
            allWavelengths.addAll(zeroMap.keySet());
            allWavelengths.addAll(measureMap.keySet());

            ArrayList<String> list = new ArrayList<>();

            if (!allWavelengths.isEmpty()) {
                Integer finalPhotoMode = photoMode;
                try {
                    // 遍历所有波长，直接从Map中获取对应的数据
                    for (String wavelength : allWavelengths) {
                        ChangeParam zeroParam = zeroMap.get(wavelength);
                        ChangeParam measureParam = measureMap.get(wavelength);

                        // 只有当测量值存在时才进行计算
                        if (measureParam != null) {
                            SpectrumPoint point = new SpectrumPoint(Double.parseDouble(wavelength));
                            point.setZeroRsd(zeroParam);  // 可能为null，calculator需要处理
                            point.setMeasureRsd(measureParam);

                            SpectrumCalcResult result = calculator.calculate(finalPhotoMode, point, SpectrumSourceType.SINGLE, deviceSerialNumber);

                            WaveLengthEnum anEnum = WaveLengthEnum.getByName(luminosityMode);
                            if (anEnum != null && result != null) {
                                JSONObject object = new JSONObject();
                                switch (anEnum) {
                                    case abs:
                                        String abs1 = result.getAbs();
                                        //abs1 保留五位小数
                                        abs1 = new BigDecimal(abs1).setScale(5, RoundingMode.HALF_UP).toString();

                                        object.put("peak", abs1);
                                        res.add(object);
                                        break;
                                    case tPercent:
                                        String tPercent1 = result.getTPercent();
                                        tPercent1 = new BigDecimal(tPercent1).setScale(5, RoundingMode.HALF_UP).toString();

                                        object.put("peak", tPercent1);
                                        res.add(object);
                                        break;
                                    case es:
                                        String es = result.getEs();
                                        es = new BigDecimal(es).setScale(5, RoundingMode.HALF_UP).toString();

                                        object.put("peak", es);
                                        res.add(object);
                                        break;
                                    case er:
                                        String er = result.getEr();
                                        er = new BigDecimal(er).setScale(5, RoundingMode.HALF_UP).toString();

                                        object.put("peak", er);
                                        res.add(object);
                                        break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("光度测量出错！");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", "光度测量出错！");
                    sendToActiveSession(null, taskId, jsonObject);
                    setTaskMeasuring(taskId, false);
                    return R.fail(map);
                }
            }
        }

// ... existing code ...

// 删除原来的pairByWavelength方法，因为不再需要
// private Map<String, Pair<ChangeParam, ChangeParam>> pairByWavelength(List<ChangeParam> zeroList, List<ChangeParam> measureList) {
//     // 原来的实现...
// }


        setTaskMeasuring(taskId, false);
        map.put("res", res);
        return R.ok(map);
    }


//    private boolean sendToActiveSession(WebSocketSession originalSession, String taskId, JSONObject payload) {
//        log.debug("准备发送消息到 taskId: {}, 消息内容: {}", taskId, payload.toJSONString());
//
//        try {
//            // 首先尝试使用原始session
//            if (originalSession != null && isSessionValid(originalSession)) {
//                try {
//                    originalSession.sendMessage(new TextMessage(payload.toJSONString()));
//                    log.info("通过原始session发送消息成功: taskId={}, 消息类型={}", taskId, payload.get("type"));
//                    return true;
//                } catch (IOException e) {
//                    log.warn("原始session发送失败: {}", e.getMessage());
//                }
//            }
//
//            // 尝试使用sessionTaskMap中的session
//            WebSocketSession mappedSession = sessionTaskMap.get(taskId);
//            if (mappedSession != null && isSessionValid(mappedSession)) {
//                try {
//                    mappedSession.sendMessage(new TextMessage(payload.toJSONString()));
//                    log.info("通过映射session发送消息成功: taskId={}, 消息类型={}", taskId, payload.get("type"));
//                    return true;
//                } catch (IOException e) {
//                    log.warn("映射session发送失败: {}", e.getMessage());
//                    // 不移除session，因为可能是临时网络问题
//                }
//            }
//
//            log.warn("所有发送方式都失败: taskId={}", taskId);
//            return false;
//        } catch (Exception e) {
//            log.error("发送消息时发生意外异常: {}", e);
//            return false;
//        }
//    }


    /**
     * 检查WebSocketSession是否有效
     */
    private boolean isSessionValid(WebSocketSession session) {
        return session != null && session.isOpen();
    }



    private static final int DEFAULT_DECIMAL_PLACES = 4;


    private void calculateFormulas(Workbook workbook) {
        try {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (int i = 0; i < 3; i++) {
                evaluator.evaluateAll();
            }

            workbook.setForceFormulaRecalculation(true);
        } catch (Exception e) {
            log.error("Formula calculation error", e);
        }
    }


    private Object getCellValue(Cell cell, FormulaEvaluator evaluator) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return formatValue(cell.getNumericCellValue());
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    CellValue cellValue = evaluator.evaluate(cell);
                    if (cellValue == null) {
                        return "";
                    }

                    switch (cellValue.getCellType()) {
                        case NUMERIC:
                            return formatValue(cellValue.getNumberValue());
                        case STRING:
                            return cellValue.getStringValue();
                        case BOOLEAN:
                            return cellValue.getBooleanValue();
                        case ERROR:
                            return "#ERROR";
                        default:
                            return "";
                    }
                } catch (Exception e) {
                    try {
                        return getFallbackCellValue(cell);
                    } catch (Exception ex) {
                        return "#ERROR";
                    }
                }
            default:
                return "";
        }
    }

    private String formatValue(double value) {
        return String.format("%." + DEFAULT_DECIMAL_PLACES + "f", value);
    }





    private boolean shouldCalculateAtLevel(Cell cell, int level) {
        String formula = cell.getCellFormula();

        switch (level) {
            case 1: // 基础层：简单计算公式
                return isBasicFormula(formula);
            case 2: // 中间层：依赖基础层的公式
                return isIntermediateFormula(formula);
            case 3: // 顶层：复杂判定公式
                return isComplexJudgmentFormula(formula);
            default:
                return true;
        }
    }

    private boolean isBasicFormula(String formula) {
        // 基础公式：不包含其他公式计算结果的引用
        return formula.contains("AVERAGE") || formula.contains("SUM") ||
                formula.contains("MAX") || formula.contains("MIN");
    }

    private boolean isIntermediateFormula(String formula) {
        // 中间层公式：包含基础计算结果但不包含判定结果
        return (formula.contains("ABS") || formula.contains("ROUND")) &&
                !formula.contains("IF(") && !formula.startsWith("IF");
    }

    private boolean isComplexJudgmentFormula(String formula) {
        // 复杂判定公式：主要是IF嵌套判断
        return formula.startsWith("IF(") || formula.contains("IF(") && formula.contains("AND(");
    }


    /**
     * 更新Workbook中的单元格
     * 用来给特征值检测结果赋值
     */
    private void updateWorkbookCell(Workbook workbook, CellUpdDto cellUpdDto) {
        Sheet sheet = workbook.getSheet(cellUpdDto.getSheetName());

        /*// 使用新方法更新特定单元格
templateService.updateCellValue(sheet, excelCellLocator, excelCellEditor,
    "测量值 (nm)", "钬溶液", "536.8", "123.45");
*/
        if (cellUpdDto.getIsMultiple()) {
            // 先更新单元格值
            templateService.updateCellValue(sheet, excelCellLocator, excelCellEditor,
                    cellUpdDto.getValueType(), cellUpdDto.getProjectName(),
                    cellUpdDto.getFeatureValue(), cellUpdDto.getCellValue(), cellUpdDto.getPosition());

        } else {
            templateService.updateCellValue(sheet, excelCellLocator, excelCellEditor,
                    cellUpdDto.getValueType(), cellUpdDto.getProjectName(), cellUpdDto.getPosition(), cellUpdDto.getCellValue());

        }

        // 计算所有公式
        calculateFormulas(workbook);
    }

    private void updateWorkbookCells(Workbook workbook, CellUpdDto cellUpdDto) {
        // 在updateWorkbookCells方法开始处添加空值检查
        if (workbook == null) {
            log.error("Workbook 为空");
            return;
        }
        Sheet sheet = workbook.getSheet(cellUpdDto.getSheetName());

        // 先更新单元格值
        templateService.updateCellValues(sheet, excelCellLocator, excelCellEditor,
                cellUpdDto.getValueType(), cellUpdDto.getProjectName(),
                cellUpdDto.getCellValue());

        // 计算所有公式
        calculateFormulas(workbook);
    }


    /**
     * 发送Sheet更新数据（结构化传输）
     */
    private void sendSheetUpdate(WebSocketSession session, Sheet sheet, String taskId) throws IOException {
        log.debug("推送Sheet更新数据");
        Workbook workbook = sheet.getWorkbook();

        // 构造完整的Sheet结构化数据
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("sheetName", sheet.getSheetName());
        updateData.put("sheetData", extractSheetData(sheet, workbook)); // 传递workbook用于公式计算

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sheetName", sheet.getSheetName());
        jsonObject.put("sheetData", extractSheetData(sheet, workbook)); // 传递workbook用于公式计算

        sendToActiveSession(null, taskId, jsonObject);
//        // 首先尝试使用提供的session
//        if (session != null && isSessionValid(session)) {
//            log.debug("推送Sheet更新数据，这个session还活着{}", session.getUri());
//            try {
//                session.sendMessage(new TextMessage(jsonData));
//            } catch (IOException e) {
//                log.warn("推送Sheet更新数据到提供的session失败: {}", e.getMessage());
//            }
//        } else {
//            // 使用sessionTaskMap中的session
//            WebSocketSession mappedSession = sessionTaskMap.get(taskId);
//            if (mappedSession != null && isSessionValid(mappedSession)) {
//                log.debug("推送Sheet更新数据，使用sessionTaskMap中的session{}", mappedSession.getUri());
//                try {
//                    mappedSession.sendMessage(new TextMessage(jsonData));
//                } catch (IOException e) {
//                    log.warn("推送Sheet更新数据到mappedSession失败: {}", e.getMessage());
//                    // 移除无效的session
//                    sessionTaskMap.remove(taskId, mappedSession);
//                }
//            } else {
//                log.warn("无法推送Sheet更新数据，没有有效的session: taskId={}", taskId);
//            }
//        }
        log.debug("推送Sheet完毕");
    }


//    private SheetDto extractSheetData(Sheet sheet, Workbook workbook) {
//        SheetDto sheetDto = new SheetDto();
//        sheetDto.setSheetName(sheet.getSheetName());
//        sheetDto.setFirstRowNum(sheet.getFirstRowNum());
//        sheetDto.setLastRowNum(sheet.getLastRowNum());
//
//        // 为每次提取数据创建新的公式评估器
//        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
//        evaluator.clearAllCachedResultValues();
//
//        // 提取合并区域
//        List<MergedRegionDto> mergedRegions = new ArrayList<>();
//
//        for (CellRangeAddress region : sheet.getMergedRegions()) {
//            MergedRegionDto merged = new MergedRegionDto();
//            merged.setFirstRow(region.getFirstRow());
//            merged.setLastRow(region.getLastRow());
//            merged.setFirstCol(region.getFirstColumn());
//            merged.setLastCol(region.getLastColumn());
//
//            // 获取左上角单元格的值
//            Row row = sheet.getRow(region.getFirstRow());
//            if (row != null) {
//                Cell cell = row.getCell(region.getFirstColumn());
//                if (cell != null) {
//                    merged.setValue(String.valueOf(getCellValue(cell, evaluator)));
//                }
//            }
//            mergedRegions.add(merged);
//        }
//        sheetDto.setMergedRegions(mergedRegions);
//
//        // 提取普通单元格数据
//        Map<String, Map<String, Object>> rowData = new HashMap<>();
//        for (int rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++) {
//            Row row = sheet.getRow(rowNum);
//            if (row != null) {
//                Map<String, Object> cellData = new HashMap<>();
//                for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
//                    Cell cell = row.getCell(cellNum);
//                    if (cell != null) {
//                        cellData.put(String.valueOf(cellNum), getCellValue(cell, evaluator));
//                    }
//                }
//                if (!cellData.isEmpty()) {
//                    rowData.put(String.valueOf(rowNum), cellData);
//                }
//            }
//        }
//        sheetDto.setRows(rowData);
//
//        return sheetDto;
//    }
private SheetDto extractSheetData(Sheet sheet, Workbook workbook) {
    SheetDto sheetDto = new SheetDto();
    sheetDto.setSheetName(sheet.getSheetName());
    sheetDto.setFirstRowNum(sheet.getFirstRowNum());
    sheetDto.setLastRowNum(sheet.getLastRowNum());

    // 为每次提取数据创建新的公式评估器
    FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
    evaluator.clearAllCachedResultValues();

    // 提取合并区域
    List<MergedRegionDto> mergedRegions = new ArrayList<>();

    for (CellRangeAddress region : sheet.getMergedRegions()) {
        MergedRegionDto merged = new MergedRegionDto();
        merged.setFirstRow(region.getFirstRow());
        merged.setLastRow(region.getLastRow());
        merged.setFirstCol(region.getFirstColumn());
        merged.setLastCol(region.getLastColumn());

        // 获取左上角单元格的值
        Row row = sheet.getRow(region.getFirstRow());
        if (row != null) {
            Cell cell = row.getCell(region.getFirstColumn());
            if (cell != null) {
                merged.setValue(String.valueOf(getCellValue(cell, evaluator)));
            }
        }
        mergedRegions.add(merged);
    }
    sheetDto.setMergedRegions(mergedRegions);

    // 提取普通单元格数据
    Map<String, Map<String, Object>> rowData = new HashMap<>();
    for (int rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++) {
        Row row = sheet.getRow(rowNum);
        if (row != null) {
            Map<String, Object> cellData = new HashMap<>();
            boolean hasValidContent = false;
            for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
                Cell cell = row.getCell(cellNum);
                if (cell != null) {
                    Object value = getCellValue(cell, evaluator);
                    if (value != null && !"".equals(value)) {
                        cellData.put(String.valueOf(cellNum), value);
                        hasValidContent = true;
                    }
                }
            }
            if (hasValidContent && !cellData.isEmpty()) {
                rowData.put(String.valueOf(rowNum), cellData);
            }
        }
    }
    sheetDto.setRows(rowData);

    return sheetDto;
}


//    /**
//     * 获取单元格的值（带公式计算器）
//     */
//    private Object getCellValue(Cell cell, FormulaEvaluator evaluator) {
//        switch (cell.getCellType()) {
//            case STRING:
//                return cell.getStringCellValue();
//            case NUMERIC:
//                return cell.getNumericCellValue();
//            case BOOLEAN:
//                return cell.getBooleanCellValue();
//            case FORMULA:
//                try {
//                    CellValue cellValue = evaluator.evaluate(cell);
//                    if (cellValue == null) {
//                        return "";
//                    }
//
//                    switch (cellValue.getCellType()) {
//                        case NUMERIC:
//                            return cellValue.getNumberValue();
//                        case STRING:
//                            return cellValue.getStringValue();
//                        case BOOLEAN:
//                            return cellValue.getBooleanValue();
//                        case ERROR:
//                            return "#ERROR";
//                        default:
//                            return "";
//                    }
//                } catch (Exception e) {
//                    // 如果公式计算失败，尝试返回缓存值
//                    try {
//                        return getFallbackCellValue(cell);
//                    } catch (Exception ex) {
//                        return "#ERROR";
//                    }
//                }
//            default:
//                return "";
//        }
//    }

    private Object getFallbackCellValue(Cell cell) {
        // 尝试从单元格获取已缓存的值
        switch (cell.getCachedFormulaResultType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                return cell.getStringCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            default:
                return "";
        }
    }


    // 在连接关闭时清理队列

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            String sessionId = session.getId();
            String taskId = getTaskIdFromSession(session);
            String update = getUpdateByFromUrl(session);
            log.info("WebSocket 连接关闭，会话 ID: {}, 任务 ID: {}, 状态：{}", sessionId, taskId, status);

            // 清理会话相关资源
            registeredSessionIds.remove(taskId);

            // 【修改】只移除当前关闭的 session，而不是整个任务的所有 session
            Set<WebSocketSession> sessions = sessionTaskMap.get(taskId);
            if (sessions != null) {
                sessions.remove(session);
                log.info("从任务{}的 session 集合中移除会话{}", taskId, sessionId);

                // 如果是最后一个连接，清理任务相关资源
                if (sessions.isEmpty()) {
                    log.info("任务{}的所有连接已断开", taskId);
                    sessionTaskMap.remove(taskId);  // 此时才移除整个任务的映射

                    // 清理前端地址映射
                    taskToFrontendAddresses.remove(taskId);

                    // 释放设备绑定
                    String boundDevice = taskDeviceBindingMap.remove(taskId);
                    log.info("任务 {} 的所有连接已断开，释放设备绑定：{}", taskId, boundDevice);

                    // 停止心跳任务
                    ScheduledFuture<?> heartbeatTask = heartbeatTasks.remove(taskId);
                    if (heartbeatTask != null && !heartbeatTask.isDone()) {
                        heartbeatTask.cancel(false);
                    }
                    // 清理暂停控制
                    pauseControls.remove(taskId);
                    taskFutures.remove(taskId);
                    currentMeasureItemMap.remove(taskId);
                    measurementStatusMap.remove(taskId);
                    halt.remove(taskId);

                    // 释放编辑锁
                    workbookCacheManager.releaseEditLock(taskId, update);
                    log.info("任务{}的编辑锁已释放", taskId);
                }
            } else {
                log.warn("任务{}的 session 集合不存在，可能已被清理", taskId);
            }

            // 清理会话信息
            sessionMap.remove(sessionId);
            operationCount.remove(sessionId);
            lastActivityTime.remove(sessionId);

            log.info("WebSocket 连接关闭完成，会话 ID: {}", sessionId);
            super.afterConnectionClosed(session, status);
        } catch (Exception e) {
            log.error("WebSocket 连接关闭异常", e);
        }
    }


    // 在停止任务时也清理队列
    public void stopTask(String taskId) throws BusinessException {
        log.info("开始停止任务: {}", taskId);

        try {
            // 唤醒暂停的任务
            //是否停止设置为true
            this.halt.put(taskId, true);
            //如果是单样品池-单项测量  会唤醒暂停的任务，唤醒后检测到停止为true后跳出
            TaskPauseControl control = pauseControls.get(taskId);
            if (control != null) {
                control.setPaused(false);
                control.semaphore.release();
            }
            // 取消任务执行
//            Future<?> future = taskFutures.get(taskId);
//            if (future != null && !future.isDone()) {
////                boolean cancel = future.cancel(false);
////                log.info("任务已取消: {}, 任务ID: {}", cancel, taskId);
//                taskFutures.remove(taskId);
//            }

            // 清理消息队列
            BlockingQueue<QueuedMessage> queue = messageQueues.remove(taskId);
            if (queue != null) {
                queue.clear();
            }

            // 清理处理标志
            queueProcessingFlags.remove(taskId);

            // 更新测量状态
//            setTaskMeasuring(taskId, false);

            log.info("任务 {} 已停止", taskId);
        } catch (Exception e) {
            log.error("停止任务 {} 时发生异常", taskId, e);
            throw new BusinessException("停止任务失败: " + e.getMessage());
        }
    }


    private void cancelHeartbeatTask(String sessionId) {
        ScheduledFuture<?> task = heartbeatTasks.remove(sessionId);
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
    }

    @PreDestroy
    public void destroy() {
        // 停止所有心跳任务
        heartbeatTasks.values().forEach(task -> task.cancel(false));
        heartbeatTasks.clear();

        // 停止心跳调度器
        if (!heartbeatScheduler.isShutdown()) {
            try {
                heartbeatScheduler.shutdown();
                if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 清理所有任务相关资源
        taskFutures.values().forEach(future -> {
            if (!future.isDone()) {
                future.cancel(true);
            }
        });
        taskFutures.clear();

        // 清理暂停控制
        pauseControls.clear();

        // 清理会话映射
        sessionTaskMap.clear();

        // 清理其他缓存
        waveformCache.clear();
        lastActivityTime.clear();
    }

    /**
     * 从会话中提取文件名
     */
    private String getFileNameFromSession(WebSocketSession session) {
        // 从URL参数中获取文件名
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("fileName=")) {
                    return param.substring(9); // "fileName=".length()
                }
            }
        }
        return null;
    }
}
