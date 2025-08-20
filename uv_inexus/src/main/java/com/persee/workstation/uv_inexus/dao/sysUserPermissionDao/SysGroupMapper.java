package com.persee.workstation.uv_inexus.dao.sysUserPermissionDao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;

/**
 * 小组表数据库访问层
 */
@Mapper
public interface SysGroupMapper extends BaseMapper<SysGroupPO> {
    // 基础CRUD方法已通过BaseMapper继承
    // 示例：如需查询小组关联的用户，可添加如下方法
    // List<SysUserPO> selectUsersByGroupId(String groupId);
}
