package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.vo.measurementVo.Sample;
import lombok.Data;

import java.util.List;

@Data
public class AddSampleDto {
    private String taskId;
    private List<Sample> samples;
}
