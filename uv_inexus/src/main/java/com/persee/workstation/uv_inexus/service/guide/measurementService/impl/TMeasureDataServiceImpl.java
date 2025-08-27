package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureDataMapper;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureData;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureDataService;
import org.springframework.stereotype.Service;

/**
 * 测量数据表(TMeasureData)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("tMeasureDataService")
public class TMeasureDataServiceImpl extends ServiceImpl<TMeasureDataMapper, TMeasureData> implements TMeasureDataService {

}

