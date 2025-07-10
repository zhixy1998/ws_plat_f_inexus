package com.persee.workstation.uv_inexus.po.standardPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标准信息表
 */
@Data
@TableName("t_report_standard_detection")
public class TReportStandardDetection {
    /**
     * 数据唯一标识，使用UUID自动分配
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 标准标识
     */
    @TableField("standard_identity")
    private String standardIdentity;

    /**
     * 行业分类
     */
    @TableField("industry")
    private String industry;

    /**
     * 检测类型
     */
    @TableField("detection_type")
    private String detectionType;

    /**
     * 检测仪器
     */
    @TableField("detection_instrument")
    private String detectionInstrument;

    /**
     * 标准号
     */
    @TableField("standard_number")
    private String standardNumber;

    /**
     * 标准名称
     */
    @TableField("standard_name")
    private String standardName;

    /**
     * 检测项目
     */
    @TableField("detection_item")
    private String detectionItem;

    /**
     * 检测方法编号
     */
    @TableField("method_number")
    private String methodNumber;

    /**
     * 备注信息
     */
    @TableField("remark")
    private String remark;

    /**
     * 检测参数
     */
    @TableField("detection_parameters")
    private String detectionParameters;

    /**
     * 发布时间
     */
    @TableField("release_date")
    private LocalDateTime releaseDate;

    /**
     * 失效时间
     */
    @TableField("expiration_date")
    private LocalDateTime expirationDate;

    /**
     * 实施时间
     */
    @TableField("effective_date")
    private LocalDateTime effectiveDate;

    /**
     * 发布状态
     */
    @TableField("release_status")
    private String releaseStatus;

    /**
     * 行业类型
     */
    @TableField("industry_type")
    private String industryType;
}
