package com.persee.workstation.uv_inexus.dao.sysUserPermissionDao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;

/**
 * 权限表数据库访问层
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissionPO> {
    // 基础CRUD方法已通过BaseMapper继承
    // 示例：查询菜单权限树（递归查询）
    List<SysPermissionPO> selectMenuTree();
}