package com.persee.workstation.uv_inexus.dao.OperateLog;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO;

/**
 * 操作日志DAO：MyBatis-Plus接口（无需手动写SQL，支持CRUD）
 */
@Mapper
public interface OperateLogMapper extends BaseMapper<OperateLogPO> {
    // 继承BaseMapper后，自动拥有insert/selectById/selectList等方法
    // 如需自定义查询（如：按用户ID查日志），可在此添加方法并编写XML
}