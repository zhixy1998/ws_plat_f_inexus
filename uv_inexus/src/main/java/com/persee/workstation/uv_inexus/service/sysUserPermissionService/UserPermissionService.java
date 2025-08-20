package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import java.util.List;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionDTO;
import com.persee.workstation.uv_inexus.model.Result;

/**
 * 用户权限查询服务
 */
public interface UserPermissionService {
    // 根据用户ID查询所有权限
    Result<List<PermissionDTO>> getUserPermissions(String userId);
}