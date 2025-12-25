package com.persee.workstation.uv_inexus.component;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * WebSocket订阅者 - 用于管理WebSocket会话和数据发送
 */
@Slf4j
public class WebSocketSubscriber {

    private final WebSocketSession session;
    private final String taskId;
    private final String sessionId;

    public WebSocketSubscriber(WebSocketSession session, String taskId) {
        this.session = session;
        this.taskId = taskId;
        this.sessionId = session.getId();
    }

    /**
     * 检查会话是否活跃
     */
    public boolean isSessionActive() {
        return session != null && session.isOpen();
    }

    /**
     * 发送波形图数据
     */
    public void sendSpectrumData(List<Map<String, Object>> spectrumData, int currentIndex, int totalCount) throws IOException {
        if (isSessionActive()) {
            JSONObject spectrumMessage = new JSONObject();
            spectrumMessage.put("type", "spectrum_data");
            spectrumMessage.put("spectrumData", spectrumData);
            spectrumMessage.put("index", currentIndex);
            spectrumMessage.put("total", totalCount);
            spectrumMessage.put("taskId", taskId);

            session.sendMessage(new TextMessage(spectrumMessage.toJSONString()));
            log.debug("发送波形图数据: task={}, index={}", taskId, currentIndex);
        }
    }

    /**
     * 发送Sheet更新数据
     */
    public void sendSheetUpdate(String sheetName, Object sheetData) throws IOException {
        if (isSessionActive()) {
            JSONObject sheetMessage = new JSONObject();
            sheetMessage.put("type", "sheet_update");
            sheetMessage.put("sheetName", sheetName);
            sheetMessage.put("sheetData", sheetData);
            sheetMessage.put("taskId", taskId);

            session.sendMessage(new TextMessage(sheetMessage.toJSONString()));
            log.debug("发送Sheet更新数据: task={}, sheet={}", taskId, sheetName);
        }
    }

    /**
     * 发送测量状态
     */
    public void sendMeasurementStatus(boolean isMeasuring, int currentCount, int totalCount) throws IOException {
        if (isSessionActive()) {
            JSONObject statusMessage = new JSONObject();
            statusMessage.put("type", "measurement_status");
            statusMessage.put("isMeasuring", isMeasuring);
            statusMessage.put("currentCount", currentCount);
            statusMessage.put("totalCount", totalCount);
            statusMessage.put("taskId", taskId);

            session.sendMessage(new TextMessage(statusMessage.toJSONString()));
            log.debug("发送测量状态: task={}, measuring={}", taskId, isMeasuring);
        }
    }

    public WebSocketSession getSession() {
        return session;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getSessionId() {
        return sessionId;
    }
}

