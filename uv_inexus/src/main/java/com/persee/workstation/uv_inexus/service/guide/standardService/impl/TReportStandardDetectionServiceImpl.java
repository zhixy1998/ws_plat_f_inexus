package com.persee.workstation.uv_inexus.service.guide.standardService.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.standardDao.TReportStandardDetectionMapper;
import com.persee.workstation.uv_inexus.po.guide.standardPo.TReportStandardDetection;
import com.persee.workstation.uv_inexus.service.guide.standardService.TReportStandardDetectionService;
import org.springframework.stereotype.Service;

@Service("tReportStandardDetectionService")
public class TReportStandardDetectionServiceImpl extends ServiceImpl<TReportStandardDetectionMapper, TReportStandardDetection> implements TReportStandardDetectionService {
}
