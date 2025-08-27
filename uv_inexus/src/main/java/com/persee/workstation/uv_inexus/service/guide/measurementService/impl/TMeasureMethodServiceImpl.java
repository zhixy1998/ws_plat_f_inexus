package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureMethodMapper;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
import org.springframework.stereotype.Service;

/**
 * 测量方法表(TMeasureMethod)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("tMeasureMethodService")
public class TMeasureMethodServiceImpl extends ServiceImpl<TMeasureMethodMapper, TMeasureMethod> implements TMeasureMethodService {

}

