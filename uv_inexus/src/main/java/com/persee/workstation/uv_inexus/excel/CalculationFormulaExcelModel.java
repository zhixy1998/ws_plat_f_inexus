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

    @ExcelProperty("是否为样品输入参数1")
    private String isSampleAttribute1;

    @ExcelProperty("软件绑定标识1")
    private String curveIdentifier1;

    @ExcelProperty("输入参数2名称")
    private String inputParam2Name;

    @ExcelProperty("输入参数2标识")
    private String inputParam2Identity;

    @ExcelProperty("是否为样品输入参数2")
    private String isSampleAttribute2;

    @ExcelProperty("软件绑定标识2")
    private String curveIdentifier2;

    @ExcelProperty("输入参数3名称")
    private String inputParam3Name;

    @ExcelProperty("输入参数3标识")
    private String inputParam3Identity;

    @ExcelProperty("是否为样品输入参数3")
    private String isSampleAttribute3;

    @ExcelProperty("软件绑定标识3")
    private String curveIdentifier3;

    @ExcelProperty("输入参数4名称")
    private String inputParam4Name;

    @ExcelProperty("输入参数4标识")
    private String inputParam4Identity;

    @ExcelProperty("是否为样品输入参数4")
    private String isSampleAttribute4;

    @ExcelProperty("软件绑定标识4")
    private String curveIdentifier4;

    @ExcelProperty("输入参数5名称")
    private String inputParam5Name;

    @ExcelProperty("输入参数5标识")
    private String inputParam5Identity;

    @ExcelProperty("是否为样品输入参数5")
    private String isSampleAttribute5;

    @ExcelProperty("软件绑定标识5")
    private String curveIdentifier5;

    @ExcelProperty("输入参数6名称")
    private String inputParam6Name;

    @ExcelProperty("输入参数6标识")
    private String inputParam6Identity;

    @ExcelProperty("是否为样品输入参数6")
    private String isSampleAttribute6;

    @ExcelProperty("软件绑定标识6")
    private String curveIdentifier6;

    @ExcelProperty("输入参数7名称")
    private String inputParam7Name;

    @ExcelProperty("输入参数7标识")
    private String inputParam7Identity;

    @ExcelProperty("是否为样品输入参数7")
    private String isSampleAttribute7;

    @ExcelProperty("软件绑定标识7")
    private String curveIdentifier7;

    @ExcelProperty("输入参数8名称")
    private String inputParam8Name;

    @ExcelProperty("输入参数8标识")
    private String inputParam8Identity;

    @ExcelProperty("是否为样品输入参数8")
    private String isSampleAttribute8;

    @ExcelProperty("软件绑定标识8")
    private String curveIdentifier8;

    @ExcelProperty("输入参数9名称")
    private String inputParam9Name;

    @ExcelProperty("输入参数9标识")
    private String inputParam9Identity;

    @ExcelProperty("是否为样品输入参数9")
    private String isSampleAttribute9;

    @ExcelProperty("软件绑定标识9")
    private String curveIdentifier9;

    @ExcelProperty("输入参数10名称")
    private String inputParam10Name;

    @ExcelProperty("输入参数10标识")
    private String inputParam10Identity;

    @ExcelProperty("是否为样品输入参数10")
    private String isSampleAttribute10;

    @ExcelProperty("软件绑定标识10")
    private String curveIdentifier10;

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

    public String getIsSampleAttribute1() {
        return isSampleAttribute1;
    }

    public void setIsSampleAttribute1(String isSampleAttribute1) {
        this.isSampleAttribute1 = isSampleAttribute1;
    }

    public String getCurveIdentifier1() {
        return curveIdentifier1;
    }

    public void setCurveIdentifier1(String curveIdentifier1) {
        this.curveIdentifier1 = curveIdentifier1;
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

    public String getIsSampleAttribute2() {
        return isSampleAttribute2;
    }

    public void setIsSampleAttribute2(String isSampleAttribute2) {
        this.isSampleAttribute2 = isSampleAttribute2;
    }

    public String getCurveIdentifier2() {
        return curveIdentifier2;
    }

    public void setCurveIdentifier2(String curveIdentifier2) {
        this.curveIdentifier2 = curveIdentifier2;
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

    public String getIsSampleAttribute3() {
        return isSampleAttribute3;
    }

    public void setIsSampleAttribute3(String isSampleAttribute3) {
        this.isSampleAttribute3 = isSampleAttribute3;
    }

    public String getCurveIdentifier3() {
        return curveIdentifier3;
    }

    public void setCurveIdentifier3(String curveIdentifier3) {
        this.curveIdentifier3 = curveIdentifier3;
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

    public String getIsSampleAttribute4() {
        return isSampleAttribute4;
    }

    public void setIsSampleAttribute4(String isSampleAttribute4) {
        this.isSampleAttribute4 = isSampleAttribute4;
    }

    public String getCurveIdentifier4() {
        return curveIdentifier4;
    }

    public void setCurveIdentifier4(String curveIdentifier4) {
        this.curveIdentifier4 = curveIdentifier4;
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

    public String getIsSampleAttribute5() {
        return isSampleAttribute5;
    }

    public void setIsSampleAttribute5(String isSampleAttribute5) {
        this.isSampleAttribute5 = isSampleAttribute5;
    }

    public String getCurveIdentifier5() {
        return curveIdentifier5;
    }

    public void setCurveIdentifier5(String curveIdentifier5) {
        this.curveIdentifier5 = curveIdentifier5;
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

    public String getIsSampleAttribute6() {
        return isSampleAttribute6;
    }

    public void setIsSampleAttribute6(String isSampleAttribute6) {
        this.isSampleAttribute6 = isSampleAttribute6;
    }

    public String getCurveIdentifier6() {
        return curveIdentifier6;
    }

    public void setCurveIdentifier6(String curveIdentifier6) {
        this.curveIdentifier6 = curveIdentifier6;
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

    public String getIsSampleAttribute7() {
        return isSampleAttribute7;
    }

    public void setIsSampleAttribute7(String isSampleAttribute7) {
        this.isSampleAttribute7 = isSampleAttribute7;
    }

    public String getCurveIdentifier7() {
        return curveIdentifier7;
    }

    public void setCurveIdentifier7(String curveIdentifier7) {
        this.curveIdentifier7 = curveIdentifier7;
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

    public String getIsSampleAttribute8() {
        return isSampleAttribute8;
    }

    public void setIsSampleAttribute8(String isSampleAttribute8) {
        this.isSampleAttribute8 = isSampleAttribute8;
    }

    public String getCurveIdentifier8() {
        return curveIdentifier8;
    }

    public void setCurveIdentifier8(String curveIdentifier8) {
        this.curveIdentifier8 = curveIdentifier8;
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

    public String getIsSampleAttribute9() {
        return isSampleAttribute9;
    }

    public void setIsSampleAttribute9(String isSampleAttribute9) {
        this.isSampleAttribute9 = isSampleAttribute9;
    }

    public String getCurveIdentifier9() {
        return curveIdentifier9;
    }

    public void setCurveIdentifier9(String curveIdentifier9) {
        this.curveIdentifier9 = curveIdentifier9;
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

    public String getIsSampleAttribute10() {
        return isSampleAttribute10;
    }

    public void setIsSampleAttribute10(String isSampleAttribute10) {
        this.isSampleAttribute10 = isSampleAttribute10;
    }

    public String getCurveIdentifier10() {
        return curveIdentifier10;
    }

    public void setCurveIdentifier10(String curveIdentifier10) {
        this.curveIdentifier10 = curveIdentifier10;
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