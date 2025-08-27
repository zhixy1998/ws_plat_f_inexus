package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.po.measurementPo.model.PeakValleyMarking;
import lombok.Data;

@Data
public class PeakValleyMarkingDto extends PeakValleyMarking {
    // 数据id
    private String dataId;
}
