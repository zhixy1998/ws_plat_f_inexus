package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SampleVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class AddOrDeleteSampleDTO {
    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("方法id")
    private String methodId;

    @ApiModelProperty("更新人")
    private String updateBy;

    @ApiModelProperty("新增的样本")
    private List<SampleVO> newSamples;

    @ApiModelProperty("删除的样本id")
    private List<String> delSampleIds;
}
