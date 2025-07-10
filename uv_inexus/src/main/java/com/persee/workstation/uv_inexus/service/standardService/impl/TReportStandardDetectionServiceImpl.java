package com.persee.workstation.uv_inexus.service.standardService.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.standardDao.TReportStandardDetectionMapper;
import com.persee.workstation.uv_inexus.po.standardPo.TReportStandardDetection;
import com.persee.workstation.uv_inexus.service.standardService.TReportStandardDetectionService;
import org.springframework.stereotype.Service;

@Service("tReportStandardDetectionService")
public class TReportStandardDetectionServiceImpl extends ServiceImpl<TReportStandardDetectionMapper, TReportStandardDetection> implements TReportStandardDetectionService {
}
