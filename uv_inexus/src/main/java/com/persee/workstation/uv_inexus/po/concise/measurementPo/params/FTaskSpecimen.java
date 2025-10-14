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

package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FTaskSpecimen {
    // 样本id
    private String specimenKey;
    // 删除状态(0.未删除,1.删除)
    private String deleteType;
    // 是否已测(0.未测,1.已测)
    private String isTested;
    //样品种类
    private String sampleType;
    //样品类型
    private String sampleKind;
}
