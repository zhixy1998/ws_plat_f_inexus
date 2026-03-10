/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.config;

import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.util.UserPermissionUtil;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Slf4j
@Aspect
@Order(2)
@Component
public class ButtonPermissionAspect {
    @Autowired
    private UserPermissionUtil userPermissionUtil;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private ApplicationContext applicationContext;

    private final ExpressionParser parser = new SpelExpressionParser();

    private Result<?> createUnauthorizedResult(String message) {
        return Result.warn(message);
    }

    @Around("@annotation(buttonPermission)")
    public Object checkButtonPermission(ProceedingJoinPoint joinPoint, ButtonPermission buttonPermission) throws Throwable {
        // ========== 1. 校验用户登录 ==========
        LoginUserInfoVO currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            log.warn("用户未登录，无法执行操作 {}", buttonPermission.description());
            return createUnauthorizedResult("用户未登录，无法执行操作 " + buttonPermission.description());
        }

        // ========== 2. 核心兼容逻辑：解析权限标识 ==========
        String actualPermission = null;
        String permissionMapping = buttonPermission.permissionMapping();
        String basePermission = buttonPermission.value();

        // 场景1：新写法（配置了permissionMapping）
        if (!permissionMapping.isEmpty()) {
            String operateType = getOperateType(joinPoint, buttonPermission);
            if (operateType == null || operateType.isEmpty()) {
                return Result.fail(400, "操作类型（operateType）不能为空");
            }
            actualPermission = getPermissionFromMapping(buttonPermission, operateType);
            if (actualPermission == null || actualPermission.isEmpty()) {
                return Result.fail(400, "操作类型[" + operateType + "]未配置对应的权限标识");
            }
        }
        // 场景2：老写法（仅配置了value）
        else if (!basePermission.isEmpty()) {
            actualPermission = basePermission; // 直接用value作为权限标识，跳过操作类型解析
        }
        // 场景3：未配置任何权限（异常）
        else {
            return Result.fail(500, "请配置value（老写法）或permissionMapping（新写法）");
        }

        // ========== 3. 校验用户权限 ==========
        if (!hasButtonPermission(currentUser, actualPermission)) {
            log.warn("用户 {} 没有权限执行操作 {} (权限标识：{})",
                    currentUser.getUsername(), buttonPermission.description(), actualPermission);
            return createUnauthorizedResult("权限不足，无法执行操作 " + buttonPermission.description());
        }

        log.info("用户 {} 执行操作：{} (权限标识：{})",
                currentUser.getUsername(), buttonPermission.description(), actualPermission);

