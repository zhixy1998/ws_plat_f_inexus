package com.persee.workstation.uv_inexus.dao.log;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.log.SysLogsPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统日志表 Mapper：路由到 log 数据源，支持基础 CRUD
 */
@DS("log")
@Mapper
public interface SysLogsMapper extends BaseMapper<SysLogsPO> {
}
