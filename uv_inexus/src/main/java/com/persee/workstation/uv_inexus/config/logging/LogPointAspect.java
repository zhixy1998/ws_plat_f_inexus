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
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("ENCRYPTED_AUDIT_LOGGER");

    @Pointcut("@annotation(com.persee.workstation.uv_inexus.config.LogPoint)")
    public void logPointCut() {
    }

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable ex = null;

        try {
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

                LinkedHashMap<String, Object> logData = new LinkedHashMap<>();
                logData.put("module", logPoint.module());
                logData.put("client", getClientIPSafely());
                logData.put("timestamp", System.currentTimeMillis());
                logData.put("paramsTypes", getParameterTypeNames(signature));
                logData.put("operation", logPoint.value());
                logData.put("className", joinPoint.getTarget().getClass().getName());
                logData.put("methodName", method.getName());
                logData.put("user", getCurrentUserSafely());

                if (logPoint.recordDuration()) {
                    logData.put("durationMs", endTime - startTime);
                }

                if (logPoint.recordParams()) {
                    logData.put("params", getNamedParameters(signature, joinPoint.getArgs()));
                }

                if (logPoint.recordResult()) {
                    logData.put("result", result);
                }

                // 从ContextHolder获取额外的数据并合并
                Map<String, Object> customData = LogPointContextHolder.getContext();
                if (customData != null && !customData.isEmpty()) {
                    logData.put("customData", customData);
                }

                // 修改：记录更详细的异常信息
                if (ex != null) {
                    Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("class", ex.getClass().getName());
                    errorMap.put("message", ex.getMessage());
                    logData.put("error", errorMap);
                }

                String jsonLog = JSON.toJSONString(logData);
                String encryptedLog = SymmetricEncryptor.encrypt(jsonLog);

                AUDIT_LOGGER.info(encryptedLog);

            } catch (Exception e) {
                LoggerFactory.getLogger(LogPointAspect.class).error("Failed to record audit log", e);
            } finally {
                // 确保在操作结束后清理ThreadLocal
                LogPointContextHolder.clear();
            }
        }
    }

    /**
     * 组合参数名和参数值
     */
    private Map<String, Object> getNamedParameters(MethodSignature signature, Object[] args) {
        Map<String, Object> params = new HashMap<>();
        String[] parameterNames = signature.getParameterNames();
        if (parameterNames != null && args != null && parameterNames.length == args.length) {
            for (int i = 0; i < parameterNames.length; i++) {
                params.put(parameterNames[i], args[i]);
            }
        }
        return params;
    }

    private List<String> getParameterTypeNames(MethodSignature signature) {
        List<String> types = new ArrayList<>();
        Class<?>[] parameterTypes = signature.getMethod().getParameterTypes();
        if (parameterTypes != null) {
            for (Class<?> t : parameterTypes) {
                types.add(t.getName());
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
