package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureSampleMapper;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureSampleService;
import org.springframework.stereotype.Service;

/**
 * 样品信息表(TMeasureSample)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("tMeasureSampleService")
public class TMeasureSampleServiceImpl extends ServiceImpl<TMeasureSampleMapper, TMeasureSample> implements TMeasureSampleService {

}

