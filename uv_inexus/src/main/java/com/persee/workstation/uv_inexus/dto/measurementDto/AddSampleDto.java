package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.List;

@Data
public class AddSampleDto {
    private String taskId;
    private List<SpectrumSpecimenDto> samples;
}
