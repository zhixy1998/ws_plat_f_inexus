package com.persee.workstation.uv_inexus.service.concise.measurementService;


import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;

/**
 * 测量任务表(FMeasureTask)表服务接口
 */
public interface FMeasureTaskService extends IService<FMeasureTask> {

    boolean save(FMeasureTask entity);

}

