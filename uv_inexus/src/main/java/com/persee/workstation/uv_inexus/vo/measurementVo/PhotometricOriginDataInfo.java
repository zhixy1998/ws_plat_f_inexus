package com.persee.workstation.uv_inexus.vo.measurementVo;

import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RepeatMeasureData;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class PhotometricOriginDataInfo {

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
    private Map<String, RepeatMeasureData> otherData;

    @ApiModelProperty("标识")
    private String identity;

    @ApiModelProperty("测量时间")
    private LocalDateTime measureTime;

}
