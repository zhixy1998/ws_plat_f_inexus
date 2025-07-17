package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class MeasurementCurrentInfoVO {

    @ApiModelProperty("任务ID")
    private String taskId;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("方法ID")
    private String methodId;

    @ApiModelProperty("方法名称")
    private String methodName;

    @ApiModelProperty("项目ID")
    private String projectId;

    @ApiModelProperty("项目名称")
    private String projectName;
}
