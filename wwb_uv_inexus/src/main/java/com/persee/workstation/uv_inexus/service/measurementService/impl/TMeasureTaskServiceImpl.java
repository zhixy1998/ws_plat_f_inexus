package com.persee.workstation.uv_inexus.service.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureTaskService;
import org.springframework.stereotype.Service;

/**
 * 测量任务表(TMeasureTask)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("tMeasureTaskService")
public class TMeasureTaskServiceImpl extends ServiceImpl<TMeasureTaskMapper, TMeasureTask> implements TMeasureTaskService {

}

