package com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemManageDAO;
import com.persee.workstation.uv_inexus.i18n.LocaleContext;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemManagePO;
import com.persee.workstation.uv_inexus.util.I18nMessageUtil;
import com.persee.workstation.uv_inexus.vo.messageVo.MessageReport;
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

    @Autowired
    private I18nMessageUtil i18nMessageUtil;

    @Autowired
    private SystemManageDAO systemManageDAO;

    @Value("${sys-manage-default.defaultId}")
    private String defaultId;

    @Value("/topic/device")
    private String deviceTopic;

    @Value("/topic/message")
    private String messageTopic;

    public String getDeviceMessageTopic(String deviceSerialNumber) {
        return deviceTopic + "/" + deviceSerialNumber;
    }

    /**
     * 发送设备消息（不国际化，用于设备状态、测量数据等）
     */
    public void sendMessage(String deviceSerialNumber, WebSocketResponseVO data) {
        try {
            data.setCurrentTime(LocalDateTime.now());
            Result<Object> result = Result.ok(data);
            messagingTemplate.convertAndSend(getDeviceMessageTopic(deviceSerialNumber), result);
        } catch (Exception e) {
            log.error("发送消息失败: {} {}", deviceTopic, data, e);
        }
    }

    private void sendOperationResult(String deviceSerialNumber, Object message) {
        WebSocketResponseVO result = new WebSocketResponseVO();
        result.setData(message);
        result.setOperation(WebSocketOperationEnum.Common.getOperation());
        result.setCurrentTime(LocalDateTime.now());
        sendMessage(deviceSerialNumber, result);
    }

    /**
     * 发送设备错误消息（已废弃，请使用 sendErrorMessageWithCode）
     * @deprecated 使用 sendErrorMessageWithCode 代替
     */
    @Deprecated
    public void sendErrorMessage(String deviceSerialNumber, String operation, String errorMessage) {
        try {
            WebSocketResponseVO error = new WebSocketResponseVO();
            if (operation != null) {
                error.setOperation(operation);
            }
            error.setErrorMsg(errorMessage);
            Result<WebSocketResponseVO> errorResult = Result.fail(errorMessage);
            messagingTemplate.convertAndSend(getDeviceMessageTopic(deviceSerialNumber), errorResult);
        } catch (Exception e) {
            log.error("发送消息失败: {} {}", deviceSerialNumber, errorMessage, e);
        }
    }

    /**
     * 发送国际化错误消息（推荐方式）
     * 用于设备操作错误提示，自动使用系统默认语言翻译
     *
     * @param deviceSerialNumber 设备序列号
     * @param operation 操作类型
     * @param messageCode 消息码
     * @param args 占位符参数
     */
    public void sendErrorMessageWithCode(String deviceSerialNumber, String operation, String messageCode, Object... args) {
        try {
            String locale = getSystemLocale();

            LocaleContext.executeWithLocale(locale, () -> {
                String localizedMessage = i18nMessageUtil.getMessage(messageCode, args);

                WebSocketResponseVO error = new WebSocketResponseVO();
                if (operation != null) {
                    error.setOperation(operation);
                }
                error.setErrorMsg(localizedMessage);

                Result<WebSocketResponseVO> errorResult = Result.fail(messageCode, args);
                messagingTemplate.convertAndSend(getDeviceMessageTopic(deviceSerialNumber), errorResult);
                return null;
            });
        } catch (Exception e) {
            log.error("发送国际化错误消息失败: device={}, code={}", deviceSerialNumber, messageCode, e);
        }
    }

    /**
     * 发送设备断开连接消息（使用系统默认语言）
     */
    public void sendDeviceDisConnectMessage(String deviceSerialNumber) {
        JSONObject errorMsg = new JSONObject();
        try {
            String locale = getSystemLocale();

            LocaleContext.executeWithLocale(locale, () -> {
                errorMsg.put("error", i18nMessageUtil.getMessage("00610001"));
                errorMsg.put("disconnect", i18nMessageUtil.getMessage("00610001"));

                Result<JSONObject> errorResult = Result.fail(ResultCodeEnum.DEVICE_DISCONNECT.getCode(), errorMsg.toString());
                messagingTemplate.convertAndSend(getDeviceMessageTopic(deviceSerialNumber), errorResult);
                return null;
            });
        } catch (Exception e) {
            log.error("发送消息失败: {} {}", deviceSerialNumber, errorMsg, e);
        }
    }

    /**
     * 发送用户消息（已构建好的 MessageReport，不自动翻译）
     * 适用场景：消息内容已经在调用方翻译好了
     */
    public void newMessageReport(String updateBy, MessageReport message) {
        WebSocketResponseVO messageVO = new WebSocketResponseVO();
        messageVO.setOperation(WebSocketOperationEnum.Message.getOperation());
        messageVO.setData(message);
        messagingTemplate.convertAndSend(messageTopic + "/" + updateBy, messageVO);
    }

    /**
     * 发送国际化用户消息（推荐方式）
     * 自动获取用户语言并翻译
     *
     * @param userId 用户ID
     * @param messageCode 消息码
     * @param args 占位符参数
     */
    public void sendI18nMessage(String userId, String messageCode, Object... args) {
        try {
            String locale = getUserLocale(userId);

            LocaleContext.executeWithLocale(locale, () -> {
                String localizedMessage = i18nMessageUtil.getMessage(messageCode, args);

                MessageReport message = new MessageReport();
                message.setMessageType(1);
                message.setMessageSource(i18nMessageUtil.getMessage("00610002"));
                message.setMessageSummary(localizedMessage);
                message.setReceiveTime(LocalDateTime.now());

                WebSocketResponseVO messageVO = new WebSocketResponseVO();
                messageVO.setOperation(WebSocketOperationEnum.Message.getOperation());
                messageVO.setData(message);

                messagingTemplate.convertAndSend(messageTopic + "/" + userId, messageVO);
                return null;
            });
        } catch (Exception e) {
            log.error("发送国际化消息失败: user={}, code={}", userId, messageCode, e);
        }
    }

    /**
     * 发送国际化用户消息（自定义消息类型和来源）
     * 适用于需要自定义消息类型的场景
     *
     * @param userId 用户ID
     * @param messageType 消息类型
     * @param sourceCode 消息来源消息码
     * @param summaryCode 消息摘要消息码
     * @param args 占位符参数
     */
    public void sendI18nMessageCustom(String userId, Integer messageType, String sourceCode, String summaryCode, Object... args) {
        try {
            String locale = getUserLocale(userId);

            LocaleContext.executeWithLocale(locale, () -> {
                String source = i18nMessageUtil.getMessage(sourceCode);
                String summary = i18nMessageUtil.getMessage(summaryCode, args);

                MessageReport message = new MessageReport();
                message.setMessageType(messageType);
                message.setMessageSource(source);
                message.setMessageSummary(summary);
                message.setReceiveTime(LocalDateTime.now());

                WebSocketResponseVO messageVO = new WebSocketResponseVO();
                messageVO.setOperation(WebSocketOperationEnum.Message.getOperation());
                messageVO.setData(message);

                messagingTemplate.convertAndSend(messageTopic + "/" + userId, messageVO);
                return null;
            });
        } catch (Exception e) {
            log.error("发送国际化消息失败: user={}, sourceCode={}, summaryCode={}", userId, sourceCode, summaryCode, e);
        }
    }


    /**
     * 获取用户语言配置
     */
    private String getUserLocale(String userId) {
        try {
            SystemManagePO systemConfig = systemManageDAO.selectById(defaultId);
            if (systemConfig != null && StringUtils.isNotBlank(systemConfig.getLanguage())) {
                String language = systemConfig.getLanguage();
                log.debug("使用系统配置的语言: {}", language);
                return language;
            }

            log.debug("未找到语言配置，使用默认语言: zh-CN");
            return "zh-CN";

        } catch (Exception e) {
            log.error("获取用户语言配置失败，使用默认语言", e);
            return "zh-CN";
        }
    }

    /**
     * 获取系统默认语言（用于设备消息）
     */
    private String getSystemLocale() {
        try {
            SystemManagePO systemConfig = systemManageDAO.selectById(defaultId);
            if (systemConfig != null && StringUtils.isNotBlank(systemConfig.getLanguage())) {
                return systemConfig.getLanguage();
            }
            return "zh-CN";
        } catch (Exception e) {
            log.error("获取系统语言配置失败，使用默认语言", e);
            return "zh-CN";
        }
    }
}
