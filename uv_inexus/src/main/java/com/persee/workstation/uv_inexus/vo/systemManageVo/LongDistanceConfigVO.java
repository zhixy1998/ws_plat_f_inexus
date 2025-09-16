package com.persee.workstation.uv_inexus.vo.systemManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class LongDistanceConfigVO {
    //是否允许远程控制
    @ApiModelProperty("是否允许远程控制")
    private Integer remote_control;
    //是否远程数据访问
    @ApiModelProperty("是否远程数据访问")
    private Integer remote_data_access;
}
