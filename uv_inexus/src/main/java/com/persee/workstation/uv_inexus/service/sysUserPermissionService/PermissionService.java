package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import java.util.List;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionSaveDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionUpdateDTO;
import com.persee.workstation.uv_inexus.model.Result;

public interface PermissionService {

    Result<List<PermissionTreeDTO>> getAllPermissionTree();

    Result<String> addPermission(PermissionSaveDTO saveDTO);

    Result<String> updatePermission(PermissionUpdateDTO updateDTO);
}

