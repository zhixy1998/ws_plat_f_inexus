package com.persee.workstation.uv_inexus.vo.systemManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemAlgorithmConfVO {
    @ApiModelProperty("算法类型")
    private Integer algorithmType;

    @ApiModelProperty("算法名称")
    private String algorithmName;

    @ApiModelProperty("参数，配置对应的参，后端查询使用")
    private String confParam;

    @ApiModelProperty("参数名，中文名，供前端参考")
    private String confParamName;

    @ApiModelProperty("用户值")
    private String confValue;

    @ApiModelProperty("最小值")
    private String minValue;

    @ApiModelProperty("最大值")
    private String maxValue;
}
