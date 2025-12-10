package com.persee.workstation.uv_inexus.dao.factory_inspection;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;
import org.apache.ibatis.annotations.Mapper;
@DS("factory_inspection")
@Mapper
public interface TemplateMapper extends BaseMapper<ExcelTemplate> {
}
