package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

/**
 * @Description: 方法数据统计返回类
 * @Author:
 */
@Data
public class MethodDataNumDto {
    // 测量模式
    private String measureMode;
    // 总任务数
    private Long methodNum;
}
