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

package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import java.util.List;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;

/**
 * 用户权限查询服务
 */
public interface UserPermissionService {
    // 根据用户ID查询所有权限
    Result<List<PermissionDTO>> getUserPermissions(String userId);

    // 新增方法：查询parent_id为0的所有权限的path值
    Result<List<String>> getRootPermissionPaths(String userId);

    // 根据权限类型和角色ID查询树形权限结构
    Result<List<PermissionTreeDTO>> getPermissionTreeByTypeAndUser(
            List<Integer> permTypeList, String groupId);

    /**
     * 更新小组权限关系
     * @param groupId 小组ID
     * @param targetPermIds 目标权限ID列表
     * @return 操作结果
     */
    Result<Boolean> updateGroupPermissions(String groupId, List<String> targetPermIds);
}