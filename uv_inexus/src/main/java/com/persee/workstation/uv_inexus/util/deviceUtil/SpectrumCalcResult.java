package com.persee.workstation.uv_inexus.util.deviceUtil;

import lombok.Data;

@Data
public class SpectrumCalcResult {

    private double wavelength;

    /** 吸光度 */
    private String abs;

    /** 透光率 */
    private String tPercent;

    /** 能量 S */
    private String es;

    /** 能量 R */
    private String er;

    // getter / setter
}

