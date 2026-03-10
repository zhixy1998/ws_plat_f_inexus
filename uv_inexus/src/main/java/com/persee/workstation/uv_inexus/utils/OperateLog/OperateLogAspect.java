/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.utils.OperateLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;
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
import com.persee.workstation.uv_inexus.config.ButtonPermission;
import com.persee.workstation.uv_inexus.dao.OperateLog.OperateLogMapper;
import com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

/**
 * 操作日志AOP切面：拦截@OperateLog注解的方法，自动记录日志
 * 核心增强：读取operateType，动态匹配permissionMapping，生成对应日志描述
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
            // -------------------------- 1. 初始化日志基础信息 --------------------------
            logPO.setCreateTime(new Date());
            logPO.setIp(LogUtils.getRealIp());
            logPO.setStatus(1);

            // -------------------------- 2. 获取注解配置 --------------------------
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 2.1 读取OperateLog注解（基础配置）
            OperateLog operateLog = method.getAnnotation(OperateLog.class);
            logPO.setModule(operateLog.module());
            logPO.setLogType(operateLog.logType());

            // 2.2 读取ButtonPermission注解（核心：解析permissionMapping和operateType）
            ButtonPermission buttonPermission = method.getAnnotation(ButtonPermission.class);
            // 存储 operateType -> 权限标识 的映射
            Map<String, String> operatePermissionMap = new HashMap<>();
            // 当前操作类型（add/edit/delete）
            String currentOperateType = "";

            if (buttonPermission != null) {
                // 解析permissionMapping（如：add=quantitySetSample,edit=quantityModifySampleInformation）
                String permissionMapping = buttonPermission.permissionMapping();
                if (StringUtils.hasText(permissionMapping)) {
                    String[] mappings = permissionMapping.split(",");
                    for (String mapping : mappings) {
                        if (mapping.contains("=")) {
                            String[] kv = mapping.split("=");
                            String operateType = kv[0].trim();
                            String permission = kv[1].trim();
                            operatePermissionMap.put(operateType, permission);
                        }
                    }
                }

                // 读取DTO中的operateType字段（核心：区分新增/修改）
                currentOperateType = getOperateTypeFromArgs(joinPoint.getArgs());
                log.info("解析到当前操作类型：{}", currentOperateType);

                // 2.3 动态生成日志描述和权限标识
                String dynamicDesc = operateLog.description();
                String dynamicPermission = "";
                // 根据operateType匹配对应的描述和权限
                if (StringUtils.hasText(currentOperateType)) {
                    // 自定义操作类型对应的描述（可根据业务扩展）
                    Map<String, String> operateDescMap = new HashMap<>();
                    operateDescMap.put("add", "设置");
                    operateDescMap.put("edit", "修改");
                    operateDescMap.put("delete", "删除");
                    operateDescMap.put("lumne", "光度测量");
                    operateDescMap.put("quantity", "定量测量");
                    operateDescMap.put("spectralScan", "光谱测量");
                    operateDescMap.put("timeScan", "时间测量");

                    // 替换描述中的占位符，或拼接操作类型
                    String operateName = operateDescMap.getOrDefault(currentOperateType, currentOperateType);
                    dynamicDesc = operateLog.description().replace("{operateType}", operateName);

                    // 获取对应的权限标识
                    dynamicPermission = operatePermissionMap.getOrDefault(currentOperateType, "");
                }

                // 设置到日志中
                logPO.setDescription(dynamicDesc);
                logPO.setPermission(dynamicPermission);
            } else {
                // 没有ButtonPermission注解时，使用原描述
                logPO.setDescription(operateLog.description());
            }

            // -------------------------- 3. 记录方法信息 --------------------------
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = signature.getName();
            logPO.setMethod(className + "." + methodName);

            // -------------------------- 4. 优化：记录请求参数（解决超长问题） --------------------------
            if (operateLog.recordParams()) {
                String paramsJson = buildParamsJson(joinPoint.getArgs());
                logPO.setParams(paramsJson);
            }

            // -------------------------- 5. 执行目标方法 --------------------------
            result = joinPoint.proceed();

            // -------------------------- 6. 获取用户信息 --------------------------
            LoginUserInfoVO currentUser = UserContext.getCurrentUser();
            if (currentUser != null) {
                logPO.setUserId(currentUser.getId());
                logPO.setUsername(currentUser.getUsername());
                log.info("日志切面获取到用户信息：id={}, username={}", currentUser.getId(), currentUser.getUsername());
            } else {
                logPO.setUsername("anonymous");
                log.warn("日志切面未获取到用户信息，记录为匿名");
            }

            // -------------------------- 7. 记录返回结果 --------------------------
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

    /**
     * 从请求参数中读取operateType字段
     * @param args 接口入参
     * @return operateType值（add/edit/delete）
     */
    private String getOperateTypeFromArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        // 遍历所有参数，找包含operateType字段的对象
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            try {
                // 反射读取operateType字段
                Field operateTypeField = arg.getClass().getDeclaredField("operateType");
                operateTypeField.setAccessible(true);
                Object operateTypeObj = operateTypeField.get(arg);
                if (operateTypeObj != null && StringUtils.hasText(operateTypeObj.toString())) {
                    return operateTypeObj.toString().trim();
                }
            } catch (NoSuchFieldException e) {
                // 该参数没有operateType字段，继续遍历下一个
                continue;
            } catch (Exception e) {
                log.warn("读取operateType字段异常", e);
                continue;
            }
        }

        return "";
    }

    /**
     * 构建参数JSON（精简超长参数，避免数据库字段溢出）
     */
    private String buildParamsJson(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        try {
            // 针对不同参数类型做精简处理
            StringBuilder paramsBuilder = new StringBuilder();
            for (Object arg : args) {
                if (arg == null) {
                    paramsBuilder.append("null,");
                    continue;
                }

                // 1. List类型：记录数量+前10个元素
                if (arg instanceof List) {
                    List<?> list = (List<?>) arg;
                    int size = list.size();
                    String listStr = size > 10 ? list.subList(0, 10).toString() + "..." : list.toString();
                    paramsBuilder.append(String.format("List(size=%d):%s,", size, listStr));
                }
                // 2. Map类型：记录key数量+前5个键值对
                else if (arg instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) arg;
                    int size = map.size();
                    paramsBuilder.append(String.format("Map(size=%d):%s,", size, map.toString().length() > 200 ? map.toString().substring(0, 200) + "..." : map.toString()));
                }
                // 3. 普通DTO/POJO：序列化后截断
                else {
                    String json = JSON.toJSONString(arg, SerializerFeature.DisableCircularReferenceDetect);
                    paramsBuilder.append(json.length() > 500 ? json.substring(0, 500) + "...," : json + ",");
                }
            }

            String params = paramsBuilder.toString();
            return params.endsWith(",") ? params.substring(0, params.length() - 1) : params;
        } catch (Exception e) {
            log.warn("构建参数JSON失败，使用默认序列化", e);
            return JSON.toJSONString(args, SerializerFeature.DisableCircularReferenceDetect);
        }
    }

    // 3. 异步保存日志（增加容错：字段超长自动截断）
    @Async
    public void asyncSaveOperateLog(OperateLogPO logPO) {
        try {
            operateLogMapper.insert(logPO);
        } catch (Exception e) {
            // 捕获字段超长异常，自动截断后重试
            if (e.getMessage() != null && e.getMessage().contains("Data too long for column")) {
                log.warn("日志插入失败（字段超长），自动截断后重试：{}", e.getMessage());

                // 截断超长字段
                if (logPO.getPermission() != null && logPO.getPermission().length() > 100) {
                    logPO.setPermission(logPO.getPermission().substring(0, 100));
                }
                if (logPO.getParams() != null && logPO.getParams().length() > 1000) {
                    logPO.setParams(logPO.getParams().substring(0, 1000) + "...");
                }
                if (logPO.getErrorMsg() != null && logPO.getErrorMsg().length() > 500) {
                    logPO.setErrorMsg(logPO.getErrorMsg().substring(0, 500) + "...");
                }

                // 重试插入
                try {
                    operateLogMapper.insert(logPO);
                } catch (Exception retryE) {
                    log.error("日志截断后仍插入失败：{}", JSON.toJSONString(logPO), retryE);
                }
            } else {
                log.error("异步保存操作日志失败：logPO={}", JSON.toJSONString(logPO), e);
            }
        }
    }
}