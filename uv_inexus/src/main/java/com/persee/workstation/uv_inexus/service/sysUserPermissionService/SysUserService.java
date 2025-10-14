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

package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PasswordUpdateDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserDetailDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

public interface SysUserService extends IService<SysUserPO> {

    /**
     * 分页查询用户
     */
    Result<PageView<SysUserPO>> getUserPage(Page<SysUserPO> page, UserQueryDTO queryDTO);

    /**
     * 新增用户
     */
    Result<String> addUser(UserSaveDTO saveDTO);

    /**
     * 修改用户
     */
    Result<String> updateUser(UserSaveDTO saveDTO);

    /**
     * 批量删除用户
     */
    Result<String> deleteBatch(List<String> ids);

    /**
     * 根据ID查询用户
     */
    Result<UserDetailDTO> getUserById(String id);

    /**
     * 根据ID删除单个用户
     */
    Result<String> deleteById(String id);

    /**
     * 修改密码
     */
    Result<String> updatePassword(PasswordUpdateDTO updateDTO);

    /**
     * 重置用户密码为默认值123456
     * @param userId 用户ID
     * @return 操作结果
     */
    Result<String> resetPassword(String userId);
}