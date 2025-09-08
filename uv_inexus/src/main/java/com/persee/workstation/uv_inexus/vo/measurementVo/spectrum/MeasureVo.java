package com.persee.workstation.uv_inexus.vo.measurementVo.spectrum;

import lombok.Data;

@Data
public class MeasureVo {
    private String measureId;       // 测量数据ID
    private String title;           // 测量名称(序号)
    private Object data;            // 测量结果
    private String serialNumber;    // 序列号
    private String type;            // 测量类型
}
