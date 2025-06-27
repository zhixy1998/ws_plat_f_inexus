package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

/**
 * 光谱样品类
 */
@Data
public class SpectrumSpecimenDto {
    // 样品ID
    private String id;
    // 样品编号
    private String sampleNumber;
    // 样品类型
    private String sampleType;
    // 样品名称
    private String sampleName;
    // 稀释比例
    private String dilutionRatio;
    // 样品体积
    private String sampleVolume;
    // 取样量
    private String constantVolume;
    // 标准浓度
    private String normalConcentration;
    // 样品位置
    private String sampleLocation;
    // 样品描述
    private String sampleDescription;
    // 输入数据
    private String inputData;
}
