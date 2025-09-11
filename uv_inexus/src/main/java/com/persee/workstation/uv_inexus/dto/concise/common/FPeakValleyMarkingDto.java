package com.persee.workstation.uv_inexus.dto.concise.common;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import lombok.Data;

@Data
public class FPeakValleyMarkingDto extends FPeakValleyMarking {
    // 数据id
    private String dataId;
}
