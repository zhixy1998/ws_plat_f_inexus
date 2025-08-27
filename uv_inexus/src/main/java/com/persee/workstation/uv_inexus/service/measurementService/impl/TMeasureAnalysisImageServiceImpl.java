package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMeasureAnalysisImageMapper;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureAnalysisImage;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureAnalysisImageService;
import org.springframework.stereotype.Service;

@Service
("tMeasureAnalysisImageService")
public class TMeasureAnalysisImageServiceImpl
        extends ServiceImpl<TMeasureAnalysisImageMapper, TMeasureAnalysisImage>
        implements TMeasureAnalysisImageService {
}