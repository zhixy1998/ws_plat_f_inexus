package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.FMeasureMethodDto;
import com.persee.workstation.uv_inexus.dto.concise.common.FMeasureMethodParam;
import com.persee.workstation.uv_inexus.dto.concise.common.QueryMethodDto;
import com.persee.workstation.uv_inexus.dto.concise.common.SelectTaskDto;

import com.persee.workstation.uv_inexus.dto.concise.spectrum.FUseMethodDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;

/**
 * 简洁模式: 通用服务
 */
public interface ConciseCommonService {

    // 应用接口
    Result applyMethod(FUseMethodDto paramDto);

    Result<Page<FMeasureMethodDto>> queryHistoryMethod(QueryMethodDto queryMethodDto);

    Result<FMeasureMethodParam> detailInformation(String methodName, String methodType);

    Result<Page<FMeasureTask>> queryHistoryTask(SelectTaskDto selectTaskDto);
}
