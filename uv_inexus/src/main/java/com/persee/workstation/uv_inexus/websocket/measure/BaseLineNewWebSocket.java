package com.persee.workstation.uv_inexus.websocket.measure;
/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumParameter;
import com.persee.workstation.uv_inexus.enums.T600ComIdEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.ScanSpeedEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.SpectrumService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskAttachmentService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.ActionCmdEnum;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketOperationEnum;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketResponseVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketSendUtil;
import com.persee.workstation.uv_inexus.websocket.measure.param.BaseLineNewWebSocketParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
public class BaseLineNewWebSocket {

    // 灯状态(true:没问题 false:有问题)
    private static boolean lightStatus = true;

    private static final Logger log = LoggerFactory.getLogger(BaseLineNewWebSocket.class);

    // 存储任务线程
    private static final Map<String, Thread> taskThreads = new ConcurrentHashMap<>();

    private static UnknowCountMessageQueue unknowCountMessageQueue = UnknowCountMessageQueue.getInstance();

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
    private SpectrumService spectrumService;

    @Autowired
    private TMeasureTaskService tMeasureTaskService;

    @Autowired
    private TMeasureTaskAttachmentService tMeasureTaskAttachmentService;

    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private SystemUserManageDAO systemUserManageDAO;

    @Autowired
    private FormulaUtil formulaUtil;

    @Autowired
    WebSocketSendUtil webSocketSendUtil;

    @Autowired
    private DevOperationUtilService devOperationUtilService;

    @Autowired
    DeviceInfoService infoService;

    String deviceSerialNumber;

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

