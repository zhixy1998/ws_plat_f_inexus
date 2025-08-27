package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureAnalysisDataMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureAnalysisData;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureAnalysisDataService;
import org.springframework.stereotype.Service;

/**
 * 分析数据表(FMeasureAnalysisData)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("FMeasureAnalysisDataService")
public class FMeasureAnalysisDataServiceImpl extends ServiceImpl<FMeasureAnalysisDataMapper, FMeasureAnalysisData> implements FMeasureAnalysisDataService {

}

