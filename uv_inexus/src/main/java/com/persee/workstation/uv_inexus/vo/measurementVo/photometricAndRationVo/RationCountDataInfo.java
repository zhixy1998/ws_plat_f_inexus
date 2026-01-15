/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class RationCountDataInfo {

    @ApiModelProperty("表头,公式的话需要标题+单位拼接")
    private String headName;

    @ApiModelProperty("标识")
    private String identity;

    @ApiModelProperty("值")
    private Double colValue;

    @ApiModelProperty("修约后原始数据，仅针对需要修约的数")
    private String originalValue;

}
