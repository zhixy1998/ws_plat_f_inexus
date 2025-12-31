package com.persee.workstation.uv_inexus.util.deviceUtil;

import lombok.Data;

@Data
public class ScanWavelengthConfig {

    /** 起始波长 */
    private double startWavelength;

    /** 终止波长 */
    private double endWavelength;

    /** 扫描间隔 */
    private double interval;

    /** 是否递减扫描 */
    private boolean descending;
}