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

package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CreateSampleNumPreInfo {

    @ApiModelProperty("名称前缀")
    private String sampleNamePre;

    @ApiModelProperty("名称开始号")
    private Integer nameStartNum;

    @ApiModelProperty("名称递增值")
    private Integer stepNum;

    @ApiModelProperty("编号前缀")
    private String sampleNumPre;

    @ApiModelProperty("编号开始号")
    private Integer numStartNum;

    @ApiModelProperty("编号递增值")
    private Integer numStepNum;

}
