package com.persee.workstation.uv_inexus.dto.concise.common;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FCheckSampleInputParamDto {

    @ApiModelProperty("输入参数列表")
    private List<FInputParam> inputParamList;

    @ApiModelProperty("新增的输入参数")
    private FInputParam newInputParam;
}
