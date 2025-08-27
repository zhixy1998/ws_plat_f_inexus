package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureProjectMapper;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureProject;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureProjectService;
import org.springframework.stereotype.Service;

/**
 * 项目表(TMeasureProject)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("tMeasureProjectService")
public class TMeasureProjectServiceImpl extends ServiceImpl<TMeasureProjectMapper, TMeasureProject> implements TMeasureProjectService {

}

