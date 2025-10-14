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

package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

@Data
public class ProjectStatisticsVo {
    private String projectId;
    private String projectName;
    private String creator;
    private String createTime;
    private String updateBy;
    private String updateTime;
    private String methodCount;
    private String taskCount;
}
