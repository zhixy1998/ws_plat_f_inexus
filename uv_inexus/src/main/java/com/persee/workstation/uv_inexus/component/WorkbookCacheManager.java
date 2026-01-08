package com.persee.workstation.uv_inexus.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.SerializableWorkbook;
import com.persee.workstation.uv_inexus.po.factory_inspection.WorkbookData;
import com.persee.workstation.uv_inexus.service.factory_inspection.WorkbookDataService;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
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

    /**
     * 尝试获取任务编辑权限
     *
     * @param taskId   任务ID
     * @param updateBy 操作用户
     * @return true表示获取成功，false表示已被占用
     */
    public boolean acquireEditLock(String taskId, String updateBy) {
        return taskEditors.putIfAbsent(taskId, updateBy) == null;
    }

    /**
     * 释放任务编辑权限
     *
     * @param taskId   任务ID
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
     *
     * @param taskId   任务ID
     * @param updateBy 操作用户
     * @return true表示正在被编辑
     */
    public boolean isTaskBeingEdited(String taskId, String updateBy) {
        if (taskEditors.containsKey(taskId)) {
            return taskEditors.get(taskId).equals(updateBy);
        }
        return false;
    }

    /**
     * 获取当前编辑者
     *
     * @param taskId 任务ID
     * @return 编辑者用户ID，null表示未被编辑
     */
    public String getCurrentEditor(String taskId) {
        return taskEditors.get(taskId);
    }

    /**
     * 检查任务是否正在被其他用户编辑
     *
     * @param taskId      任务ID
     * @param currentUser 当前用户
     * @return true表示正在被其他用户编辑
     */
    public boolean isTaskBeingEditedByOthers(String taskId, String currentUser) {
        String editor = taskEditors.get(taskId);
        return editor != null && !editor.equals(currentUser);
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

        boolean b = releaseEditLock(taskId, updateBy);
        if (b) {
            log.info("任务结束，释放任务编辑权限：{}", taskId);
        }
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
}
