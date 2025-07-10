package com.persee.workstation.uv_inexus.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;

public class StandardPretreatmentExcelModel {
    @ExcelIgnore // 忽略此字段，不导出到 Excel
    private Long id;

    @ExcelProperty("标准标识")
    private String detectionId;

    @ExcelProperty("前处理描述")
    private String pretreatmentDescription;

    public StandardPretreatmentExcelModel() {
    }

    // public StandardPretreatmentExcelModel(String detectionId, String pretreatmentDescription) {
    //     this.detectionId = detectionId;
    //     this.pretreatmentDescription = pretreatmentDescription;
    // }

    // getters and setters
    public String getDetectionId() {
        return detectionId;
    }

    public void setDetectionId(String detectionId) {
        this.detectionId = detectionId;
    }

    public String getPretreatmentDescription() {
        return pretreatmentDescription;
    }

    public void setPretreatmentDescription(String pretreatmentDescription) {
        this.pretreatmentDescription = pretreatmentDescription;
    }
}
