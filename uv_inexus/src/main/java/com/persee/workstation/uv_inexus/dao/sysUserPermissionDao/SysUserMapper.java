package com.persee.workstation.uv_inexus.dao.sysUserPermissionDao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;

/**
 * 用户表数据库访问层
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserPO> {
    // 继承BaseMapper后，已包含CRUD基础方法：
    // selectById、selectList、insert、updateById、deleteById、deleteBatchIds等
    // 如需自定义复杂查询，可在此添加方法并在XML中实现
}