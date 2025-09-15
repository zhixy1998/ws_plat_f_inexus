package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

@Data
public class AddNewMethodVo {

    // 方法名称
    private String methodName;
    // 创建人
    private String creator;
    // 所属项目
    private String project;
    // 测量类型
    private String measurementType;
    // 最近使用时间
    private String recentUsageTime;
}
