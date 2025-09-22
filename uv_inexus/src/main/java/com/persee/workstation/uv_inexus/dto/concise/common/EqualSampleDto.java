package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class EqualSampleDto {
    @ApiModelProperty("平行样唯一标识")
    private String equalSampleKey;
    @ApiModelProperty("包含样品id集合")
    private List<String> equalSampleIds;
}
