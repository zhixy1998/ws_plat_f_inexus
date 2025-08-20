package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import java.util.List;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;

public interface RoleGroupUserTreeService {
    // 获取角色-组-用户树形结构
    Result<List<RoleTreeDTO>> getRoleGroupUserTree();
}