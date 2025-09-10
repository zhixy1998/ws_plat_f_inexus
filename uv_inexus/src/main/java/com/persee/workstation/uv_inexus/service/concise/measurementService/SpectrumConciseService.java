package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.persee.workstation.uv_inexus.model.Result;

/**
 * 简洁模式: 光谱扫描服务业务层
 */
public interface SpectrumConciseService {

    // 查询最近方法数据
    Result queryNearestMethodData(String updateBy, String measureType);

    // 查询样品数据列表
    Result querySampleDataList(String methodId,String type,Integer digit);

    // 查询最近测量数据
    Result queryNearestMeasureData(String methodId);

    // 查找方法按钮
    Result searchMethodButton(Integer peageNum,Integer pageSize, String updateBy, String measureType, String methodName);
}
