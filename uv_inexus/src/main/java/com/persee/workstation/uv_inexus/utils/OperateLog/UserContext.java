/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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