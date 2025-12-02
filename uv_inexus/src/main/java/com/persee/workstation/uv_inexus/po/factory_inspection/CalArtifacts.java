/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.factory_inspection;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel("标准物质实体表")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_cal_artifacts")
public class CalArtifacts extends BasePO {
    /**
     * 标准实体物质ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    @ApiModelProperty(value="标准实体物质ID")
    private String id;

    /**
     * 标准实体物质名称
     */
    @TableField(value = "entity_name")
    @ApiModelProperty(value="标准实体物质名称")
    private String name;

    /**
     * 标准物ID
     */
    @TableField(value = "category_id")
    @ApiModelProperty(value="标准物ID")
    private String categoryId;
    /**
     * 标准物类型名称
     */
    @TableField(exist = false)
    @ApiModelProperty(value="标准物类型名称")
    private String categoryName;

    /**
     * 特征值
     */
    @TableField(value = "characteristic_value")
    @ApiModelProperty(value="特征值")
    private String characteristicValue;

    /**
     * 标准值
     */
    @TableField(value = "standard_value")
    @ApiModelProperty(value="标准值")
    private String standardValue;
    /**
     * 层级
     */
    @TableField(exist = false)
    @ApiModelProperty(value="层级")
    private Integer level=2;
    /**
     * 是否删除
     */
    @TableField(value = "is_delete")
    @ApiModelProperty(value="是否删除")
    private String isDelete;
}