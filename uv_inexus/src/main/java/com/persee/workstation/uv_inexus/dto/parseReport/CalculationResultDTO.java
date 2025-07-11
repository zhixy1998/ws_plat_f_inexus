package com.persee.workstation.uv_inexus.dto.parseReport;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 计算公式表数据DTO
 */
@ApiModel("待计算结果")
@Data
public class CalculationResultDTO {
    @ApiModelProperty("标识")
    private String resultIdentity;
    
    @ApiModelProperty("标题")
    private String resultName;
    
    @ApiModelProperty("单位")
    private String unit;
    
    @ApiModelProperty("公式")
    private String formulaText;
}