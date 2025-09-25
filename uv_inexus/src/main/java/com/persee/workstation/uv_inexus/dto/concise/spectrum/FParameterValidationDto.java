package com.persee.workstation.uv_inexus.dto.concise.spectrum;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 方法参数
 */
@Data
public class FParameterValidationDto {
    // 方法id
    private String methodId;
    // 测量类型，光度测量,定量测量,光谱扫描,时间扫描
    private String measureType;
    // 前端传的参数，先用通用 JSON 接收
    private JSONObject parameter;
}
