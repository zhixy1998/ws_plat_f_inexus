package com.persee.workstation.uv_inexus.dao.concise.measurementDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureProject;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectManagementMapper extends BaseMapper<FMeasureProject> {
}
