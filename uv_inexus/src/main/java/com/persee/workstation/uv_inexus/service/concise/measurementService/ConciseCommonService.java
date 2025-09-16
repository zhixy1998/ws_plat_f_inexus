package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.persee.workstation.uv_inexus.model.Result;

/**
 * 简洁模式: 通用服务
 */
public interface ConciseCommonService {

    // 获取默认参数
    Result queryDefaultParameters(String measureType);

    // 查找方法按钮
    Result searchMethodButton(Integer pageNum, Integer pageSize, String updateBy, String measureType, String methodName);



}
