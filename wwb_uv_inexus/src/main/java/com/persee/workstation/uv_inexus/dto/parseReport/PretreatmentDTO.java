package com.persee.workstation.uv_inexus.dto.parseReport;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 前处理表数据DTO
 */
@ApiModel("参考前处理")
@Data
public class PretreatmentDTO {
    @ApiModelProperty("前处理描述")
    private String pretreatmentDescription;
}