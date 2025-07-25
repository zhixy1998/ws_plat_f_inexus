package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.List;

@Data
public class QueryTaskDto {
    // 项目ID
    private String projectId;
    // 方法ID
    private List<String> methodIds;
    // 测量类型
    private String measurementType;
    // 页码
    private Integer pageNum;
    // 每页大小
    private Integer pageSize;
    // 开始时间
    private String startTime;
    // 结束时间
    private String endTime;
}
