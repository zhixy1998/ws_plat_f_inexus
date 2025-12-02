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