        // ========== 4. 调用业务方法（兼容逻辑） ==========
        String methodMapping = buttonPermission.methodMapping();
        // 新写法：配置了methodMapping则自动调用
        if (!methodMapping.isEmpty()) {
            String operateType = getOperateType(joinPoint, buttonPermission);
            return invokeBusinessMethod(joinPoint, buttonPermission, operateType);
        }
        // 老写法：未配置methodMapping则执行接口原有代码
        else {
            return joinPoint.proceed();
        }
    }

    // ========== 原有辅助方法（无需修改） ==========
    private String getPermissionFromMapping(ButtonPermission buttonPermission, String operateType) {
        String permissionMapping = buttonPermission.permissionMapping();
        Map<String, String> permissionMap = new HashMap<>();
        String[] mappings = permissionMapping.split(",");
        for (String mapping : mappings) {
            String[] kv = mapping.split("=");
            if (kv.length == 2) {
                permissionMap.put(kv[0].trim(), kv[1].trim());
            }
        }
        return permissionMap.get(operateType);
    }

    /**
     * 修复版：直接通过反射解析operateType，绕过SpEL兼容问题
     */
    private String getOperateType(ProceedingJoinPoint joinPoint, ButtonPermission buttonPermission) {
        try {
            Object[] args = joinPoint.getArgs();
            // 1. 无参数直接返回null
            if (args == null || args.length == 0) {
                return null;
            }
            Object firstArg = args[0];
            if (firstArg == null) {
                return null;
            }

            // 2. 通过反射获取operateType字段值（不管参数名/SpEL，直接取）
            Class<?> clazz = firstArg.getClass();
            Field operateTypeField = null;
            // 先找当前类的字段，找不到找父类
            try {
                operateTypeField = clazz.getDeclaredField("operateType");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
                if (clazz != null) {
                    operateTypeField = clazz.getDeclaredField("operateType");
                }
            }

            if (operateTypeField == null) {
                log.warn("参数对象中未找到operateType字段");
                return null;
            }

            // 3. 读取字段值
            operateTypeField.setAccessible(true);
            Object operateTypeObj = operateTypeField.get(firstArg);
            return operateTypeObj == null ? null : operateTypeObj.toString().trim();
        } catch (Exception e) {
            log.warn("直接解析operateType失败", e);
            return null;
        }
    }

    /**
     * 修复版：反射调用方法时，严格匹配参数类型
     */
    private Object invokeBusinessMethod(ProceedingJoinPoint joinPoint, ButtonPermission buttonPermission, String operateType) throws Throwable {
        String methodMapping = buttonPermission.methodMapping();
        if (methodMapping.isEmpty()) {
            return joinPoint.proceed();
        }

        // 解析方法映射
        Map<String, String> mappingMap = new HashMap<>();
        String[] mappings = methodMapping.split(",");
        for (String mapping : mappings) {
            String[] kv = mapping.split("=");
            if (kv.length == 2) {
                mappingMap.put(kv[0].trim(), kv[1].trim());
            }
        }

        String targetMethodStr = mappingMap.get(operateType);
        if (targetMethodStr == null) {
            return Result.fail(400, "不支持的操作类型：" + operateType);
        }

        String[] beanAndMethod = targetMethodStr.split("\\.");
        if (beanAndMethod.length != 2) {
            return Result.fail(500, "方法映射格式错误：" + targetMethodStr);
        }
        String beanName = beanAndMethod[0];
        String methodName = beanAndMethod[1];

        // 获取Spring Bean
        Object bean = applicationContext.getBean(beanName);
        if (bean == null) {
            return Result.fail(500, "未找到Bean：" + beanName);
        }

        // 获取接口参数
        Object[] args = joinPoint.getArgs();
        Object businessParam = args.length > 0 ? args[0] : null;
        Class<?> paramType = businessParam == null ? null : businessParam.getClass();

        // 关键修复：严格匹配「方法名+参数个数+参数类型」
        Method targetMethod = null;
        Method[] methods = bean.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)
                    && method.getParameterCount() == (paramType == null ? 0 : 1)
                    && (paramType == null || method.getParameterTypes()[0].isAssignableFrom(paramType))) {
                targetMethod = method;
                break;
            }
        }

        // 未找到匹配的方法
        if (targetMethod == null) {
            String errorMsg = "未找到方法：" + beanName + "." + methodName
                    + "，参数类型：" + (paramType == null ? "无参数" : paramType.getName());
            log.error(errorMsg);
            return Result.fail(500, errorMsg);
        }

        // 调用方法
        targetMethod.setAccessible(true);
        return targetMethod.invoke(bean, businessParam);
    }

    private boolean hasButtonPermission(LoginUserInfoVO user, String permission) {
        try {
            List<String> userPermissions = getUserPermissions(user);
            return userPermissions.contains(permission);
        } catch (Exception e) {
            log.error("检查用户权限时发生异常", e);
            return false;
        }
    }

    private List<String> getUserPermissions(LoginUserInfoVO user) {
        return userPermissionUtil.getCurrentUserPermissionCodes();
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Double.class ||
                clazz == Float.class ||
                clazz == Boolean.class ||
                clazz == Number.class ||
                clazz == Date.class ||
                clazz == java.sql.Date.class ||
                clazz == java.time.LocalDateTime.class;
    }

    private Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }
}