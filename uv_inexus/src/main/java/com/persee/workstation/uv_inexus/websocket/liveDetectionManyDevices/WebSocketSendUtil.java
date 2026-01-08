/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices;

import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class WebSocketSendUtil {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("/topic/device")
    private String deviceTopic;

    /**
     * 获取设备消息主题
     */
    public String getDeviceMessageTopic(String deviceSerialNumber) {
        return deviceTopic + "/" + deviceSerialNumber;
//        return deviceTopic;
    }

    /**
     * 发送成功结果
     */
    public void sendMessage(String deviceSerialNumber, WebSocketResponseVO data) {
        try {


            data.setCurrentTime(LocalDateTime.now());

            Result<Object> result = Result.ok(data);
            messagingTemplate.convertAndSend(getDeviceMessageTopic(deviceSerialNumber),
                    result);

        } catch (Exception e) {
            log.error("发送消息失败: {} {}", deviceTopic, data, e);
        }
    }

    /**
     * 发送通用消息
     */
    private void sendOperationResult(String deviceSerialNumber, Object message) {
        WebSocketResponseVO result = new WebSocketResponseVO();
        result.setData(message);
        result.setOperation(WebSocketOperationEnum.Common.getOperation());
        result.setCurrentTime(LocalDateTime.now());
        sendMessage(deviceSerialNumber, result);
    }

    /**
     * 发送错误消息
     */
    public void sendErrorMessage(String deviceSerialNumber, String operation, String errorMessage) {

        try {
            WebSocketResponseVO error = new WebSocketResponseVO();
            if (operation != null) {
                error.setOperation(operation);
            }

            error.setErrorMsg(errorMessage);
            Result<WebSocketResponseVO> errorResult = Result.fail(errorMessage);

            messagingTemplate.convertAndSend(getDeviceMessageTopic(deviceSerialNumber),
                    errorResult);

        } catch (Exception e) {
            log.error("发送消息失败: {} {}", deviceSerialNumber, errorMessage, e);
        }
    }

    /**
     * 发送错误消息
     */
    public void sendDeviceDisConnectMessage(String deviceSerialNumber) {
        JSONObject errorMsg = new JSONObject();
        try {

            errorMsg.put("error", "仪器无响应！");
            errorMsg.put("disconnect", "仪器无响应，请检查仪器状态和网线连接后，重新查找并联接仪器！");
            Result<JSONObject> errorResult = Result.fail(ResultCodeEnum.DEVICE_DISCONNECT.getCode(), errorMsg.toString());

            messagingTemplate.convertAndSend(getDeviceMessageTopic(deviceSerialNumber),
                    errorResult);

        } catch (Exception e) {
            log.error("发送消息失败: {} {}", deviceSerialNumber, errorMsg, e);
        }
    }
}
