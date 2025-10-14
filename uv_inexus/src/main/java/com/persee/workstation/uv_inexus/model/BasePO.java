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

package com.persee.workstation.uv_inexus.model;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode()
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePO implements Serializable {

    @TableId(value = "id",type = IdType.ASSIGN_UUID)
    @ApiModelProperty("主键id，插入时自动生成")
    private String id;

    @TableField(value = "create_user")
    @ApiModelProperty("创建者")
    private String createBy;

    @TableField(value = "update_user")
    @ApiModelProperty("修改者")
    private String updateBy;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    @ApiModelProperty("创建时间,插入数据库时自动填充")
    private LocalDateTime created;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @ApiModelProperty("修改时间，更新操作时自动填充")
    private LocalDateTime updated;

}
