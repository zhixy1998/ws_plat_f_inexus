package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.List;

@Data
public class SpectralReportVo {
    // 仪器波长
    private String InstrumentWavelength;
    // 仪器编号
    private String InstrumentSerialNumber;
    // 光度模式
    private String LuminosityMode;
    // 光谱带宽
    private String SpectrumBandwidth;
    // 参考方法
    private String ReferenceMethod;
    // 参考标准
    private String ReferenceStandard;
    // 备注
    private String Remark;
    // 样品名称
    private String SampleName;
    // 样品编号
    private String SampleNumber;
    // 检测开始时间
    private String DetectionStartTime;
    // 检测结束时间
    private String DetectionEndTime;
    // 项目
    private String Project;
    // 检测人
    private String Detector;
    // 能量灯
    private String EnergyLight;
    // 计算公式
    private String CalculationFormula;
    // 阶梯校正
    private String StairCorrection;
    // 光谱扫描图片
    private byte[] SpectrumScanImage;
    // 原始数据
    private String OriginalData;
    // 增益
    private String Gain;
    // 扫描范围
    private String ScanRange;
    // 扫描速度
    private String ScanSpeed;
    // 扫描间隔
    private String ScanInterval;
    // 数据类型
    private String DataType;
    // 方法名称
    private String MethodName;
    //操作步骤
    private List<String> steps;
}
