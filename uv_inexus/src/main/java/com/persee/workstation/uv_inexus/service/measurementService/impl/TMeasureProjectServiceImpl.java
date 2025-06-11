package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMeasureProjectDao;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureProject;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureProjectService;
import org.springframework.stereotype.Service;

@Service("TMeasureProjectService")
public class TMeasureProjectServiceImpl extends ServiceImpl<TMeasureProjectDao, TMeasureProject> implements TMeasureProjectService {
    @Override
    public TMeasureProject defaultProject() {
        // 创建一个默认项目id的查询条件,根据默认项目id的查询条件去项目表中进行查询并返回
        return this.getOne(Wrappers.lambdaQuery(TMeasureProject.class).eq(TMeasureProject::getId, "1"));
    }
}
