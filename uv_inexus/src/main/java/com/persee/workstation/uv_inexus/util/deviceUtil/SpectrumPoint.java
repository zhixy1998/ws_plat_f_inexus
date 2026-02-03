package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import lombok.Data;

@Data
public class SpectrumPoint {

    /** 约束后的波长，如 656.2 */
    private double wavelength;

    /** baseline 阶段拟合得到的 rsd */
    private ChangeParam baselineRsd;

    /** scan 阶段拟合得到的 rsd */
    private ChangeParam scanRsd;


    private ChangeParam zeroRsd;


    private ChangeParam measureRsd;

    //时间戳
    private Long timestamp;

    private SpectrumCalcResult absResult;
    private SpectrumCalcResult tResult;
    private SpectrumCalcResult esResult;
    private SpectrumCalcResult erResult;

    private Double darkZ;



    public SpectrumPoint(double wavelength) {
        this.wavelength = wavelength;
    }

    public double getWavelength() {
        return wavelength;
    }

    public ChangeParam getBaselineRsd() {
        return baselineRsd;
    }

    public ChangeParam getScanRsd() {
        return scanRsd;
    }

}

