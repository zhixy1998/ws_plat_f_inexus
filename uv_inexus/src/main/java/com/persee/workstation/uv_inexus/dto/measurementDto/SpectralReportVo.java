package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class SpectralReportVo {

    //仪器名称
    private String instrumentName;
    //仪器型号
    private String instrumentModel;
    // 仪器波长
    private String InstrumentWavelength;
    // 起始波长
    private String StartWaveLength;
    // 结束波长
    private String EndWaveLength;
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
    // 时间扫描图片
    private byte[] TimeScanImage;
    // 原始数据
    private String OriginalData;
    // 时间单位
    private String TimeUnit;
    // 时间单位名称
    private String TimeUnitName;
    // 扫描时间
    private String ScanTime;
    // 扫描总个数
    private String totalNumber;
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
    // 任务名称
    private String TaskName;
    //操作步骤
    private String steps;
    //环境温度
    private String ambientTemperature;
    //相对湿度
    private String relativeHumidity;
    //质量等级
    private String qualityLevel;
    //公式参数
    private String formulaParams;
    //比色皿光程
    private String colorPlateWavelength;
    //样品状态
    private String sampleState;
    //样品采样时间
    private String samplingTime;
    //标准浓度
    private String standardConcentration;
    //曲线公式
    private String curveFormula;
    //曲线参数，k0，k1，k2，k3，k4---k0代表截距（参考曲线拟合）
    private String curveParams;
    //R
    private String R;
    //曲线类型：一次曲线，二次曲线
    private String curveType;

    // 时间间隔
    private String timeInterval;
    // 重复模式
    private String repetitionPattern;
    // 重复次数
    private String repetitionTime;
    // 波长间隔
    private String waveLengthInterval;
    // 响应时间
    private String responseTime;
    // 换灯波长
    private String changeLampWaveLength;
    // 多联池模式
    private String multiPoolMode;


}
