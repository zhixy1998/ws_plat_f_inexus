/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Pattern;

/**
 * 寻找峰谷差参数
 */
@Data
public class FSeekingPeaksValleysDto {
    // 参数id(测量id和分析id共用一个)
    private String dataId;
    // 峰谷差阈值
    @ApiModelProperty("峰谷差阈值")
    @Digits(integer = 10, fraction = 2, message = "峰谷差阈值必须是数字，最多保留2位小数")
    @DecimalMin(value = "0.0", inclusive = false, message = "峰谷差阈值必须大于0")
    private String height;  // 可以为空，有值时才验证

    // 峰间距
    @ApiModelProperty("峰间距")
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "峰间距必须是有效的数字")
    private String distance;  // 可以为空，有值时才验证
}
