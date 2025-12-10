/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.concise.timesweep;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
public class FTimeSweepParamterVo {
    // 光度模式(1.abs 2.T% 3.Es 4.Er)
    @NotNull(message = "光度模式不能为空")
    @Pattern(regexp = "^[1-4]$", message = "光度模式值无效")
    private String luminosity;
    // 光度模式名称
    private String luminosityName;
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
    // 扫描单位名称
    private String scanUnitName;
    // 时间间隔
    @NotNull(message = "时间间隔不能为空")
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "时间间隔必须是数字")
    private String intervalTime;
    // 扫描总个数
    private String totalNumber;
    // 多联池模式(0.顺序单池连续采样,1.循环多池交替采样)
    private String multiPoolMode;
    // 多联池模式名称
    private String multiPoolModeName;
    // 参考标准名称
    private String referenceStandardName;
    // 前处理参数
    private List<String> preProcessParameters;
    // 光谱带宽
    @NotNull(message = "光谱带宽不能为空")
    private String spectralBandwidth;
    // 响应时间
    @NotNull(message = "响应时间不能为空")
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
