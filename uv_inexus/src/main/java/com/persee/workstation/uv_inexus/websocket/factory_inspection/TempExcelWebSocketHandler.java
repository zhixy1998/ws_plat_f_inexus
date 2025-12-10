package com.persee.workstation.uv_inexus.websocket.factory_inspection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
@Component
@Slf4j
public class TempExcelWebSocketHandler extends TextWebSocketHandler {

    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        session.sendMessage(new TextMessage(session.getId()));
        System.out.println("新连接建立: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 接收前端发送的单元格更新消息
        String payload = message.getPayload();
        System.out.println("收到消息: " + payload);

        // 广播给所有连接的客户端（包括发送者）
        broadcastMessage(payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("连接关闭: " + session.getId());
    }

    private void broadcastMessage(String message) {
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 【核心方法】主动向所有连接的客户端广播消息。
     * 这个方法可以被其他业务类调用。
     * @param message 要发送的文本内容
     */
    public void sendMessageToAll(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) { // 检查会话是否仍然开放
                    session.sendMessage(textMessage);
                    System.out.println("向会话 " + session.getId() + " 推送消息成功");
                }
            } catch (IOException e) {
                System.err.println("向会话 " + session.getId() + " 推送消息失败: " + e.getMessage());
            }
        }
    }

    /**
     * 可选：向特定会话发送消息（如果需要点对点推送）。
     * @param sessionId 目标会话的ID
     * @param message 要发送的文本内容
     */
    public void sendMessageToSession(String sessionId, String message) {
        for (WebSocketSession session : sessions) {
            if (session.getId().equals(sessionId) && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    System.out.println("向指定会话 " + sessionId + " 推送消息成功");
                } catch (IOException e) {
                    System.err.println("向指定会话 " + sessionId + " 推送消息失败");
                }
                break;
            }
        }
    }
}
