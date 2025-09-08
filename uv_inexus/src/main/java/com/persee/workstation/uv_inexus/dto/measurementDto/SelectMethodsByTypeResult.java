package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SelectMethodsByTypeResult {
    private Integer methodNumber;
    private String methodtype;
}
