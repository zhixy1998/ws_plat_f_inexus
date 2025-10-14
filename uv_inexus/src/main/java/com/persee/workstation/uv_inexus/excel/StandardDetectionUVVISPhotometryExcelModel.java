/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;

public class StandardDetectionUVVISPhotometryExcelModel {
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

    @ExcelProperty("波长4")
    private String wavelength4;

    @ExcelProperty("波长5")
    private String wavelength5;

    @ExcelProperty("波长6")
    private String wavelength6;

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

    public String getWavelength4() {
        return wavelength4;
    }

    public void setWavelength4(String wavelength4) {
        this.wavelength4 = wavelength4;
    }

    public String getWavelength5() {
        return wavelength5;
    }

    public void setWavelength5(String wavelength5) {
        this.wavelength5 = wavelength5;
    }

    public String getWavelength6() {
        return wavelength6;
    }

    public void setWavelength6(String wavelength6) {
        this.wavelength6 = wavelength6;
    }
}
