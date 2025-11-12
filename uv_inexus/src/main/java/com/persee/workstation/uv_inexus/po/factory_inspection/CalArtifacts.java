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