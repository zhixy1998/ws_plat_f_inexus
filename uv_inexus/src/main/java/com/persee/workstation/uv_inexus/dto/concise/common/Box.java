package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
@Data
public class Box {
    @ApiModelProperty("需要删除内容集合")
    private List<String> params;
}
