package com.persee.workstation.uv_inexus.po.guide.measurementPo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskSpecimen {
    // 样本id
    private String specimenKey;
    // 删除状态(0.未删除,1.删除)
    private String deleteType;
    // 是否已测(0.未测,1.已测,2.正在测)
    private String isTested;
    //样品种类
    private String sampleType;
    //样品类型
    private String sampleKind;
}
