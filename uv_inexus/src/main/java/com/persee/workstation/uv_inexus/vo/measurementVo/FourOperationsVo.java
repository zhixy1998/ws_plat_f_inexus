package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

@Data
public class FourOperationsVo {
    // x轴数据
    private String x;
    // y轴数据
    private String y;
    // 标记点
    private String marked;
    // 峰
    private String feng;
    // 谷
    private String gu;
}
