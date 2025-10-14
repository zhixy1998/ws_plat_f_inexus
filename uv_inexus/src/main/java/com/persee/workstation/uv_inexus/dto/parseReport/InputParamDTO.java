/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.dto.parseReport;

import java.util.Objects;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 输入参数DTO
 */
@ApiModel("输入参数")
@Data
public class InputParamDTO {
    @ApiModelProperty("参数名称（显示标题）")
    private String paramName;
    
    @ApiModelProperty("单位")
    private String unit;
    
    @ApiModelProperty("标记")
    private String resultIdentity;

    // 重写equals和hashCode用于去重
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputParamDTO that = (InputParamDTO) o;
        return Objects.equals(paramName, that.paramName) &&
                Objects.equals(unit, that.unit) &&
                Objects.equals(resultIdentity, that.resultIdentity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paramName, unit, resultIdentity);

    }
}