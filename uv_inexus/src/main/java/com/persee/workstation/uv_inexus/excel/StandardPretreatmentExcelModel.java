package com.persee.workstation.uv_inexus.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;

public class StandardPretreatmentExcelModel {
    @ExcelIgnore // 忽略此字段，不导出到 Excel
    private Long id;

    @ExcelProperty("标准标识")
    private String standardIdentity;

    @ExcelProperty("前处理描述")
    private String pretreatmentDescription;

    public StandardPretreatmentExcelModel() {
    }

    // public StandardPretreatmentExcelModel(String standardIdentity, String pretreatmentDescription) {
    //     this.standardIdentity = standardIdentity;
    //     this.pretreatmentDescription = pretreatmentDescription;
    // }

    // getters and setters
    public String getStandardIdentity() {
        return standardIdentity;
    }

    public void setStandardIdentity(String standardIdentity) {
        this.standardIdentity = standardIdentity;
    }

    public String getPretreatmentDescription() {
        return pretreatmentDescription;
    }

    public void setPretreatmentDescription(String pretreatmentDescription) {
        this.pretreatmentDescription = pretreatmentDescription;
    }
}
