package com.persee.workstation.uv_inexus.vo.measurementVo;


import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.util.List;
import java.util.Map;

@Data
public class MethodDetailVo {

    @ApiModelProperty("方法名称")
    private String methodName;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("测量类型")
    private String measureType;

    @ApiModelProperty("测量参数")
    private Map<String,T> measureParam;


}
