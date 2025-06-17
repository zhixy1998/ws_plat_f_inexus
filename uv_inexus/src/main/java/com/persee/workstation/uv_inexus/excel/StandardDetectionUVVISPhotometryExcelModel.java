package com.persee.workstation.uv_inexus.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;

public class StandardDetectionUVVISPhotometryExcelModel {
    @ExcelIgnore // 忽略此字段，不导出到 Excel
    private Long id;
    
    @ExcelProperty("标准标识")
    private String standardIdentity;

    @ExcelProperty("波长1")
    private String wavelength1;

    @ExcelProperty("波长2")
    private String wavelength2;

    @ExcelProperty("波长3")
    private String wavelength3;

    // Getters and Setters
    public String getStandardIdentity() {
        return standardIdentity;
    }

    public void setStandardIdentity(String standardIdentity) {
        this.standardIdentity = standardIdentity;
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
}
