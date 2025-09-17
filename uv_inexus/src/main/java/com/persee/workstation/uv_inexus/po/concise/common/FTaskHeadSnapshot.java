package com.persee.workstation.uv_inexus.po.concise.common;

import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FTaskHeadSnapshot {

    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("任务表头信息，包含除样品和公式外，所有表头")
    private List<FHeadVO> taskHeads;

    @ApiModelProperty("映射关系")
    private List<InputParamMapTableHead> mapRelation;

    @ApiModelProperty("样品表头")
    private List<FHeadVO> sampleHeads;

}
