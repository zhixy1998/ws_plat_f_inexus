/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.parseReport;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 检测报告列表DTO（去重后）
 */
@Data
@ApiModel("检测报告列表返回DTO")
public class DetectionListDTO {
    private String id;
    
    @ApiModelProperty("行业")
    private String industry;

    @ApiModelProperty("标准名称")
    private String standardName;

    @ApiModelProperty("标准号（去重依据）")
    private String standardNumber;

    @ApiModelProperty("发布时间")
    private String releaseDate; // 保持与PO一致的String类型

    @ApiModelProperty("失效时间")
    private String expirationDate;

    @ApiModelProperty("实施时间")
    private String effectiveDate;

    @ApiModelProperty("行业类型")
    private String industryType;

    @ApiModelProperty("状态")
    private String releaseStatus;

    @ApiModelProperty("标准号出现次数（统计字段）")
    private Integer count;
}