package com.persee.workstation.uv_inexus.vo.concise.common;

import lombok.Data;

@Data
public class MethodStatisticVo {
    // 用户名
    private String updateBy;
    // 方法名称
    private String methodName;
    // 页数
    private Integer pageNum;
    // 长度
    private Integer pageSize;
}
