package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

import java.util.List;

@Data
public class ReportDataVo {
    // 名称
    private String name;
    // 波峰
    private List<String> peak;
    // 波谷
    private List<String> valley;
    // 标记点
    private List<String> markPoint;
    // 参数数据
    private List<String> parameterData;
    // 数据类型
    private String dataType;


}
