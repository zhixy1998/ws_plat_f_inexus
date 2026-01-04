package com.persee.workstation.uv_inexus.util.deviceUtil;

import lombok.Data;

@Data
public class ScanWavelengthConfig {

    /** 起始波长 */
    private Double startWavelength;

    /** 终止波长 */
    private Double endWavelength;

    /** 扫描间隔 */
    private Double interval;

    /** 是否递减扫描 */
    private Boolean descending;
}