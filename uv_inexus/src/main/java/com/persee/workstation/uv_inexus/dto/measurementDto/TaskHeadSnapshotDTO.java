package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class TaskHeadSnapshotDTO {
    @ApiModelProperty(value = "任务id", required = true)
    private String taskId;

    @ApiModelProperty(value = "样品种类", required = true)
    private String sampleKind;

    @ApiModelProperty("所有表头")
    private List<HeadVO> taskHeads;

    @ApiModelProperty("映射关系")
    private List<InputParamMapTableHead> mapRelation;
}
