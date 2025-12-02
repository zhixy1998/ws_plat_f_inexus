/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.parseReport;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

// import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_report_standard_detection")
public class Detection extends BasePO {
    @TableId(type = IdType.AUTO) // 自增主键
    @ApiModelProperty("ID")
    private String id; // 原类型可能是String，改为Integer
    
    @TableField("standard_identity")
    @ApiModelProperty("标准标识")
    private String standardIdentity;

    @TableField("industry")
    @ApiModelProperty("行业")
    private String industry;

    @TableField("detection_type")
    @ApiModelProperty("检测类型")
    private String detectionType;

    @TableField("detection_instrument")
    @ApiModelProperty("检测仪器")
    private String detectionInstrument;

    @TableField("standard_number")
    @ApiModelProperty("标准号")
    private String standardNumber;

    @TableField("standard_name")
    @ApiModelProperty("标准名称")
    private String standardName;

    @TableField("detection_item")
    @ApiModelProperty("检测项目")
    private String detectionItem;

    @TableField("method_number")
    @ApiModelProperty("第几法")
    private String methodNumber;

    @TableField("remark")
    @ApiModelProperty("备注")
    private String remark;

    @TableField("detection_parameters")
    @ApiModelProperty("检测参数")
    private String detectionParameters;

    @TableField("release_date")
    @ApiModelProperty("发布时间")
    private String releaseDate;

    @TableField("expiration_date")
    @ApiModelProperty("失效时间")
    private String expirationDate;

    @TableField("effective_date")
    @ApiModelProperty("实施时间")
    private String effectiveDate;

    @TableField("release_status")
    @ApiModelProperty("状态")
    private String releaseStatus;
    
    @TableField("industry_type") // 如果这个字段不存储在数据库，可以不加这个注解，或者加 value="industry_type" 指定列名
    @ApiModelProperty("类型")
    private String industryType; // 标准类型
    
    // 如果 BasePO 类已经包含 create_time 和 update_time 字段，可以移除以下字段
    // @TableField("create_time")
    // private LocalDateTime createTime;

    // @TableField("update_time")
    // private LocalDateTime updateTime;
}

