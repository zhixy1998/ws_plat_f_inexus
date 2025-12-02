/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FMethodParamRequestDto {
    private String methodName;  // 测量方法名称
    private String methodType; // 测量类型
    private String projectId; // 项目id
    private List<String> pretreatmentIds; // 前处理的多个id
    private String updateBy; // 修改人
    private Map<String, Object> params; // 不定参数
}