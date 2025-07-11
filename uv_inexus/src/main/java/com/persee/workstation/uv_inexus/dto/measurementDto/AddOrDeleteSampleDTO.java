package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureSample;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class AddOrDeleteSampleDTO {
    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("新增的样本")
    private List<TMeasureSample> newSamples;

    @ApiModelProperty("删除的样本id")
    private List<String> delSampleIds;
}
