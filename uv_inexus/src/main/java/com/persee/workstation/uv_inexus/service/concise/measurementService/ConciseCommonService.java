package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodListWithPageVO;

/**
 * 简洁模式: 通用服务
 */
public interface ConciseCommonService {

    // 获取默认参数
    Result queryDefaultParameters(String measureType);

    // 查找方法按钮
    Result<FMethodListWithPageVO> searchMethodButton(FQueryMethodWithPageDto dto);
    // 查询保存状态
    Result querySaveState(String taskId);

    // 保存草稿按钮
    Result saveDraftButton(String taskId);

    Result<Page<FMeasureMethodParam>> queryHistoryMethod(QueryMethodDto queryMethodDto);

    // 查询方法参数
    Result queryMethodParameter(String methodId);

    Result<Page<FMeasureTaskDTO>> queryHistoryTask(SelectTaskDto selectTaskDto);

    Result<String> updateMethod(UpdateMethodDto updateMethodDto);

    // 修改任务名称
    Result updateTask(UpdateTaskDto updateTaskDto);

    /**
     * 光度和定量方法里输入参数校验
     */
    Result<Boolean> checkSampleInputParam(FCheckSampleInputParamDto checkSampleInputParamDto);
    /*
    光度和定量方法里公式校验
     */
    Result<Boolean> checkFormula(FCheckFormulaDto checkFormulaDto);


}
