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

    Result<Page<FMeasureTaskDTO>> queryHistoryTask(SelectTaskDto selectTaskDto);

    Result<String> updateMethod(UpdateMethodDto updateMethodDto);

    // 修改任务名称
    Result updateTask(UpdateTaskDto updateTaskDto);
}
