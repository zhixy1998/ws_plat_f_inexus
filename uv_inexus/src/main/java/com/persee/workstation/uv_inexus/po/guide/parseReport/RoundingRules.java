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

package com.persee.workstation.uv_inexus.po.guide.parseReport;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;

import lombok.Data;
import lombok.EqualsAndHashCode;

// import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_report_rounding_rules") // 修改了表名注解
public class RoundingRules extends BasePO { // 修改了类名

    @TableField("detection_id")
    private String detectionId; // 标准标识

    @TableField("result_identity")
    private String resultIdentity; // 结果标识

    @TableField("rounding_type")
    private String roundingType; // 修约类型

    @TableField("rounding_condition")
    private String roundingCondition; // 修约条件

    @TableField("decimal_places")
    private Integer decimalPlaces; // 保留小数位数，使用 Integer 对应数据库的 int 类型

    // 如果 BasePO 类已经包含 create_time 和 update_time 字段，可以移除以下字段
    // @TableField("create_time")
    // private LocalDateTime createTime;

    // @TableField("update_time")
    // private LocalDateTime updateTime;
}
