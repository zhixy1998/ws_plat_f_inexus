package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

import java.util.List;

@Data
public class ProjectStatisticDto {
    // 项目个数
    private Long projectNum;
    // 方法个数
    private Long methodNum;
    // 任务个数
    private Long taskNum;
    // 项目统计
    private List<ProjectDataNumDto> projectDataNum;
    // 总记录数
    private Long total;
    // 总页数
    private Long pages;
    // 当前页码
    private Integer pageNum;
    // 每页大小
    private Integer pageSize;
    // 项目列表
    private List<ProjectSumListDto> projectList;
}
