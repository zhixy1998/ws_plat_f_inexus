package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import com.persee.workstation.uv_inexus.model.Result;

public interface RationService {
    Result updateMethodParameter(MethodParamsDto methodDto);

    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, RationParameter rationParameter);
}
