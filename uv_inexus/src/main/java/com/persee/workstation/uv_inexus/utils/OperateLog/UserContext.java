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

package com.persee.workstation.uv_inexus.utils.OperateLog;

import java.util.Objects;

import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

public class UserContext {
    // 线程局部变量：存储当前请求的用户信息
    private static final ThreadLocal<LoginUserInfoVO> USER_THREAD_LOCAL = new ThreadLocal<>();

    // 设置当前用户
    public static void setCurrentUser(LoginUserInfoVO userVO) {
        if (Objects.nonNull(userVO)) {
            USER_THREAD_LOCAL.set(userVO);
        }
    }

    // 获取当前用户
    public static LoginUserInfoVO getCurrentUser() {
        return USER_THREAD_LOCAL.get();
    }

    // 清除当前用户（防止内存泄漏）
    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }
}