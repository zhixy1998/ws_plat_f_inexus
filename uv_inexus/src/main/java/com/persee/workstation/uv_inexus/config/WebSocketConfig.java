package com.persee.workstation.uv_inexus.config;

import com.persee.workstation.uv_inexus.websocket.PhotometricWebSocket;
import com.persee.workstation.uv_inexus.websocket.SpectrumWebSocket;
import com.persee.workstation.uv_inexus.websocket.SpectrumWebSocketTwo;
import com.persee.workstation.uv_inexus.websocket.TimeWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private SpectrumWebSocket spectrumWebSocket;

    @Autowired
    private PhotometricWebSocket photometricWebSocket;

    @Autowired
    private TimeWebSocket TimeWebSocket;

    @Autowired
    private SpectrumWebSocketTwo spectrumWebSocketTwo;

    // 注册WebSocket处理器
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 添加处理器，并设置允许的来源
        registry.addHandler(spectrumWebSocket, "/ws/device")
                .setAllowedOrigins("*");
        registry.addHandler(photometricWebSocket, "/ws/photometric")
                .setAllowedOrigins("*");
        registry.addHandler(TimeWebSocket, "/ws/timeScan")
                .setAllowedOrigins("*");
        registry.addHandler(spectrumWebSocketTwo, "/ws/deviceTwo")
                .setAllowedOrigins("*");

    }
}