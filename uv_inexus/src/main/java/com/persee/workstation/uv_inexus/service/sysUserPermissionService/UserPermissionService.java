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
            List<Integer> permTypeList, String userId);
}