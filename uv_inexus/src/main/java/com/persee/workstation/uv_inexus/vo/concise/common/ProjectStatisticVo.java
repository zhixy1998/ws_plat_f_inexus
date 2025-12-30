package com.persee.workstation.uv_inexus.vo.concise.common;

import lombok.Data;

@Data
public class ProjectStatisticVo {
    // 用户名
    private String updateBy;
    // 项目名称
    private String projectName;
    // 页数
    private Integer pageNum;
    // 长度
    private Integer pageSize;
}
