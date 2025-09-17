package com.persee.workstation.uv_inexus.dto.concise.photometricDto;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPhotometricParameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FPhotometricMethodParameterDto extends FPhotometricParameter {
    private String methodId;
    private String methodName;
}
