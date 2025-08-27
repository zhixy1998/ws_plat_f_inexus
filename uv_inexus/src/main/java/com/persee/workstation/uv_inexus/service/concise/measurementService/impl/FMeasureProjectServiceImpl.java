package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureProjectMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureProject;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureProjectService;
import org.springframework.stereotype.Service;

/**
 * 项目表(FMeasureProject)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("FMeasureProjectService")
public class FMeasureProjectServiceImpl extends ServiceImpl<FMeasureProjectMapper, FMeasureProject> implements FMeasureProjectService {

}

