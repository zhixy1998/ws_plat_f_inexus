package com.persee.workstation.uv_inexus.service.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMeasurePreprocessInfoMapper;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasurePreprocessInfo;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasurePreprocessInfoService;
import org.springframework.stereotype.Service;

/**
 * 前处理信息表(TMeasurePreprocessInfo)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("tMeasurePreprocessInfoService")
public class TMeasurePreprocessInfoServiceImpl extends ServiceImpl<TMeasurePreprocessInfoMapper, TMeasurePreprocessInfo> implements TMeasurePreprocessInfoService {

}

