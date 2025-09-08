package com.persee.workstation.uv_inexus.config;

import com.persee.workstation.uv_inexus.websocket.*;
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
    private SpectrumOneMeasureWebSocket spectrumOneMeasureWebSocket;

    @Autowired
    private PhotometricWebSocket photometricWebSocket;

    @Autowired
    private TimeWebSocket TimeWebSocket;

    @Autowired
    private TimeWebSocket2 TimeWebSocket2;

    @Autowired
    private RationMeasureWebSocket RationWebSocket;

    @Autowired
    private MyWebSocketHandler myWebSocketHandler;

    @Autowired
    private BaseLineWebSocket baseLineWebSocket;

    @Autowired
    private ZeroCalibrationWebSocket zeroCalibrationWebSocket;

    @Autowired
    MeasureWebSocketHandler measureWebSocketHandler;

    // 注册WebSocket处理器
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 添加处理器，并设置允许的来源
        registry.addHandler(spectrumWebSocket, "/ws/device")
                .setAllowedOrigins("*");
        registry.addHandler(photometricWebSocket, "/ws/photometric")
                .setAllowedOrigins("*");
        registry.addHandler(RationWebSocket, "/ws/ration")
                .setAllowedOrigins("*");
        registry.addHandler(TimeWebSocket, "/ws/timeScan")
                .setAllowedOrigins("*");
        registry.addHandler(spectrumOneMeasureWebSocket, "/ws/deviceOneMeasure")
                .setAllowedOrigins("*");
        registry.addHandler(myWebSocketHandler, "/ws/myWebSocket")
                .setAllowedOrigins("*");
        registry.addHandler(baseLineWebSocket, "/ws/baseLine")
                .setAllowedOrigins("*");
        registry.addHandler(zeroCalibrationWebSocket, "/ws/zeroCalibration")
                .setAllowedOrigins("*");
        registry.addHandler(TimeWebSocket2, "/ws/timeScan2")
                .setAllowedOrigins("*");
        registry.addHandler(measureWebSocketHandler, "/ws/measure")
                .setAllowedOrigins("*");


    }
}