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

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

/**
 * 用户登录Service接口（定义登录业务抽象方法）
 */
public interface SysUserLoginService {

    /**
     * 用户登录核心方法
     * @param loginDTO 登录请求参数（包含用户名、密码、快捷登录标识）
     * @return R<LoginUserInfoVO> 登录结果（成功返回用户信息+token，失败返回错误信息）
     */
    R<LoginUserInfoVO> login(UserLoginDTO loginDTO);
}
