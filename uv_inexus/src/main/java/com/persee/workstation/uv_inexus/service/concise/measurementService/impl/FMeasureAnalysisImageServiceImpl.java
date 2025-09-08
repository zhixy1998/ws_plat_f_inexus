package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureAnalysisImageMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureAnalysisImage;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureAnalysisImageService;
import org.springframework.stereotype.Service;

@Service
("FMeasureAnalysisImageService")
@DS("concise")
public class FMeasureAnalysisImageServiceImpl
        extends ServiceImpl<FMeasureAnalysisImageMapper, FMeasureAnalysisImage>
        implements FMeasureAnalysisImageService {
}