        public void reset() {
            deviceOccupied.set(false);
            measurementStarted.set(false);
            stopCommandSent.set(false);
            stopCommandSent2.set(false);
            cleanupInProgress.set(false);
            sessionClosing.set(false);
            dataReceivingThreadStopped.set(false);
        }
    }

    /**
     * 当WebSocket连接建立后调用此方法
     */
    public void measureTask(BaseLineNewWebSocketParam param) {
        String sessionId = param.getDeviceSerialNumber();
        log.info("【连接建立】客户端进入: {} 时间: {}", sessionId, LocalDateTime.now());

        deviceSerialNumber=param.getDeviceSerialNumber();

        // 初始化会话状态
        SessionState state = new SessionState();
        sessionStates.put(sessionId, state);

        // 初始化发送锁
        sessionSendLocks.put(sessionId, new Object());

        // 初始化灯状态值
        lightStatus = true;

        infoService.updateDeviceCurrentStatus(deviceSerialNumber,WebSocketOperationEnum.BaseLine.getOperation(), param.getTaskId(), Constants.DEVICE_STATUS_MEASURE);

        // 往前端推送数据的线程
        Thread taskThread = new Thread(() -> {
            log.info("【任务开始】创建任务线程, session: {}", sessionId);
            try {
                runBusinessLogic(param);
            } catch (Exception e) {
                log.error("【任务异常】基线任务执行异常, session: {}", sessionId, e);
                sendErrorClose(deviceSerialNumber, "执行异常: " + e.getMessage());
            }
        });

        taskThread.setName("Baseline-Task-" + sessionId);
        taskThreads.put(sessionId, taskThread);
        taskThread.start();
        log.info("【任务启动】任务线程已启动, session: {}", sessionId);
    }
    /**
     * 主要的业务逻辑
     */
    private void runBusinessLogic(BaseLineNewWebSocketParam param) {
        String sessionId = param.getDeviceSerialNumber();
        log.info("【业务逻辑开始】session: {}", sessionId);

        // 获取参数
        String taskId = param.getTaskId();
        String methodId = param.getMethodId();
        String deviceSerialNumber = param.getDeviceSerialNumber();
        lastChangeParam=null;

        log.info("【参数获取】session: {}, taskId: {}, methodId: {}, deviceSerialNumber: {}",
                sessionId, taskId, methodId, deviceSerialNumber);

        // 参数验证
        if (!validateParameters(methodId, deviceSerialNumber)) {
            return;
        }

        try {
            // 设置参数
            if (!setupParameters(methodId, deviceSerialNumber)) {
                return;
            }

            // 获取方法参数
            TMeasureMethod method = tMeasureMethodService.getById(methodId);
            if (method == null) {
                sendErrorClose(deviceSerialNumber, "方法不存在");
                return;
            }
            // 将方法中的参数转换为对象
            SpectrumParameter methodParam = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
            // 开始测量
            if (!startMeasurement(method, deviceSerialNumber)) {
                return;
            }

            // 收集数据
            List<ChangeParam> dataList = collectBaselineData( method, deviceSerialNumber);
            if (dataList == null) {
                return; // 数据收集被中断或失败
            }

            // 标记数据收集完成
            SessionState state = sessionStates.get(sessionId);
            if (state != null) {
                log.info("【数据收集完成】数据收集完成, 数据点数: {}, session: {}", dataList.size(), sessionId);
            }
            // 保存任务结果
            if (Constants.measureStopStatus.get(deviceSerialNumber)) {
                saveTaskResult(taskId, methodId, deviceSerialNumber, method, dataList);
                // 进行校零
                DevOperationInputParam inputParam = new DevOperationInputParam();
                inputParam.setOperationType(OperationTypeEnums.Zero.getType());
                inputParam.setDeviceSerialNumber(deviceSerialNumber);
                inputParam.setWavelength(Collections.singletonList(methodParam.getStartWaveLength()));
                R<OperationResultEntity> r = devOperationUtilService.operation(inputParam);
                if (!r.getSuccess()) {
                    sendError(deviceSerialNumber, r.getMessage());
                    return;
                }
                log.info("【校零成功】零点校零成功完成, session: {}", sessionId);
            }else{
                log.info("【停止成功】停止成功,不保存数据, session: {}", sessionId);
            }

            JSONObject object = new JSONObject();
            if (Constants.measureStopStatus.get(deviceSerialNumber)){
                if (lightStatus){
                    object.put("success", "基线成功");
                }else {
                    object.put("error", "能量异常，请重新测量");
                }

            }else{
                object.put("success", "停止成功");
            }
            infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
            sendClose(sessionId, object);

        } catch (UserInterruptedException e) {
            // 用户主动停止，不处理，由 handleStopRequest 统一发送停止成功
            log.info("【用户中断】用户主动停止任务, session: {}", sessionId);
        } catch (Exception e) {
            log.error("【业务逻辑异常】基线任务执行异常, session: {}", sessionId, e);
            sendErrorClose(deviceSerialNumber, "基线任务执行异常: " + e.getMessage());
        }
    }

    /**
     * 参数验证
     */
    private boolean validateParameters(String methodId, String deviceSerialNumber) {
        if (methodId == null || methodId.trim().isEmpty()) {
            sendErrorClose(deviceSerialNumber, "方法编号不能为空");
            return false;
        }
        if (deviceSerialNumber == null || deviceSerialNumber.trim().isEmpty()) {
            log.error("【参数验证失败】设备编号为空");
            sendErrorClose(deviceSerialNumber,  "设备编号不能为空");
            return false;
        }
        log.info("【参数验证成功】参数验证通过");
        return true;
    }

    /**
     * 设置参数
     */
    private boolean setupParameters(String methodId, String deviceSerialNumber) {
        try {

            log.info("【开始设置参数】 methodId: {}, device: {}",methodId, deviceSerialNumber);
            Result result = spectrumService.setUpParametersPort(methodId, deviceSerialNumber);
            if (result.getCode() != 200) {
                log.error("【设置参数失败】设置参数服务返回失败 code: {}, message: {}",
                        result.getCode(), result.getMessage());
                sendErrorClose(deviceSerialNumber,result.getMessage() );
                return false;
            }

            log.info("【设置参数成功】参数设置完成");
            return true;
        } catch (Exception e) {
            log.info("【设置参数中断】设置参数被中断");
            throw new UserInterruptedException("用户主动停止");
        }
    }

    /**
     * 开始测量
     */
    private boolean startMeasurement(TMeasureMethod method, String deviceSerialNumber) {
        try {

            SessionState state = sessionStates.get(deviceSerialNumber);
            if (state == null) {
                log.error("【开始测量失败】会话状态异常, session: {}", deviceSerialNumber);
                sendErrorClose(deviceSerialNumber,"会话状态异常");
                return false;
            }

            log.info("【获取设备IP】开始获取设备IP, device: {}", deviceSerialNumber);
            String ip = deviceInfoService.getIp(deviceSerialNumber);
            if (ip == null) {
                log.error("【获取设备IP失败】无法获取设备IP, device: {}", deviceSerialNumber);
                sendErrorClose(deviceSerialNumber,"获取设备IP失败");
                return false;
            }
            log.info("【获取设备IP成功】设备IP: {}, session: {}", ip, deviceSerialNumber);

            SpectrumParameter param = JSONObject.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);

            // 清空全局数据，确保从干净状态开始
            clearGlobalData();
            // 设置停止初始状态
            Constants.measureStopStatus.put(deviceSerialNumber, true);

            DevOperationInputParam inputParam = new DevOperationInputParam();
            inputParam.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());
            inputParam.setDeviceSerialNumber(deviceSerialNumber);
            // 发送基线命令
            inputParam.setAction(ActionCmdEnum.BaseLine);
            boolean isFastScan = ScanSpeedEnum.EXTREME_FAST.getCode().equals(param.getScanSpeed());
            if (isFastScan) {
                inputParam.setAction(ActionCmdEnum.BaseLineFast);
            }

            R<OperationResultEntity> r = devOperationUtilService.operation(inputParam);
            if (!r.getSuccess()) {
                sendError(deviceSerialNumber,r.getMessage());
            }

            dataPointIndexMap.put(deviceSerialNumber, 0);
            log.info("【开始基线测量】设备: {}, 模式: {}, IP: {}",
                    deviceSerialNumber, isFastScan ? "极快扫描" : "普通扫描", ip);

            // 标记测量已开始
            state.measurementStarted.set(true);
            log.info("【基线测量开始】测量状态已标记为开始, session: {}", deviceSerialNumber);

            return true;
        } catch (Exception e) {
            log.info("【开始测量中断】开始测量被中断, session: {}", deviceSerialNumber);
            throw new UserInterruptedException("用户主动停止");
        }
    }

    /**
     * 收集基线数据 - 优化版本，正确处理停止逻辑
     */
    private List<ChangeParam> collectBaselineData(TMeasureMethod method, String deviceSerialNumber) {
        String sessionId = deviceSerialNumber;
        try {
            log.info("【开始收集数据】开始收集基线数据, session: {}", sessionId);
            List<ChangeParam> dataList = new ArrayList<>();
            SpectrumParameter param = JSONObject.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);

            double wavelengthInterval = Double.parseDouble(param.getWaveLengthInterval());
            double startWl = Double.parseDouble(param.getStartWaveLength());
            double endWl = Double.parseDouble(param.getEndWaveLength());

            int expectedPoints = calculateExpectedDataPoints(startWl, endWl, wavelengthInterval);
            log.info("【数据点预期】期望数据点数: {}, 波长范围: {} - {}, 间隔: {}, session: {}",
                    expectedPoints, startWl, endWl, wavelengthInterval, sessionId);

            SessionState state = sessionStates.get(sessionId);
            if (state == null) {
                log.error("【收集数据失败】会话状态异常, session: {}", sessionId);
                sendErrorClose(sessionId, "会话状态异常");
                return null;
            }
            while (true) {
                Object result = null;
                if (param.getScanSpeed().equals(ScanSpeedEnum.EXTREME_FAST.getCode())){
                    result = unknowCountMessageQueue.pollMessage(deviceSerialNumber, T600ComIdEnums.BaseLineFast.getComId());
                }else {
                    result = unknowCountMessageQueue.pollMessage(deviceSerialNumber, T600ComIdEnums.BaseLine.getComId());
                }
                // 如果队列为空（可选：根据你的 pollMessage 实现决定是否需要）
                if (result == null) {
                    // 可选：短暂休眠避免 CPU 空转
                    Thread.sleep(10); // 比如 10ms
                    continue;
                }
                // 判断是否是结束标记
                if (result instanceof EndMarker) {
                    EndMarker endMarker = (EndMarker) result;
                    // 可选：记录结束原因或打日志
                    switch (endMarker.getReason()) {
                        case TIMEOUT:
                            log.warn("设备 {} 的测量超时", deviceSerialNumber);
                            break;
                        case UN_NORMAL:
                            log.error("设备 {} 的测量异常结束", deviceSerialNumber);
                            break;
                        case NORMAL_OVERPOWER:
                            lightStatus = false;
                            log.error("设备 {} 能量异常，请重新测量", deviceSerialNumber);
                            break;
                        case NORMAL:
                            log.info("设备 {} 的测量正常完成", deviceSerialNumber);
                            break;
                        case NORMAL_STOP:
                            log.info("设备 {} 的测量已停止", deviceSerialNumber);
                            break;
                    }
                    // 结束循环
                    break;
                }
                SpectrumPoint message = (SpectrumPoint) result;
                if (message.getWavelength()>startWl||message.getWavelength()<endWl){
                    log.info("当前波长不在测量范围内，跳过该波长: "+message.getWavelength());
                    continue;
                }
                // 推送单个数据点
                pushBaselineDataPoint(message.getWavelength());
                dataList.add(message.getBaselineRsd());
                // 短暂休眠，减少CPU占用
                try {
                    Thread.sleep(ScanSpeedEnum.EXTREME_FAST.getCode().equals(param.getScanSpeed()) ? 2 : 10);
                } catch (InterruptedException e) {
                    log.info("【数据收集中断】数据收集被中断, session: {}", sessionId);
                    throw new UserInterruptedException("用户主动停止");
                }
            }

            // 数据收集完成后的清理工作
            log.info("【数据收集完成】停止数据接收线程并清理数据, session: {}", sessionId);
            Constants.measurd_data.clear();

            if (dataList.isEmpty()&&Constants.measureStopStatus.get(deviceSerialNumber) && lightStatus) {
                log.error("【数据为空】基线数据为空，失败, session: {}", sessionId);
                sendErrorClose(deviceSerialNumber,"基线数据为空，失败" );
                return null;
            }

            log.info("【数据收集成功】基线数据收集完成, dataList 大小: {}, session: {}", dataList.size(), sessionId);
            return dataList;
        } catch (Exception e) {
            log.info("【数据收集中断】数据收集被中断, session: {}", sessionId);
            throw new UserInterruptedException("用户主动停止");
        }
    }

    /**
     * 保存任务结果
     */
    private boolean saveTaskResult(String taskId, String methodId,
                                   String deviceSerialNumber, TMeasureMethod method, List<ChangeParam> dataList) {
        String sessionId = deviceSerialNumber;
        try {
            log.info("【开始保存任务】保存任务结果, session: {}", sessionId);

            TMeasureTask task;
            if (taskId != null && !taskId.trim().isEmpty()) {
                task = tMeasureTaskService.getById(taskId);
                if (task == null) {
                    log.error("【保存任务失败】任务不存在, session: {}, taskId: {}", sessionId, taskId);
                    sendErrorClose(sessionId,  "任务不存在");
                    return false;
                }
            } else {
                task = new TMeasureTask();
                SysUserPO user = systemUserManageDAO
                        .selectOne(new QueryWrapper<SysUserPO>().eq("user_name", method.getMethodCreator()));
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
                    sendErrorClose(sessionId, "保存任务失败");
                    return false;
                }

                TMeasureTaskAttachment taskAttachment = new TMeasureTaskAttachment();
                taskAttachment.setTaskId(task.getId());
                boolean save = tMeasureTaskAttachmentService.save(taskAttachment);
                if (!save) {
                    log.error("【保存附件失败】保存任务附件失败, session: {}", sessionId);
                    sendErrorClose(sessionId, "保存任务附件失败");
                    return false;
                }
            }

            task.setZeroParam(JSON.toJSONString(dataList));
            log.info("【更新任务】更新任务基线参数, session: {}, 数据点数: {}", sessionId, dataList.size());
            boolean update = tMeasureTaskService.updateById(task);
            if (!update) {
                log.error("【更新任务失败】更新任务失败, session: {}", sessionId);
                sendErrorClose(sessionId, "更新任务失败");
                return false;
            }

            Constants.measureStatus = true;
            log.info("【保存任务成功】任务结果保存成功, session: {}", sessionId);
            return true;
        } catch (Exception e) {
            log.info("【保存任务中断】保存任务结果被中断, session: {}", sessionId);
            throw new UserInterruptedException("用户主动停止");
        }
    }

    /**
     * 清理资源 - 严格版本，确保设备释放
     */
    private void cleanupResources(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
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


            // 4. 清理线程引用
            Thread taskThread = taskThreads.remove(sessionId);
            if (taskThread != null) {
                log.info("【清理任务线程】移除任务线程引用, session: {}", sessionId);
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
            log.info("【清理会话】会话状态和会话已移除, session: {}", sessionId);

            // 清空全局数据
            clearGlobalData();
            log.info("【清理全局数据】全局数据已清空, session: {}", sessionId);

        } catch (Exception e) {
            log.error("【清理剩余资源异常】清理剩余资源异常, session: {}", sessionId, e);
        }
    }


    // 是否是测量完成
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

    /**
     * 计算预计数据点数
     * @param startWl 测量起始波长
     * @param endWl 测量结束波长
     * @param interval 测量间隔
     * @return 预计数据点数
     */
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

    private void pushBaselineDataPoint(Double wavelength) {
        SessionState state = sessionStates.get(deviceSerialNumber);
        if (state != null && state.sessionClosing.get()) {
            return;
        }
        Object sendLock = sessionSendLocks.get(deviceSerialNumber);
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
                sendMessage(deviceSerialNumber, payload);
            } catch (Exception e) {
                log.debug("推送基线数据点异常（可能连接已关闭）, session: {}", deviceSerialNumber);
            }
        }
    }


    private boolean isMeasurementComplete() {
        List<String> snapshot = new ArrayList<>(Constants.instrumentData);
        return !snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData);
    }



    private void sendClose(String sessionId, JSONObject msg) {
        log.info("【发送关闭】发送消息并关闭连接, session: {}, message: {}", sessionId, msg);
        sendMessage(sessionId, msg);
        cleanupResources(sessionId);
    }

    private void sendErrorClose(String sessionId, String msg) {
        log.info("【发送关闭】发送消息并关闭连接, session: {}, message: {}", sessionId, msg);
        sendError(sessionId, msg);
        cleanupResources(sessionId);
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
        Constants.measureStopStatus.clear();
    }

    private void sendMessage(String deviceSerialNumber, JSONObject object) {

        WebSocketResponseVO vo = new WebSocketResponseVO();
        vo.setOperation(WebSocketOperationEnum.BaseLine.getOperation());
        vo.setData(object);

        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }

    private void sendError(String deviceSerialNumber, String message) {
        infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
        webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.BaseLine.getOperation(), message);
        cleanupResources(deviceSerialNumber);
    }


}