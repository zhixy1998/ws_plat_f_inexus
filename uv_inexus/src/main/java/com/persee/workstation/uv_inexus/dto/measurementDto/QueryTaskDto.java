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
