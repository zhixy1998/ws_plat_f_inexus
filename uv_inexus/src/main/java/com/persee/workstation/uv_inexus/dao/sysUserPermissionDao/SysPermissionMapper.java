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

package com.persee.workstation.uv_inexus.dao.sysUserPermissionDao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;

/**
 * 权限表数据库访问层
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissionPO> {
    // 基础CRUD方法已通过BaseMapper继承
    // 示例：查询菜单权限树（递归查询）
    List<SysPermissionPO> selectMenuTree();
}