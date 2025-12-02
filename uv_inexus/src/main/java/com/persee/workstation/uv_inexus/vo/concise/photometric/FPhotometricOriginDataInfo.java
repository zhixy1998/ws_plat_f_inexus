/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.concise.photometric;

import com.persee.workstation.uv_inexus.vo.concise.ration.FRepeatMeasureData;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class FPhotometricOriginDataInfo {

    @ApiModelProperty("光度模式")
    private String mode;

    @ApiModelProperty("波段")
    private String wavelength;

//    @ApiModelProperty("abs的数据值")
//    private String absValue;
//
//    @ApiModelProperty("T%的值")
//    private String tPercentValue;
//
//    @ApiModelProperty("ER的值")
//    private String erValue;
//
//    @ApiModelProperty("ES的值")
//    private String esValue;

    @ApiModelProperty("测量数据")
    private Map<String, String> data;

    @ApiModelProperty("其他数据")
    private Map<String, FRepeatMeasureData> otherData;

    @ApiModelProperty("标识")
    private String identity;

    @ApiModelProperty("测量时间")
    private LocalDateTime measureTime;

}
