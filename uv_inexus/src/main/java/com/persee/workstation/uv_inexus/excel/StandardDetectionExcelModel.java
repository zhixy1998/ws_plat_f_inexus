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

public class StandardDetectionExcelModel {
    @ExcelIgnore // 忽略此字段，不导出到 Excel
    private Long id;

    @ExcelProperty("标准标识")
    private String standardIdentity;
    
    @ExcelProperty("行业")
    private String industry;

    @ExcelProperty("检测类型")
    private String detectionType;

    @ExcelProperty("检测仪器")
    private String detectionInstrument;

    @ExcelProperty("标准号")
    private String standardNumber;

    @ExcelProperty("标准名称")
    private String standardName;

    @ExcelProperty("检测项目")
    private String detectionItem;

    @ExcelProperty("第几法")
    private String methodNumber;
    
    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("检测参数")
    private String detectionParameters;

    @ExcelProperty("发布时间")
    private String releaseDate;

    @ExcelProperty("废止日期")
    private String expirationDate;

    @ExcelProperty("实施日期")
    private String effectiveDate;
    
    @ExcelProperty("状态")
    private String releaseStatus;

    @ExcelProperty("类型")
    private String industryType;

    // Getters and Setters
    public String getStandardIdentity() {
        return standardIdentity;
    }

    public void setStandardIdentity(String standardIdentity) {
        this.standardIdentity = standardIdentity;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getDetectionType() {
        return detectionType;
    }

    public void setDetectionType(String detectionType) {
        this.detectionType = detectionType;
    }

    public String getDetectionInstrument() {
        return detectionInstrument;
    }

    public void setDetectionInstrument(String detectionInstrument) {
        this.detectionInstrument = detectionInstrument;
    }

    public String getStandardNumber() {
        return standardNumber;
    }

    public void setStandardNumber(String standardNumber) {
        this.standardNumber = standardNumber;
    }

    public String getStandardName() {
        return standardName;
    }

    public void setStandardName(String standardName) {
        this.standardName = standardName;
    }

    public String getDetectionItem() {
        return detectionItem;
    }

    public void setDetectionItem(String detectionItem) {
        this.detectionItem = detectionItem;
    }

    public String getMethodNumber() {
        return methodNumber;
    }

    public void setMethodNumber(String methodNumber) {
        this.methodNumber = methodNumber;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getDetectionParameters() {
        return detectionParameters;
    }

    public void setDetectionParameters(String detectionParameters) {
        this.detectionParameters = detectionParameters;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getReleaseStatus() {
        return releaseStatus;
    }

    public void setReleaseStatus(String releaseStatus) {
        this.releaseStatus = releaseStatus;
    }

    public String getIndustryType() {
        return industryType;
    }

    public void setIndustryType(String industryType) {
        this.industryType = industryType;
    }
}
