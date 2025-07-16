package com.persee.workstation.uv_inexus.config;

import com.persee.workstation.uv_inexus.websocket.SpectrumWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private SpectrumWebSocket spectrumWebSocket;
    // 注册WebSocket处理器
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 添加处理器，并设置允许的来源
        registry.addHandler(spectrumWebSocket, "/ws/device")
                .setAllowedOrigins("*");
    }
}