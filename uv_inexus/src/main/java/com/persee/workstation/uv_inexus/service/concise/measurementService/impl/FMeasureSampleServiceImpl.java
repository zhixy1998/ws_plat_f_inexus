package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureSampleMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSample;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureSampleService;
import org.springframework.stereotype.Service;

/**
 * 样品信息表(FMeasureSample)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("FMeasureSampleService")
public class FMeasureSampleServiceImpl extends ServiceImpl<FMeasureSampleMapper, FMeasureSample> implements FMeasureSampleService {

}

