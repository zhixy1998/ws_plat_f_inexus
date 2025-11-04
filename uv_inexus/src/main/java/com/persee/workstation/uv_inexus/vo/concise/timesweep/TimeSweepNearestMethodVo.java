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

package com.persee.workstation.uv_inexus.vo.concise.timesweep;

import lombok.Data;

/**
 * 最近方法返回
 */
@Data
public class TimeSweepNearestMethodVo {

    // 方法id
    private String methodId;

    // 前处理Key
    private String preprocessKey;

    // 多个计算公式Key
    private String multipleCalculationFormulaKey;

    // 方法名称
    private String methodName;

    // 测量类型
    private String measurementType;

    // 测量参数信息
    private FTimeSweepParamterVo methodParam;

    // 是否共享方法(0.否 1.是)
    private String isSharingMethod;

    //  方法创建时间
    private String methodCreateTime;

    // 方法创建人
    private String methodCreator;

    // 方法使用时间
    private String methodUseTime;

    // 是否草稿(0.不是草稿 1.是草稿)
    private String isDraft;

    // 使用状态
    private String useState;

    // 任务id
    private String taskId;
}
