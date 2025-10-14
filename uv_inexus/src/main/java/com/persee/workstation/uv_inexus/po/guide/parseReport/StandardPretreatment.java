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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

// import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;

import lombok.Data;
import lombok.EqualsAndHashCode;
// import java.time.LocalDateTime;
@Entity
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_report_standard_pretreatment")
public class StandardPretreatment extends BasePO {
    @Id
    @Column(name = "detection_id")
    private String detectionId;

    @Column(name = "pretreatment_description")
    private String pretreatmentDescription;

}