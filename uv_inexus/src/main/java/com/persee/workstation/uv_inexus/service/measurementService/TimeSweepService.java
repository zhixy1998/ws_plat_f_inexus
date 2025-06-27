package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.model.Result;

public interface TimeSweepService {
    Result updateMethodParameter(MethodParamsDto methodDto);
}
