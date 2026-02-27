package com.persee.workstation.uv_inexus.dto.measurementDto.experimentTest;

import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class TimeSweepMethodParamsDto extends TimeSweepParamter {
    // 用户名
    private String updateBy;
    // 项目id
    private String projectId;
    // 方法id
    private String currentMethodId;
    // 方法名称
    private String methodName;
    // 任务id
    private String taskId;
    //任务名
    private String newTaskName;

    @ApiModelProperty("清除数据范围")
    private String clearDataRange;

    @ApiModelProperty("分析参数是否应用到当前")
    private Boolean isApplyToCurrent;

    private String deviceSerialNumber;

    @ApiModelProperty("前处理Key")
    private String preprocessKey;

}
