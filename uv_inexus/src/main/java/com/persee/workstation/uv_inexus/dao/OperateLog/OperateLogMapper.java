/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

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