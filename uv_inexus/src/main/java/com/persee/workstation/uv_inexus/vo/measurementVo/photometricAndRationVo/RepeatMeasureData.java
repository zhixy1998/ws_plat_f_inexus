package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RepeatMeasureData {

    private String sd;

    private String rsd;

    public RepeatMeasureData(String sd, String rsd) {
        this.sd = sd;
        this.rsd = rsd;
    }
}
