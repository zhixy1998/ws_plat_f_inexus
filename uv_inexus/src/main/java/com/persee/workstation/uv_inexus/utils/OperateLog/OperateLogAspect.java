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
import com.persee.workstation.uv_inexus.util.UserPermissionUtil;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

/**
 * 操作日志AOP切面：拦截@OperateLog注解的方法，自动记录日志
 * 核心增强：
 * 1. 仅有权限通过时才记录日志（复用ButtonPermissionAspect的权限校验逻辑）
 * 2. 读取operateType，动态匹配permissionMapping，生成对应日志描述
 * 3. 新增operateTypeExt纯占位字段（带中文映射），支持{operateTypeExt}占位符
 */
@Aspect
@Component
public class OperateLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperateLogAspect.class);

    // 注入日志DAO（操作数据库）
    @Autowired
    private OperateLogMapper operateLogMapper;

    // 注入项目已有的权限工具类（和ButtonPermissionAspect复用）
    @Autowired
    private UserPermissionUtil userPermissionUtil;

    // 1. 定义切点：拦截所有标注@OperateLog注解的方法
    @Pointcut("@annotation(com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog)")
    public void operateLogPointCut() {}

    // 2. 环绕通知：方法执行前后记录日志（仅有权限通过时记录）
    @Around("operateLogPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperateLogPO logPO = null; // 初始化为null，无权限时不创建
        Object result = null;
        boolean hasPermission = false; // 权限标记

        try {
            // -------------------------- 1. 获取注解配置 --------------------------
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 2.1 读取OperateLog注解（基础配置）
            OperateLog operateLog = method.getAnnotation(OperateLog.class);

            // 2.2 读取ButtonPermission注解（核心：解析permissionMapping和operateType）
            ButtonPermission buttonPermission = method.getAnnotation(ButtonPermission.class);
            Map<String, String> operatePermissionMap = new HashMap<>();
            String currentOperateType = "";
            String actualPermission = null; // 最终要校验的权限标识

            if (buttonPermission != null) {
                // 解析permissionMapping（和ButtonPermissionAspect逻辑完全一致）
                String permissionMapping = buttonPermission.permissionMapping();
                String basePermission = buttonPermission.value();

                // 读取DTO中的operateType字段
                currentOperateType = getFieldValueFromArgs(joinPoint.getArgs(), "operateType");
                log.info("解析到当前操作类型：{}", currentOperateType);

                // ========== 核心：复用ButtonPermissionAspect的权限标识解析逻辑 ==========
                // 场景1：新写法（配置了permissionMapping）
                if (!permissionMapping.isEmpty()) {
                    if (currentOperateType == null || currentOperateType.isEmpty()) {
                        log.warn("操作类型（operateType）不能为空，权限校验失败");
                    } else {
                        // 解析operateType对应的权限标识
                        actualPermission = getPermissionFromMapping(buttonPermission, currentOperateType);
                        if (actualPermission == null || actualPermission.isEmpty()) {
                            log.warn("操作类型[" + currentOperateType + "]未配置对应的权限标识");
                        }
                    }
                }
                // 场景2：老写法（仅配置了value）
                else if (!basePermission.isEmpty()) {
                    actualPermission = basePermission; // 直接用value作为权限标识
                }

                // ========== 核心：复用项目已有的权限校验逻辑 ==========
                if (actualPermission != null && !actualPermission.isEmpty()) {
                    LoginUserInfoVO currentUser = UserContext.getCurrentUser();
                    if (currentUser != null) {
                        // 调用和ButtonPermissionAspect完全一样的权限校验逻辑
                        hasPermission = hasButtonPermission(currentUser, actualPermission);
                    } else {
                        log.warn("未获取到当前用户信息，权限校验失败");
                    }
                }
                log.info("权限校验结果：operateType={}, permission={}, hasPermission={}",
                        currentOperateType, actualPermission, hasPermission);

                // -------------------------- 仅有权限时初始化日志 --------------------------
                if (hasPermission) {
                    logPO = new OperateLogPO();
                    // 初始化日志基础信息
                    logPO.setCreateTime(new Date());
                    logPO.setIp(LogUtils.getRealIp());
                    logPO.setStatus(1);
                    logPO.setModule(operateLog.module());
                    logPO.setLogType(operateLog.logType());

                    // 2.3 动态生成日志描述和权限标识
                    String dynamicDesc = operateLog.description();
                    String dynamicPermission = "";
                    if (StringUtils.hasText(currentOperateType)) {
                        Map<String, String> operateDescMap = new HashMap<>();
                        operateDescMap.put("add", "设置");
                        operateDescMap.put("edit", "修改");
                        operateDescMap.put("delete", "删除");
                        operateDescMap.put("lumne", "光度测量-测量");
                        operateDescMap.put("quantity", "定量测量-测量");
                        operateDescMap.put("spectralScan", "光谱扫描-扫描");
                        operateDescMap.put("timeScan", "时间扫描-扫描");
                        operateDescMap.put("spectralScanBaseline", "光谱扫描-基线");
                        operateDescMap.put("timeScanBaseline", "时间扫描-基线");
                        operateDescMap.put("lumnePreviewReport", "光度测量-预览报告");
                        operateDescMap.put("quantityPreviewReport", "定量测量-预览报告");
                        operateDescMap.put("spectralPreviewReport", "光谱扫描-预览报告");
                        operateDescMap.put("timeScanPreviewReport", "时间扫描-预览报告");
                        operateDescMap.put("wmlumneGenerationReport", "光度测量-生成报告");
                        operateDescMap.put("wmquantityGenerationReport", "定量测量-生成报告");
                        operateDescMap.put("wmspectralGenerationReport", "光谱扫描-生成报告");
                        operateDescMap.put("wmtimeScanGenerationReport", "时间扫描-生成报告");
                        operateDescMap.put("instrumentCalibrationWavelengthCorrection", "波长校正");
                        operateDescMap.put("instrumentCalibrationDarkCurrentCorrection", "暗电流校正");

                        // 替换描述中的operateType占位符
                        String operateName = operateDescMap.getOrDefault(currentOperateType, currentOperateType);
                        dynamicDesc = dynamicDesc.replace("{operateType}", operateName);

                        // ========== 新增：operateTypeExt纯占位映射 ==========
                        String currentOperateTypeExt = getFieldValueFromArgs(joinPoint.getArgs(), "operateTypeExt");
                        log.info("解析到纯占位操作类型：{}", currentOperateTypeExt);

                        Map<String, String> operateExtDescMap = new HashMap<>();
                        operateExtDescMap.put("classicMode", "经典模式");
                        operateExtDescMap.put("guideMode", "向导模式");

                        String operateExtName = StringUtils.hasText(currentOperateTypeExt)
                                ? operateExtDescMap.getOrDefault(currentOperateTypeExt, currentOperateTypeExt)
                                : "无";
                        dynamicDesc = dynamicDesc.replace("{operateTypeExt}", operateExtName);

                        // 获取对应的权限标识
                        dynamicPermission = actualPermission; // 使用解析后的真实权限标识
                    }

                    logPO.setDescription(dynamicDesc);
                    logPO.setPermission(dynamicPermission);

                    // 记录方法信息
                    String className = joinPoint.getTarget().getClass().getName();
                    String methodName = signature.getName();
                    logPO.setMethod(className + "." + methodName);

                    // 优化：记录请求参数（解决超长问题）
                    if (operateLog.recordParams()) {
                        logPO.setParams(buildParamsJson(joinPoint.getArgs()));
                    }
                }
            }

            // -------------------------- 5. 执行目标方法 --------------------------
            result = joinPoint.proceed();

            // -------------------------- 6. 仅有权限时补充日志信息 --------------------------
            if (hasPermission && logPO != null) {
                // 获取用户信息
                LoginUserInfoVO currentUser = UserContext.getCurrentUser();
                if (currentUser != null) {
                    logPO.setUserId(currentUser.getId());
                    logPO.setUsername(currentUser.getUsername());
                    log.info("日志切面获取到用户信息：id={}, username={}", currentUser.getId(), currentUser.getUsername());
                } else {
                    logPO.setUsername("anonymous");
                    log.warn("日志切面未获取到用户信息，记录为匿名");
                }

                // 记录返回结果
                if (operateLog.recordParams()) {
                    String resultJson = JSON.toJSONString(result, SerializerFeature.DisableCircularReferenceDetect);
                    logPO.setResult(resultJson);
                }
            }

            return result;

        } catch (Exception e) {
            // 仅有权限时记录异常信息
            if (hasPermission && logPO != null) {
                logPO.setStatus(0);
                logPO.setErrorMsg(e.getMessage());
                log.error("操作日志记录异常：method={}", logPO.getMethod(), e);
            } else {
                log.error("无权限，日志记录异常：{}", e.getMessage(), e);
            }
            throw e;

        } finally {
            // -------------------------- 仅有权限时保存日志 --------------------------
            if (hasPermission && logPO != null) {
                logPO.setOperateTime(System.currentTimeMillis() - startTime);
                asyncSaveOperateLog(logPO);
                log.info("权限通过，已保存操作日志");
            } else {
                log.info("权限校验失败/无权限配置，跳过日志记录");
            }
        }
    }

    // ========== 复用ButtonPermissionAspect的核心辅助方法 ==========
    /**
     * 解析permissionMapping，获取operateType对应的权限标识
     */
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
     * 复用ButtonPermissionAspect的权限校验逻辑
     */
    private boolean hasButtonPermission(LoginUserInfoVO user, String permission) {
        try {
            List<String> userPermissions = getUserPermissions(user);
            return userPermissions.contains(permission);
        } catch (Exception e) {
            log.error("检查用户权限时发生异常", e);
            return false;
        }
    }

    /**
     * 复用项目已有的权限获取方法
     */
    private List<String> getUserPermissions(LoginUserInfoVO user) {
        return userPermissionUtil.getCurrentUserPermissionCodes();
    }

    /**
     * 通用字段读取方法（支持operateType/operateTypeExt，解耦）
     * @param args 接口入参
     * @param fieldName 要读取的字段名
     * @return 字段值（空则返回""）
     */
    private String getFieldValueFromArgs(Object[] args, String fieldName) {
        if (args == null || args.length == 0 || !StringUtils.hasText(fieldName)) {
            return "";
        }

        // 遍历所有参数，找包含目标字段的对象
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            try {
                // 先查当前类，再查父类
                Field targetField = null;
                try {
                    targetField = arg.getClass().getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    Class<?> superClass = arg.getClass().getSuperclass();
                    if (superClass != null) {
                        targetField = superClass.getDeclaredField(fieldName);
                    }
                }

                if (targetField != null) {
                    targetField.setAccessible(true);
                    Object fieldObj = targetField.get(arg);
                    return fieldObj != null ? fieldObj.toString().trim() : "";
                }
            } catch (Exception e) {
                log.warn("读取字段{}异常，跳过该参数", fieldName, e);
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