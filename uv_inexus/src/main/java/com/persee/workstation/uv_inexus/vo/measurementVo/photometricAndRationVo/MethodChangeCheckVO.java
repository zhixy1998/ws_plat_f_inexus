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

/**
 * @author xudian
 */
@Data
public class MethodChangeCheckVO {

    @ApiModelProperty("当前方法id")
    private String currentMethodId;

    @ApiModelProperty("参数是否有改变，true-改变，false-未改变")
    private Boolean paramChange;

    @ApiModelProperty("提示信息")
    private String message;

    @ApiModelProperty("清除数据范围")
    private String clearDataRange;
}
