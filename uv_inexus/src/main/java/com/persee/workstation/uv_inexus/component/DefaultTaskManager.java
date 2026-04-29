package com.persee.workstation.uv_inexus.component;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelDataAsset;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskItemStatus;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskManagement;
import com.persee.workstation.uv_inexus.service.factory_inspection.ExcelDataAssetService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TaskItemStatusService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TaskManagementService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TemplateService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.TASK_NOT_STARTED;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.TEMPLATE_BUILT_IN_TASK;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Component
@DS("factory_inspection")
@Slf4j
public class DefaultTaskManager {

    @Autowired
    private TaskManagementService taskManagementService;
    @Autowired
    private TemplateService templateService;
    @Autowired
    private DeviceManageService deviceManageService;

    @Autowired
    private ExcelDataAssetService dataAssetService;
    @Autowired
    private TaskItemStatusService taskItemStatusService;
    private volatile String defaultTaskId;

    // 获取默认任务ID
    public String getDefaultTaskId(String updateBy, String taskType, Integer templateStatus) {
        synchronized (this) {
            ensureDefaultTaskExists(updateBy,taskType,templateStatus);
        }
        return defaultTaskId;
    }
    /**
     * 为新任务初始化任务执行详情记录和检测项状态
     *
     * @param taskId       任务ID
     * @param templateCode 模板编码
     */
    private void initializeTaskExecutionDetails(String taskId, String templateCode) {
        try {
            // 查询模板对应的所有检测项
            LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExcelDataAsset::getTemplateCode, templateCode);
            List<ExcelDataAsset> assets = dataAssetService.list(wrapper);

            if (CollectionUtils.isEmpty(assets)) {
                log.warn("模板 {} 没有检测项", templateCode);
                return;
            }

            // 为每个检测项创建任务执行详情记录和状态记录
            List<TaskItemStatus> statusList = new ArrayList<>();

            for (ExcelDataAsset asset : assets) {

                // 创建任务检测项状态（用于记录测量状态）
                TaskItemStatus status = new TaskItemStatus();
                status.setId(IdGeneratorUtil.generateId());
                status.setTaskId(taskId);
                status.setItemId(asset.getId());
                status.setMeasured("0");
                status.setCheckResult("0");
                statusList.add(status);
            }


            if (!statusList.isEmpty()) {
                taskItemStatusService.saveBatch(statusList);
                log.info("为任务 {} 初始化了 {} 个检测项状态", taskId, statusList.size());
            }
        } catch (Exception e) {
            log.error("初始化任务执行详情失败，任务ID: {}", taskId, e);
        }
    }
    // 确保存在未开始的默认任务
    @Transactional(rollbackFor = Exception.class)
    public void ensureDefaultTaskExists(String updateBy, String taskType,Integer templateStatus) {
//        QueryWrapper<TaskManagement> wrapper = new QueryWrapper<>();
//        wrapper.eq("completion_status", TASK_NOT_STARTED.getCode())
//                .eq("content_category", TEMPLATE_BUILT_IN_TASK.getCode()); // 假设用名称标识默认任务
//
//        TaskManagement task = taskManagementService.getOne(wrapper);
//
//        if (task == null) {
//
        //看一下有没有updateBy创建的任务
        LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskManagement::getCreateBy, updateBy)
                .eq(TaskManagement::getTaskType, taskType);
        long count = taskManagementService.count(wrapper);
//        long count = taskManagementService.count();
        if (count == 0) {
            // 创建新默认任务
            TaskManagement newTask = createDefaultTask(updateBy,taskType,templateStatus);
            ExcelTemplate excelTemplate = templateService.getById(newTask.getUseTemplate());
            initializeTaskExecutionDetails(newTask.getId(), excelTemplate.getTemplateCode());

            taskManagementService.save(newTask);
            this.defaultTaskId = newTask.getId();

        } else {
            //使用最新的任务，不ai管有没有使用过
            TaskManagement task1 = taskManagementService.getOne(
                    new LambdaQueryWrapper<TaskManagement>()
                            .eq(TaskManagement::getCreateBy, updateBy)
                            .eq(TaskManagement::getTaskType, taskType)
                            .orderByDesc(TaskManagement::getUpdated)  // 按创建时间降序
                            .last("LIMIT 1")  // 只取最新的一条
            );
            this.defaultTaskId = task1.getId();
        }
    }
//    public TaskManagement createFactoryInspectionTask(String updateBy) {
//        TaskManagement task = createDefaultTask(updateBy,FACTORY_INSPECTION_TASK.getCode());
//        taskManagementService.save(task);
//        return task;
//    }
    private TaskManagement createDefaultTask(String updateBy, String taskType, Integer templateStatus) {
        TaskManagement task = new TaskManagement();
        task.setId(IdGeneratorUtil.generateIdWithPrefix("task"));
        task.setDetectName("默认任务" + IdGeneratorUtil.generateShortId() + System.currentTimeMillis());
        task.setCompletionStatus(TASK_NOT_STARTED.getCode());
        ExcelTemplate template = templateService.getOne(new LambdaQueryWrapper<ExcelTemplate>().eq(ExcelTemplate::getStatus, templateStatus));
        task.setResultTemplate(template.getTemplateCode());
        task.setUseTemplate(template.getId());
        task.setContentCategory(TEMPLATE_BUILT_IN_TASK.getCode());
        task.setCreated(LocalDateTime.now());
        task.setCreateBy(updateBy);
        task.setTemplateName(template.getTemplateName());
        if (StringUtils.isNotBlank(taskType)){
            task.setTaskType(taskType);
        }
        R<String> device = deviceManageService.getConnectDevice(updateBy);
        if (device != null && StringUtils.isNotBlank(device.getData())) {
            task.setOperationInstrument(device.getData());
        }

        ExcelTemplate excelTemplate = new ExcelTemplate();
        excelTemplate.setId(template.getId());
        Integer useCount = template.getUseCount();
        if (ObjectUtils.isEmpty(useCount)){
            useCount = 0;
        }
        excelTemplate.setUseCount( + 1);
        excelTemplate.setLastUsed(new Date());
        templateService.updateById(excelTemplate);
        return task;
    }
}

