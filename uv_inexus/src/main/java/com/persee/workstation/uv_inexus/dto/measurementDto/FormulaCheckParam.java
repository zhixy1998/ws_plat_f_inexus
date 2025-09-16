package com.persee.workstation.uv_inexus.dto.measurementDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FormulaCheckParam {
    @ApiModelProperty("参数列表")
   private  List<String>  totalParamList;
    @ApiModelProperty("公式列表")
    private List<String> latexFormulaList;
}
