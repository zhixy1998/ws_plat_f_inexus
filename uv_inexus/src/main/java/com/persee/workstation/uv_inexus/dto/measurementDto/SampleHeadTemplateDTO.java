package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class SampleHeadTemplateDTO {

    @ApiModelProperty("模板名称")
    private String templateName;

    @ApiModelProperty("模板描述")
    private String templateDesc;

    @ApiModelProperty("测量类型")
    private String measureType;

    @ApiModelProperty("模板表头信息，包含全部表头")
    private List<HeadVO> templateHeads;

    @ApiModelProperty("创建者")
    private String updateBy;
}
