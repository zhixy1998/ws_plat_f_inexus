package com.persee.workstation.uv_inexus.dto.parseReport;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 检测参数及子表字段DTO
 */
@ApiModel("检测参数及子表字段DTO")
@Data
public class DetectionParametersDTO {
    @ApiModelProperty("测量参数")
    private String detectionParameters; // 主表JSON参数

    @ApiModelProperty("前处理描述（前处理表）")
    private String preTreatmentDesc; // 对应pretreatment_description

    @ApiModelProperty("质控类型（质量控制表）")
    private String qcType; // 对应control_type

    @ApiModelProperty("结果表示（计算公式表）")
    private String resultExpression; // 对应result_expression
}