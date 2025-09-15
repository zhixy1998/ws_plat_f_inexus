package com.persee.workstation.uv_inexus.vo.concise.common;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FSampleListVo {
    // 标题
    private String title;
    // 测量id
    private String analysisId;
    // 样品id
    private String sampleId;
    // 分析编号
    private Integer analysisNumber;
    // 颜色
    private String color;
    // 数据
    private List<Map<String, Object>> data;
    // 类型(0.测量数据 1.分析数据)
    private String type;
    // deleteType
    private String deleteType;
}
