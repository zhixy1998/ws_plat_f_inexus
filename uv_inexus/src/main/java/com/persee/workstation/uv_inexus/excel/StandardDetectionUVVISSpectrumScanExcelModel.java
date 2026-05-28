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

// UV-VIS 光谱扫描-检测参数表
public class StandardDetectionUVVISSpectrumScanExcelModel {
    @ExcelIgnore
    private Long id;

    @ExcelProperty("标准标识")
    private String detectionId;

    @ExcelProperty("起始波长")
    private String startWavelength;

    @ExcelProperty("终止波长")
    private String endWavelength;

    @ExcelProperty("扫描间隔")
    private String scanInterval;

    @ExcelProperty("扫描速度")
    private String scanSpeed;

    @ExcelProperty("是否重复")
    private String isRepeat;

    @ExcelProperty("重复次数")
    private String repeatCount;

    @ExcelProperty("重复时间间隔(s)")
    private String repeatInterval;

    // Getters and Setters
    public String getDetectionId() {
        return detectionId;
    }

    public void setDetectionId(String detectionId) {
        this.detectionId = detectionId;
    }

    public String getStartWavelength() {
        return startWavelength;
    }

    public void setStartWavelength(String startWavelength) {
        this.startWavelength = startWavelength;
    }

    public String getEndWavelength() {
        return endWavelength;
    }

    public void setEndWavelength(String endWavelength) {
        this.endWavelength = endWavelength;
    }

    public String getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(String scanInterval) {
        this.scanInterval = scanInterval;
    }

    public String getScanSpeed() {
        return scanSpeed;
    }

    public void setScanSpeed(String scanSpeed) {
        this.scanSpeed = scanSpeed;
    }

    public String getIsRepeat() {
        return isRepeat;
    }

    public void setIsRepeat(String isRepeat) {
        this.isRepeat = isRepeat;
    }

    public String getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(String repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(String repeatInterval) {
        this.repeatInterval = repeatInterval;
    }
}
