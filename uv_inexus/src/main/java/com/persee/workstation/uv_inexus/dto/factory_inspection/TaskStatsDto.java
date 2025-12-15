package com.persee.workstation.uv_inexus.dto.factory_inspection;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import java.util.Map;

/**
 * 任务统计图表数据 DTO - 支持动态模板
 */
public class TaskStatsDto {

    /**
     * 时间点，格式为 "YYYY年MM月"
     */
    private String time;

    /**
     * 模板任务数量映射：模板名称 -> 当月任务数
     */
    private Map<String, Long> templateTasks;

    // Getters and Setters
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Map<String, Long> getTemplateTasks() {
        return templateTasks;
    }

    public void setTemplateTasks(Map<String, Long> templateTasks) {
        this.templateTasks = templateTasks;
    }
}
