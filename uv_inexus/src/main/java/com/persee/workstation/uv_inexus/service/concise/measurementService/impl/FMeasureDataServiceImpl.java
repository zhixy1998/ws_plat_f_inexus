package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureDataMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureData;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureDataService;
import org.springframework.stereotype.Service;

/**
 * 测量数据表(FMeasureData)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("FMeasureDataService")
public class FMeasureDataServiceImpl extends ServiceImpl<FMeasureDataMapper, FMeasureData> implements FMeasureDataService {

}

