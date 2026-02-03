package com.persee.workstation.uv_inexus.controller.factory_inspection;

import com.persee.workstation.uv_inexus.dto.factory_inspection.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskManagement;
import com.persee.workstation.uv_inexus.service.factory_inspection.TaskManagementService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
@Slf4j
@RestController
@Api("任务管理")
@RequestMapping("/com/persee/workstation/uv-inexus/factory-inspection/task-manage")
public class TaskManagementController {
    @Autowired
    private TaskManagementService taskManagementService;
    @RequestMapping("/list")
    public Result getList(@RequestParam(required = false) String query, @RequestParam String updateBy,@RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        TaskMgrQueryDto taskMgrQueryDto = new TaskMgrQueryDto();
        taskMgrQueryDto.setQuery(query);
        taskMgrQueryDto.setUpdateBy(updateBy);
        taskMgrQueryDto.setPageNum(pageNum);
        taskMgrQueryDto.setPageSize(pageSize);


        return taskManagementService.getList(taskMgrQueryDto);
    }
    /**
     * 保存当前任务的标准物质信息
     * @param
     * @return 保存结果
     */
//    @RequestMapping("/save-standard-material")
//    public Result saveStandardMaterial( ) {
////        return taskManagementService.saveStandardMaterial(queryDto);
//    }
    /**
     *展示当前任务对应的模板
     */
    @RequestMapping("/show-template")
    public Result<TaskCheckItemDto> showTemplate(@RequestParam String id) {
        return taskManagementService.showTemplate(id);
    }
    /**
     * 新建任务
     */
    @RequestMapping("/new-task")
    public Result<HashMap<String, String>> newTask(@RequestBody@Valid TaskAddDto task) {
        log.info("新建任务");
        return taskManagementService.newTask(task);
    }
    /**
     * 根据当前任务新建任务，任务绑定的是模板的code值
     */
    @RequestMapping("/new-task-by-current")
    public Result newTaskByCurrent(@RequestBody TaskCopyDto taskCreateDto) {
        return taskManagementService.newTaskByCurrent(taskCreateDto);
    }
    /**
     * 单项测量,传入的是检测项的id
     */
    @RequestMapping("/check-task-usage")
    public Result<Boolean> checkTaskUsage(@RequestBody TaskUsageDto dto) {
        return taskManagementService.checkTaskUsage(dto);
    }
    /**
     * 自动测量
     */
    @RequestMapping("/auto-measure")
    public Result autoMeasure(@RequestParam String id) {
        return null;
    }
    /**
     * 任务统计
     */
    @RequestMapping("/task-count")
    public Result<List<TaskStatsDto>> taskCount(@RequestParam String updateBy) {
        return taskManagementService.taskCount(updateBy);
    }
    /**
     * 保存检测项配置
     */
    @RequestMapping("/save-check-item")
    public Result saveCheckItem(@RequestBody TaskDetectionDTO dto) {
        return taskManagementService.saveCheckItem(dto);
    }
    /**
     * 删除任务
     */
    @RequestMapping("/delete-task")
    public Result<String> deleteTask(@RequestParam String taskId) {
       return taskManagementService.deleteTask(taskId);
    }
    /**
     * 批量删除任务
     */
    @RequestMapping("/batch-delete")
    public Result<String> batchDelete(@RequestBody BatchDelDto batchDelDto) {
       return taskManagementService.batchDelete(batchDelDto);
    }
    /**
     * 可选择的标准物
     */
    @RequestMapping("/standard-material-list")
    public Result<List<TaskCheckItemDto.Standard>> standardMaterialList(@RequestParam String taskId) {
        return taskManagementService.standardMaterialList(taskId);
    }
    /**
     * 样品池设置标准物
     */
    @RequestMapping("/set-standard-material")
    public Result<Boolean> setStandardMaterial(@RequestBody SetSamplePoolDto dto) {
        return taskManagementService.setStandardMaterial(dto);
    }

    /**
     * 清空样品池
     */
    @RequestMapping("/clear-sample-pool")
    public Result<Boolean> clearSamplePool(@RequestParam String taskId) {
        return taskManagementService.clearSamplePool(taskId);
    }
    //先看一下选了哪些标准物
    @RequestMapping("/get-standard-material")
    public Result<List<SetSamplePoolDto>> getStandardMaterial(@RequestParam String taskId) {
        return taskManagementService.getStandardMaterial(taskId);
    }
    /**
     * 获取任务详情
     */
    @RequestMapping("/get-task-detail")
    public Result<TaskManagement> getTaskDetail(@RequestParam String taskId, @RequestParam String updateBy) {

        return taskManagementService.getTaskDetail(taskId,updateBy);
    }
    /**
     * 任务重命名
     */
    @RequestMapping("/rename-task")
    public Result<String> renameTask(@RequestBody TaskRenameDto dto) {
        return taskManagementService.renameTask(dto);
    }
    /**
     * 生成报告
     */
    @RequestMapping("/generate-report")
    public Result<Map<String, Object>> generateReport(@RequestParam String taskId) {
        return taskManagementService.generateReport(taskId);
    }
}
