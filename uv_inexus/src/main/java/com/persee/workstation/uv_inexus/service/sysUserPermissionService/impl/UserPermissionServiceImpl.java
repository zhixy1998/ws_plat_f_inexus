package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
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

        // 2. 查用户关联的小组ID
        QueryWrapper<SysUserGroupPO> userGroupQuery = new QueryWrapper<>();
        userGroupQuery.eq("user_id", userId);
        List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(userGroupQuery);
        if (userGroupList.isEmpty()) {
            return Result.fail("该用户未关联任何小组");
        }
        List<String> groupIds = userGroupList.stream()
                .map(SysUserGroupPO::getGroupId)
                .collect(Collectors.toList());

        // 3. 查小组关联的权限ID（去重）
        QueryWrapper<SysGroupPermissionPO> groupPermQuery = new QueryWrapper<>();
        groupPermQuery.in("group_id", groupIds);
        List<SysGroupPermissionPO> groupPermList = sysGroupPermissionMapper.selectList(groupPermQuery);
        if (groupPermList.isEmpty()) {
            return Result.ok(new ArrayList<>()); // 无权限返回空列表
        }
        List<String> permIds = groupPermList.stream()
                .map(SysGroupPermissionPO::getPermId)
                .distinct()
                .collect(Collectors.toList());

        // 4. 查权限详情：SQL层面排除“首页”（源头过滤，关键修改1）
        QueryWrapper<SysPermissionPO> permQuery = new QueryWrapper<>();
        permQuery.in("id", permIds)
                .ne("title", "首页") // 核心：排除数据库中title为“首页”的权限
                .orderByAsc("sort"); // 数据库预排序，提升效率
        List<SysPermissionPO> permissionList = sysPermissionMapper.selectList(permQuery);

        // 5. 转换为DTO + 二次排除“首页”（双重保险）+ 扁平列表排序（关键修改2）
        List<PermissionDTO> flatPermissionDTOs = permissionList.stream()
                .map(perm -> {
                    PermissionDTO dto = new PermissionDTO();
                    BeanUtils.copyProperties(perm, dto);
                    return dto;
                })
                // 二次排除：避免SQL筛选失效（如字段映射问题），字段改为title（关键）
                .filter(dto -> dto.getTitle() == null || !"首页".equals(dto.getTitle()))
                // 扁平列表按sort升序，为树形排序打基础
                .sorted((d1, d2) -> {
                    int sort1 = (d1.getSort() == null) ? 0 : d1.getSort(); // 处理null
                    int sort2 = (d2.getSort() == null) ? 0 : d2.getSort();
                    return Integer.compare(sort1, sort2); // 升序：小→大
                })
                .collect(Collectors.toList());

        // 6. 构建树形结构（含全层级排序）
        List<PermissionDTO> treePermissionDTOs = buildPermissionTree(flatPermissionDTOs);

        return Result.ok(treePermissionDTOs);
    }

    // ------------------------------ 树形结构组装 + 全层级sort排序（核心方法） ------------------------------
    private List<PermissionDTO> buildPermissionTree(List<PermissionDTO> flatPermissions) {
        // 1. 构建权限ID映射，O(1)快速查找父权限
        Map<String, PermissionDTO> permissionMap = flatPermissions.stream()
                .collect(Collectors.toMap(
                        PermissionDTO::getId,
                        dto -> dto,
                        (existing, replacement) -> existing // 重复ID保留第一个
                ));

        // 2. 遍历建立父子关系 + 子权限内部排序
        List<PermissionDTO> rootPermissions = new ArrayList<>();
        for (PermissionDTO currentPerm : flatPermissions) {
            Integer parentId = currentPerm.getParentId();
            // 判断根权限（parentId为null/0或父权限不存在）
            if (parentId == null || parentId == 0 || !permissionMap.containsKey(parentId.toString())) {
                rootPermissions.add(currentPerm);
            } else {
                // 非根权限：添加到父权限的children，并立即排序
                PermissionDTO parentPerm = permissionMap.get(parentId.toString());
                if (parentPerm.getChildren() == null) {
                    parentPerm.setChildren(new ArrayList<>());
                }
                parentPerm.getChildren().add(currentPerm);
                // 子权限内部按sort升序（关键：确保子集有序）
                parentPerm.setChildren(sortPermissionBySort(parentPerm.getChildren()));
            }
        }

        // 3. 根权限按sort升序（关键：确保父级有序）
        return sortPermissionBySort(rootPermissions);
    }

    // ------------------------------ 通用工具：按sort升序排序（复用） ------------------------------
    private List<PermissionDTO> sortPermissionBySort(List<PermissionDTO> permissions) {
        return permissions.stream()
                .sorted((p1, p2) -> {
                    int sort1 = (p1.getSort() == null) ? 0 : p1.getSort();
                    int sort2 = (p2.getSort() == null) ? 0 : p2.getSort();
                    return Integer.compare(sort1, sort2);
                })
                .collect(Collectors.toList());
    }

    // ------------------------------ 原有方法：getRootPermissionPaths（同步排除首页） ------------------------------
    @Override
    public Result<List<String>> getRootPermissionPaths(String userId) {
        SysUserPO userPO = sysUserMapper.selectById(userId);
        if (userPO == null) {
            return Result.fail("用户不存在");
        }

        QueryWrapper<SysUserGroupPO> userGroupQuery = new QueryWrapper<>();
        userGroupQuery.eq("user_id", userId);
        List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(userGroupQuery);
        if (userGroupList.isEmpty()) {
            return Result.fail("该用户未关联任何小组");
        }

        List<String> groupIds = userGroupList.stream()
                .map(SysUserGroupPO::getGroupId)
                .collect(Collectors.toList());

        QueryWrapper<SysGroupPermissionPO> groupPermQuery = new QueryWrapper<>();
        groupPermQuery.in("group_id", groupIds);
        List<SysGroupPermissionPO> groupPermList = sysGroupPermissionMapper.selectList(groupPermQuery);
        if (groupPermList.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        List<String> permIds = groupPermList.stream()
                .map(SysGroupPermissionPO::getPermId)
                .distinct()
                .collect(Collectors.toList());

        // 同步排除“首页”+按sort排序（与主方法逻辑一致）
        QueryWrapper<SysPermissionPO> permQuery = new QueryWrapper<>();
        permQuery.in("id", permIds)
                .eq("parent_id", 0)
                .ne("title", "首页") // 排除首页
                .orderByAsc("sort") // 按sort升序
                .select("path");
        List<SysPermissionPO> permissionList = sysPermissionMapper.selectList(permQuery);

        List<String> rootPermissionPaths = permissionList.stream()
                .map(SysPermissionPO::getPath)
                .filter(path -> path != null && !path.trim().isEmpty())
                .collect(Collectors.toList());

        return Result.ok(rootPermissionPaths);
    }

    @Override
    public Result<List<PermissionTreeDTO>> getPermissionTreeByTypeAndRole(List<Integer> permTypeList, String roleId) {
        try {
            // 1. 查询该角色拥有的所有权限ID
            Set<String> rolePermIds = getRolePermissionIds(roleId);
            
            // 2. 查询符合权限类型的所有权限
            List<SysPermissionPO> permissions = queryPermissionsByType(permTypeList);
            
            // 3. 转换为树形结构DTO并设置isHave属性
            List<PermissionTreeDTO> treeList = buildPermissionTree(permissions, rolePermIds);
            
            return Result.ok(treeList);
        } catch (Exception e) {
            return Result.fail("查询权限树形结构失败: " + e.getMessage());
        }
    }

    /**
     * 获取角色拥有的所有权限ID
     */
    private Set<String> getRolePermissionIds(String roleId) {
        QueryWrapper<SysGroupPermissionPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", roleId);
        List<SysGroupPermissionPO> groupPermissions = sysGroupPermissionMapper.selectList(queryWrapper);
        
        return groupPermissions.stream()
                .map(SysGroupPermissionPO::getPermId)
                .collect(Collectors.toSet());
    }

    /**
     * 根据权限类型列表查询权限
     */
    private List<SysPermissionPO> queryPermissionsByType(List<Integer> permTypeList) {
        QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
        // 如果有指定权限类型，则使用in查询
        if (permTypeList != null && !permTypeList.isEmpty()) {
            queryWrapper.in("perm_type", permTypeList);
        }
        // 可以根据需要添加其他条件，如状态为启用等
        queryWrapper.eq("status", 1);
        return sysPermissionMapper.selectList(queryWrapper);
    }

    /**
     * 构建权限树形结构
     */
    private List<PermissionTreeDTO> buildPermissionTree(List<SysPermissionPO> permissions, Set<String> rolePermIds) {
        // 实现逻辑不变
        List<PermissionTreeDTO> allNodes = permissions.stream().map(po -> {
            PermissionTreeDTO dto = new PermissionTreeDTO();
            BeanUtils.copyProperties(po, dto);
            // 设置是否拥有该权限
            dto.setIsHave(rolePermIds.contains(po.getId()));
            return dto;
        }).collect(Collectors.toList());

        Map<String, PermissionTreeDTO> nodeMap = new HashMap<>();
        for (PermissionTreeDTO node : allNodes) {
            nodeMap.put(node.getId(), node);
        }
        
        List<PermissionTreeDTO> rootNodes = new ArrayList<>();
        for (PermissionTreeDTO node : allNodes) {
            String parentId = String.valueOf(node.getParentId());
            if ("0".equals(parentId)) {
                rootNodes.add(node);
            } else {
                PermissionTreeDTO parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    if (parentNode.getChildren() == null) {
                        parentNode.setChildren(new ArrayList<>());
                    }
                    parentNode.getChildren().add(node);
                }
            }
        }
        
        sortChildren(rootNodes);
        return rootNodes;
    }

    /**
     * 递归排序子节点
     */
    private void sortChildren(List<PermissionTreeDTO> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        
        nodes.sort(Comparator.comparingInt(PermissionTreeDTO::getSort));
        
        for (PermissionTreeDTO node : nodes) {
            sortChildren(node.getChildren());
        }
    }
}