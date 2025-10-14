/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.dao.sysUserPermissionDao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 用户表数据库访问层
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserPO> {
    // 继承BaseMapper后，已包含CRUD基础方法：
    // selectById、selectList、insert、updateById、deleteById、deleteBatchIds等
    // 如需自定义复杂查询，可在此添加方法并在XML中实现


    /**
     * 根据用户名查询用户（用于登录校验）
     * @param username 用户名
     * @return 完整用户信息
     */
    default SysUserPO selectByUsername(String username) {
        LambdaQueryWrapper<SysUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPO::getUsername, username)
                .eq(SysUserPO::getStatus, 1); // 只查询启用状态的用户（1-启用）
        return this.selectOne(wrapper);
    }

    /**
     * 根据用户名查询当前token
     * @param username 用户名
     * @return 数据库中存储的token
     */
    default String selectTokenByUsername(String username) {
        LambdaQueryWrapper<SysUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPO::getUsername, username)
                .select(SysUserPO::getToken); // 只查询token字段，减少开销
        SysUserPO userPO = this.selectOne(wrapper);
        return userPO != null ? userPO.getToken() : null;
    }

    /**
     * 更新用户token和最后登录时间
     * @param username 用户名
     * @param newToken 新生成的token
     * @param lastLoginTime 本次登录时间
     * @return 影响行数（1-成功，0-失败）
     */
    @Update("UPDATE t_sys_user SET token = #{newToken}, last_login_time = #{lastLoginTime}, update_time = #{lastLoginTime} " +
            "WHERE username = #{username}")
    int updateTokenAndLoginTime(@Param("username") String username,
                                @Param("newToken") String newToken,
                                @Param("lastLoginTime") LocalDateTime lastLoginTime);
}