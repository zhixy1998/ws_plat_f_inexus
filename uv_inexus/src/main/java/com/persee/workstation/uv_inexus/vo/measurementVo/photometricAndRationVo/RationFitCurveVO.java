package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class RationFitCurveVO {

    @ApiModelProperty("任务ID")
    private String taskId;

    @ApiModelProperty("方程类型,Abs=f(C)..")
    private String equationType;

    @ApiModelProperty("横坐标")
    private List<Double> xList;

    @ApiModelProperty("纵坐标值")
    private List<Double> yList;

    @ApiModelProperty("方程k值，从高次到低")
    private List<Double> coefficientList;

    @ApiModelProperty("曲线类型,一次曲线，二次曲线...")
    private String curveType;

}
