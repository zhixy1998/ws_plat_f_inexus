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

package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.UserRoutePermissionService;

@Service
public class UserRoutePermissionServiceImpl implements UserRoutePermissionService {

    @Autowired
    private SysUserGroupMapper sysUserGroupMapper;

    @Autowired
    private SysGroupPermissionMapper sysGroupPermissionMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Override
    public Result<List<String>> getUserRoutePermissions(String userId) {
        // 1. 查询用户所属的所有小组ID
        QueryWrapper<SysUserGroupPO> userGroupQuery = new QueryWrapper<>();
        userGroupQuery.eq("user_id", userId);
        List<String> groupIds = sysUserGroupMapper.selectList(userGroupQuery).stream()
                .map(SysUserGroupPO::getGroupId)
                .collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            return Result.fail("用户未关联任何小组，无访问权限");
        }

        // 2. 查询小组关联的所有权限ID
        QueryWrapper<SysGroupPermissionPO> groupPermQuery = new QueryWrapper<>();
        groupPermQuery.in("group_id", groupIds);
        List<String> permIds = sysGroupPermissionMapper.selectList(groupPermQuery).stream()
                .map(SysGroupPermissionPO::getPermId)
                .distinct()
                .collect(Collectors.toList());
        if (permIds.isEmpty()) {
            return Result.fail("用户所在小组未配置权限");
        }

        // 3. 查询权限详情（只查菜单类型的权限），并提取 name 字段
        QueryWrapper<SysPermissionPO> permQuery = new QueryWrapper<>();
        permQuery.in("id", permIds)
                .eq("perm_type", 1); // 只筛选菜单类型的权限
        List<String> routeNames = sysPermissionMapper.selectList(permQuery).stream()
                .map(SysPermissionPO::getPermName) // 提取 name 字段
                .filter(name -> name != null && !name.isEmpty()) // 过滤空值
                .distinct() // 去重
                .collect(Collectors.toList());

        return Result.ok(routeNames);
    }
}