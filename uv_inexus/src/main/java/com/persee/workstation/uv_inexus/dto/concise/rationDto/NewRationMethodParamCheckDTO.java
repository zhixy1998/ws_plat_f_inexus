package com.persee.workstation.uv_inexus.dto.concise.rationDto;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRationParameter;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class NewRationMethodParamCheckDTO {

    @ApiModelProperty("当前方法id")
    private String currentMethodId;

    @ApiModelProperty("当前任务id")
    private String currentTaskId;

    @ApiModelProperty("新方法id，如果有新方法，即使用新方法id，没有则不传")
    private String newMethodId;

    @ApiModelProperty("测量类型")
    private String measureType;

    @ApiModelProperty("方法名称")
    private String methodName;

    @ApiModelProperty("新方法参数")
    private FRationParameter newMethodParam;

    @ApiModelProperty("清除数据范围")
    private String clearDataRange;

    @ApiModelProperty("是否保存原有数据, true-保存，false-不保存")
    private Boolean isSaveDraft;

    @ApiModelProperty("用户")
    private String updateBy;
}
