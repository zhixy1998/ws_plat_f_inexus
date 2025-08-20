package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

public interface SysRoleService extends IService<SysRolePO> {

    /**
     * 分页查询角色
     */
    Result<PageView<SysRolePO>> getRolePage(Page<SysRolePO> page, RoleQueryDTO queryDTO);

    /**
     * 新增角色
     */
    Result<String> addRole(RoleSaveDTO saveDTO);

    /**
     * 修改角色
     */
    Result<String> updateRole(RoleSaveDTO saveDTO);

    /**
     * 批量删除角色
     */
    Result<String> deleteBatch(List<String> ids);

    /**
     * 根据ID查询角色
     */
    Result<SysRolePO> getRoleById(String id);

    /**
     * 根据ID删除单个角色
     */
    Result<String> deleteById(String id);
}