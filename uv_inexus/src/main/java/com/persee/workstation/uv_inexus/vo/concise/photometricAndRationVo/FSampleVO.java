/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("样品信息")
public class FSampleVO {

    @ApiModelProperty("样品ID")
    private String sampleId;

    @ApiModelProperty("样品编号")
    private String sampleNumber;

    @ApiModelProperty("样品名称")
    private String sampleName;

    @ApiModelProperty("样品类型")
    private String sampleType;

    @ApiModelProperty("样品种类,0-标准样品，1-未知样品")
    private String sampleKind;

    @ApiModelProperty("标准浓度")
    private String normalConcentration;

    @ApiModelProperty("稀释比")
    private String dilutionRatio;

    @ApiModelProperty("样品位置")
    private String sampleLocation;

    @ApiModelProperty("平行样组id")
    private String parallelGroupId;

    // 取样量
    @ApiModelProperty("取样量")
    private String sampleVolume;

}
