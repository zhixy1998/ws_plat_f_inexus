package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

import java.util.List;

@Data
public class SampleListVo {
    // 标题
    private String title;
    // id
    private String key;
    // 颜色
    private String color;
    // 数据
    private List<FourOperationsVo> data;
    // 0. 测量中 1. 测量完成
    private String measureType;
    // 0. 正常 1. 删除
    private String deleteType;
}
