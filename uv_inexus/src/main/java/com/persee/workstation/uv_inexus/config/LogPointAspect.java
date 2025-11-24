package com.persee.workstation.uv_inexus.config;

import com.alibaba.fastjson.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

@Aspect
@Component
public class LogPointAspect {
    // 埋点的日志记录器
    private static final Logger MdLogger = LoggerFactory.getLogger(LogPointAspect.class);

    @Around("@annotation(logPointAnnotation)")
    public Object handleLogPoint(ProceedingJoinPoint joinPoint, LogPoint logPointAnnotation) throws Throwable {
        // 获取方法签名信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String fullMethodName = className + "." + methodName;
        
        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = signature.getParameterNames();
        
        // 获取当前用户信息和客户端IP（安全方式）
        String currentUser = getCurrentUserSafely();
        String clientIP = getClientIPSafely();
        
        // 使用MDC添加上下文信息
        MDC.put("operation", logPointAnnotation.value());
        MDC.put("module", logPointAnnotation.module());
        MDC.put("method", fullMethodName);
        
        // 根据注解配置决定是否记录用户信息和客户端IP
        if (logPointAnnotation.recordUser()) {
            MDC.put("user", currentUser);
        }
        if (logPointAnnotation.recordClientIP()) {
            MDC.put("clientIP", clientIP);
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;
        
        // 记录方法开始执行
        if (logPointAnnotation.recordParams()) {
            // 记录参数信息
            StringBuilder paramsInfo = new StringBuilder();
            if (parameterNames != null && args != null && parameterNames.length == args.length) {
                for (int i = 0; i < parameterNames.length; i++) {
                    paramsInfo.append(parameterNames[i])
                              .append("=")
                              .append(serializeObject(args[i]))
                              .append(", ");
                }
            }
            
            MdLogger.info("方法开始执行，参数: {}", paramsInfo.length() > 2 ? paramsInfo.substring(0, paramsInfo.length() - 2) : "");
        } else {
            MdLogger.info("方法开始执行");
        }

        try {
            // 执行目标方法
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            try {
                long duration = System.currentTimeMillis() - startTime;

                // 记录方法执行完成（在MDC清理前完成所有日志记录）
                if (exception != null) {
                    MdLogger.error("方法执行异常，耗时: {}ms, 异常: {}", duration, exception.getMessage());
                } else {
                    if (logPointAnnotation.recordResult()) {
                        MdLogger.info("方法执行完成，耗时: {}ms, 返回结果: {}", duration, serializeObject(result));
                    } else {
                        MdLogger.info("方法执行完成，耗时: {}ms", duration);
                    }
                }
            } finally {
                // 确保MDC被清理
                MDC.clear();
            }
        }

    }
    
    /**
     * 安全地获取当前用户信息（避免依赖特定安全框架）
     * @return 当前用户名或"anonymous"
     */
    private String getCurrentUserSafely() {
        // 旧逻辑（已弃用）：通过反射读取Spring Security或从Request/Session取用户
        // try {
        //     Class<?> securityContextClass = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
        //     Object context = securityContextClass.getMethod("getContext").invoke(null);
        //     if (context != null) {
        //         Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
        //         if (authentication != null) {
        //             Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
        //             if (principal != null) {
        //                 return authentication.getClass().getMethod("getName").invoke(authentication).toString();
        //             }
        //         }
        //     }
        // } catch (Exception e) {
        // }
        // try {
        //     ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        //     if (attributes != null) {
        //         HttpServletRequest request = attributes.getRequest();
        //         Object userAttr = request.getAttribute("user");
        //         if (userAttr != null) {
        //             return userAttr.toString();
        //         }
        //         Object userSession = request.getSession().getAttribute("user");
        //         if (userSession != null) {
        //             return userSession.toString();
        //         }
        //     }
        // } catch (Exception e) {
        // }

        // 新逻辑：统一从 UserContext 读取，由全局拦截器在请求入口写入
        LoginUserInfoVO current = UserContext.getCurrentUser();
        if (current != null && current.getUsername() != null && !current.getUsername().isEmpty()) {
            return current.getUsername();
        }
        return "anonymous";
    }
    
    /**
     * 安全地获取客户端IP地址
     * @return 客户端IP地址
     */
    private String getClientIPSafely() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    // 多个IP时取第一个
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
            // 没有获取到的话就使用默认值存储
        }
        return "unknown";
    }
    
    /**
     * 序列化对象为字符串
     * @param obj 待序列化的对象
     * @return 序列化后的字符串
     */
    private String serializeObject(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        try {
            // 对于基本类型和字符串，直接返回
            if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
                return obj.toString();
            }
            
            // 使用FastJSON序列化复杂对象
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}