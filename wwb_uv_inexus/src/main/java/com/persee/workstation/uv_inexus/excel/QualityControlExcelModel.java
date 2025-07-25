package com.persee.workstation.uv_inexus.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
public class QualityControlExcelModel {
    @ExcelIgnore // 忽略此字段，不导出到 Excel
    private Long id;

    @ExcelProperty("标准标识")
    private String detectionId;

    @ExcelProperty("质控类型")
    private String controlType;

    @ExcelProperty("质控描述")
    private String controlDescription;

    @ExcelProperty("质控公式")
    private String controlFormula;

    public String getDetectionId() {
        return detectionId;
    }

    public void setDetectionId(String detectionId) {
        this.detectionId = detectionId;
    }

    public String getControlType() {
        return controlType;
    }

    public void setControlType(String controlType) {
        this.controlType = controlType;
    }

    public String getControlDescription() {
        return controlDescription;
    }

    public void setControlDescription(String controlDescription) {
        this.controlDescription = controlDescription;
    }

    public String getControlFormula() {
        return controlFormula;
    }

    public void setControlFormula(String controlFormula) {
        this.controlFormula = controlFormula;
    }
}
