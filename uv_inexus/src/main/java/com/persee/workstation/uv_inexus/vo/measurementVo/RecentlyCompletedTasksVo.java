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
public class RecentlyCompletedTasksVo {
    // 任务名称
    private String taskName;
    // 任务id
    private String taskId;
    // 方法id
    private String methodId;
    // 方法名称
    private String methodName;
    // 项目名称
    private String projectName;
    // 测量样品
    private String measurementSample;
    // 测量开始时间
    private String measurementStartTime;
    // 测量结束时间
    private String measurementEndTime;
}
