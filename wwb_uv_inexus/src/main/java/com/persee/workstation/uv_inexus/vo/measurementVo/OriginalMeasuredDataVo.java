package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

@Data
public class OriginalMeasuredDataVo {
    private int id;           // 编号
    private String x;
    private String y;
    private String feature;   // 特征描述，如“峰值”或“无”
    private String tag;       // 标记，如“未处理”
}
