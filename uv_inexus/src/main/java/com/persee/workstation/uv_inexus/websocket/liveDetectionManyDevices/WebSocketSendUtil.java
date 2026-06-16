package com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices;


import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.i18n.LocaleContext;
import com.persee.workstation.uv_inexus.i18n.LocaleProvider;
import com.persee.workstation.uv_inexus.i18n.OutputTranslator;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.util.I18nMessageUtil;
import com.persee.workstation.uv_inexus.vo.messageVo.MessageReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WebSocketSendUtil {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private I18nMessageUtil i18nMessageUtil;

    @Autowired
    private LocaleProvider localeProvider;

    @Autowired
    private OutputTranslator outputTranslator;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("/topic/device")
    private String deviceTopic;

    @Value("/topic/message")
    private String messageTopic;

    public String getDeviceMessageTopic(String deviceSerialNumber) {
        return deviceTopic + "/" + deviceSerialNumber;
    }

    public void sendMessage(String deviceSerialNumber, WebSocketResponseVO data) {
        try {
            data.setCurrentTime(LocalDateTime.now());
            String locale = localeProvider.getSystemLocale();
            boolean needTranslate = !"zh-CN".equals(locale) && !"zh_CN".equals(locale) && locale != null;
            if (needTranslate && data.getData() != null) {
                data.setData(translateWsData(data.getData()));
            }
            if (needTranslate && data.getErrorMsg() != null) {
                data.setErrorMsg(i18nMessageUtil.getMessageWithDefault(data.getErrorMsg(), data.getErrorMsg()));
            }
            Result<Object> result = Result.ok(data);
            if (needTranslate) {
                String msg = result.getMessage();
                if (msg != null) {
                    result.setMessage(i18nMessageUtil.getMessageWithDefault(msg, msg));
                }
            }
            messagingTemplate.convertAndSend(getDeviceMessageTopic(deviceSerialNumber), result);
        } catch (Exception e) {
            log.error("发送消息失败: {} {}", deviceTopic, data, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object translateWsData(Object data) {
        if (data instanceof CharSequence || data instanceof Number || data instanceof Boolean
                || data instanceof java.time.temporal.Temporal) {
            return data;
        }
        try {
            Object converted = objectMapper.convertValue(data, Object.class);
            if (converted instanceof Map) {
                return outputTranslator.translate((Map<String, Object>) converted);
            } else if (converted instanceof List) {
                return outputTranslator.translate((List<Object>) converted);
            }
            return data;
        } catch (Exception e) {
            log.debug("[i18n] WS data翻译跳过: {}", e.getMessage());
            return data;
        }
    }

    private void sendOperationResult(String deviceSerialNumber, Object message) {
        WebSocketResponseVO result = new WebSocketResponseVO();
        result.setData(message);
        result.setOperation(WebSocketOperationEnum.Common.getOperation());
        result.setCurrentTime(LocalDateTime.now());
        sendMessage(deviceSerialNumber, result);
    }

    @Deprecated
    public void sendErrorMessage(String deviceSerialNumber, String operation, String errorMessage) {
        try {
            String locale = localeProvider.getSystemLocale();
            String translatedMessage = i18nMessageUtil.getMessageWithDefault(errorMessage, errorMessage);
            log.info("[i18n] sendErrorMessage: locale={}, original={}, translated={}", locale, errorMessage, translatedMessage);

            WebSocketResponseVO error = new WebSocketResponseVO();
            if (operation != null) {
                error.setOperation(operation);
            }
            error.setErrorMsg(translatedMessage);
            Result<WebSocketResponseVO> errorResult = Result.fail(translatedMessage);
            messagingTemplate.convertAndSend(getDeviceMessageTopic(deviceSerialNumber), errorResult);
        } catch (Exception e) {
            log.error("发送消息失败: {} {}", deviceSerialNumber, errorMessage, e);
        }
    }

    public void sendErrorMessageWithCode(String deviceSerialNumber, String operation, String messageCode, Object... args) {
        try {
            String locale = localeProvider.getSystemLocale();

            LocaleContext.executeWithLocale(locale, () -> {
                String localizedMessage = i18nMessageUtil.getMessage(messageCode, args);

                WebSocketResponseVO error = new WebSocketResponseVO();
                if (operation != null) {
                    error.setOperation(operation);
                }
                error.setErrorMsg(localizedMessage);

                Result<WebSocketResponseVO> errorResult = Result.fail(localizedMessage, args);
                messagingTemplate.convertAndSend(getDeviceMessageTopic(deviceSerialNumber), errorResult);
                return null;
            });
        } catch (Exception e) {
            log.error("发送国际化错误消息失败: device={}, code={}", deviceSerialNumber, i18nMessageUtil.getMessage(messageCode, args), e);
        }
    }

    public void sendDeviceDisConnectMessage(String deviceSerialNumber) {
        JSONObject errorMsg = new JSONObject();
        try {
            String locale = localeProvider.getSystemLocale();

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

    public void newMessageReport(String updateBy, MessageReport message) {
        WebSocketResponseVO messageVO = new WebSocketResponseVO();
        messageVO.setOperation(WebSocketOperationEnum.Message.getOperation());
        messageVO.setData(message);
        messagingTemplate.convertAndSend(messageTopic + "/" + updateBy, messageVO);
    }

    public void sendI18nMessage(String userId, String messageCode, Object... args) {
        try {
            String locale = localeProvider.getUserLocale(userId);

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

    public void sendI18nMessageCustom(String userId, Integer messageType, String sourceCode, String summaryCode, Object... args) {
        try {
            String locale = localeProvider.getUserLocale(userId);

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
}
