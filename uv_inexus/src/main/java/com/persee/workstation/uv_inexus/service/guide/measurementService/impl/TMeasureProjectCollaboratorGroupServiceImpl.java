package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureProjectCollaboratorGroupMapper;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TMeasureProjectCollaboratorGroup;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureProjectCollaboratorGroupService;
import org.springframework.stereotype.Service;

@Service("tMeasureProjectCollaboratorGroupService")
public class TMeasureProjectCollaboratorGroupServiceImpl  extends ServiceImpl<TMeasureProjectCollaboratorGroupMapper, TMeasureProjectCollaboratorGroup> implements TMeasureProjectCollaboratorGroupService {

}