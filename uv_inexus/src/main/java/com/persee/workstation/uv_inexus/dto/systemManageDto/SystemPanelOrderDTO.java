package com.persee.workstation.uv_inexus.dto.systemManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class SystemPanelOrderDTO {

    @ApiModelProperty("面板顺序")
    private List<String> panelOrder;

    @ApiModelProperty(value = "用户")
    private String updateBy;
}
