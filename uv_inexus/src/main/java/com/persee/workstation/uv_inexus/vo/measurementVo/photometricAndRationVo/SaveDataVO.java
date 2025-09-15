package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SaveDataVO {
    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("样本id")
    private String sampleId;

    @ApiModelProperty("备注")
    private String remark;
}
