package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.List;

@Data
public class CreateTaskParamDto {
    private String projectId;
    private String taskName;
    private String methodId;
    private String measurementType;
    private String deviceSerialNumber;
    private List<SpectrumSpecimenDto> specimens;
}
