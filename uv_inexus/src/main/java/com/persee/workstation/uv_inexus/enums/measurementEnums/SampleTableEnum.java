/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum SampleTableEnum {

    SAMPLE_ID("sampleId", "样品ID"),
    SAMPLE_NUMBER("sampleNumber", "样品编号"),
    SAMPLE_TYPE("sampleType", "样品类型"),
    SAMPLE_KIND("sampleKind", "样品类别"),
    SAMPLE_NAME("sampleName", "样品名称"),
    DILUTION_RATIO("dilutionRatio", "稀释倍数"),
    AMOUNT("amount", "取样量"),
    NORMAL_CONCENTRATION("normalConcentration", "标准浓度"),
    FIT_CONCENTRATION("fitConcentration", "拟合浓度"),
    SAMPLE_LOCATION("sampleLocation", "样品位置"),
    QUALITY("quality", "质控结果"),
    Parallel_ID("parallelSampleId", "最终平行样结果"),
    Parallel_value("parallelSampleResult", "平行值"),
    Abs("Abs", "Abs"),
    SUM_Abs("sumAbs", "ΔAbs"),
    REMARK("remark", "备注"),
    SAMPLING_TIME("samplingTime", "采集时间"),
    ID("id","编号"),
    WAVELENGTH("waveLength", "波长"),
    LUMINOSITY("luminosity", "光度"),
    TIME("time", "时间"),

    //重量
    WEIGHT("2", "重量"),

    PEAKVALLEYGAUGEPOINT("peakValleyGaugePoint", "峰谷/标记点");



    private final String identity;

    private final String name;


    SampleTableEnum(String identity, String name){
        this.identity = identity;
        this.name = name;
    }

    public static String getName(String identity) {
        for (SampleTableEnum c : SampleTableEnum.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.name;
            }
        }
        return null;
    }
}
