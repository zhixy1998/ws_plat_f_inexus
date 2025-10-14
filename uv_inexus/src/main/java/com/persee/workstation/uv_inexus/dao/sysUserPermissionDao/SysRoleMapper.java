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

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;

/**
 * 角色表数据库访问层
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRolePO> {
    // 基础CRUD方法已通过BaseMapper继承
    // 示例：如需查询角色关联的权限，可添加如下方法
    // List<SysPermissionPO> selectPermissionsByRoleId(String roleId);
}