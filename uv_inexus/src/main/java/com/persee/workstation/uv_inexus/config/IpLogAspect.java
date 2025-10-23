package com.persee.workstation.uv_inexus.config;

import com.persee.workstation.uv_inexus.utils.IPUtils.ipUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class IpLogAspect {

    // 切入点：匹配所有Controller中的方法
    @Before("execution(* com.persee.workstation.uv_inexus.controller..*(..))")
    public void logIp(JoinPoint joinPoint) {
        // 获取当前请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            // 使用工具类获取IP
            String ip = ipUtils.getIpAddress(request);
            // 记录日志（可根据需要调整，如存入日志系统）
//            System.out.println("访问接口: " + joinPoint.getSignature() + ", 访问IP: " + ip);
        }
    }
}