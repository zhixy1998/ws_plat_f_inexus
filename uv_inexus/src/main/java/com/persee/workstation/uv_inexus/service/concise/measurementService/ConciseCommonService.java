package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.persee.workstation.uv_inexus.dto.concise.spectrum.FUseMethodDto;
import com.persee.workstation.uv_inexus.model.Result;

/**
 * 简洁模式: 通用服务
 */
public interface ConciseCommonService {

    // 应用接口
    Result applyMethod(FUseMethodDto paramDto);
}
