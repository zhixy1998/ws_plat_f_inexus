package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import com.persee.workstation.uv_inexus.model.Result;

/**
 * 定量测定
 */
public interface RationService {
    Result updateMethodParameter(MethodParamsDto methodDto);

    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, RationParameter rationParameter);

    /**
     * 新增或删除样本
     * @param dto
     * @return
     */
    Result addOrDeleteSample(AddOrDeleteSampleDTO dto);

    Result revokeDel(String taskId, String sampleId);

    /**
     * 查询方法参数
     * @param methodId
     * @return
     */
    Result queryMethodParam(String methodId);
}
