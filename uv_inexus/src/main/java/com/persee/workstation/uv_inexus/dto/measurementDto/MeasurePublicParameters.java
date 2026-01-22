/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
public class MeasurePublicParameters {
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
    @Pattern(regexp = "^[0-2]$", message = "能量灯值无效")
    private String energyLamp;
    //能量灯的名称
    private String energyLampName;
    // 增益
    @Min(value = 0, message = "增益不能小于0")
    private Integer gain;
}
