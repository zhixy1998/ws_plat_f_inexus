/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeviceCalibrationParams {

    @ApiModelProperty("id")
    private String id;

    @ApiModelProperty("参数名称")
    private String paramName;

    @ApiModelProperty("参数--英文名")
    private String param;

    @ApiModelProperty("参数类型：输入框或下拉")
    private Integer paramType;

    @ApiModelProperty("上次设置的值")
    private String lastValue;

    @ApiModelProperty("下拉参数")
    private List<DataNameValue> data;
}
