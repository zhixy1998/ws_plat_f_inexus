package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.UserPermissionService;

@Service
public class UserPermissionServiceImpl implements UserPermissionService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserGroupMapper sysUserGroupMapper;

    @Autowired
    private SysGroupPermissionMapper sysGroupPermissionMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Override
    public Result<List<PermissionDTO>> getUserPermissions(String userId) {
        // 1. 校验用户是否存在
        SysUserPO userPO = sysUserMapper.selectById(userId);
        if (userPO == null) {
            return Result.fail("用户不存在");
        }

        // 2. 根据用户ID查询关联的小组ID（用户-小组关联表）
        QueryWrapper<SysUserGroupPO> userGroupQuery = new QueryWrapper<>();
        userGroupQuery.eq("user_id", userId);
        List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(userGroupQuery);
        if (userGroupList.isEmpty()) {
            return Result.fail("该用户未关联任何小组");
        }

        // 提取小组ID列表
        List<String> groupIds = userGroupList.stream()
                .map(SysUserGroupPO::getGroupId)
                .collect(Collectors.toList());

        // 3. 根据小组ID查询关联的权限ID（小组-权限关联表）
        QueryWrapper<SysGroupPermissionPO> groupPermQuery = new QueryWrapper<>();
        groupPermQuery.in("group_id", groupIds);
        List<SysGroupPermissionPO> groupPermList = sysGroupPermissionMapper.selectList(groupPermQuery);
        if (groupPermList.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        // 提取权限ID列表（去重）
        List<String> permIds = groupPermList.stream()
                .map(SysGroupPermissionPO::getPermId)
                .distinct()  // 避免权限重复
                .collect(Collectors.toList());

        // 4. 根据权限ID查询权限详情
        QueryWrapper<SysPermissionPO> permQuery = new QueryWrapper<>();
        permQuery.in("id", permIds);
        List<SysPermissionPO> permissionList = sysPermissionMapper.selectList(permQuery);

        // 5. 转换为DTO返回
        List<PermissionDTO> permissionDTOs = permissionList.stream()
                .map(perm -> {
                    PermissionDTO dto = new PermissionDTO();
                    BeanUtils.copyProperties(perm, dto);
                    return dto;
                })
                .collect(Collectors.toList());

        return Result.ok(permissionDTOs);
    }

    // 新增方法实现：查询parent_id=0的path列表
    @Override
    public Result<List<String>> getRootPermissionPaths() {
        // 1. 构造查询条件：parent_id = 0
        QueryWrapper<SysPermissionPO> permQuery = new QueryWrapper<>();
        permQuery.eq("parent_id", 0); // 只筛选菜单类型的权限
        List<String> routeNames = sysPermissionMapper.selectList(permQuery).stream()
                .map(SysPermissionPO::getPermName) // 提取 name 字段
                .filter(name -> name != null && !name.isEmpty()) // 过滤空值
                .distinct() // 去重
                .collect(Collectors.toList());

        // 4. 返回结果
        return Result.ok(routeNames);
    }
}