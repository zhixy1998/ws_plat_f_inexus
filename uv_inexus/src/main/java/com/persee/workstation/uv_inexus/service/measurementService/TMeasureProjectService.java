package com.persee.workstation.uv_inexus.service.measurementService;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureProject;

public interface TMeasureProjectService extends IService<TMeasureProject> {
    TMeasureProject defaultProject();
}
