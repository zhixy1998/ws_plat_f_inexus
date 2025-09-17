package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodListWithPageVO;

/**
 * 简洁模式: 通用服务
 */
public interface ConciseCommonService {

    Result<Page<FMeasureMethodParam>> queryHistoryMethod(QueryMethodDto queryMethodDto);

    Result<FMeasureMethodParam> detailInformation(String methodName, String methodType);

    Result<Page<FMeasureTask>> queryHistoryTask(SelectTaskDto selectTaskDto);

    Result<String> updateMethod(UpdateMethodDto updateMethodDto);


    // 获取默认参数
    Result queryDefaultParameters(String measureType);

    // 查找方法按钮
    Result<FMethodListWithPageVO> searchMethodButton(FQueryMethodWithPageDto dto);





}
