package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.TimeSweepService;
import org.springframework.stereotype.Service;

@Service
("timeSweepService")
public class TimeSweepServiceImpl implements TimeSweepService {

    @Override
    public Result updateMethodParameter(MethodParamsDto methodDto) {
        return null;
    }
}
