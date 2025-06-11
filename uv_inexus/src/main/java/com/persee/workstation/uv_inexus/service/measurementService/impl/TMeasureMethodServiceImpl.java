package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMeasureMethodDao;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureMethodService;
import org.springframework.stereotype.Service;

@Service("TMeasureMethodService")
public class TMeasureMethodServiceImpl extends ServiceImpl<TMeasureMethodDao, TMeasureMethod> implements TMeasureMethodService {
}
