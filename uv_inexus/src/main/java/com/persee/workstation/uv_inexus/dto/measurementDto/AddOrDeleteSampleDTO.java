package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SampleVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AddOrDeleteSampleDTO {
    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("方法id")
    private String methodId;

    @ApiModelProperty("更新人")
    private String updateBy;

//    @ApiModelProperty("新增的样本")
//    private List<SampleVO> newSamples;

    @ApiModelProperty("新增的样本")
    private List<Map<String, String>> newSamples;

    @ApiModelProperty("新增的样本表头")
    private List<HeadVO> heads;

    @ApiModelProperty("删除的样本id")
    private List<String> delSampleIds;
}
