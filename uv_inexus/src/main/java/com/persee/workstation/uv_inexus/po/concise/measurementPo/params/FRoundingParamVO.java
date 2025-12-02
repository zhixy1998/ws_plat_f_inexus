/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("修约条件")
public class FRoundingParamVO {

    @ApiModelProperty("上限值")
    private Double upperLimit;
    @ApiModelProperty("是否包含上限值")
    private Boolean isUpperLimitIncluded;

    @ApiModelProperty("下限值")
    private Double lowerLimit;
    @ApiModelProperty("是否包含下限值")
    private Boolean isLowerLimitIncluded;

    @ApiModelProperty("修约方式")
    private String roundingType;

    @ApiModelProperty("前端用")
    private String id;

    @ApiModelProperty("位数")
    private Integer decimalPlaces;

}
