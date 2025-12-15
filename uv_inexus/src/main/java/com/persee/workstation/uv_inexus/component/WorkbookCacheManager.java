package com.persee.workstation.uv_inexus.component;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Component
public class WorkbookCacheManager {
    // 使用WebSocket会话ID作为key
    private final Map<String, Workbook> workbookCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();
    private final Map<String, String> sessionFileMap = new ConcurrentHashMap<>(); // sessionId -> fileName

    @Value("${excel.save.path}")
    private String excelSavePath;

    /**
     * 为WebSocket会话创建Workbook实例
     */
    public Workbook createSessionWorkbook(String sessionId, String fileName) throws IOException {
        String filePath = excelSavePath + fileName + ".xlsx";
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Workbook workbook = new XSSFWorkbook(fis);
            workbookCache.put(sessionId, workbook);
            sessionFileMap.put(sessionId, fileName);
            lastAccessTime.put(sessionId, System.currentTimeMillis());
            return workbook;
        }
    }

    /**
     * 获取会话专属Workbook实例
     */
    public Workbook getSessionWorkbook(String sessionId) {
        return workbookCache.get(sessionId);
    }

    /**
     * 更新会话Workbook
     */
    public void updateSessionWorkbook(String sessionId, Workbook workbook) {
        workbookCache.put(sessionId, workbook);
        lastAccessTime.put(sessionId, System.currentTimeMillis());
    }

    /**
     * 会话结束时清理并持久化
     */
    public void cleanupSession(String sessionId) throws IOException {
        Workbook workbook = workbookCache.get(sessionId);
        String fileName = sessionFileMap.get(sessionId);

        if (workbook != null && fileName != null) {
            // 持久化到文件
            saveWorkbookToFile(fileName, workbook);
            // 清理缓存
            workbookCache.remove(sessionId);
            sessionFileMap.remove(sessionId);
            lastAccessTime.remove(sessionId);
        }
    }

    /**
     * 将Workbook持久化到文件
     */
    public void saveWorkbookToFile(String fileName, Workbook workbook) throws IOException {
        String filePath = excelSavePath + fileName + ".xlsx";
        String tempFilePath = filePath + ".tmp";

        // 写入临时文件
        try (FileOutputStream fos = new FileOutputStream(tempFilePath)) {
            workbook.write(fos);
        }

        // 原子性替换
        Files.move(Paths.get(tempFilePath), Paths.get(filePath),
                StandardCopyOption.REPLACE_EXISTING);
    }
}


