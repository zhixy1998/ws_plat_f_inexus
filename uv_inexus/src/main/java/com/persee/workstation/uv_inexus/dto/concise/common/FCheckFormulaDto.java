package com.persee.workstation.uv_inexus.dto.concise.common;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParam;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPreCountResultParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FCheckFormulaDto {

    @ApiModelProperty("所有的输入参数")
    private List<FInputParam> inputParamList;

    private FPreCountResultParam newFormula;
}
