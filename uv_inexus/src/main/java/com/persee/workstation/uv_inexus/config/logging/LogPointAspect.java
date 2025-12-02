/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.config.logging;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.config.LogPoint;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
public class LogPointAspect {
    // 加密审计日志记录器
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("ENCRYPTED_AUDIT_LOGGER");

    // 定义切点：拦截所有带有@LogPoint注解的方法
    @Pointcut("@annotation(com.persee.workstation.uv_inexus.config.LogPoint)")
    public void logPointCut() {
    }

    // 环绕通知：拦截带有@LogPoint注解的方法执行
    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable ex = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            ex = e;
            throw e;
        } finally {
            try {
                long endTime = System.currentTimeMillis();
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Method method = signature.getMethod();
                LogPoint logPoint = method.getAnnotation(LogPoint.class);

                // 构建日志数据结构，使用LinkedHashMap保持字段顺序
                LinkedHashMap<String, Object> logData = new LinkedHashMap<>();
                logData.put("module", logPoint.module()); // 模块名称
                logData.put("client", getClientIPSafely()); // 客户端IP地址
                logData.put("timestamp", System.currentTimeMillis()); // 时间戳（毫秒）
                logData.put("paramsTypes", getParameterTypeNames(signature)); // 参数类型列表
                logData.put("operation", logPoint.value()); // 操作描述
                logData.put("className", joinPoint.getTarget().getClass().getName()); // 完整类名
                logData.put("methodName", method.getName()); // 方法名
                logData.put("user", getCurrentUserSafely()); // 当前用户

                if (logPoint.recordDuration()) {
                    logData.put("durationMs", endTime - startTime); // 方法执行耗时（毫秒）
                }

                if (logPoint.recordParams()) {
                    logData.put("params", getNamedParameters(signature, joinPoint.getArgs())); // 参数名值对
                }

                if (logPoint.recordResult()) {
                    logData.put("result", result); // 方法返回值
                }

                // 从ContextHolder获取额外的数据并合并
                Map<String, Object> customData = LogPointContextHolder.getContext();
                if (customData != null && !customData.isEmpty()) {
                    logData.put("customData", customData); // 自定义数据
                }

                // 记录更详细的异常信息
                if (ex != null) {
                    Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("class", ex.getClass().getName()); // 异常类名
                    errorMap.put("message", ex.getMessage()); // 异常消息
                    logData.put("error", errorMap); // 错误信息
                }

                String jsonLog = JSON.toJSONString(logData); // JSON序列化
                String encryptedLog = SymmetricEncryptor.encrypt(jsonLog); // AES加密

                AUDIT_LOGGER.info(encryptedLog); // 记录加密日志

            } catch (Exception e) {
                LoggerFactory.getLogger(LogPointAspect.class).error("Failed to record audit log", e);
            } finally {
                // 确保在操作结束后清理ThreadLocal，防止内存泄漏
                LogPointContextHolder.clear();
            }
        }
    }

    /**
     * 组合参数名和参数值
     * 将方法参数名和对应的参数值组合成键值对
     */
    private Map<String, Object> getNamedParameters(MethodSignature signature, Object[] args) {
        Map<String, Object> params = new HashMap<>();
        String[] parameterNames = signature.getParameterNames();
        if (parameterNames != null && args != null && parameterNames.length == args.length) {
            for (int i = 0; i < parameterNames.length; i++) {
                params.put(parameterNames[i], args[i]); // 参数名: 参数值
            }
        }
        return params;
    }

    /**
     * 获取方法参数类型名称列表
     * 返回方法所有参数类型的完整类名
     */
    private List<String> getParameterTypeNames(MethodSignature signature) {
        List<String> types = new ArrayList<>();
        Class<?>[] parameterTypes = signature.getMethod().getParameterTypes();
        if (parameterTypes != null) {
            for (Class<?> t : parameterTypes) {
                types.add(t.getName()); // 参数类型的完整类名
            }
        }
        return types;
    }

    /**
     * 安全地获取当前用户信息
     */
    private String getCurrentUserSafely() {
        LoginUserInfoVO current = UserContext.getCurrentUser();
        if (current != null && current.getUsername() != null && !current.getUsername().isEmpty()) {
            return current.getUsername();
        }
        return "anonymous";
    }

    /**
     * 安全地获取客户端IP地址
     */
    private String getClientIPSafely() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    if (ip.contains(",")) {
                        ip = ip.split(",")[0];
                    }
                } else {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return "unknown";
    }
}
