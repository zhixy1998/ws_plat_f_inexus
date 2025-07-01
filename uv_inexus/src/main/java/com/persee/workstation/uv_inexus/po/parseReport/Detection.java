package com.persee.workstation.uv_inexus.po.parseReport;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;

import lombok.Data;
import lombok.EqualsAndHashCode;

// import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_report_standard_detection")
public class Detection extends BasePO {

    @TableField("standard_identity")
    private String standardIdentity;

    @TableField("industry")
    private String industry;

    @TableField("detection_type")
    private String detectionType;

    @TableField("detection_instrument")
    private String detectionInstrument;

    @TableField("standard_number")
    private String standardNumber;

    @TableField("standard_name")
    private String standardName;

    @TableField("detection_item")
    private String detectionItem;

    @TableField("method_number")
    private String methodNumber;

    @TableField("remark")
    private String remark;

    @TableField("detection_parameters")
    private String detectionParameters;

    @TableField("release_date")
    private String releaseDate;

    @TableField("expiration_date")
    private String expirationDate;

    @TableField("effective_date")
    private String effectiveDate;

    @TableField("release_status")
    private String releaseStatus;
    
    @TableField("industry_type") // 如果这个字段不存储在数据库，可以不加这个注解，或者加 value="industry_type" 指定列名
    private String industryType; // 标准类型
    
    // 如果 BasePO 类已经包含 create_time 和 update_time 字段，可以移除以下字段
    // @TableField("create_time")
    // private LocalDateTime createTime;

    // @TableField("update_time")
    // private LocalDateTime updateTime;
}

