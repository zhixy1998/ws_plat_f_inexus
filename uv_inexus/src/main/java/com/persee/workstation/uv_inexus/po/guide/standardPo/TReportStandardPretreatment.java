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

package com.persee.workstation.uv_inexus.po.guide.standardPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 标准前处理表
 */
@Data
@TableName("t_report_standard_pretreatment")
public class TReportStandardPretreatment {
    /**
     * 数据唯一标识，使用UUID自动分配
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private Integer id;

    /**
     * 检测标识ID
     */
    @TableField("detection_id")
    private String detectionId;

    /**
     * 前处理描述
     */
    @TableField("pretreatment_description")
    private String pretreatmentDescription;
}
