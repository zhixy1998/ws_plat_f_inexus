/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.concise.spectrum;

import com.persee.workstation.uv_inexus.vo.concise.spectrum.FSpectrumParameterVo;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
public class FUseMethodDto {
    // 方法id
    private String operateType;
    // 方法id
    private String methodId;
    // 方法id2
    private String methodId2;
    // 用户名
    private String updateBy;
    // 测量类型
    private String measureType;
    // 测量方式 (0. 向导数据库,1. 简洁数据库)
    private String type;
    // 方法参数
    @Valid
    @NotNull(message = "方法参数不能为空")
    private FSpectrumParameterVo parameter;
    // 序列号
    private String deviceSerialNumber;
}
