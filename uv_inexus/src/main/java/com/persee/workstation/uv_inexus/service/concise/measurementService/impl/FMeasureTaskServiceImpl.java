package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import org.springframework.stereotype.Service;

/**
 * 测量任务表(FMeasureTask)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("FMeasureTaskService")
public class FMeasureTaskServiceImpl extends ServiceImpl<FMeasureTaskMapper, FMeasureTask> implements FMeasureTaskService {

}

