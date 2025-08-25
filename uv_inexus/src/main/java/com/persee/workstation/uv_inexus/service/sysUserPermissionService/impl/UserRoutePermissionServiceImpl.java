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

        // 3. 查询权限详情（只查菜单类型的权限），并提取 path 字段
        QueryWrapper<SysPermissionPO> permQuery = new QueryWrapper<>();
        permQuery.in("id", permIds)
                .eq("perm_type", 1); // 只筛选菜单类型的权限
        List<String> routePaths = sysPermissionMapper.selectList(permQuery).stream()
                .map(SysPermissionPO::getPath) // 提取 path 字段
                .filter(path -> path != null && !path.isEmpty()) // 过滤空值
                .distinct() // 去重
                .collect(Collectors.toList());

        return Result.ok(routePaths);
    }
}