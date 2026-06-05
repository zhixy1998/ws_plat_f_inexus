/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import com.persee.workstation.uv_inexus.i18n.NoI18n;
import lombok.Data;

import java.util.List;

/**
 * 光谱扫描方法参数
 */
@Data
public class FSpectrumParameter extends FMeasurePublicParameters {
    // 光度模式(1.abs 2.T% 3.Es 4.Er)
    private String luminosity;
    // 开始波长
    private String startWaveLength;
    // 结束波长
    private String endWaveLength;
    // 波长间隔
    private String waveLengthInterval;
    // 重复模式(0.不重复 1.单池重复)
    @NoI18n
    private String repetitionPattern;
    // 重复次数
    private String repetitionTime;
    // 扫描间隔
    private String timeInterval;
    // 阶梯校正(false.不启用 true.启用)
    private Boolean ladderCorrection;
    // 扫描速度(0.慢速 1.中速 2.快速 3.极快)
    private String scanSpeed;
    // 消除尖刺(false.不启用 true.启用)
    private Boolean spikeRemoval;
    // 尖刺数组
    private List<String> spikeArray;
}
