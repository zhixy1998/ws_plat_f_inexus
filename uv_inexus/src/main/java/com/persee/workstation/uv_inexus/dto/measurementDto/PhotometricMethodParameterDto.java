package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class PhotometricMethodParameterDto extends PhotometricParameter{
    private String methodId;
    private String methodName;
}
