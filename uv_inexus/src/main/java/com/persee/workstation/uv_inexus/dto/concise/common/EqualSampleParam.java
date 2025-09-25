package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class EqualSampleParam {
    @ApiModelProperty("平行样唯一标识'")
    private String equalSampleKey;
    @ApiModelProperty("要加入平行样的样品id")
    private List<String> sampleIds;
    @ApiModelProperty("操作人")
    private String updateBy;
}
