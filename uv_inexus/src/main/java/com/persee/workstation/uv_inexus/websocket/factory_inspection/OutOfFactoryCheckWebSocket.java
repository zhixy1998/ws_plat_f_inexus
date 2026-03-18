package com.persee.workstation.uv_inexus.websocket.factory_inspection;

import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Component
@Slf4j
public class OutOfFactoryCheckWebSocket extends TextWebSocketHandler {
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("连接成功");
    }
    @Override
    public void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) throws Exception {
        log.info("接收到消息：{}", message);
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        log.info("连接关闭");
    }

    /**
     * 查找仪器是否初始化成功
     */
    public Boolean findDeviceInitSuccess(String deviceSerialNumber) {
        return false;
    }

}
