package com.persee.workstation.uv_inexus.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Component
public class ExcelUpdateNotifier {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 通知前端特定工作表已更新
     */
    public void notifySheetUpdate(String fileName, Integer sheetIndex, Map<String, Object> updatedData) {
        Map<String, Object> message = new HashMap<>();
        message.put("fileName", fileName);
        message.put("sheetIndex", sheetIndex);
        message.put("updatedData", updatedData);
        message.put("timestamp", System.currentTimeMillis());

        // 推送到前端订阅的主题
        messagingTemplate.convertAndSend("/topic/excel-sheet-update", message);
    }
}


