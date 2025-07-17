package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.AddSampleDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import com.persee.workstation.uv_inexus.model.Result;

public interface TimeSweepService {
    Result updateMethodParameter(MethodParamsDto methodDto);

    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, TimeSweepParamter timeSweepParamter);
    
    Result queryProjectTaskMethod(String updateBy);

    Result queryMethodParameter(String methodId);

    Result addSamplePort(AddSampleDto paramDto);

    Result deleteSamplePort(String sampleId, String taskId);
}
