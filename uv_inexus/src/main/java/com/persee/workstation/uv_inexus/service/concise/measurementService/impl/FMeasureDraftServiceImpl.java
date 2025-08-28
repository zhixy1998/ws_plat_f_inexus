package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureDraftMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureDraft;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureDraftService;
import org.springframework.stereotype.Service;

/**
 * 草稿数据表 Service 实现类
 */
@Service("FMeasureDraftService")
@DS("concise")
public class FMeasureDraftServiceImpl
        extends ServiceImpl<FMeasureDraftMapper, FMeasureDraft>
        implements FMeasureDraftService {
}
