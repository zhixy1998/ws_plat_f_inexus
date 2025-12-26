/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.concise.spectrum;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
public class FSpectrumParameterVo {
    // 光度模式(1.abs 2.T% 3.Es 4.Er)
    @NotNull(message = "光度模式不能为空")
    @Pattern(regexp = "^[1-4]$", message = "光度模式值无效")
    private String luminosity;
    // 光度模式名称
    private String luminosityName;
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
    // 阶梯校正名称
    private String ladderCorrectionName;
    // 扫描速度(0.慢速 1.中速 2.快速 3.极快)
    @NotNull(message = "扫描速度不能为空")
    @Pattern(regexp = "^[0-3]$", message = "扫描速度值无效")
    private String scanSpeed;
    // 扫描速度名称
    private String scanSpeedName;
    // 参考标准名称
    private String referenceStandardName;
    // 前处理参数
    private List<String> preProcessParameters;
    // 光谱带宽
    @NotNull(message = "光谱带宽不能为空")
    private String spectralBandwidth;
    // 响应时间
    private String responseTime;
    // 换灯波长
    @NotNull(message = "换灯波长不能为空")
    private String changeLampWaveLength;
    // 能量灯(0.钨灯 1.氘灯 2.汞灯)
    @NotNull(message = "能量灯不能为空")
    @Pattern(regexp = "^[0-2]$", message = "能量灯值无效")
    private String energyLamp;
    //能量灯的名称
    private String energyLampName;
    // 增益
    @NotNull(message = "增益不能为空")
    @Min(value = 0, message = "增益不能小于0")
    private Integer gain;
}
