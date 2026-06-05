package com.persee.workstation.uv_inexus.vo.measurementVo.spectrum;

import com.persee.workstation.uv_inexus.dto.measurementDto.MeasurePublicParameters;
import com.persee.workstation.uv_inexus.i18n.NoI18n;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * 光谱扫描方法参数Vo类
 */
@Data
public class SpectrumParameterVo extends MeasurePublicParameters {
    // 光度模式(1.abs 2.T% 3.Es 4.Er)
    @NotNull(message = "光度模式不能为空")
    @Pattern(regexp = "^[1-4]$", message = "光度模式值无效")
    private String luminosity;
    // 开始波长
    @NotNull(message = "开始波长不能为空")
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "开始波长必须是数字")
    private String startWaveLength;
    // 结束波长
    @NotNull(message = "结束波长不能为空")
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "结束波长必须是数字")
    private String endWaveLength;
    // 波长间隔
    @NotNull(message = "波长间隔不能为空")
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "波长间隔必须是数字")
    private String waveLengthInterval;
    // 重复模式(0.不重复 1.单池重复)
    @NoI18n
    private String repetitionPattern;
    // 重复次数
    @NotNull(message = "重复次数不能为空")
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "重复次数必须是数字")
    private String repetitionTime;
    // 扫描间隔
    @NotNull(message = "扫描间隔不能为空")
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "扫描间隔必须是数字")
    private String timeInterval;
    // 阶梯校正(false.不启用 true.启用)
    private Boolean ladderCorrection;
    // 扫描速度(0.慢速 1.中速 2.快速 3.极快)
    @NotNull(message = "扫描速度不能为空")
    private String scanSpeed;
    // 消除尖刺(false.不启用 true.启用)
    private Boolean spikeRemoval;
    // 尖刺数组
    private List<String> spikeArray;
}
