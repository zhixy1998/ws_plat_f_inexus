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

    // 样品编号配置
    private SampleNumConfig sampleNumberConfig;

    // 样品名称配置
    private SampleNameConfig sampleNameConfig;
}
