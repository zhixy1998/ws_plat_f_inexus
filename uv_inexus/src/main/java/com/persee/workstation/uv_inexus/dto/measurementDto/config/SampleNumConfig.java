package com.persee.workstation.uv_inexus.dto.measurementDto.config;

import lombok.Data;

// 样品编号配置类
@Data
public class SampleNumConfig {
    private String preName;      // 名称
    private Integer startNum;  // 开始号
    private Integer step;// 递增量
}
