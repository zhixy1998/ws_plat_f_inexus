/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.concise.common;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FCheckSampleInputParamDto {

    @ApiModelProperty("输入参数列表")
    private List<FInputParam> inputParamList;

    @ApiModelProperty("新增的输入参数")
    private FInputParam newInputParam;

    @ApiModelProperty("类型/修改还是新增,false：新增，true：修改")
    private Boolean type;
}
