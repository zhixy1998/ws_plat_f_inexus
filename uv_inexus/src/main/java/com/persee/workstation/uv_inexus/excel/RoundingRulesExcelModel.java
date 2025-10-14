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

public class RoundingRulesExcelModel {
    @ExcelIgnore // 忽略此字段，不导出到 Excel
    private Long id;
    
    @ExcelProperty("标准标识")
    private String detectionId;

    @ExcelProperty("结果标识")
    private String resultIdentity;

    @ExcelProperty("修约类型")
    private String roundingType;

    @ExcelProperty("修约条件")
    private String roundingCondition;

    @ExcelProperty("位数")
    private Integer decimalPlaces;

    // Getters and Setters
    public String getDetectionId() {
        return detectionId;
    }

    public void setDetectionId(String detectionId) {
        this.detectionId = detectionId;
    }

    public String getResultIdentity() {
        return resultIdentity;
    }

    public void setResultIdentity(String resultIdentity) {
        this.resultIdentity = resultIdentity;
    }

    public String getRoundingType() {
        return roundingType;
    }

    public void setRoundingType(String roundingType) {
        this.roundingType = roundingType;
    }

    public String getRoundingCondition() {
        return roundingCondition;
    }

    public void setRoundingCondition(String roundingCondition) {
        this.roundingCondition = roundingCondition;
    }

    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(Integer decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }
}
