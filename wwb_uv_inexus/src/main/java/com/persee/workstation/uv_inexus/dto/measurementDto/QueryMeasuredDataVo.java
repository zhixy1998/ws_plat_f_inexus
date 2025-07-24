package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class QueryMeasuredDataVo {
    private Integer pageNum;
    private Integer pageSize;
    private String taskId;
}
