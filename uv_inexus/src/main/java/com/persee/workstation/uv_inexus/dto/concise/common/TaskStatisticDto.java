package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

import java.util.List;

@Data
public class TaskStatisticDto {
    // 任务总个数
    private Long taskTotal;
    // 任务完成数
    private Long taskCompleteNum;
    // 任务统计
    private List<TaskDataNumDto> taskDataNum;
    // 总记录数
    private Long total;
    // 总页数
    private Long pages;
    // 当前页码
    private Integer pageNum;
    // 每页大小
    private Integer pageSize;
    // 任务列表
    private List<TaskSumListDto> taskList;
}
