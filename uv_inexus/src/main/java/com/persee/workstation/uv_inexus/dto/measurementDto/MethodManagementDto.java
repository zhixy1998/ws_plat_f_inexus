package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class MethodManagementDto {
    private String methodName;

    private String  methodCreator ;

    private Date methodCreateTime ;

    private String projectItemKey;

    private LocalDateTime lastUsedTime;

    private String measurementType;
}
