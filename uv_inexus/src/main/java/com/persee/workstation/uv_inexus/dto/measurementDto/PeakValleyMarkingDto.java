package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.po.measurementPo.model.PeakValleyMarking;
import lombok.Data;

@Data
public class PeakValleyMarkingDto extends PeakValleyMarking {
    // 数据id
    private String dataId;
    // 数据类型 (0样品数据 1变换数据)
    private String dataType;
}
