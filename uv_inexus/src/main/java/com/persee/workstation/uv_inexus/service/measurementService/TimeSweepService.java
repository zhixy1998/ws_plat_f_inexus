package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.AddSampleDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import com.persee.workstation.uv_inexus.model.Result;

public interface TimeSweepService {

    // 创建方法
    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, TimeSweepParamter timeSweepParamter);

    // 查询项目下的所有方法
    Result queryProjectAllMethods(String projectId, String measureForm);

    // 添加样品接口
    Result addSamplePort(AddSampleDto paramDto);

    Result sampleListPort(String taskId, String type);

    Result deleteSampleListPort(String measureId);

    Result changeTheListPort(String taskId, String type);

    Result deleteChangeListPort(String analysisId);
}
