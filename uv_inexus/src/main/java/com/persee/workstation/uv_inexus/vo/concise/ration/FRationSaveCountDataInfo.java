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

package com.persee.workstation.uv_inexus.vo.concise.ration;

import lombok.Data;

import java.util.List;

@Data
public class FRationSaveCountDataInfo {

    private String taskId;

    private String sampleId;

    private String measureDataId;

    private String analyzeDataId;

    private List<FRationCountDataInfo> countData;


}
