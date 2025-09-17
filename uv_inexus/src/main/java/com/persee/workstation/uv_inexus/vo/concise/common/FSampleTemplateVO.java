package com.persee.workstation.uv_inexus.vo.concise.common;

import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FSampleTemplateVO {
    @ApiModelProperty("模板id")
    private String id;

    @ApiModelProperty("模板名称")
    private String templateName;

    @ApiModelProperty("模板描述")
    private String templateDesc;

    @ApiModelProperty("测量类型")
    private String measureType;

    @ApiModelProperty("模板表头信息，包含全部表头")
    private List<FHeadVO> templateHeads;

    @ApiModelProperty("创建者")
    private String updateBy;

    private LocalDateTime created;
}

