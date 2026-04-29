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

import javax.validation.constraints.Size;

@ApiModel("标准物质实体表")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalArtifactsSaveDto  {
    /**
     * 标准实体物质ID
     */
    @ApiModelProperty(value="标准实体物质ID")
    private String id;

    /**
     * 标准实体物质名称
     */
    @ApiModelProperty(value="标准实体物质名称")
    @Size(max = 32, message = "名称长度不能超过32个字符")
    private String name;

    /**
     * 标准物ID
     */
    @ApiModelProperty(value="标准物ID")
    private String categoryId;

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

    /**
     * 有效期截止日期
     */
    @ApiModelProperty(value="有效期截止日期")
    private java.util.Date expiryDate;

    /**
     * 是否启用效期提醒：0-否，1-是
     */
    @ApiModelProperty(value="是否启用效期提醒：0-否，1-是")
    private Integer reminderEnabled;

    /**
     * 提前提醒天数
     */
    @ApiModelProperty(value="提前提醒天数")
    private Integer reminderDays;

}