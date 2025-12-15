package com.persee.workstation.uv_inexus.controller.factory_inspection;

import com.persee.workstation.uv_inexus.dto.factory_inspection.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.factory_inspection.TaskManagementService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public Result getList(TaskMgrQueryDto queryDto) {
        return taskManagementService.getList(queryDto);
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
    public Result newTask(@RequestBody TaskAddDto task) {
        return taskManagementService.newTask(task);
    }
    /**
     * 根据当前任务新建任务，任务绑定的是模板的code值
     */
    @RequestMapping("/new-task-by-current")
    public Result newTaskByCurrent(@RequestParam TaskCopyDto taskCreateDto) {
        return taskManagementService.newTaskByCurrent(taskCreateDto);
    }
    /**
     * 单项测量
     */
    @RequestMapping("/single-measure")
    public Result singleMeasure(@RequestParam String id) {

        return null;
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
    public Result<List<TaskStatsDto>> taskCount() {
        return taskManagementService.taskCount();
    }
    /**
     * 保存检测项配置
     */
    @RequestMapping("/save-check-item")
    public Result saveCheckItem(@RequestBody TaskDetectionDTO dto) {
        return taskManagementService.saveCheckItem(dto);
    }
}
