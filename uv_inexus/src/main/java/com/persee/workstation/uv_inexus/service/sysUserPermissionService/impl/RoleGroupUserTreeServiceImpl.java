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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysRoleMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupTreeDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleTreeDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.RoleGroupUserTreeService;

@Service
public class RoleGroupUserTreeServiceImpl implements RoleGroupUserTreeService {

    private static final Logger log = LoggerFactory.getLogger(RoleGroupUserTreeServiceImpl.class);
    // 常量：超级管理员角色编码（统一定义，避免硬编码）
    private static final String SUPER_ADMIN_ROLE_CODE = "SYSTEM_USER_MANAGEMENT";

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysGroupMapper sysGroupMapper;

    @Autowired
    private SysUserGroupMapper sysUserGroupMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    // 优化：返回类型改为 List<RoleTreeDTO>，支持多角色返回
    @Override
    public Result<List<RoleTreeDTO>> getRoleGroupUserTree(String userId) {
        try {
            // -------------------------- 1. 基础校验：查询用户及所属角色ID --------------------------
            SysUserPO userPO = getUserByUserId(userId);
            if (userPO == null) {
                //用户不存在或已禁用
                return Result.fail(YamlMessageSource.getMessageStatic("00604001"));
            }

            Integer userRoleId = userPO.getRoleId();
            if (userRoleId == null) {
                log.warn("用户未分配角色，userId={}", userId);
                //该用户未分配任何角色
                return Result.fail("00604002");
            }

            // -------------------------- 2. 查询用户所属角色，判断是否为超级管理员 --------------------------
            SysRolePO userRolePO = getRoleByRoleId(userRoleId);
            if (userRolePO == null) {
                //用户所属角色不存在或已禁用
                return Result.fail(YamlMessageSource.getMessageStatic("00604003"));
            }

            // -------------------------- 3. 分场景处理：超级管理员 vs 普通用户 --------------------------
            List<RoleTreeDTO> roleTreeList = new ArrayList<>();
            if (SUPER_ADMIN_ROLE_CODE.equals(userRolePO.getRoleCode())) {
                // 3.1 超级管理员：返回所有启用的角色树形
                roleTreeList = assembleAllEnabledRoleTree();
                log.info("SUPER_ADMIN查询所有角色树形：角色数量={}，userId={}", roleTreeList.size(), userId);
            } else {
                // 3.2 普通用户：返回仅当前用户所属角色的树形（包装为List，统一返回格式）
                RoleTreeDTO userRoleTree = assembleSingleRoleTree(userRolePO);
                roleTreeList.add(userRoleTree);
                log.info("普通用户查询角色树形：角色ID={}，userId={}", userRoleId, userId);
            }

            return Result.ok(roleTreeList);

        } catch (Exception e) {
            log.error("获取角色-组-用户树形异常：userId={}", userId, e);
            //"查询失败：" + e.getMessage()
            return Result.fail(YamlMessageSource.getMessageStatic("0060300B",e.getMessage()));
        }
    }


    // -------------------------- 私有工具方法：代码复用，减少冗余 --------------------------

    /**
     * 工具方法1：根据用户ID查询启用的用户
     */
    private SysUserPO getUserByUserId(String userId) {
        LambdaQueryWrapper<SysUserPO> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(SysUserPO::getId, userId)
                 .eq(SysUserPO::getStatus, 1); // 仅查启用用户
        return sysUserMapper.selectOne(userQuery);
    }

    /**
     * 工具方法2：根据角色ID查询启用的角色
     */
    private SysRolePO getRoleByRoleId(Integer roleId) {
        LambdaQueryWrapper<SysRolePO> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.eq(SysRolePO::getId, roleId)
                 .eq(SysRolePO::getStatus, 1); // 仅查启用角色
        return sysRoleMapper.selectOne(roleQuery);
    }

    /**
     * 工具方法3：组装单个角色的树形（含角色→组→用户）
     */
    private RoleTreeDTO assembleSingleRoleTree(SysRolePO rolePO) {
        RoleTreeDTO roleTree = new RoleTreeDTO();
        BeanUtils.copyProperties(rolePO, roleTree); // 复制角色基础信息

        // 调用工具方法组装该角色下的组和用户
        List<GroupTreeDTO> groupTreeList = assembleGroupTreeList(rolePO.getId().toString());
        roleTree.setGroupList(groupTreeList);

        return roleTree;
    }

    /**
     * 工具方法4：查询所有启用角色，并组装树形
     */
    private List<RoleTreeDTO> assembleAllEnabledRoleTree() {
        // 查询所有启用的角色
        LambdaQueryWrapper<SysRolePO> allRoleQuery = new LambdaQueryWrapper<>();
        allRoleQuery.eq(SysRolePO::getStatus, 1)
                    .orderByAsc(SysRolePO::getRoleName); // 按角色名升序

        List<SysRolePO> allEnabledRoles = sysRoleMapper.selectList(allRoleQuery);
        if (allEnabledRoles.isEmpty()) {
            return new ArrayList<>();
        }

        // 遍历每个角色，组装树形
        return allEnabledRoles.stream()
                .map(this::assembleSingleRoleTree) // 复用单个角色组装逻辑
                .collect(Collectors.toList());
    }

    /**
     * 工具方法5：根据角色ID组装组树形（含组→用户）
     * 核心复用逻辑：普通用户和超级管理员的组-用户组装逻辑一致
     */
    private List<GroupTreeDTO> assembleGroupTreeList(String roleId) {
        // 1. 查询该角色下的所有启用组
        LambdaQueryWrapper<SysGroupPO> groupQuery = new LambdaQueryWrapper<>();
        groupQuery.eq(SysGroupPO::getRoleId, roleId)

                  .orderByAsc(SysGroupPO::getGroupName);
        List<SysGroupPO> groupList = sysGroupMapper.selectList(groupQuery);

        if (groupList.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 遍历组，组装每个组下的用户
        return groupList.stream().map(group -> {
            GroupTreeDTO groupTree = new GroupTreeDTO();
            BeanUtils.copyProperties(group, groupTree); // 复制组基础信息

            // 2.1 查询组-用户关联关系
            LambdaQueryWrapper<SysUserGroupPO> userGroupQuery = new LambdaQueryWrapper<>();
            userGroupQuery.eq(SysUserGroupPO::getGroupId, group.getId());
            List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(userGroupQuery);

            // 2.2 提取用户ID（去重）
            List<String> userIds = userGroupList.stream()
                    .map(SysUserGroupPO::getUserId)
                    .distinct()
                    .collect(Collectors.toList());

            // 2.3 查询启用的用户
            List<UserTreeDTO> userTreeList = assembleUserTreeList(userIds);
            groupTree.setUserList(userTreeList);

            return groupTree;
        }).collect(Collectors.toList());
    }

    /**
     * 工具方法6：根据用户ID列表组装用户树形
     */
    private List<UserTreeDTO> assembleUserTreeList(List<String> userIds) {
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询启用的用户
        LambdaQueryWrapper<SysUserPO> userQuery = new LambdaQueryWrapper<>();
        userQuery.in(SysUserPO::getId, userIds)

                 .orderByAsc(SysUserPO::getRealName);
        List<SysUserPO> userList = sysUserMapper.selectList(userQuery);

        // 转换为UserTreeDTO
        return userList.stream().map(user -> {
            UserTreeDTO userTree = new UserTreeDTO();
            BeanUtils.copyProperties(user, userTree);
            return userTree;
        }).collect(Collectors.toList());
    }
}