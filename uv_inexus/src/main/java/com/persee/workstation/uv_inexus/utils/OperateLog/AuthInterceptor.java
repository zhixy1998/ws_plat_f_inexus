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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import com.persee.workstation.uv_inexus.util.tokenUtil.JWTUtils;

/**
 * 认证拦截器：验证请求头中的token，解析用户信息并存储到UserContext
 */
public class AuthInterceptor implements HandlerInterceptor {
    // 日志对象
    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    /**
     * 请求处理前执行：验证token并存储用户信息
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头获取token（与前端约定的键名：Authorization）
        String token = request.getHeader("Authorization");
        log.info("拦截到请求：{}，请求方法：{}，携带的token：{}",
                request.getRequestURI(),
                request.getMethod(),
                token != null ? token : "null"); // 打印token（为null时显示"null"）

        // 2. 验证token并存储用户信息（调用JWTUtils.verify自动处理）
        if (token != null && !token.trim().isEmpty()) {
            // 调用verify方法：验证通过会自动将用户名存入UserContext
            boolean isVerified = JWTUtils.verify(token);
            if (isVerified) {
                log.info("token验证通过，用户信息已存入UserContext");
            } else {
                log.warn("token验证失败：无效或已过期");
            }
        } else {
            log.warn("请求头中未携带token");
        }

        // 3. 允许请求继续处理（即使token无效也不拦截，由业务逻辑决定是否拒绝）
        return true;
    }
}
    