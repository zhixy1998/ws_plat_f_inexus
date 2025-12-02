/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.factory_inspection;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@ApiModel("标准物质分类")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalStdCatsSaveDto {
    /**
     * 标准物质ID
     */
    @ApiModelProperty(value="标准物质ID")
    private String id;

    /**
     * 标准物质名称
     */
    @ApiModelProperty(value="标准物质名称")
    private String name;

    /**
     * 标准物质描述
     */
    @ApiModelProperty(value="标准物质描述")
    private String remark ;

    /**
     * 特征值
     */
    @ApiModelProperty(value="特征值")
    private String characteristicValue;

    /**
     * 标准值
     */
    @ApiModelProperty(value="标准值")
    private String standardValue;
}