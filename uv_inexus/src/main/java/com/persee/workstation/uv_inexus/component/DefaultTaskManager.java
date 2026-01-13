package com.persee.workstation.uv_inexus.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskManagement;
import com.persee.workstation.uv_inexus.service.factory_inspection.TaskManagementService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TemplateService;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.*;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Component
public class DefaultTaskManager {

    @Autowired
    private TaskManagementService taskManagementService;
    @Autowired
    private TemplateService templateService;

    private volatile String defaultTaskId;

    // 获取默认任务ID
    public String getDefaultTaskId() {
        synchronized (this) {
            ensureDefaultTaskExists();
        }
        return defaultTaskId;
    }

    // 确保存在未开始的默认任务
    public void ensureDefaultTaskExists() {
//        QueryWrapper<TaskManagement> wrapper = new QueryWrapper<>();
//        wrapper.eq("completion_status", TASK_NOT_STARTED.getCode())
//                .eq("content_category", TEMPLATE_BUILT_IN_TASK.getCode()); // 假设用名称标识默认任务
//
//        TaskManagement task = taskManagementService.getOne(wrapper);
//
//        if (task == null) {
//
            long count = taskManagementService.count();
            if (count==0){
                // 创建新默认任务
                TaskManagement newTask = createDefaultTask();
                taskManagementService.save(newTask);
                this.defaultTaskId = newTask.getId();
            }else {
                //使用最新的任务，不管有没有使用过
                TaskManagement task1 = taskManagementService.getOne(
                        new LambdaQueryWrapper<TaskManagement>()
                                .orderByDesc(TaskManagement::getCreated)  // 按创建时间降序
                                .last("LIMIT 1")  // 只取最新的一条
                );
                this.defaultTaskId = task1.getId();
            }


    }

    private TaskManagement createDefaultTask() {
        TaskManagement task = new TaskManagement();
        task.setId(IdGeneratorUtil.generateIdWithPrefix("task"));
        task.setDetectName("默认任务"+System.currentTimeMillis());
        task.setCompletionStatus(TASK_NOT_STARTED.getCode());
        ExcelTemplate template = templateService.getOne(new LambdaQueryWrapper<ExcelTemplate>().eq(ExcelTemplate::getCategory, TEMPLATE_BUILT_IN.getCode()));
        task.setResultTemplate(template.getTemplateCode());
        task.setUseTemplate(template.getId());
        task.setContentCategory(TEMPLATE_BUILT_IN_TASK.getCode());
        task.setCreated(LocalDateTime.now());
        task.setCreateBy("sys");
        task.setTemplateName(template.getTemplateName());
        return task;
    }
}

