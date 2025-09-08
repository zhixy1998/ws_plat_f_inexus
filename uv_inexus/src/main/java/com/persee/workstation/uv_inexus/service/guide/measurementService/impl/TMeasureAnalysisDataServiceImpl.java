package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureAnalysisDataMapper;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureAnalysisData;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureAnalysisDataService;
import org.springframework.stereotype.Service;

/**
 * 分析数据表(TMeasureAnalysisData)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("tMeasureAnalysisDataService")
public class TMeasureAnalysisDataServiceImpl extends ServiceImpl<TMeasureAnalysisDataMapper, TMeasureAnalysisData> implements TMeasureAnalysisDataService {

}

