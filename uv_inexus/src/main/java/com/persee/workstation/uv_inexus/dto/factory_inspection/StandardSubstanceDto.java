package com.persee.workstation.uv_inexus.dto.factory_inspection;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Data
public class StandardSubstanceDto {
    @ApiModelProperty("标准物质类型")
    private String type;
    @ApiModelProperty("标准物质名称")
    private String name;
    @ApiModelProperty("添加日期")
    private String addDate;
    @ApiModelProperty("添加人")
    private String addBy;
    @ApiModelProperty("特征值")
    private String feature;
    @ApiModelProperty("描述")
    private String remark;
}
