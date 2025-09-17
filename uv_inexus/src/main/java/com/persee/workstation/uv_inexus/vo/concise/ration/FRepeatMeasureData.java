package com.persee.workstation.uv_inexus.vo.concise.ration;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FRepeatMeasureData {

    private String sd;

    private String rsd;

    public FRepeatMeasureData(String sd, String rsd) {
        this.sd = sd;
        this.rsd = rsd;
    }
}
