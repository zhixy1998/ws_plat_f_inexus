package com.persee.workstation.uv_inexus.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;

// 标曲法
public class StandardDetectionUVVISCurveExcelModel {
    @ExcelIgnore // 忽略此字段，不导出到 Excel
    private Long id;

    @ExcelProperty("标准标识")
    private String detectionId;

    @ExcelProperty("波长1")
    private String wavelength1;

    @ExcelProperty("波长2")
    private String wavelength2;

    @ExcelProperty("波长3")
    private String wavelength3;

    @ExcelProperty("光谱带宽")
    private String spectralBandwidth;

    @ExcelProperty("拟合公式")
    private String fittingFormula;

    @ExcelProperty("推荐标样浓度")
    private String recommendedStandardConcentration;

    @ExcelProperty("推荐单位")
    private String recommendedUnits;

    // Getters and Setters
    public String getDetectionId() {
        return detectionId;
    }

    public void setDetectionId(String detectionId) {
        this.detectionId = detectionId;
    }

    public String getWavelength1() {
        return wavelength1;
    }

    public void setWavelength1(String wavelength1) {
        this.wavelength1 = wavelength1;
    }

    public String getWavelength2() {
        return wavelength2;
    }

    public void setWavelength2(String wavelength2) {
        this.wavelength2 = wavelength2;
    }

    public String getWavelength3() {
        return wavelength3;
    }

    public void setWavelength3(String wavelength3) {
        this.wavelength3 = wavelength3;
    }

    public String getSpectralBandwidth() {
        return spectralBandwidth;
    }

    public void setSpectralBandwidth(String spectralBandwidth) {
        this.spectralBandwidth = spectralBandwidth;
    }

    public String getFittingFormula() {
        return fittingFormula;
    }

    public void setFittingFormula(String fittingFormula) {
        this.fittingFormula = fittingFormula;
    }

    public String getRecommendedStandardConcentration() {
        return recommendedStandardConcentration;
    }

    public void setRecommendedStandardConcentration(String recommendedStandardConcentration) {
        this.recommendedStandardConcentration = recommendedStandardConcentration;
    }

    public String getRecommendedUnits() {
        return recommendedUnits;
    }

    public void setRecommendedUnits(String recommendedUnits) {
        this.recommendedUnits = recommendedUnits;
    }
}
