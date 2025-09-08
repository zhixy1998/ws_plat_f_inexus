package com.persee.workstation.uv_inexus.vo.measurementVo.spectrum;

import lombok.Data;

import java.util.List;

@Data
public class SampleWithMeasuresVo {
    private String sampleId;        // 样品ID
    private String title;           // 样品名称
    private String color;           // 样品颜色
    private String deleteType;      // 删除标识
    private String type;            // 类型
    private List<MeasureVo> children; // 样品下的测量数据列表
}