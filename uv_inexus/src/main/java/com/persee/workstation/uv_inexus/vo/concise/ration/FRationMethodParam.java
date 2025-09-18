package com.persee.workstation.uv_inexus.vo.concise.ration;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRationParameter;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FRationMethodParam extends FRationParameter {
    @ApiModelProperty("方法ID")
    private String methodId;

    @ApiModelProperty("方法名称")
    private String methodName;

    @ApiModelProperty("测量类型")
    private String measurementType;
}
