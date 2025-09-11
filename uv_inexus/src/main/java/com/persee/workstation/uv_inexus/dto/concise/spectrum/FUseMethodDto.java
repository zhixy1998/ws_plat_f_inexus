package com.persee.workstation.uv_inexus.dto.concise.spectrum;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import lombok.Data;

@Data
public class FUseMethodDto extends FSpectrumParameter {
    // 方法id
    private String methodId;
    // 用户名
    private String updateBy;
    // 测量类型
    private String measureType;
    // 测量方式
    private String type;
}
