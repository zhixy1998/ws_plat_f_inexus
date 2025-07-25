package com.persee.workstation.uv_inexus.po.measurementPo.model;

import lombok.Data;

@Data
public class TaskSpecimen {
    // 样本id
    private String specimenKey;
    // 删除状态(0.未删除,1.删除)
    private String deleteType;
    // 是否已测(0.未测,1.已测)
    private String isTested;
}
