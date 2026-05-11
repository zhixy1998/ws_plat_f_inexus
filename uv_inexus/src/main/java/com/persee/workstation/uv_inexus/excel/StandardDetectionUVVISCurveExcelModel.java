/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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

    @ExcelProperty("系数1")
    private String coefficient1;

    @ExcelProperty("系数2")
    private String coefficient2;

    @ExcelProperty("系数3")
    private String coefficient3;

    @ExcelProperty("光谱带宽")
    private String spectralBandwidth;

    @ExcelProperty("拟合公式")
    private String fittingFormula;

    @ExcelProperty("推荐标样浓度")
    private String recommendedStandardConcentration;

    @ExcelProperty("推荐单位")
    private String recommendedUnits;

    @ExcelProperty("曲线方程")
    private String curveEquation;

    @ExcelProperty("是否需要扣空白")
    private String isBlank;

    @ExcelProperty("加入零点")
    private String joinZeroPoint;

    @ExcelProperty("强制过零点")
    private String forceZeroCrossing;

    @ExcelProperty("斜率")
    private String slope;

    @ExcelProperty("截距")
    private String intercept;

    @ExcelProperty("是否重复")
    private String isRepeat;

    @ExcelProperty("重复次数")
    private String repeatCount;

    @ExcelProperty("重复时间间隔(s)")
    private String repeatInterval;

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

    public String getCoefficient1() {
        return coefficient1;
    }

    public void setCoefficient1(String coefficient1) {
        this.coefficient1 = coefficient1;
    }

    public String getCoefficient2() {
        return coefficient2;
    }

    public void setCoefficient2(String coefficient2) {
        this.coefficient2 = coefficient2;
    }

    public String getCoefficient3() {
        return coefficient3;
    }

    public void setCoefficient3(String coefficient3) {
        this.coefficient3 = coefficient3;
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

    public String getCurveEquation() {
        return curveEquation;
    }

    public void setCurveEquation(String curveEquation) {
        this.curveEquation = curveEquation;
    }

    public String getIsBlank() {
        return isBlank;
    }

    public void setIsBlank(String isBlank) {
        this.isBlank = isBlank;
    }

    public String getJoinZeroPoint() {
        return joinZeroPoint;
    }

    public void setJoinZeroPoint(String joinZeroPoint) {
        this.joinZeroPoint = joinZeroPoint;
    }

    public String getForceZeroCrossing() {
        return forceZeroCrossing;
    }

    public void setForceZeroCrossing(String forceZeroCrossing) {
        this.forceZeroCrossing = forceZeroCrossing;
    }

    public String getSlope() {
        return slope;
    }

    public void setSlope(String slope) {
        this.slope = slope;
    }

    public String getIntercept() {
        return intercept;
    }

    public void setIntercept(String intercept) {
        this.intercept = intercept;
    }

    public String getIsRepeat() {
        return isRepeat;
    }

    public void setIsRepeat(String isRepeat) {
        this.isRepeat = isRepeat;
    }

    public String getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(String repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(String repeatInterval) {
        this.repeatInterval = repeatInterval;
    }
}
