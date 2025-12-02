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
 * 检测参数及子表字段DTO
 */
@ApiModel("检测参数及子表字段DTO")
@Data
public class DetectionParametersDTO {
    @ApiModelProperty("测量参数")
    private String detectionParameters; // 主表JSON参数

    @ApiModelProperty("前处理描述（前处理表）")
    private String preTreatmentDesc; // 对应pretreatment_description

    @ApiModelProperty("质控类型（质量控制表）")
    private String qcType; // 对应control_type

    @ApiModelProperty("结果表示（计算公式表）")
    private String resultExpression; // 对应result_expression
}