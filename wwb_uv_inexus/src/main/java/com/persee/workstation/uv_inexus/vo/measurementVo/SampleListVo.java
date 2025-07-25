package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SampleListVo {
    // 标题
    private String title;
    // id
    private String key;
    // 颜色
    private String color;
    // 数据
    private List<Map<String, Object>> data;
    // 类型(0.测量数据 1.分析数据)
    private String type;
    // deleteType
    private String deleteType;
}
