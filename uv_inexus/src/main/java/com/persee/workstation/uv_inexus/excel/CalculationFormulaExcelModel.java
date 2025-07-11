package com.persee.workstation.uv_inexus.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
public class CalculationFormulaExcelModel {
    @ExcelIgnore // 忽略此字段，不导出到 Excel
    private Long id;
    
    @ExcelProperty("标准标识")
    private String detectionId;

    @ExcelProperty("结果标识")
    private String resultIdentity;

    @ExcelProperty("结果名称")
    private String resultName;

    @ExcelProperty("单位")
    private String unit;

    @ExcelProperty("输入参数1名称")
    private String inputParam1Name;

    @ExcelProperty("输入参数1标识")
    private String inputParam1Identity;

    @ExcelProperty("输入参数2名称")
    private String inputParam2Name;

    @ExcelProperty("输入参数2标识")
    private String inputParam2Identity;

    @ExcelProperty("输入参数3名称")
    private String inputParam3Name;

    @ExcelProperty("输入参数3标识")
    private String inputParam3Identity;

    @ExcelProperty("输入参数4名称")
    private String inputParam4Name;

    @ExcelProperty("输入参数4标识")
    private String inputParam4Identity;

    @ExcelProperty("输入参数5名称")
    private String inputParam5Name;

    @ExcelProperty("输入参数5标识")
    private String inputParam5Identity;

    @ExcelProperty("输入参数6名称")
    private String inputParam6Name;

    @ExcelProperty("输入参数6标识")
    private String inputParam6Identity;

    @ExcelProperty("输入参数7名称")
    private String inputParam7Name;

    @ExcelProperty("输入参数7标识")
    private String inputParam7Identity;

    @ExcelProperty("输入参数8名称")
    private String inputParam8Name;

    @ExcelProperty("输入参数8标识")
    private String inputParam8Identity;

    @ExcelProperty("输入参数9名称")
    private String inputParam9Name;

    @ExcelProperty("输入参数9标识")
    private String inputParam9Identity;

    @ExcelProperty("输入参数10名称")
    private String inputParam10Name;

    @ExcelProperty("输入参数10标识")
    private String inputParam10Identity;

    @ExcelProperty("公式文本")
    private String formulaText;

    @ExcelProperty("公式图片")
    private String formulaImage;

    @ExcelProperty("检出限")
    private String detectionLimit;

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

    public String getResultName() {
        return resultName;
    }

    public void setResultName(String resultName) {
        this.resultName = resultName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getInputParam1Name() {
        return inputParam1Name;
    }

    public void setInputParam1Name(String inputParam1Name) {
        this.inputParam1Name = inputParam1Name;
    }
    public String getInputParam1Identity() {
        return inputParam1Identity;
    }

    public void setInputParam1Identity(String inputParam1Identity) {
        this.inputParam1Identity = inputParam1Identity;
    }

    public String getInputParam2Name() {
        return inputParam2Name;
    }

    public void setInputParam2Name(String inputParam2Name) {
        this.inputParam2Name = inputParam2Name;
    }

    public String getInputParam2Identity() {
        return inputParam2Identity;
    }

    public void setInputParam2Identity(String inputParam2Identity) {
        this.inputParam2Identity = inputParam2Identity;
    }

    public String getInputParam3Name() {
        return inputParam3Name;
    }

    public void setInputParam3Name(String inputParam3Name) {
        this.inputParam3Name = inputParam3Name;
    }

    public String getInputParam3Identity() {
        return inputParam3Identity;
    }

    public void setInputParam3Identity(String inputParam3Identity) {
        this.inputParam3Identity = inputParam3Identity;
    }

    public String getInputParam4Name() {
        return inputParam4Name;
    }

    public void setInputParam4Name(String inputParam4Name) {
        this.inputParam4Name = inputParam4Name;
    }

    public String getInputParam4Identity() {
        return inputParam4Identity;
    }

    public void setInputParam4Identity(String inputParam4Identity) {
        this.inputParam4Identity = inputParam4Identity;
    }

    public String getInputParam5Name() {
        return inputParam5Name;
    }

    public void setInputParam5Name(String inputParam5Name) {
        this.inputParam5Name = inputParam5Name;
    }

    public String getInputParam5Identity() {
        return inputParam5Identity;
    }

    public void setInputParam5Identity(String inputParam5Identity) {
        this.inputParam5Identity = inputParam5Identity;
    }

    public String getInputParam6Name() {
        return inputParam6Name;
    }

    public void setInputParam6Name(String inputParam6Name) {
        this.inputParam6Name = inputParam6Name;
    }

    public String getInputParam6Identity() {
        return inputParam6Identity;
    }

    public void setInputParam6Identity(String inputParam6Identity) {
        this.inputParam6Identity = inputParam6Identity;
    }

    public String getInputParam7Name() {
        return inputParam7Name;
    }

    public void setInputParam7Name(String inputParam7Name) {
        this.inputParam7Name = inputParam7Name;
    }

    public String getInputParam7Identity() {
        return inputParam7Identity;
    }

    public void setInputParam7Identity(String inputParam7Identity) {
        this.inputParam7Identity = inputParam7Identity;
    }

    public String getInputParam8Name() {
        return inputParam8Name;
    }

    public void setInputParam8Name(String inputParam8Name) {
        this.inputParam8Name = inputParam8Name;
    }

    public String getInputParam8Identity() {
        return inputParam8Identity;
    }

    public void setInputParam8Identity(String inputParam8Identity) {
        this.inputParam8Identity = inputParam8Identity;
    }

    public String getInputParam9Name() {
        return inputParam9Name;
    }

    public void setInputParam9Name(String inputParam9Name) {
        this.inputParam9Name = inputParam9Name;
    }

    public String getInputParam9Identity() {
        return inputParam9Identity;
    }

    public void setInputParam9Identity(String inputParam9Identity) {
        this.inputParam9Identity = inputParam9Identity;
    }
    
    public String getInputParam10Name() {
        return inputParam10Name;
    }

    public void setInputParam10Name(String inputParam10Name) {
        this.inputParam10Name = inputParam10Name;
    }

    public String getInputParam10Identity() {
        return inputParam10Identity;
    }

    public void setInputParam10Identity(String inputParam10Identity) {
        this.inputParam10Identity = inputParam10Identity;
    }

    public String getFormulaText() {
        return formulaText;
    }

    public void setFormulaText(String formulaText) {
        this.formulaText = formulaText;
    }
    
    public String getFormulaImage() {
        return formulaImage;
    }

    public void setFormulaImage(String formulaImage) {
        this.formulaImage = formulaImage;
    }

    public String getDetectionLimit() {
        return detectionLimit;
    }

    public void setDetectionLimit(String detectionLimit) {
        this.detectionLimit = detectionLimit;
    }
}