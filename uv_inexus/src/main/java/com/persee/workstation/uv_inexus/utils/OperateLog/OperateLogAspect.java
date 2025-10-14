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

import java.lang.reflect.Method;
import java.util.Date;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.persee.workstation.uv_inexus.dao.OperateLog.OperateLogMapper;
import com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

/**
 * 操作日志AOP切面：拦截@OperateLog注解的方法，自动记录日志
 */
@Aspect
@Component
public class OperateLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperateLogAspect.class);

    // 注入日志DAO（操作数据库）
    @Autowired
    private OperateLogMapper operateLogMapper;

    // 1. 定义切点：拦截所有标注@OperateLog注解的方法
    @Pointcut("@annotation(com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog)")
    public void operateLogPointCut() {}

    // 2. 环绕通知：方法执行前后记录日志（支持异常捕获）
    @Around("operateLogPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperateLogPO logPO = new OperateLogPO();
        Object result = null;

        try {
            // -------------------------- 1. 初始化日志基础信息（仅初始化不涉及用户） --------------------------
            logPO.setCreateTime(new Date());
            logPO.setIp(LogUtils.getRealIp());
            logPO.setStatus(1);

            // -------------------------- 2. 获取注解配置（模块、描述） --------------------------
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            OperateLog operateLog = method.getAnnotation(OperateLog.class);
            logPO.setModule(operateLog.module());
            logPO.setDescription(operateLog.description());

            // -------------------------- 3. 记录方法信息 --------------------------
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = signature.getName();
            logPO.setMethod(className + "." + methodName);

            // -------------------------- 4. 记录请求参数 --------------------------
            if (operateLog.recordParams()) {
                String paramsJson = JSON.toJSONString(joinPoint.getArgs(), SerializerFeature.DisableCircularReferenceDetect);
                logPO.setParams(paramsJson);
            }

            // -------------------------- 关键修改：先执行目标方法（Service登录），再获取用户信息 --------------------------
            result = joinPoint.proceed(); // 执行Service.login()，此时Service会存储UserContext

            // -------------------------- 5. 现在再获取用户信息（Service已存储，能拿到值） --------------------------
            LoginUserInfoVO currentUser = UserContext.getCurrentUser();
            if (currentUser != null) {
                // logPO.setUserId(currentUser.getId());
                logPO.setUsername(currentUser.getUsername());
                log.info("日志切面获取到用户信息：id={}, username={}", currentUser.getId(), currentUser.getUsername());
            } else {
                // logPO.setUserId("anonymous");
                logPO.setUsername("anonymous");
                log.warn("日志切面未获取到用户信息，记录为匿名");
            }

            // -------------------------- 6. 记录返回结果 --------------------------
            if (operateLog.recordResult()) {
                String resultJson = JSON.toJSONString(result, SerializerFeature.DisableCircularReferenceDetect);
                logPO.setResult(resultJson);
            }

            return result;

        } catch (Exception e) {
            logPO.setStatus(0);
            logPO.setErrorMsg(e.getMessage());
            log.error("操作日志记录异常：method={}", logPO.getMethod(), e);
            throw e;

        } finally {
            logPO.setOperateTime(System.currentTimeMillis() - startTime);
            asyncSaveOperateLog(logPO);
        }
    }

    // 3. 异步保存日志（减少主业务耗时）
    @Async
    public void asyncSaveOperateLog(OperateLogPO logPO) {
        try {
            operateLogMapper.insert(logPO);  // 插入数据库
        } catch (Exception e) {
            log.error("异步保存操作日志失败：logPO={}", JSON.toJSONString(logPO), e);
        }
    }
}