/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.config;

import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.util.UserPermissionUtil;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;

import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 按钮权限检查切面
 * 用于拦截带有 @ButtonPermission 注解的方法，进行权限检查
 */
@Slf4j
@Aspect
@Order(2) // 设置切面执行顺序，确保在其他切面之前执行
@Component
public class ButtonPermissionAspect {
    @Autowired
    private UserPermissionUtil userPermissionUtil;
    @Autowired
    private SysUserService sysUserService;

    /**
     * 拦截带有 @ButtonPermission 注解的方法
     *
     * @param joinPoint        连接点
     * @param buttonPermission 按钮权限注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(buttonPermission)")
    public Object checkButtonPermission(ProceedingJoinPoint joinPoint, ButtonPermission buttonPermission) throws Throwable {
        // 获取当前用户信息
        LoginUserInfoVO currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            log.warn("用户未登录，无法执行需要权限的操作: {}", buttonPermission.description());
            throw new SecurityException("用户未登录，无法执行操作: " + buttonPermission.description());
        }

        // 检查用户是否有执行该操作的权限
        if (!hasButtonPermission(currentUser, buttonPermission.value())) {
            log.warn("用户 {} 没有权限执行操作: {} (权限标识: {})",
                    currentUser.getUsername(),
                    buttonPermission.description(),
                    buttonPermission.value());
            throw new SecurityException("权限不足，无法执行操作: " + buttonPermission.description());
        }

        log.info("用户 {} 执行操作: {} (权限标识: {})",
                currentUser.getUsername(),
                buttonPermission.description(),
                buttonPermission.value());

        // 执行原方法
        return joinPoint.proceed();
    }

    /**
     * 检查用户是否有指定按钮权限
     *
     * @param user       当前用户
     * @param permission 权限标识
     * @return 是否有权限
     */
    private boolean hasButtonPermission(LoginUserInfoVO user, String permission) {
        try {
            // 获取用户权限列表
            List<String> userPermissions = getUserPermissions(user);

            // 检查是否包含所需权限
            return userPermissions.contains(permission);
        } catch (Exception e) {
            log.error("检查用户权限时发生异常", e);
            return false; // 出现异常默认拒绝访问
        }
    }


    /**
     * 获取用户权限列表（根据RBAC模型）
     *
     * @param user 用户信息
     * @return 用户拥有的权限列表
     */
    private List<String> getUserPermissions(LoginUserInfoVO user) {
        List<String> userPermissionCodes = userPermissionUtil.getCurrentUserPermissionCodes();
        log.info("用户 {} 权限列表: {}", user.getUsername(), userPermissionCodes);
        return userPermissionCodes;
    }
}

