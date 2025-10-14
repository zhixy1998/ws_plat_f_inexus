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

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_report_upgrade_version")
public class UpgradeVersion {
    private Long id;

    @TableField("version")
    private String version; // 版本号（如v2.1.3）

    @TableField("create_time")
    private LocalDateTime createTime; // 创建时间

    @TableField("update_description")
    private String updateDescription; // 上传的文件名
}