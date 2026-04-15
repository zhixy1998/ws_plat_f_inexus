package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.dto.measurementDto.config.SampleNameConfig;
import com.persee.workstation.uv_inexus.dto.measurementDto.config.SampleNumConfig;
import lombok.Data;

import javax.validation.constraints.NotBlank;

// 添加样品请求DTO
@Data
public class AddSampleDTO {
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @NotBlank(message = "设备序列号不能为空")
    private String deviceSerialNumber;

    // 样品编号和名称配置（四个测量和定量测定通用）
    private SampleNumConfig sampleNumConfig;
    private SampleNameConfig sampleNameConfig;

    // 定量测定特有的字段
    private String sampleKind;          // 样品种类：未知样品或标准样品
    private String normalConcentration; // 标准浓度
    private String updateBy;            // 更新人

    // 可选字段
    private String measureType;         // 测量类型（可选，不传则从任务获取）
    private Boolean autoChangePool;     // 是否自动换池（可选）
}
