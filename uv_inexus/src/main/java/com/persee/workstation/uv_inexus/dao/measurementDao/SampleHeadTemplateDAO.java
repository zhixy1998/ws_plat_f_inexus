package com.persee.workstation.uv_inexus.dao.measurementDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.measurementPo.SampleHeadTemplatePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 样品模板dao
 */
@Mapper
public interface SampleHeadTemplateDAO extends BaseMapper<SampleHeadTemplatePO> {
}
