package com.persee.workstation.uv_inexus.service.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMeasureStandardMapper;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureStandard;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureStandardService;
import org.springframework.stereotype.Service;

/**
 * 测量标准表(TMeasureStandard)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("tMeasureStandardService")
public class TMeasureStandardServiceImpl extends ServiceImpl<TMeasureStandardMapper, TMeasureStandard> implements TMeasureStandardService {

}

