/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dao.sysUserPermissionDao;

import java.util.List;

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
    // 根据角色ID查询组
    List<SysGroupPO> selectByRoleId(String roleId);
}
