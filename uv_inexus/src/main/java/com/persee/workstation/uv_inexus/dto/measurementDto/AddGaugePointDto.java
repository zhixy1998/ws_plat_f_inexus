package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.po.measurementPo.model.PeakValleyMarking;
import lombok.Data;

import java.util.List;

@Data
public class AddGaugePointDto extends PeakValleyMarking {
    // 标记点数组
    private List<String> gaugePoints;
    // 数据id
    private String dataId;
    // 类型(0.是测量数据1.是分析数据)
    private String type;
}
