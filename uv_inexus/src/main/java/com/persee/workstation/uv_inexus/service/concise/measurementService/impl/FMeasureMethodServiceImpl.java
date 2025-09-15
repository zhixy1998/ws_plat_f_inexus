package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureMethodMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureMethodService;
import org.springframework.stereotype.Service;

/**
 * 测量方法表(FMeasureMethod)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("FMeasureMethodService")
@DS("concise")
public class FMeasureMethodServiceImpl extends ServiceImpl<FMeasureMethodMapper, FMeasureMethod> implements FMeasureMethodService {
}

