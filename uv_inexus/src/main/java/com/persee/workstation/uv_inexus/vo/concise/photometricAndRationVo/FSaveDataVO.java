package com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

@Data
public class FSaveDataVO {
    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("样本id")
    private String sampleId;

    @ApiModelProperty("要保存的数据")
    private Map<String, String> data;
}
