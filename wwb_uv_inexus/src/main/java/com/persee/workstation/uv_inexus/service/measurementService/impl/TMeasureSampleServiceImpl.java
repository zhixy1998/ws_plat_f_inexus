package com.persee.workstation.uv_inexus.service.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMeasureSampleMapper;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureSampleService;
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

