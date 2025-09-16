package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class UnFinishTasksAndNumParam {
    private String selectKey;
    private String status;
    private String taskType;
    private Integer needSelectNum;
}
