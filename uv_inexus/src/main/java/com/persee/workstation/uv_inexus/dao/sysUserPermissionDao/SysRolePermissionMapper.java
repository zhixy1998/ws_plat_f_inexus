package com.persee.workstation.uv_inexus.dao.sysUserPermissionDao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePermissionPO;

/**
 * 角色-权限关联表数据库访问层
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermissionPO> {
    // 基础CRUD方法已通过BaseMapper继承
    // 示例：如需根据角色ID查询拥有的权限，可添加如下方法
    // List<SysPermissionPO> selectPermissionsByRoleId(String roleId);
}
