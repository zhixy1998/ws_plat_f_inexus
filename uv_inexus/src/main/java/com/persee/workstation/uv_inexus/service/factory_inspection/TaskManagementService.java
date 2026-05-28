package com.persee.workstation.uv_inexus.service.factory_inspection;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.factory_inspection.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskManagement;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
public interface TaskManagementService extends IService<TaskManagement> {
    Result getList(TaskMgrQueryDto queryDto,String taskType);

    Result<TaskCheckItemDto> showTemplate(String id);

    Result<HashMap<String, String>> newTask(TaskAddDto task);

    Result newTaskByCurrent(TaskCopyDto taskCreateDto);

    Result<List<TaskStatsDto>> taskCount(String updateBy);

    Result saveCheckItem(TaskDetectionDTO dto);

    Result<Boolean> checkTaskUsage(TaskUsageDto dto);

    Result<List<TaskCheckItemDto.Standard>> standardMaterialList(String taskId);

    Result<Boolean> setStandardMaterial(SetSamplePoolDto dto);

    Result<TaskManagement> getTaskDetail(String taskId, String updateBy);

    Result<String> renameTask(TaskRenameDto dto);

    Result<String> deleteTask(String taskId);

    Result<String> batchDelete(BatchDelDto batchDelDto);

    Result<Map<String, Object>> generateReport(String taskId);

    Result<List<SetSamplePoolDto>> getStandardMaterial(String taskId);

    Result<Boolean> clearSamplePool(String taskId);

    Result<Boolean> setTaskReminder(@Valid TaskReminderDto dto);
}
