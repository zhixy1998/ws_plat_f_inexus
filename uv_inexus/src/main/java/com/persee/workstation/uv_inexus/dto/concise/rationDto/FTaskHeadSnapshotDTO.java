package com.persee.workstation.uv_inexus.dto.concise.rationDto;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParamMapTableHead;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FTaskHeadSnapshotDTO {
    @ApiModelProperty(value = "任务id", required = true)
    private String taskId;

    @ApiModelProperty(value = "样品种类", required = true)
    private String sampleKind;

    @ApiModelProperty("所有表头")
    private List<FHeadVO> taskHeads;

    @ApiModelProperty("映射关系")
    private List<FInputParamMapTableHead> mapRelation;
}
