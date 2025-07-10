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
    // deleteType
    private String deleteType;
}
