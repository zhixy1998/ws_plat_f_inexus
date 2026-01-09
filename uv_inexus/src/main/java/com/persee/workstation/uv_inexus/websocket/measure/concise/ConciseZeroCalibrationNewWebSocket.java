package com.persee.workstation.uv_inexus.websocket.measure.concise;
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
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTimeSweepParamter;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureMethodService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskAttachmentService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.TimeSweepConciseService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationInputParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationUtilService;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationResultEntity;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationTypeEnums;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketOperationEnum;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketResponseVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketSendUtil;
import com.persee.workstation.uv_inexus.websocket.measure.param.FZeroCalibrationWebSocketParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * WebSocket处理器类，继承自TextWebSocketHandler
 * 用于处理WebSocket连接的建立、消息接收和连接关闭等事件
 */
@Component
@Slf4j
public class ConciseZeroCalibrationNewWebSocket {

    private String deviceSerialNumber;

    @Autowired
    private TimeSweepConciseService timeSweepService;

    @Autowired
    private FMeasureTaskService tMeasureTaskService;

    @Autowired
    private FMeasureTaskAttachmentService tMeasureTaskAttachmentService;

    @Autowired
    private FMeasureMethodService tMeasureMethodService;

    @Autowired
    private SystemUserManageDAO systemUserManageDAO;

    @Autowired
    WebSocketSendUtil webSocketSendUtil;

    @Autowired
    private DevOperationUtilService devOperationUtilService;

    /**
     * 当WebSocket连接建立后调用此方法
     */
    public void measureTask(FZeroCalibrationWebSocketParam param) {
        System.out.println("客户端进入, 时间: " + LocalDateTime.now());

        try {
            runBusinessLogic(param);
        } catch (InterruptedException e) {
            // 线程被中断，立即停止
            System.out.println("任务被中断: " + e.getMessage());
            sendError(param.getDeviceSerialNumber(), "操作已取消，连接关闭");
            return;
        } catch (Exception e) {
            e.printStackTrace();
            sendError(param.getDeviceSerialNumber(), "执行异常: " + e.getMessage());
            return;
        }
    }

    /**
     * 运行业务逻辑
     *
     * @param param
     * @throws InterruptedException
     */
    private void runBusinessLogic(FZeroCalibrationWebSocketParam param) throws Exception {

        String taskId = param.getTaskId();
        String methodId = param.getMethodId();
        deviceSerialNumber = param.getDeviceSerialNumber();

        if (methodId == null || methodId.equals("")) {
            sendError(deviceSerialNumber, "方法编号不能为空");
            return;
        }
        if (deviceSerialNumber == null || deviceSerialNumber.equals("")) {
            sendError(deviceSerialNumber, "设备编号不能为空");
            return;
        }
        FMeasureMethod methodData = tMeasureMethodService.getById(methodId);
        if (methodData == null) {
            sendError(deviceSerialNumber, "方法不存在");
            return;
        }
        FTimeSweepParamter timeSweepParamter = JSON.parseObject(methodData.getMeasurementParamInfo(), FTimeSweepParamter.class);
        if (timeSweepParamter.getWaveLength() == null || timeSweepParamter.getWaveLength().isEmpty()) {
            sendError(deviceSerialNumber, "波长不能为空");
            return;
        }

        // 设置参数
        Result result = timeSweepService.setUpParametersPort(methodId, deviceSerialNumber);
        if (result.getCode() != 200) {
            sendError(deviceSerialNumber, "设置参数失败");
            return;
        }
        // 进行校零
        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Zero.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setWavelength(Collections.singletonList(timeSweepParamter.getWaveLength()));
        R<OperationResultEntity> r = devOperationUtilService.operation(inputParam);
        if (!r.getSuccess()) {
            sendError(deviceSerialNumber, r.getMessage());
            return;
        }
        ChangeParam zeroData = r.getData().getZeroResult().get(0);
        DetectionConstants.zeroResult.put(timeSweepParamter.getWaveLength(), zeroData);

        FMeasureTask task = null;
        if (taskId != null && !taskId.equals("")) {
            task = tMeasureTaskService.getById(taskId);
        } else {
            task = new FMeasureTask();
            FMeasureMethod method = tMeasureMethodService.getById(methodId);
            SystemUserManagePO user = systemUserManageDAO.selectOne(new QueryWrapper<>(SystemUserManagePO.class).eq("user_name", method.getMethodCreator()));
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
            FMeasureTaskAttachment taskAttachment = new FMeasureTaskAttachment();
            taskAttachment.setTaskId(task.getId());
            boolean save = tMeasureTaskAttachmentService.save(taskAttachment);
            if (!save || !save1) {
                sendError(deviceSerialNumber, "校零失败");
                return;
            }
        }
        task.setZeroParam(JSON.toJSONString(zeroData));
        boolean update = tMeasureTaskService.updateById(task);
        if (!update) {
            sendError(deviceSerialNumber, "校零失败");
            return;
        }
        // 向前端发送当前波长,格式是nm 当前波长值nm
        JSONObject point = new JSONObject();
        point.put("nm", timeSweepParamter.getWaveLength() + "nm");
        sendMessage(deviceSerialNumber, point);
        point.clear();
        point.put("success", "校零成功");
        sendMessage(deviceSerialNumber, point);
        point.clear();
        point.put("result", "finish");
        sendMessage(deviceSerialNumber, point);
    }


    private void sendMessage(String deviceSerialNumber, JSONObject object) {

        WebSocketResponseVO vo = new WebSocketResponseVO();
        vo.setOperation(WebSocketOperationEnum.ConciseZeroCalibration.getOperation());
        vo.setData(object);

        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }

    private void sendError(String deviceSerialNumber, String message) {

        webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.ConciseZeroCalibration.getOperation(), message);
    }
}
