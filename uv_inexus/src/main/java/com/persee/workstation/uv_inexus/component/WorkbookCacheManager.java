package com.persee.workstation.uv_inexus.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.SerializableWorkbook;
import com.persee.workstation.uv_inexus.po.factory_inspection.WorkbookData;
import com.persee.workstation.uv_inexus.service.factory_inspection.WorkbookDataService;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WorkbookCacheManager {
    // 使用任务ID作为key
    private final Map<String, Long> taskLastAccessTime = new ConcurrentHashMap<>();
    private final Map<String, String> taskFileMap = new ConcurrentHashMap<>(); // taskId -> fileName

    @Autowired
    private EhcacheUtil ehcacheUtil;

    @Autowired
    private WorkbookDataService workbookDataService;

    // 使用 calibrationCache 缓存名称
    private static final String WORKBOOK_CACHE = "calibrationCache";
/*
    @Value("${excel.save.path}")
    private String excelSavePath;*/

    // 替换原有的 Set 结构
    private final Map<String, String> taskEditors = new ConcurrentHashMap<>(); // taskId -> updateBy
    // 替换原有的 Map 结构，使用更复杂的锁信息类
    private final Map<String, LockInfo> taskLocks = new ConcurrentHashMap<>();

    // 内部类：锁信息
    private static class LockInfo {
        String editor;      // 编辑者
        long lockTime;      // 上锁时间

        public LockInfo(String editor) {
            this.editor = editor;
            this.lockTime = System.currentTimeMillis();
        }
    }

    /**
     * 尝试获取任务编辑权限
     *
     * @param taskId   任务 ID
     * @param updateBy 操作用户
     * @return true 表示获取成功，false 表示已被占用（包括被自己占用）
     */
    public boolean acquireEditLock(String taskId, String updateBy) {
        LockInfo lockInfo = taskLocks.get(taskId);

        // 如果没有锁，创建新锁
        if (lockInfo == null) {
            LockInfo newLock = new LockInfo(updateBy);
            LockInfo existing = taskLocks.putIfAbsent(taskId, newLock);
            if (existing == null) {
                log.info("用户{}获取任务{}的编辑锁成功", updateBy, taskId);
                return true;  // 获取成功
            } else {
                // 并发情况下被别人抢先了
                log.warn("用户{}尝试获取任务{}的编辑锁失败，已被其他用户抢占", updateBy, taskId);
                return false;
            }
        }

        // 锁已经存在，无论是谁都不允许再次获取（包括同一个用户）
        log.warn("任务{}正在被用户{}编辑，用户{}无法重复获取锁（禁止多窗口）", taskId, lockInfo.editor, updateBy);
        return false;
    }

    /**
     * 释放任务编辑权限
     *
     * @param taskId   任务 ID
     * @param updateBy 操作用户
     * @return true 表示释放成功，false 表示不是持有者
     */
    public boolean releaseEditLock(String taskId, String updateBy) {
        LockInfo lockInfo = taskLocks.get(taskId);

        if (lockInfo == null) {
            log.warn("释放锁失败：任务{}没有锁", taskId);
            return false;
        }

        if (!lockInfo.editor.equals(updateBy)) {
            log.warn("释放锁失败：任务{}的锁属于用户{}，不是用户{}", taskId, lockInfo.editor, updateBy);
            return false;
        }

        taskLocks.remove(taskId);
        log.info("用户{}释放任务{}的编辑锁", updateBy, taskId);
        return true;
    }

    /**
     * 检查任务是否正在被编辑
     *
     * @param taskId   任务 ID
     * @param updateBy 操作用户
     * @return true 表示正在被编辑
     */
    public boolean isTaskBeingEdited(String taskId, String updateBy) {
        LockInfo lockInfo = taskLocks.get(taskId);
        return lockInfo != null && lockInfo.editor.equals(updateBy);
    }

    /**
     * 获取当前编辑者
     *
     * @param taskId 任务 ID
     * @return 编辑者用户 ID，null 表示未被编辑
     */
    public String getCurrentEditor(String taskId) {
        LockInfo lockInfo = taskLocks.get(taskId);
        return lockInfo != null ? lockInfo.editor : null;
    }

    /**
     * 检查任务是否正在被其他用户编辑
     *
     * @param taskId      任务 ID
     * @param currentUser 当前用户
     * @return true 表示正在被其他用户编辑
     */
    public boolean isTaskBeingEditedByOthers(String taskId, String currentUser) {
        LockInfo lockInfo = taskLocks.get(taskId);
        return lockInfo != null && !lockInfo.editor.equals(currentUser);
    }

    /**
     * 任务结束时清理缓存
     */
    public void cleanupTask(String taskId, String updateBy) {
        // 从 Ehcache 移除缓存
        ehcacheUtil.remove(WORKBOOK_CACHE, taskId, String.class, SerializableWorkbook.class);

        // 移除数据库中的持久化数据
        updateWorkbookDataToDB(taskId, null, updateBy);

        // 移除内存中的任务信息
        taskFileMap.remove(taskId);
        taskLastAccessTime.remove(taskId);

        // 释放锁
        releaseEditLock(taskId, updateBy);
    }


    /**
     * 尝试获取任务编辑权限
     *
     * @param taskId   任务ID
     * @param updateBy 操作用户
     * @return true表示获取成功，false表示已被占用
     */
    /**
     * 尝试获取任务编辑权限
     *
     * @param taskId   任务 ID
     * @param updateBy 操作用户
     * @return true表示获取成功（或已经是持有者），false表示已被其他用户占用
     */
//    public boolean acquireEditLock(String taskId, String updateBy) {
//        String existingEditor = taskEditors.putIfAbsent(taskId, updateBy);
//
//        // 如果返回 null，说明之前没有编辑者，获取成功
//        if (existingEditor == null) {
//            return true;
//        }
//
//        // 如果编辑者是当前用户自己，也返回 true（允许重复获取）
//        if (existingEditor.equals(updateBy)) {
//            return true;
//        }
//
//        // 编辑者是其他用户，返回 false
//        return false;
//    }


    /**
     * 释放任务编辑权限
     *
     * @param taskId   任务ID
     * @param updateBy 操作用户
     * @return true表示释放成功，false表示不是持有者
     */
//    public boolean releaseEditLock(String taskId, String updateBy) {
//        String holder = taskEditors.get(taskId);
//        if (holder != null && holder.equals(updateBy)) {
//            taskEditors.remove(taskId);
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * 检查任务是否正在被编辑
//     *
//     * @param taskId   任务ID
//     * @param updateBy 操作用户
//     * @return true表示正在被编辑
//     */
//    public boolean isTaskBeingEdited(String taskId, String updateBy) {
//        if (taskEditors.containsKey(taskId)) {
//            return taskEditors.get(taskId).equals(updateBy);
//        }
//        return false;
//    }
//
//    /**
//     * 获取当前编辑者
//     *
//     * @param taskId 任务ID
//     * @return 编辑者用户ID，null表示未被编辑
//     */
//    public String getCurrentEditor(String taskId) {
//        return taskEditors.get(taskId);
//    }
//
//    /**
//     * 检查任务是否正在被其他用户编辑
//     *
//     * @param taskId      任务ID
//     * @param currentUser 当前用户
//     * @return true表示正在被其他用户编辑
//     */
//    public boolean isTaskBeingEditedByOthers(String taskId, String currentUser) {
//        String editor = taskEditors.get(taskId);
//        return editor != null && !editor.equals(currentUser);
//    }

    /**
     * 任务结束时清理缓存
     */
//    public void cleanupTask(String taskId, String updateBy) {
//        // 从 Ehcache 移除缓存
//        ehcacheUtil.remove(WORKBOOK_CACHE, taskId, String.class, SerializableWorkbook.class);
//
//        // 移除数据库中的持久化数据
//        updateWorkbookDataToDB(taskId, null, updateBy);
//
//        // 移除内存中的任务信息
//        taskFileMap.remove(taskId);
//        taskLastAccessTime.remove(taskId);
//
//        boolean b = releaseEditLock(taskId, updateBy);
//        if (b) {
//            log.info("任务结束，释放任务编辑权限：{}", taskId);
//        }
//    }

    /**
     * 为任务创建Workbook实例
     */
    public Workbook createTaskWorkbook(String taskId, String fileName) throws IOException {
        // 先检查是否已有缓存的工作簿
        Workbook existingWorkbook = getTaskWorkbook(taskId);
        if (existingWorkbook != null) {
            return existingWorkbook;
        }

        // 检查数据库中是否存在持久化的工作簿数据
        Workbook workbook = getWorkbookFromDB(taskId);
        if (workbook != null) {
            return workbook;
        }
        LambdaQueryWrapper<WorkbookData> wrapper = new LambdaQueryWrapper<>();
        //这里传递的是模板的模板code值
        wrapper.eq(WorkbookData::getTaskId, fileName);
        List<WorkbookData> list = workbookDataService.list(wrapper);
        if (!CollectionUtils.isEmpty(list)) {
            byte[] data = list.get(0).getWorkbookData();
            if (data != null) {
                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
                    Workbook book = new XSSFWorkbook(inputStream);
                    saveWorkbookToCacheAndDB(taskId, book, fileName, "system");

                } catch (IOException e) {
                    log.error("转换字节数组到Workbook失败", e);
                }
            }
        }

//        ClassPathResource resource = new ClassPathResource(excelSavePath + fileName + ".xlsx");
//        //内置模板
//        if (resource.exists()) {
//            try (InputStream fis = resource.getInputStream()) {
//                workbook = new XSSFWorkbook(fis);
//
//                // 保存到缓存和数据库
//                saveWorkbookToCacheAndDB(taskId, workbook, fileName, "system");
//
//                return workbook;
//            }
//        }
//        //自定义模板
//        if (!resource.exists()){
//            InputStream inputStream = Files.newInputStream(Paths.get(excelSavePath + fileName + ".xlsx"));
//            workbook = new XSSFWorkbook(inputStream);
//
//            // 保存到缓存和数据库
//            saveWorkbookToCacheAndDB(taskId, workbook, fileName, "system");
//
//            return workbook;
//        }
        return null;
    }

    /**
     * 获取任务专属Workbook实例
     */
    public Workbook getTaskWorkbook(String taskId) {
        // 从缓存获取可序列化包装类
        SerializableWorkbook serializableWorkbook = ehcacheUtil.get(WORKBOOK_CACHE, taskId, String.class, SerializableWorkbook.class);
        Workbook workbook = null;
        if (serializableWorkbook != null) {
            workbook = serializableWorkbook.getWorkbook();
            taskLastAccessTime.put(taskId, System.currentTimeMillis());
        } else {
            // 缓存未命中，尝试从数据库加载
            workbook = getWorkbookFromDB(taskId);
            if (workbook != null) {
                // 重新放入缓存
                SerializableWorkbook serializableWorkbookToCache = new SerializableWorkbook(workbook);
                ehcacheUtil.put(WORKBOOK_CACHE, taskId, serializableWorkbookToCache, String.class, SerializableWorkbook.class);
                taskLastAccessTime.put(taskId, System.currentTimeMillis());
            }
        }
        return workbook;
    }

    /**
     * 更新任务Workbook
     */
    public void updateTaskWorkbook(String taskId, Workbook workbook, String updateBy) {
        // 更新缓存
        SerializableWorkbook serializableWorkbook = new SerializableWorkbook(workbook);
        ehcacheUtil.put(WORKBOOK_CACHE, taskId, serializableWorkbook, String.class, SerializableWorkbook.class);

        taskLastAccessTime.put(taskId, System.currentTimeMillis());
        //异步更新数据库（避免阻塞主线程）
        CompletableFuture.runAsync(() -> {
            try {
                updateWorkbookDataToDB(taskId, workbook, updateBy);
                log.debug("异步更新数据库完成: {}", taskId);
            } catch (Exception e) {
                log.error("异步更新数据库失败: {}", taskId, e);
            }
        });
    }

    /**
     * 检查任务是否有缓存的工作簿
     */
    public boolean hasTaskWorkbook(String taskId) {
        return ehcacheUtil.get(WORKBOOK_CACHE, taskId, String.class, SerializableWorkbook.class) != null
                || getWorkbookFromDB(taskId) != null;
    }

    /**
     * 从数据库获取工作簿数据
     */
    private Workbook getWorkbookFromDB(String taskId) {
        LambdaQueryWrapper<WorkbookData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkbookData::getTaskId, taskId);
        List<WorkbookData> list = workbookDataService.list(wrapper);
        WorkbookData workbookData = new WorkbookData();

        if (!CollectionUtils.isEmpty(list)) {
            workbookData = list.get(0);
        }
        if (workbookData != null && workbookData.getWorkbookData() != null) {
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(workbookData.getWorkbookData());
                Workbook workbook = new XSSFWorkbook(inputStream);

                // 同时更新到缓存
                SerializableWorkbook serializableWorkbook = new SerializableWorkbook(workbook);
                ehcacheUtil.put(WORKBOOK_CACHE, taskId, serializableWorkbook, String.class, SerializableWorkbook.class);

                taskLastAccessTime.put(taskId, System.currentTimeMillis());
                taskFileMap.put(taskId, workbookData.getTemplateCode());

                return workbook;
            } catch (IOException e) {
                log.error("从数据库加载工作簿失败: {}", taskId, e);
                return null;
            }
        }
        return null;
    }

    /**
     * 保存工作簿数据到缓存和数据库
     */
    private void saveWorkbookToCacheAndDB(String taskId, Workbook workbook, String fileName, String updateBy) {
        // 保存到缓存
        SerializableWorkbook serializableWorkbook = new SerializableWorkbook(workbook);
        ehcacheUtil.put(WORKBOOK_CACHE, taskId, serializableWorkbook, String.class, SerializableWorkbook.class);
        // 存储任务信息到内存Map
        taskFileMap.put(taskId, fileName);
        taskLastAccessTime.put(taskId, System.currentTimeMillis());
        // 保存到数据库

        // 异步更新数据库，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                updateWorkbookDataToDB(taskId, workbook, updateBy);
                log.debug("异步更新数据库完成: {}", taskId);
            } catch (Exception e) {
                log.error("异步更新数据库失败: {}", taskId, e);
            }
        });


    }

    /**
     * 更新工作簿数据到数据库
     */
    private void updateWorkbookDataToDB(String taskId, Workbook workbook, String updateBy) {
        try {
            LambdaQueryWrapper<WorkbookData> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WorkbookData::getTaskId, taskId);
            WorkbookData existingData = workbookDataService.list(wrapper).stream().findFirst().orElse(null);
            WorkbookData workbookData;

            if (existingData != null) {
                workbookData = existingData;
            } else {
                workbookData = new WorkbookData();
                workbookData.setId(IdGeneratorUtil.generateId());
                workbookData.setTaskId(taskId);
                workbookData.setCreateTime(LocalDateTime.now());
            }

            if (workbook != null) {
                // 序列化工作簿为字节数组
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                workbookData.setWorkbookData(outputStream.toByteArray());
                workbookData.setUpdateTime(LocalDateTime.now());
                workbookData.setUpdateBy(updateBy);
                workbookData.setLastAccessTime(LocalDateTime.now());
                workbookData.setIsActive(true);

                // 如果是新记录，设置模板代码
                if (taskFileMap.containsKey(taskId)) {
                    workbookData.setTemplateCode(taskFileMap.get(taskId));
                }
            } else {
                // 清除工作簿数据
                workbookData.setWorkbookData(null);
                workbookData.setIsActive(false);
            }

            workbookDataService.saveOrUpdate(workbookData);
        } catch (IOException e) {
            log.error("保存工作簿到数据库失败: {}", taskId, e);
        }
    }
    // ... existing code ...

    /**
     * 处理前端编辑单元格后的更新
     *
     * @param taskId    任务 ID
     * @param sheetName Sheet 名称
     * @param rowNum    行号（从 0 开始）
     * @param colNum    列号（从 0 开始）
     * @param newValue  新的单元格值
     * @param updateBy  操作用户
     * @return 更新后的 Sheet 数据，用于推送给前端
     */
    public Map<String, Object> updateCellByFrontend(String taskId, String sheetName,
                                                    int rowNum, int colNum, String newValue,
                                                    String updateBy) {
        try {
            // 1. 获取工作簿
            Workbook workbook = getTaskWorkbook(taskId);
            if (workbook == null) {
                log.error("更新单元格失败：工作簿不存在，taskId={}", taskId);
                return null;
            }

            // 2. 获取 Sheet
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                log.error("更新单元格失败：Sheet 不存在，taskId={}, sheetName={}", taskId, sheetName);
                return null;
            }

            // 3. 获取或创建行
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                row = sheet.createRow(rowNum);
            }

            // 4. 获取或创建单元格
            Cell cell = row.getCell(colNum);
            if (cell == null) {
                cell = row.createCell(colNum);
            }

            // 5. 设置新值（自动判断类型）
            setCellValue(cell, newValue);

            // 6. 计算公式（确保依赖此单元格的其他公式重新计算）
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateInCell(cell);

            // 强制重新计算所有公式
            for (int i = 0; i < 3; i++) {
                evaluator.evaluateAll();
            }
            workbook.setForceFormulaRecalculation(true);

            // 7. 更新缓存和数据库
            updateTaskWorkbook(taskId, workbook, updateBy);

            // 8. 准备返回数据（提取更新后的 Sheet 数据）
            Map<String, Object> result = new HashMap<>();
            result.put("sheetName", sheetName);
            result.put("sheetData", extractSheetData(sheet, workbook));

            log.info("前端编辑单元格成功：taskId={}, sheet={}, row={}, col={}, value={}",
                    taskId, sheetName, rowNum, colNum, newValue);

            return result;

        } catch (Exception e) {
            log.error("前端编辑单元格失败：taskId={}, sheet={}, row={}, col={}",
                    taskId, sheetName, rowNum, colNum, e);
            return null;
        }
    }

    /**
     * 设置单元格值（自动判断类型）
     */
    private void setCellValue(Cell cell, String value) {
        if (value == null || value.trim().isEmpty()) {
            cell.setCellType(CellType.BLANK);
            return;
        }

        String trimmedValue = value.trim();

        // 尝试解析为数字
        try {
            double numValue = Double.parseDouble(trimmedValue);
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(numValue);
            return;
        } catch (NumberFormatException e) {
            // 不是数字，继续判断
        }

        // 判断是否为布尔值
        if ("TRUE".equalsIgnoreCase(trimmedValue)) {
            cell.setCellType(CellType.BOOLEAN);
            cell.setCellValue(true);
            return;
        }
        if ("FALSE".equalsIgnoreCase(trimmedValue)) {
            cell.setCellType(CellType.BOOLEAN);
            cell.setCellValue(false);
            return;
        }

        // 判断是否为公式
        if (trimmedValue.startsWith("=")) {
            cell.setCellType(CellType.FORMULA);
            cell.setCellValue(trimmedValue.substring(1));
            return;
        }

        // 默认为字符串
        cell.setCellType(CellType.STRING);
        cell.setCellValue(trimmedValue);
    }

    /**
     * 提取 Sheet 数据（用于返回给前端）
     */
    private Map<String, Object> extractSheetData(Sheet sheet, Workbook workbook) {
        Map<String, Object> sheetDto = new HashMap<>();
        sheetDto.put("sheetName", sheet.getSheetName());
        sheetDto.put("firstRowNum", sheet.getFirstRowNum());
        sheetDto.put("lastRowNum", sheet.getLastRowNum());

        // 公式评估器
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.clearAllCachedResultValues();

        // 提取合并区域
        List<Map<String, Object>> mergedRegions = new ArrayList<>();
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            Map<String, Object> merged = new HashMap<>();
            merged.put("firstRow", region.getFirstRow());
            merged.put("lastRow", region.getLastRow());
            merged.put("firstCol", region.getFirstColumn());
            merged.put("lastCol", region.getLastColumn());

            // 获取合并区域的值
            Row row = sheet.getRow(region.getFirstRow());
            if (row != null) {
                Cell cell = row.getCell(region.getFirstColumn());
                if (cell != null) {
                    merged.put("value", getCellValueAsString(cell, evaluator));
                }
            }
            mergedRegions.add(merged);
        }
        sheetDto.put("mergedRegions", mergedRegions);

        // 提取普通单元格数据
        Map<String, Map<String, Object>> rowData = new HashMap<>();
        for (int rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                Map<String, Object> cellData = new HashMap<>();
                for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    if (cell != null) {
                        cellData.put(String.valueOf(cellNum), getCellValueAsString(cell, evaluator));
                    }
                }
                if (!cellData.isEmpty()) {
                    rowData.put(String.valueOf(rowNum), cellData);
                }
            }
        }
        sheetDto.put("rows", rowData);

        return sheetDto;
    }

    /**
     * 获取单元格值（字符串格式）
     */
    private String getCellValueAsString(Cell cell, FormulaEvaluator evaluator) {
        try {
            switch (cell.getCellType()) {
                case FORMULA:
                    // 使用 evaluator 获取公式计算结果
                    org.apache.poi.ss.usermodel.CellValue cellValue = evaluator.evaluate(cell);
                    switch (cellValue.getCellType()) {
                        case NUMERIC:
                            return String.valueOf(cellValue.getNumberValue());
                        case STRING:
                            return cellValue.getStringValue();
                        case BOOLEAN:
                            return String.valueOf(cellValue.getBooleanValue());
                        default:
                            return "";
                    }
                case NUMERIC:
                    return String.valueOf(cell.getNumericCellValue());
                case STRING:
                    return cell.getStringCellValue();
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                default:
                    return "";
            }
        } catch (Exception e) {
            log.warn("获取单元格值失败：{}", e.getMessage());
            return "";
        }
    }




}
