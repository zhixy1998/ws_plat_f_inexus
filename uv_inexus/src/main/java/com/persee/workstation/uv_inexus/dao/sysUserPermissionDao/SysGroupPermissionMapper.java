package com.persee.workstation.uv_inexus.dao.sysUserPermissionDao;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPermissionPO;

/**
 * 角色-权限关联表数据库访问层
 */
@Mapper
public interface SysGroupPermissionMapper extends BaseMapper<SysGroupPermissionPO> {
    // 基础CRUD方法已通过BaseMapper继承
    // 示例：如需根据角色ID查询拥有的权限，可添加如下方法
    // List<SysPermissionPO> selectPermissionsByRoleId(String roleId);

    /**
     * 批量插入小组-权限关系
     * @param records 关系列表
     * @return 插入成功条数
     */
    @Insert("<script>" +
            "INSERT INTO t_sys_group_permission (group_id, perm_id) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','> " +
            "(#{item.groupId}, #{item.permId}) " +
            "</foreach> " +
            "</script>")
    int insertBatch(@Param("list") List<SysGroupPermissionPO> records);
}
