/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
