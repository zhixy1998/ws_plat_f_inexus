/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

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
