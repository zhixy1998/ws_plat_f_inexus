package com.persee.workstation.uv_inexus.dao.sysUserPermissionDao;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;

/**
 * SysUserPO表DAO接口
 */
@Mapper
public interface SysUserDAO extends BaseMapper<SysUserPO> {

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