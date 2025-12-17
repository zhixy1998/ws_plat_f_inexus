package com.persee.workstation.uv_inexus.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
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
@Slf4j
@Component
public class WorkbookCacheManager {
    // 使用任务ID作为key
    private final Map<String, Workbook> taskWorkbookCache = new ConcurrentHashMap<>();
    private final Map<String, Long> taskLastAccessTime = new ConcurrentHashMap<>();
    private final Map<String, String> taskFileMap = new ConcurrentHashMap<>(); // taskId -> fileName
// 存储哪些任务被打开  数据结构
    @Value("${excel.save.path}")
    private String excelSavePath;
    // 替换原有的 Set 结构
    private final Map<String, String> taskEditors = new ConcurrentHashMap<>(); // taskId -> updateBy

    /**
     * 尝试获取任务编辑权限
     * @param taskId 任务ID
     * @param updateBy 操作用户
     * @return true表示获取成功，false表示已被占用
     */
    public boolean acquireEditLock(String taskId, String updateBy) {
        return taskEditors.putIfAbsent(taskId, updateBy) == null;
    }

    /**
     * 释放任务编辑权限
     * @param taskId 任务ID
     * @param updateBy 操作用户
     * @return true表示释放成功，false表示不是持有者
     */
    public boolean releaseEditLock(String taskId, String updateBy) {
        String holder = taskEditors.get(taskId);
        if (holder != null && holder.equals(updateBy)) {
            taskEditors.remove(taskId);
            return true;
        }
        return false;
    }

    /**
     * 检查任务是否正在被编辑
     * @param taskId 任务ID
     * @return true表示正在被编辑
     */
    public boolean isTaskBeingEdited(String taskId, String updateBy) {
        if (taskEditors.containsKey(taskId)){
            return taskEditors.get(taskId).equals(updateBy);
        }
        return false;
    }

    /**
     * 获取当前编辑者
     * @param taskId 任务ID
     * @return 编辑者用户ID，null表示未被编辑
     */
    public String getCurrentEditor(String taskId) {
        return taskEditors.get(taskId);
    }

    /**
     * 检查任务是否正在被其他用户编辑
     * @param taskId 任务ID
     * @param currentUser 当前用户
     * @return true表示正在被其他用户编辑
     */
    public boolean isTaskBeingEditedByOthers(String taskId, String currentUser) {
        String editor = taskEditors.get(taskId);
        return editor != null && !editor.equals(currentUser);
    }


    /**
     * 为任务创建Workbook实例
     */
    public Workbook createTaskWorkbook(String taskId, String fileName) throws IOException {
        // 先检查是否已有缓存的工作簿
        Workbook existingWorkbook = getTaskWorkbook(taskId);
        if (existingWorkbook != null) {
            return existingWorkbook;
        }
        String filePath = excelSavePath + fileName + ".xlsx";

        try (FileInputStream fis = new FileInputStream(filePath)) {
            Workbook workbook = new XSSFWorkbook(fis);
            taskWorkbookCache.put(taskId, workbook);
            taskFileMap.put(taskId, fileName);
            taskLastAccessTime.put(taskId, System.currentTimeMillis());
            return workbook;
        }
    }

    /**
     * 获取任务专属Workbook实例
     */
    public Workbook getTaskWorkbook(String taskId) {
        Workbook workbook = taskWorkbookCache.get(taskId);
        if (workbook != null) {
            taskLastAccessTime.put(taskId, System.currentTimeMillis());
        }
        return workbook;
    }

    /**
     * 更新任务Workbook
     */
    public void updateTaskWorkbook(String taskId, Workbook workbook) {
        taskWorkbookCache.put(taskId, workbook);
        taskLastAccessTime.put(taskId, System.currentTimeMillis());
    }

    /**
     * 任务结束时清理缓存
     */
    public void cleanupTask(String taskId,String updateBy) {
        taskWorkbookCache.remove(taskId);
        taskFileMap.remove(taskId);
        taskLastAccessTime.remove(taskId);
        boolean b = releaseEditLock(taskId,updateBy);
        if (b) {
            log.info("任务结束，释放任务编辑权限：{}", taskId);
        }
    }

    /**
     * 检查任务是否有缓存的工作簿
     */
    public boolean hasTaskWorkbook(String taskId) {
        return taskWorkbookCache.containsKey(taskId);
    }
}
