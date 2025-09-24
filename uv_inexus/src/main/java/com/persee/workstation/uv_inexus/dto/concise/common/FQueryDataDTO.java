package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FQueryDataDTO {
    private String taskId;
    @ApiModelProperty("是否显示被删除的数据")
    private Boolean allDisplay;
    @ApiModelProperty("小数位数")
    private Integer digit;
    @ApiModelProperty("要显示的数据类型：平均值/sd/rsd等")
    private List<String> option;

    @ApiModelProperty("仪器序列号")
    private String deviceSerialNumber;
 }
