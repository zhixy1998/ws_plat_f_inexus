package com.persee.workstation.uv_inexus.vo.concise.common;

import lombok.Data;

@Data
public class TaskStatisticVo {
    // 用户名
    private String updateBy;
    // 项目名称
    private String taskName;
    // 页数
    private Integer pageNum;
    // 长度
    private Integer pageSize;
    // 测量模式
    private String measureMode;
}
