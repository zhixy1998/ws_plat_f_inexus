/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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

    @ApiModelProperty("是否样品属性")
    private String isSampleAttribute;

    @ApiModelProperty("曲线标识")
    private String curveIdentifier;
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