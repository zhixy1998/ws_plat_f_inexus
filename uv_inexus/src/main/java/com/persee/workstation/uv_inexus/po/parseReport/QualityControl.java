package com.persee.workstation.uv_inexus.po.parseReport;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;

import lombok.Data;
import lombok.EqualsAndHashCode;

// import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_report_quality_control") // 修改表名为 t_report_quality_control
public class QualityControl extends BasePO { // 修改类名为 QualityControl

    @TableField("detection_id") // 映射到 detection_id 字段
    private String detectionId;

    @TableField("controltype") // 映射到 controltype 字段
    private String controlType; // 修改变量名为驼峰命名法

    @TableField("controldescription") // 映射到 controldescription 字段
    private String controlDescription; // 修改变量名为驼峰命名法

    @TableField("controlformula") // 映射到 controlformula 字段
    private String controlFormula; // 修改变量名为驼峰命名法

    // @TableField("create_datetime") // 映射到 create_datetime 字段
    // private java.sql.Timestamp createDatetime; // 使用 java.sql.Timestamp 以匹配 timestamp 类型

    // @TableField("update_datetime") // 映射到 update_datetime 字段
    // private java.sql.Timestamp updateDatetime; // 使用 java.sql.Timestamp 以匹配 timestamp 类型

    // 如果 BasePO 类已经包含 create_time 和 update_time 字段，可以移除以下字段
    // @TableField("create_time")
    // private LocalDateTime createTime;

    // @TableField("update_time")
    // private LocalDateTime updateTime;
}
