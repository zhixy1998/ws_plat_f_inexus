package com.persee.workstation.uv_inexus.dto.measurementDto.experimentTest;

import lombok.Data;

import java.util.List;

@Data
public class MethodExportRequest {
    private List<String> ids;
}