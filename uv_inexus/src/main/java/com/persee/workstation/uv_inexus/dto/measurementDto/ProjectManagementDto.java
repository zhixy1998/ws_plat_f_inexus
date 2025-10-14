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

@Data
public class ProjectManagementDto {

    private String id;
    //项目名称
    private String projectName;
    //创建时间
    private LocalDateTime createTime;
    //创建人
    private String creatorKey;
    //方法个数
    private Long methodNum;
    //任务个数
    private Long taskNum;
    //更新时间
    private LocalDateTime lastUpdatedTime;

}
