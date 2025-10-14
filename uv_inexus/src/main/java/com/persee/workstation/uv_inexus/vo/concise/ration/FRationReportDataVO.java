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

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParamMapTableHead;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FRationReportDataVO {
    /**
     * 标准表头
     */
    private List<FHeadVO> standHeadList;
    /**
     * 未知表头
     */
    private List<FHeadVO> unknowHeadList;
    /**
     * 标准数据
     */
    private List<Map<String, Object>> standDataList;
    /**
     * 未知数据
     */
    private List<Map<String, Object>> unknowDataList;

    /**
     * 映射关系
     */
    private List<FInputParamMapTableHead> mapRelation;

    /**
     * 曲线信息
     */
    private FRationFitCurveVO curveInfo;
}
