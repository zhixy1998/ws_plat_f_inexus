package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import java.util.List;

import com.persee.workstation.uv_inexus.model.Result;

public interface UserRoutePermissionService {
    // 根据用户ID查询有权访问的路由结构
    Result<List<String>> getUserRoutePermissions(String userId);
}