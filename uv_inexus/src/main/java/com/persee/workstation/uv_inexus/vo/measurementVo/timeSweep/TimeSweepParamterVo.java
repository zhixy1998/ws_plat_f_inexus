package com.persee.workstation.uv_inexus.vo.measurementVo.timeSweep;

import com.persee.workstation.uv_inexus.dto.measurementDto.MeasurePublicParameters;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class TimeSweepParamterVo  extends MeasurePublicParameters {
    // 光度模式(1.abs 2.T% 3.Es 4.Er)
    @NotNull(message = "光度模式不能为空")
    @Pattern(regexp = "^[1-4]$", message = "光度模式值无效")
    private String luminosity;
    // 波长值
    @NotNull(message = "波长不能为空")
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "波长必须是数字")
    private String waveLength;
    // 扫描总时长
    @NotNull(message = "扫描总时长不能为空")
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "扫描总时长必须是数字")
    private String scanTime;
    // 扫描单位(0.分 1.秒)
    @NotNull(message = "扫描单位不能为空")
    @Pattern(regexp = "^[0-1]$", message = "扫描单位值无效")
    private String scanUnit;
    // 时间间隔
    @NotNull(message = "时间间隔不能为空")
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "时间间隔必须是数字")
    private String intervalTime;
    // 扫描总个数
    private String totalNumber;
    // 多联池模式(0.顺序单池连续采样,1.循环多池交替采样)
    private String multiPoolMode;
}
