package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.Result;

public interface PhotometricService {
    Result createTask(CreateTaskParamDto paramDto);

    Result queryTaskPort(QueryTaskDto queryTask);

    Result queryMethodProt(String projectId, String measureType);

    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, PhotometricParameter photometricParameter);

    Result saveMethodParameter(PhotometricMethodParameterDto methodDto);
}
