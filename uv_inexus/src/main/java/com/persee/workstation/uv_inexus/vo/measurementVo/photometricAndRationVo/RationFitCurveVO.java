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

package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class RationFitCurveVO {

    @ApiModelProperty("任务ID")
    private String taskId;

    @ApiModelProperty("方程类型,Abs=f(C)..")
    private String equationType;

    @ApiModelProperty("数据")
    List<JSONObject> dataList;

    @ApiModelProperty("方程k值，从高次到低")
    private List<Double> coefficientList;

    @ApiModelProperty("曲线类型,一次曲线，二次曲线...")
    private String curveType;

    @ApiModelProperty("具体方程")
    private String equation;

    @ApiModelProperty("R值 （标准差, 代表拟合准确率）")
    private String R;

    @ApiModelProperty("R2值 （标准差, 代表拟合准确率）")
    private String R2;

    @ApiModelProperty("标准样品点")
    private List<JSONObject> standPoints;

    @ApiModelProperty("未知样品点")
    private List<JSONObject> unknowPoints;

}
