package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMethodDao;
import com.persee.workstation.uv_inexus.po.measurementPo.TMethod;
import com.persee.workstation.uv_inexus.service.measurementService.TMethodService;
import org.springframework.stereotype.Service;

@Service("tMethodService")
public class TMethodServiceImpl extends ServiceImpl<TMethodDao, TMethod> implements TMethodService {
}
