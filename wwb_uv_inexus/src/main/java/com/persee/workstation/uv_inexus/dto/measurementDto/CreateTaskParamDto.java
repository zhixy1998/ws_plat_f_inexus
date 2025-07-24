package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.List;

@Data
public class CreateTaskParamDto {
    private String projectId;
    private String taskName;
    private String methodId;
    private List<SpectrumSpecimenDto> specimens;
}
