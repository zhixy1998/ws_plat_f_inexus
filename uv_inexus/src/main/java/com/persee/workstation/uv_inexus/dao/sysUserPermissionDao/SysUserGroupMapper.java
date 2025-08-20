package com.persee.workstation.uv_inexus.dao.sysUserPermissionDao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;

/**
 * 用户-小组关联表数据库访问层
 */
@Mapper
public interface SysUserGroupMapper extends BaseMapper<SysUserGroupPO> {
    // 基础CRUD方法已通过BaseMapper继承
    // 示例：如需根据用户ID查询所属小组，可添加如下方法
    // List<SysGroupPO> selectGroupsByUserId(String userId);
}