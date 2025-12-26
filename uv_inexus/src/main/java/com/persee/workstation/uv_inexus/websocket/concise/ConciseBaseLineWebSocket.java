/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.concise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.LinearSegment;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureMethodService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskAttachmentService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * WebSocket处理器类，继承自TextWebSocketHandler
 * 用于处理WebSocket连接的建立、消息接收和连接关闭等事件
 */
@Component
public class ConciseBaseLineWebSocket extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ConciseBaseLineWebSocket.class);

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();

    // 用于存储数据点索引（适用于所有扫描模式）
    private static final Map<String, Integer> dataPointIndexMap = new ConcurrentHashMap<>();

    // 会话状态管理
    private static final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    // WebSocket消息发送队列，避免并发发送冲突
    private static final Map<String, Object> sessionSendLocks = new ConcurrentHashMap<>();

    // 设备占用锁，确保同一设备不会被多个会话同时占用
    private static final Map<String, String> deviceOccupiedBy = new ConcurrentHashMap<>();

    private static ChangeParam lastChangeParam;

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

    @Autowired
    private FormulaUtil formulaUtil;

    /**
     * 自定义异常类，用于区分用户主动停止和其他异常
     */
    private static class UserInterruptedException extends RuntimeException {
        public UserInterruptedException(String message) {
            super(message);
        }
    }

    /**
     * 会话状态管理类
     */
    private static class SessionState {
        private final AtomicBoolean deviceOccupied = new AtomicBoolean(false);
        private final AtomicBoolean measurementStarted = new AtomicBoolean(false);
        private final AtomicBoolean stopCommandSent = new AtomicBoolean(false);
        private final AtomicBoolean stopCommandSent2 = new AtomicBoolean(false);
        private final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);
        private final AtomicBoolean sessionClosing = new AtomicBoolean(false);
        private final AtomicBoolean dataReceivingThreadStopped = new AtomicBoolean(false);
        private String deviceSerialNumber;
        private String ip;

        public void reset() {
            deviceOccupied.set(false);
            measurementStarted.set(false);
            stopCommandSent.set(false);
            stopCommandSent2.set(false);
            cleanupInProgress.set(false);
            sessionClosing.set(false);
            dataReceivingThreadStopped.set(false);
            deviceSerialNumber = null;
            ip = null;
        }
    }

    /**
     * 当WebSocket连接建立后调用此方法
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("【连接建立】客户端进入: {} 时间: {}", sessionId, LocalDateTime.now());
        sessions.put(sessionId, session);

        // 初始化会话状态
        SessionState state = new SessionState();
        sessionStates.put(sessionId, state);

        // 初始化发送锁
        sessionSendLocks.put(sessionId, new Object());

        // 往前端推送数据的线程
        Thread taskThread = new Thread(() -> {
            log.info("【任务开始】创建任务线程, session: {}", sessionId);
            try {
                runBusinessLogic(session);
            } catch (UserInterruptedException e) {
                log.info("【用户停止】用户主动停止任务, session: {}", sessionId);
                // 不在这里发送停止成功，由 handleStopRequest 统一发送
            } catch (Exception e) {
                log.error("【任务异常】基线任务执行异常, session: {}", sessionId, e);
                sendMessageSafe(session, "执行异常: " + e.getMessage());
            } finally {
                log.info("【任务结束】任务线程执行完成, session: {}", sessionId);
                // 确保所有资源都被清理
                cleanupResources(session);
            }
        });

        taskThread.setName("Baseline-Task-" + sessionId);
        taskThreads.put(sessionId, taskThread);
        taskThread.start();
        log.info("【任务启动】任务线程已启动, session: {}", sessionId);
    }

    /**
     * 处理接收到的文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        log.info("【收到消息】session: {} -> {}", sessionId, payload);

        if ("close".equals(payload)) {
            log.info("【处理停止】收到停止请求, session: {}", sessionId);
            handleStopRequest(session);
        } else {
            log.warn("【未知消息】未知消息类型, session: {}, message: {}", sessionId, payload);
        }
    }

    /**
     * 处理停止请求
     */
    private void handleStopRequest(WebSocketSession session) {
        String sessionId = session.getId();
        SessionState state = sessionStates.get(sessionId);
        if (state == null) {
            log.warn("【停止失败】会话状态不存在, session: {}", sessionId);
            return;
        }

        try {
            log.info("【处理停止】开始处理停止请求, session: {}", sessionId);

            // 标记停止状态
            state.stopCommandSent.set(true);

            // 发送停止命令到设备
            sendStopCommandToDevice(session);

            // 停止数据接收线程（关键：先停止接收数据）
            stopDataReceivingThread();

            // 中断任务线程
            Thread thread = taskThreads.get(sessionId);
            if (thread != null && thread.isAlive()) {
                log.info("【中断线程】中断任务线程, session: {}", sessionId);
                thread.interrupt();
            } else {
                log.info("【线程状态】任务线程不存在或未运行, session: {}", sessionId);
            }
            // 发送停止成功消息
            log.info("【发送停止成功】向前端发送停止成功消息, session: {}", sessionId);
            sendMessageSafe(session, "停止成功");

        } catch (Exception e) {
            log.error("【停止异常】处理停止消息异常, session: {}", sessionId, e);
        }
    }

    /**
     * 发送停止命令到设备
     */
    private void sendStopCommandToDevice(WebSocketSession session) {
        String sessionId = session.getId();
        SessionState state = sessionStates.get(sessionId);
        if (state == null) {
            return;
        }

        // 只有在测量已开始且停止命令未发送过的情况下才发送停止命令
        if (state.measurementStarted.get() && state.stopCommandSent.getAndSet(true) && !state.stopCommandSent2.getAndSet(true)) {
            if (state.ip != null) {
                try {
                    log.info("【发送设备停止】向设备发送停止命令, session: {}, ip: {}", sessionId, state.ip);
                    R<String> stopResult = devMessageUtil.stop(buildCommand(SetParamEnum.Stop, new Object[]{}), state.ip);
                    if (stopResult.getSuccess()) {
                        log.info("【设备停止成功】设备停止命令发送成功, session: {}", sessionId);
                    } else {
                        log.error("【设备停止失败】设备停止命令发送失败, session: {}", sessionId);
                    }
                } catch (Exception e) {
                    log.error("【发送停止异常】发送停止命令异常, session: {}", sessionId, e);
                }
            } else {
                log.warn("【无IP地址】无法发送停止命令，IP地址为空, session: {}", sessionId);
            }
        } else {
            log.info("【无需停止】测量未开始或停止命令已发送, session: {}, measurementStarted: {}, stopCommandSent: {}",
                    sessionId, state.measurementStarted.get(), state.stopCommandSent.get());
        }
    }

    /**
     * 停止数据接收线程
     */
    private void stopDataReceivingThread() {
        try {
            Thread dataReceivingThread = Constants.sendMeasureThread;
            if (dataReceivingThread != null && dataReceivingThread.isAlive()) {
                log.info("【停止数据线程】开始停止数据接收线程");

                // 中断数据接收线程
                dataReceivingThread.interrupt();

                // 重试直到线程停止或者超时
                long startTime = System.currentTimeMillis();
                while (dataReceivingThread.isAlive() && (System.currentTimeMillis() - startTime) < 5000) {
                    try {
                        dataReceivingThread.join(100);  // 等待100ms，重试直到线程停止或超时
                    } catch (InterruptedException e) {
                        log.warn("【等待线程中断】等待数据接收线程结束被中断");
                        Thread.currentThread().interrupt();
                    }
                }

                if (dataReceivingThread.isAlive()) {
                    log.warn("数据接收线程未在超时时间内停止，强制停止");
                } else {
                    log.info("【数据线程停止】数据接收线程已停止");
                }
            } else {
                log.info("【数据线程状态】数据接收线程不存在或未运行");
            }

            // 清空线程引用
            Constants.sendMeasureThread = null;
            log.info("【数据线程清理】数据接收线程引用已清理");

        } catch (Exception e) {
            log.error("【停止数据线程异常】停止数据接收线程异常", e);
        }
    }

    /**
     * 连接异常时触发
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = session.getId();
        log.error("【连接异常】session: {}, 异常: {}", sessionId, exception.getMessage(), exception);
        cleanupResources(session);
    }

    /**
     * 重写afterConnectionClosed方法，用于处理WebSocket连接关闭后的操作
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        log.info("【连接关闭】客户端退出: {} 时间: {} 原因: {}", sessionId, LocalDateTime.now(), status);
    }

    /**
     * 主要的业务逻辑
     */
    private void runBusinessLogic(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("【业务逻辑开始】session: {}", sessionId);

        // 获取参数
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String methodId = getQueryParam(query, "methodId");
        String deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");
        lastChangeParam=null;

        log.info("【参数获取】session: {}, taskId: {}, methodId: {}, deviceSerialNumber: {}",
                sessionId, taskId, methodId, deviceSerialNumber);

        // 参数验证
        if (!validateParameters(session, methodId, deviceSerialNumber)) {
            return;
        }

        // 占用设备
        if (!occupyDevice(session, deviceSerialNumber)) {
            return;
        }

        try {
            // 设置参数
            if (!setupParameters(session, methodId, deviceSerialNumber)) {
                return;
            }

            // 获取方法参数
            FMeasureMethod method = tMeasureMethodService.getById(methodId);
            if (method == null) {
                sendAndClose(session, "方法不存在");
                return;
            }
            // 将方法中的参数转换为对象
            FSpectrumParameter param = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

            // 开始测量
            if (!startMeasurement(session, method, deviceSerialNumber)) {
                return;
            }

            // 收集数据
            List<ChangeParam> dataList = collectBaselineData(session, method, deviceSerialNumber);
            if (dataList == null) {
                return; // 数据收集被中断或失败
            }

            // 标记数据收集完成
            SessionState state = sessionStates.get(sessionId);
            if (state != null) {
                log.info("【数据收集完成】数据收集完成, 数据点数: {}, session: {}", dataList.size(), sessionId);
            }

            // 保存任务结果
            if (!saveTaskResult(session, taskId, methodId, deviceSerialNumber, method, dataList)) {
                return;
            }

            log.info("【基线成功】基线测量成功完成, session: {}", sessionId);
            if (!state.stopCommandSent.get()) {
                 try {
                     Thread.sleep(3000);
                     log.info("【等待完成】固定等待3秒完成, session: {}", sessionId);
                 } catch (InterruptedException e) {
                     log.warn("【等待中断】等待被中断, session: {}", sessionId);
                     Thread.currentThread().interrupt();
                 }

                String ip = deviceInfoService.getIp(deviceSerialNumber);
                log.info("【设置波长开始】开始设置波长, session: {}", sessionId);
                double parsed = Double.parseDouble(param.getStartWaveLength());
                Double va = formulaUtil.calculateStep(parsed, deviceSerialNumber);
                Command com = new Command(SetParamEnum.SetWl.getComId(), new Object[]{va});
                devMessageUtil.SetOneCommand(com, ip);
                DetectionConstants.currentWavelength = param.getStartWaveLength();
                log.info("【设置波长结束】结束设置波长, session: {}", sessionId);

                log.info("【零点校零开始】开始进行校零, session: {}", sessionId);
                /// 基线结束之后横插一个校零
                // 进行校零
                Command command = new Command(SetParamEnum.Zero.getComId(), new Object[]{});
                R<ChangeParam> zero = devMessageUtil.zero(command, ip);
                if (!zero.getSuccess()) {
                    sendAndClose(session, zero.getMessage());
                    return;
                }
                ChangeParam zeroData = zero.getData();
                Double v = formulaUtil.calculateWl(Double.parseDouble(zeroData.getWl()), deviceSerialNumber);
                DetectionConstants.zeroResult.put(param.getStartWaveLength(), zero.getData());
                log.info("【零点校零成功】零点校零成功, session: {}", sessionId);
            }

            sendAndClose(session, "基线成功");

        } catch (UserInterruptedException e) {
            // 用户主动停止，不处理，由 handleStopRequest 统一发送停止成功
            log.info("【用户中断】用户主动停止任务, session: {}", sessionId);
        } catch (Exception e) {
            log.error("【业务逻辑异常】基线任务执行异常, session: {}", sessionId, e);
            sendAndClose(session, "基线任务执行异常: " + e.getMessage());
        }
    }

    /**
     * 参数验证
     */
    private boolean validateParameters(WebSocketSession session, String methodId, String deviceSerialNumber) {
        String sessionId = session.getId();
        if (methodId == null || methodId.trim().isEmpty()) {
            log.error("【参数验证失败】方法编号为空, session: {}", sessionId);
            sendClose(session, "方法编号不能为空");
            return false;
        }
        if (deviceSerialNumber == null || deviceSerialNumber.trim().isEmpty()) {
            log.error("【参数验证失败】设备编号为空, session: {}", sessionId);
            sendClose(session, "设备编号不能为空");
            return false;
        }
        log.info("【参数验证成功】参数验证通过, session: {}", sessionId);
        return true;
    }

    /**
     * 占用设备 - 严格版本
     */
    private boolean occupyDevice(WebSocketSession session, String deviceSerialNumber) {
        String sessionId = session.getId();
        SessionState state = sessionStates.get(sessionId);
        if (state == null) {
            log.error("【占用设备失败】会话状态异常, session: {}", sessionId);
            sendClose(session, "会话状态异常");
            return false;
        }

        // 检查设备是否已被其他会话占用
        String occupiedBy = deviceOccupiedBy.get(deviceSerialNumber);
        if (occupiedBy != null && !occupiedBy.equals(sessionId)) {
            log.error("【占用设备失败】设备已被其他会话占用, device: {}, 被占用者: {}, 当前会话: {}",
                    deviceSerialNumber, occupiedBy, sessionId);
            sendClose(session, "设备已被其他会话占用");
            return false;
        }

        if (state.deviceOccupied.get()) {
            log.warn("【设备已占用】设备已被当前会话占用，重复占用请求, session: {}, device: {}", sessionId, deviceSerialNumber);
            return true;
        }

        log.info("【开始占用设备】尝试占用设备, session: {}, device: {}", sessionId, deviceSerialNumber);
        R<Boolean> occupyResult = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);
        if (!occupyResult.getSuccess()) {
            log.error("【占用设备失败】设备占用服务返回失败, session: {}, device: {}, 错误: {}",
                    sessionId, deviceSerialNumber, occupyResult.getMessage());
            sendClose(session, occupyResult.getMessage());
            return false;
        }

        // 标记设备占用
        state.deviceOccupied.set(true);
        state.deviceSerialNumber = deviceSerialNumber;
        deviceOccupiedBy.put(deviceSerialNumber, sessionId);

        log.info("【占用设备成功】设备占用成功, session: {}, device: {}", sessionId, deviceSerialNumber);
        return true;
    }

    /**
     * 设置参数
     */
    private boolean setupParameters(WebSocketSession session, String methodId, String deviceSerialNumber) {
        String sessionId = session.getId();
        try {
            checkInterrupted(session);

            log.info("【开始设置参数】session: {}, methodId: {}, device: {}", sessionId, methodId, deviceSerialNumber);
            Result result = spectrumService.setUpParametersPort(methodId, deviceSerialNumber);
            if (result.getCode() != 200) {
                log.error("【设置参数失败】设置参数服务返回失败, session: {}, code: {}, message: {}",
                        sessionId, result.getCode(), result.getMessage());
                sendAndClose(session, "设置参数失败");
                return false;
            }

            log.info("【设置参数成功】参数设置完成, session: {}", sessionId);
            return true;
        } catch (InterruptedException e) {
            log.info("【设置参数中断】设置参数被中断, session: {}", sessionId);
            throw new UserInterruptedException("用户主动停止");
        }
    }

    /**
     * 开始测量
     */
    private boolean startMeasurement(WebSocketSession session, FMeasureMethod method, String deviceSerialNumber) {
        String sessionId = session.getId();
        try {
            checkInterrupted(session);

            SessionState state = sessionStates.get(sessionId);
            if (state == null) {
                log.error("【开始测量失败】会话状态异常, session: {}", sessionId);
                sendAndClose(session, "会话状态异常");
                return false;
            }

            log.info("【获取设备IP】开始获取设备IP, session: {}, device: {}", sessionId, deviceSerialNumber);
            String ip = deviceInfoService.getIp(deviceSerialNumber);
            if (ip == null) {
                log.error("【获取设备IP失败】无法获取设备IP, session: {}, device: {}", sessionId, deviceSerialNumber);
                sendAndClose(session, "获取设备IP失败");
                return false;
            }

            state.ip = ip;
            log.info("【获取设备IP成功】设备IP: {}, session: {}", ip, sessionId);

            FSpectrumParameter param = JSONObject.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

            // 清空全局数据，确保从干净状态开始
            clearGlobalData();

            // 发送基线命令
            Command command = buildCommand(SetParamEnum.BaseLine, new Object[]{});
            boolean isFastScan = "3".equals(param.getScanSpeed());
            if (isFastScan) {
                command = buildCommand(SetParamEnum.BaseLineFast, new Object[]{});
            }

            dataPointIndexMap.put(sessionId, 0);
            log.info("【开始基线测量】设备: {}, 模式: {}, IP: {}, session: {}",
                    deviceSerialNumber, isFastScan ? "极快扫描" : "普通扫描", ip, sessionId);

            devMessageUtil.spectrumScanGetBaselineRealTime(command, ip);

            // 标记测量已开始
            state.measurementStarted.set(true);
            log.info("【基线测量开始】测量状态已标记为开始, session: {}", sessionId);

            return true;
        } catch (InterruptedException e) {
            log.info("【开始测量中断】开始测量被中断, session: {}", sessionId);
            throw new UserInterruptedException("用户主动停止");
        }
    }

    /**
     * 收集基线数据 - 优化版本，正确处理停止逻辑
     */
    private List<ChangeParam> collectBaselineData(WebSocketSession session, FMeasureMethod method, String deviceSerialNumber) {
        String sessionId = session.getId();
        try {
            log.info("【开始收集数据】开始收集基线数据, session: {}", sessionId);
            List<ChangeParam> dataList = new ArrayList<>();
            FSpectrumParameter param = JSONObject.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
            List<LinearSegment> linearSegment = formulaUtil.getLinearSegment(deviceSerialNumber);

            double wavelengthInterval = Double.parseDouble(param.getWaveLengthInterval());
            double startWl = Double.parseDouble(param.getStartWaveLength());
            double endWl = Double.parseDouble(param.getEndWaveLength());
            boolean isDescending = startWl > endWl;
            Double stopWaveLength = endWl + (wavelengthInterval * 3);

            int expectedPoints = calculateExpectedDataPoints(startWl, endWl, wavelengthInterval);
            log.info("【数据点预期】期望数据点数: {}, 波长范围: {} - {}, 间隔: {}, session: {}",
                    expectedPoints, startWl, endWl, wavelengthInterval, sessionId);

            SessionState state = sessionStates.get(sessionId);
            if (state == null) {
                log.error("【收集数据失败】会话状态异常, session: {}", sessionId);
                sendAndClose(session, "会话状态异常");
                return null;
            }

            int consecutiveEndWavelengthCount = 0;
            final int MAX_CONSECUTIVE_END_WAVELENGTH = 3;
            long startTime = System.currentTimeMillis();
            final long TIMEOUT_MS = 5 * 60 * 1000; // 5分钟超时
            final long NO_DATA_TIMEOUT_MS = 120 * 1000; // 120秒无数据超时（增加以处理换灯暂停）
            // Increased timeout to handle lamp change pauses
            long lastDataTime = System.currentTimeMillis();
            int cont = 0;
            boolean isEndData = false;
            while (true) {
                checkInterrupted(session);
                // 检查无数据超时
                if (System.currentTimeMillis() - lastDataTime > NO_DATA_TIMEOUT_MS) {
                    log.error("【无数据超时】长时间没有收到数据, session: {}, 最后数据时间: {}ms前",
                            sessionId, System.currentTimeMillis() - lastDataTime);
                    sendAndClose(session, "设备无响应");
                    return null;
                }

                // 处理测量数据
                while (!Constants.measurd_data.isEmpty()) {
                    cont++;
                    checkInterrupted(session);
                    ChangeParam changeParam = Constants.measurd_data.remove(0);
                    Double originalWavelength = Double.parseDouble(changeParam.getWl());
                    // 步数转波长
                    Double correctedWavelength = formulaUtil.calculateWavelengthTwo(originalWavelength, linearSegment);
                    changeParam.setWl(correctedWavelength.toString());
                    if (cont<=1){
                        lastChangeParam = changeParam;
                        continue;
                    }
                    // 强制约束波长
                    Double constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                            wavelengthInterval, isDescending, sessionId);

                    while (constrainedWavelength>=correctedWavelength&&!isEndData){
                        if (constrainedWavelength==endWl){
                            isEndData = true;
                        }
                        // 计算拟合数据
                        ChangeParam param1 = calculate(lastChangeParam, changeParam, constrainedWavelength);
                        dataList.add(param1);
                        // 推送单个数据点
                        pushBaselineDataPoint(session, constrainedWavelength);
                        constrainedWavelength = forceConstrainWavelength(correctedWavelength, startWl, endWl,
                                wavelengthInterval, isDescending, sessionId);
                    }
                    if (constrainedWavelength<correctedWavelength&&constrainedWavelength!=endWl){
                        Integer currentIndex = dataPointIndexMap.get(sessionId);
                        dataPointIndexMap.put(sessionId, currentIndex - 1);
                    }
                    lastChangeParam=changeParam;


                    if (stopWaveLength.equals(constrainedWavelength)){
                        log.info("【停止命令不会再发送】session: {}", sessionId);
                        state.stopCommandSent2.set(true);
                    }

                    if (constrainedWavelength != null) {
                        lastDataTime = System.currentTimeMillis();
                        // 检查是否到达结束波长
                        if (isEndWavelengthReached(constrainedWavelength, endWl, isDescending)) {
                            consecutiveEndWavelengthCount++;
                            if (consecutiveEndWavelengthCount >= MAX_CONSECUTIVE_END_WAVELENGTH) {
                                log.info("【结束波长检测】连续检测到结束波长 {} 次，完成数据收集, session: {}",
                                        consecutiveEndWavelengthCount, sessionId);
                                break;
                            }
                        } else {
                            consecutiveEndWavelengthCount = 0;
                        }
                    }
                }

                // 检查测量是否完成（通过检查全局数据中的结束标志）
                if (isMeasurementComplete()) {
                    log.info("【测量完成】检测到测量完成标志, session: {}", sessionId);
                    state.measurementStarted.set(false);
                    break;
                }

                // 检查数据点数是否达到期望
                if (dataList.size() >= expectedPoints) {
                    log.info("【数据点达到】达到期望数据点数: {}, session: {}", expectedPoints, sessionId);
                    state.measurementStarted.set(false);
                    break;
                }

                // 检查是否连续检测到结束波长
                if (consecutiveEndWavelengthCount >= MAX_CONSECUTIVE_END_WAVELENGTH) {
                    log.info("【结束波长完成】通过结束波长检测完成数据收集, session: {}", sessionId);
                    state.measurementStarted.set(false);
                    break;
                }

                // 短暂休眠，减少CPU占用
                try {
                    Thread.sleep(isFastScan(param) ? 2 : 10);
                } catch (InterruptedException e) {
                    log.info("【数据收集中断】数据收集被中断, session: {}", sessionId);
                    throw new UserInterruptedException("用户主动停止");
                }
            }

            // 数据收集完成后的清理工作
            log.info("【数据收集完成】停止数据接收线程并清理数据, session: {}", sessionId);
            stopDataReceivingThread();
            Constants.measurd_data.clear();

            if (dataList.isEmpty()) {
                log.error("【数据为空】基线数据为空，失败, session: {}", sessionId);
                sendAndClose(session, "基线数据为空，失败");
                return null;
            }

            log.info("【数据收集成功】基线数据收集完成, dataList 大小: {}, session: {}", dataList.size(), sessionId);
            return dataList;
        } catch (InterruptedException e) {
            log.info("【数据收集中断】数据收集被中断, session: {}", sessionId);
            throw new UserInterruptedException("用户主动停止");
        }
    }


    /**
     * 根据起始点和终止点，计算指定波长对应的ChangeParam
     *
     * @param start 起始点
     * @param end 终止点
     * @param targetWl 目标波长，支持字符串或数字
     * @return 计算得到的ChangeParam
     */
    public static ChangeParam calculate(ChangeParam start, ChangeParam end, Double targetWl) {

        // 解析参数
        double startWl = Double.parseDouble(start.getWl());
        double endWl = Double.parseDouble(end.getWl());
        int startR = Integer.parseInt(start.getR());
        int endR = Integer.parseInt(end.getR());
        int startS = Integer.parseInt(start.getS());
        int endS = Integer.parseInt(end.getS());
        int dValue = Integer.parseInt(start.getD());

        // 计算拟合曲线
        double slopeR = (double)(endR - startR) / (endWl - startWl);
        double interceptR = startR - slopeR * startWl;

        double slopeS = (double)(endS - startS) / (endWl - startWl);
        double interceptS = startS - slopeS * startWl;

        // 计算r和s值
        int r = (int)Math.round(slopeR * targetWl + interceptR);
        int s = (int)Math.round(slopeS * targetWl + interceptS);

        // 格式化wl字符串
        String wlStr = String.valueOf(targetWl);

        return new ChangeParam(wlStr, wlStr, String.valueOf(r), String.valueOf(s), String.valueOf(dValue));
    }

    /**
     * 保存任务结果
     */
    private boolean saveTaskResult(WebSocketSession session, String taskId, String methodId,
                                   String deviceSerialNumber, FMeasureMethod method, List<ChangeParam> dataList) {
        String sessionId = session.getId();
        try {
            checkInterrupted(session);
            log.info("【开始保存任务】保存任务结果, session: {}", sessionId);

            FMeasureTask task;
            if (taskId != null && !taskId.trim().isEmpty()) {
                task = tMeasureTaskService.getById(taskId);
                if (task == null) {
                    log.error("【保存任务失败】任务不存在, session: {}, taskId: {}", sessionId, taskId);
                    sendAndClose(session, "任务不存在");
                    return false;
                }
            } else {
                task = new FMeasureTask();
                SystemUserManagePO user = systemUserManageDAO
                        .selectOne(new QueryWrapper<SystemUserManagePO>().eq("user_name", method.getMethodCreator()));
                if (user != null) {
                    task.setUserId(user.getId());
                }

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

                log.info("【创建新任务】创建新测量任务, session: {}, taskName: {}", sessionId, task.getTaskName());
                boolean save1 = tMeasureTaskService.save(task);
                if (!save1) {
                    log.error("【保存任务失败】保存任务到数据库失败, session: {}", sessionId);
                    sendAndClose(session, "保存任务失败");
                    return false;
                }

                FMeasureTaskAttachment taskAttachment = new FMeasureTaskAttachment();
                taskAttachment.setTaskId(task.getId());
                boolean save = tMeasureTaskAttachmentService.save(taskAttachment);
                if (!save) {
                    log.error("【保存附件失败】保存任务附件失败, session: {}", sessionId);
                    sendAndClose(session, "保存任务附件失败");
                    return false;
                }
            }

            checkInterrupted(session);
            task.setZeroParam(JSON.toJSONString(dataList));
            log.info("【更新任务】更新任务基线参数, session: {}, 数据点数: {}", sessionId, dataList.size());
            boolean update = tMeasureTaskService.updateById(task);
            if (!update) {
                log.error("【更新任务失败】更新任务失败, session: {}", sessionId);
                sendAndClose(session, "更新任务失败");
                return false;
            }

            Constants.measureStatus = true;
            log.info("【保存任务成功】任务结果保存成功, session: {}", sessionId);
            return true;
        } catch (InterruptedException e) {
            log.info("【保存任务中断】保存任务结果被中断, session: {}", sessionId);
            throw new UserInterruptedException("用户主动停止");
        }
    }

    /**
     * 清理资源 - 严格版本，确保设备释放
     */
    private void cleanupResources(WebSocketSession session) {
        String sessionId = session.getId();
        SessionState state = sessionStates.get(sessionId);

        if (state.cleanupInProgress.getAndSet(true)) {
            log.info("【清理资源】清理正在进行中，跳过重复清理, session: {}", sessionId);
            return;
        }
        while (Constants.sendMeasureThread!=null&&Constants.sendMeasureThread.isAlive()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (state == null) {
            log.info("【清理资源】会话状态为空，无需清理, session: {}", sessionId);
            // 即使状态为空，也要清理其他资源
            cleanupRemainingResources(sessionId);
            return;
        }
        try {
            log.info("【开始清理资源】session: {}", sessionId);

            // 1. 标记会话正在关闭，阻止新的消息发送
            state.sessionClosing.set(true);

            // 2. 停止数据接收线程（关键：先停止接收数据）
            if (!state.dataReceivingThreadStopped.getAndSet(true)) {
                stopDataReceivingThread();
            }

            // 3. 发送停止命令到设备（如果需要）
            if (state.measurementStarted.get() && !state.stopCommandSent.get()) {
                log.info("【清理时发送停止】在清理时发送停止命令到设备, session: {}", sessionId);
                sendStopCommandToDevice(session);
            }

            // 4. 清理线程引用
            Thread taskThread = taskThreads.remove(sessionId);
            if (taskThread != null) {
                log.info("【清理任务线程】移除任务线程引用, session: {}", sessionId);
            }

            // 5. 释放设备（关键步骤）- 严格确保释放
            if (state.deviceOccupied.get() && state.deviceSerialNumber != null) {
                String deviceSerialNumber = state.deviceSerialNumber;
                try {
                    log.info("【开始释放设备】释放设备占用, session: {}, device: {}", sessionId, deviceSerialNumber);
                    deviceInfoService.releaseDevice(deviceSerialNumber);

                    // 从设备占用映射中移除
                    String occupiedBy = deviceOccupiedBy.get(deviceSerialNumber);
                    if (sessionId.equals(occupiedBy)) {
                        deviceOccupiedBy.remove(deviceSerialNumber);
                        log.info("【设备释放成功】设备释放成功并从占用映射中移除, session: {}, device: {}", sessionId, deviceSerialNumber);
                    } else {
                        log.warn("【设备占用不匹配】设备占用者不匹配, session: {}, device: {}, 占用者: {}",
                                sessionId, deviceSerialNumber, occupiedBy);
                    }

                    state.deviceOccupied.set(false);
                    state.deviceSerialNumber = null;
                } catch (Exception e) {
                    log.error("【释放设备异常】释放设备异常, session: {}, device: {}", sessionId, deviceSerialNumber, e);
                    // 即使异常也要继续清理其他资源，并强制从占用映射中移除
                    deviceOccupiedBy.remove(deviceSerialNumber);
                }
            } else {
                log.info("【无需释放设备】设备未被占用或设备序列号为空, session: {}, occupied: {}, deviceSN: {}",
                        sessionId, state.deviceOccupied.get(), state.deviceSerialNumber);
            }

            // 6. 清理其他资源
            cleanupRemainingResources(sessionId);

            log.info("【资源清理完成】所有资源清理完成, session: {}", sessionId);

        } catch (Exception e) {
            log.error("【清理资源异常】清理资源异常, session: {}", sessionId, e);
            // 确保基本的清理工作完成
            cleanupRemainingResources(sessionId);
        }
    }

    /**
     * 清理剩余资源
     */
    private void cleanupRemainingResources(String sessionId) {
        try {
            // 清理数据点索引
            dataPointIndexMap.remove(sessionId);
            log.info("【清理数据索引】数据点索引已清理, session: {}", sessionId);

            // 清理发送锁
            sessionSendLocks.remove(sessionId);
            log.info("【清理发送锁】发送锁已清理, session: {}", sessionId);

            // 从会话管理中移除
            sessionStates.remove(sessionId);
            sessions.remove(sessionId);
            log.info("【清理会话】会话状态和会话已移除, session: {}", sessionId);

            // 清空全局数据
            clearGlobalData();
            log.info("【清理全局数据】全局数据已清空, session: {}", sessionId);

        } catch (Exception e) {
            log.error("【清理剩余资源异常】清理剩余资源异常, session: {}", sessionId, e);
        }
    }

    // 其他辅助方法...
    private boolean isFastScan(FSpectrumParameter param) {
        return "3".equals(param.getScanSpeed());
    }

    private boolean isEndWavelengthReached(Double currentWavelength, Double endWavelength, boolean isDescending) {
        if (currentWavelength == null || endWavelength == null) {
            return false;
        }
        double tolerance = 0.001;
        if (isDescending) {
            return currentWavelength <= endWavelength + tolerance;
        } else {
            return currentWavelength >= endWavelength - tolerance;
        }
    }

    /**
     * 强制约束波长，确保波长在指定范围内
     * @param originalWl 原始波长值
     * @param startWl 波长范围起始值
     * @param endWl 波长范围结束值
     * @param interval 波长间隔
     * @param isDescending 是否为递减模式
     * @param sessionId 会话ID，用于追踪当前数据点索引
     * @return 约束后的波长值
     */
    private Double forceConstrainWavelength(Double originalWl, double startWl, double endWl,
                                            double interval, boolean isDescending, String sessionId) {
    // 如果原始波长为null，直接返回null
        if (originalWl == null) {
            return null;
        }
    // 获取当前会话对应的数据点索引，如果不存在则初始化为0
        Integer currentIndex = dataPointIndexMap.get(sessionId);
        if (currentIndex == null) {
            currentIndex = 0;
        }
        double expectedWl; // 期望的波长值
    // 根据是否递减模式计算期望波长
        if (isDescending) {
            expectedWl = startWl - (currentIndex * interval);
        // 如果期望波长小于结束波长，则设置为结束波长
            if (expectedWl < endWl) {
                expectedWl = endWl;
            }
        } else {
            expectedWl = startWl + (currentIndex * interval);
        // 如果期望波长大于结束波长，则设置为结束波长
            if (expectedWl > endWl) {
                expectedWl = endWl;
            }
        }
    // 获取间隔的小数位数，用于精度控制
        int decimalPlaces = getDecimalPlaces(interval);
    // 将期望波长四舍五入到指定精度
        double constrainedWl = roundToPrecision(expectedWl, decimalPlaces);
    // 根据递增或递减模式约束波长范围
        if (isDescending) {
            if (constrainedWl > startWl) constrainedWl = startWl; // 确保不超过起始波长
            if (constrainedWl < endWl) constrainedWl = endWl;   // 确保不低于结束波长
        } else {
            if (constrainedWl < startWl) constrainedWl = startWl; // 确保不低于起始波长
            if (constrainedWl > endWl) constrainedWl = endWl;     // 确保不超过结束波长
        }
    // 更新当前会话的数据点索引
        dataPointIndexMap.put(sessionId, currentIndex + 1);
    // 返回约束后的波长值
        return constrainedWl;
    }

    private int calculateExpectedDataPoints(double startWl, double endWl, double interval) {
        if (interval <= 0) {
            return 1000;
        }
        int points = (int) Math.abs((startWl - endWl) / interval) + 1;
        return Math.max(points, 100);
    }

    private int getDecimalPlaces(double interval) {
        if (interval == Math.floor(interval)) {
            return 0;
        } else {
            String intervalStr = String.valueOf(interval);
            if (intervalStr.contains(".")) {
                return intervalStr.length() - intervalStr.indexOf(".") - 1;
            }
            return 1;
        }
    }

    private double roundToPrecision(double value, int decimalPlaces) {
        double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }

    private void pushBaselineDataPoint(WebSocketSession session, Double wavelength) {
        SessionState state = sessionStates.get(session.getId());
        if (state != null && state.sessionClosing.get()) {
            return;
        }
        Object sendLock = sessionSendLocks.get(session.getId());
        if (sendLock == null) {
            return;
        }
        synchronized (sendLock) {
            if (state != null && state.sessionClosing.get()) {
                return;
            }
            try {
                JSONObject point = new JSONObject();
                point.put("nm", wavelength);
                JSONObject payload = new JSONObject();
                payload.put("data", point);
                sendMessageSynchronized(session, payload.toJSONString());
            } catch (Exception e) {
                log.debug("推送基线数据点异常（可能连接已关闭）, session: {}", session.getId());
            }
        }
    }

    /**
     * 发送消息,并关闭连接
     * @param session
     * @param message
     */
    private void sendMessageSynchronized(WebSocketSession session, String message) {
        SessionState state = sessionStates.get(session.getId());
        if (state != null && state.sessionClosing.get()) {
            return;
        }
        Object sendLock = sessionSendLocks.get(session.getId());
        if (sendLock == null) {
            return;
        }
        synchronized (sendLock) {
            if (state != null && state.sessionClosing.get()) {
                return;
            }
            if (session.isOpen()) {
                try {
                    boolean wasInterrupted = Thread.interrupted();
                    session.sendMessage(new TextMessage(message));
                    if (wasInterrupted) {
                        Thread.currentThread().interrupt();
                    }
                } catch (IOException e) {
                    log.debug("发送消息失败，连接可能已关闭, session: {}, 消息: {}", session.getId(), message);
                }
            }
        }
    }

    /**
     * 发送消息
     * @param session
     * @param message
     */
    private void sendMessageSafe(WebSocketSession session, String message) {
        sendMessageSynchronized(session, message);
    }

    private boolean isMeasurementComplete() {
        List<String> snapshot = new ArrayList<>(Constants.instrumentData);
        return !snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData);
    }

    private void checkInterrupted(WebSocketSession session) throws InterruptedException {
        if (Thread.currentThread().isInterrupted() || !session.isOpen()) {
            throw new InterruptedException("任务被中断或连接关闭");
        }
    }

    private void sendAndClose(WebSocketSession session, String msg) {
        String sessionId = session.getId();
        log.info("【发送并关闭】发送消息并关闭连接, session: {}, message: {}", sessionId, msg);
        try {
            sendMessageSafe(session, msg);
        } finally {
            cleanupResources(session);
            closeSessionQuietly(session);
        }
    }

    private void sendClose(WebSocketSession session, String msg) {
        String sessionId = session.getId();
        log.info("【发送关闭】发送消息并关闭连接, session: {}, message: {}", sessionId, msg);
        try {
            sendMessageSafe(session, msg);
        } finally {
            cleanupResources(session);
            closeSessionQuietly(session);
        }
    }

    private void closeSessionQuietly(WebSocketSession session) {
        String sessionId = session.getId();
        try {
            if (session.isOpen()) {
                SessionState state = sessionStates.get(sessionId);
                if (state != null) {
                    state.sessionClosing.set(true);
                }
                session.close();
                log.info("【关闭会话】WebSocket会话已关闭, session: {}", sessionId);
            }
        } catch (IOException e) {
            log.debug("关闭会话异常（连接可能已关闭）, session: {}", sessionId);
        }
    }

    private String getQueryParam(String query, String key) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) return pair[1];
        }
        return null;
    }

    private Command buildCommand(SetParamEnum cmdEnum, Object value) {
        Command cmd = new Command();
        cmd.setCmdId(cmdEnum.getComId());
        cmd.setParams(new Object[]{value});
        return cmd;
    }

    private void clearGlobalData() {
        Constants.startTime = null;
        Constants.endTime = null;
        Constants.measurd_data.clear();
        Constants.instrumentData.clear();
        Constants.measureStatus = false;
        Constants.sendMeasureThread = null;
    }
}