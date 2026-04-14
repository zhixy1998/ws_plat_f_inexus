package com.persee.workstation.uv_inexus.utils.OperateLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.persee.workstation.uv_inexus.config.ButtonPermission;
import com.persee.workstation.uv_inexus.dao.OperateLog.OperateLogMapper;
import com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO;
import com.persee.workstation.uv_inexus.util.UserPermissionUtil;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

@Aspect
@Component
public class OperateLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperateLogAspect.class);

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Autowired
    private UserPermissionUtil userPermissionUtil;

    @Pointcut("@annotation(com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog)")
    public void operateLogPointCut() {}

    @Around("operateLogPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperateLogPO logPO = null;
        Object result = null;
        boolean hasPermission = false;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperateLog operateLog = method.getAnnotation(OperateLog.class);

        // 🔥 【关键】只要 ignore=true，直接跳过所有逻辑，不生成任何日志！
        if (operateLog != null && operateLog.ignore()) {
            return joinPoint.proceed();
        }

        try {
            // 👇 下面不再重复定义 signature/method/operateLog
            ButtonPermission buttonPermission = method.getAnnotation(ButtonPermission.class);
            Map<String, String> operatePermissionMap = new HashMap<>();
            String currentOperateType = "";
            String actualPermission = null;

            if (buttonPermission != null) {
                String permissionMapping = buttonPermission.permissionMapping();
                String basePermission = buttonPermission.value();

                currentOperateType = getFieldValueFromArgs(joinPoint.getArgs(), "operateType");
                log.info("解析到当前操作类型：{}", currentOperateType);

                if (!permissionMapping.isEmpty()) {
                    if (currentOperateType == null || currentOperateType.isEmpty()) {
                        log.warn("操作类型（operateType）不能为空，权限校验失败");
                    } else {
                        actualPermission = getPermissionFromMapping(buttonPermission, currentOperateType);
                        if (actualPermission == null || actualPermission.isEmpty()) {
                            log.warn("操作类型[" + currentOperateType + "]未配置对应的权限标识");
                        }
                    }
                } else if (!basePermission.isEmpty()) {
                    actualPermission = basePermission;
                }

                if (actualPermission != null && !actualPermission.isEmpty()) {
                    LoginUserInfoVO currentUser = UserContext.getCurrentUser();
                    if (currentUser != null) {
                        hasPermission = hasButtonPermission(currentUser, actualPermission);
                    } else {
                        log.warn("未获取到当前用户信息，权限校验失败");
                    }
                }
                log.info("权限校验结果：operateType={}, permission={}, hasPermission={}",
                        currentOperateType, actualPermission, hasPermission);

                if (hasPermission) {
                    logPO = new OperateLogPO();
                    logPO.setCreateTime(new Date());
                    logPO.setIp(LogUtils.getRealIp());
                    logPO.setStatus(1);
                    logPO.setModule(operateLog.module());
                    logPO.setLogType(operateLog.logType());

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

                        String operateName = operateDescMap.getOrDefault(currentOperateType, currentOperateType);
                        dynamicDesc = dynamicDesc.replace("{operateType}", operateName);

                        String currentOperateTypeExt = getFieldValueFromArgs(joinPoint.getArgs(), "operateTypeExt");
                        log.info("解析到纯占位操作类型：{}", currentOperateTypeExt);

                        Map<String, String> operateExtDescMap = new HashMap<>();
                        operateExtDescMap.put("classicMode", "经典模式");
                        operateExtDescMap.put("guideMode", "向导模式");

                        String operateExtName = StringUtils.hasText(currentOperateTypeExt)
                                ? operateExtDescMap.getOrDefault(currentOperateTypeExt, currentOperateTypeExt)
                                : "无";
                        dynamicDesc = dynamicDesc.replace("{operateTypeExt}", operateExtName);

                        dynamicPermission = actualPermission;
                    }

                    logPO.setDescription(dynamicDesc);
                    logPO.setPermission(dynamicPermission);

                    String className = joinPoint.getTarget().getClass().getName();
                    String methodName = signature.getName();
                    logPO.setMethod(className + "." + methodName);

                    if (operateLog.recordParams()) {
                        logPO.setParams(buildParamsJson(joinPoint.getArgs()));
                    }
                }
            }

            result = joinPoint.proceed();

            if (hasPermission && logPO != null) {
                LoginUserInfoVO currentUser = UserContext.getCurrentUser();
                if (currentUser != null) {
                    logPO.setUserId(currentUser.getId());
                    logPO.setUsername(currentUser.getUsername());
                } else {
                    logPO.setUsername("anonymous");
                }

                if (operateLog.recordParams()) {
                    String resultJson = buildResultJson(result);
                    logPO.setResult(resultJson);
                }

                if (operateLog.compare()) {
                    Object oldData = LogContext.getOldData();
                    Object newData = LogContext.getNewData();

                    if (oldData != null && newData != null) {
                        logPO.setBeforeData(JSON.toJSONString(oldData));
                        logPO.setAfterData(JSON.toJSONString(newData));
                        String changeContent = LogCompareUtil.generateCompareContent(oldData, newData);
                        logPO.setChangeContent(changeContent);
                    }
                    LogContext.clear();
                }
            }

            return result;

        } catch (Exception e) {
            if (hasPermission && logPO != null) {
                logPO.setStatus(0);
                logPO.setErrorMsg(e.getMessage());
                log.error("操作日志记录异常：method={}", logPO.getMethod(), e);
            } else {
                log.error("无权限，日志记录异常：{}", e.getMessage(), e);
            }
            throw e;

        } finally {
            if (hasPermission && logPO != null) {
                logPO.setOperateTime(System.currentTimeMillis() - startTime);
                asyncSaveOperateLog(logPO);
                log.info("权限通过，已保存操作日志");
            } else {
                log.info("权限校验失败/无权限配置，跳过日志记录");
            }
        }
    }

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

    private String getFieldValueFromArgs(Object[] args, String fieldName) {
        if (args == null || args.length == 0 || !StringUtils.hasText(fieldName)) {
            return "";
        }

        for (Object arg : args) {
            if (arg == null) continue;

            try {
                Field targetField = null;
                // 先找本类
                try {
                    targetField = arg.getClass().getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    // 找不到就找父类，找不到也不抛错
                    try {
                        Class<?> superClass = arg.getClass().getSuperclass();
                        if (superClass != null) {
                            targetField = superClass.getDeclaredField(fieldName);
                        }
                    } catch (NoSuchFieldException ignored) {
                        // 父类也找不到 → 直接跳过，不抛异常
                    }
                }

                if (targetField != null) {
                    targetField.setAccessible(true);
                    Object fieldObj = targetField.get(arg);
                    return fieldObj != null ? fieldObj.toString().trim() : "";
                }
            } catch (Exception e) {
                // 只在真正异常时打warn，找不到字段不打
                log.debug("读取字段{}异常，跳过该参数", fieldName);
                continue;
            }
        }
        return "";
    }

    private String buildParamsJson(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        try {
            Object[] safeArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg != null && arg.getClass().getName().contains("MultipartFile")) {
                    safeArgs[i] = "[MultipartFile: " + arg.toString() + "]";
                } else {
                    safeArgs[i] = arg;
                }
            }

            StringBuilder paramsBuilder = new StringBuilder();
            for (Object arg : safeArgs) {
                if (arg == null) {
                    paramsBuilder.append("null,");
                    continue;
                }

                if (arg instanceof List) {
                    List<?> list = (List<?>) arg;
                    int size = list.size();
                    String listStr = size > 10 ? list.subList(0, 10).toString() + "..." : list.toString();
                    paramsBuilder.append(String.format("List(size=%d):%s,", size, listStr));
                } else if (arg instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) arg;
                    int size = map.size();
                    String mapStr = map.toString().length() > 200 ? map.toString().substring(0, 200) + "..." : map.toString();
                    paramsBuilder.append(String.format("Map(size=%d):%s,", size, mapStr));
                } else {
                    String json = JSON.toJSONString(arg, SerializerFeature.DisableCircularReferenceDetect);
                    paramsBuilder.append(json.length() > 500 ? json.substring(0, 500) + "...,": json + ",");
                }
            }

            String params = paramsBuilder.toString();
            return params.endsWith(",") ? params.substring(0, params.length() - 1) : params;
        } catch (Exception e) {
            log.warn("构建参数JSON失败，使用默认序列化", e);
            return JSON.toJSONString(args, SerializerFeature.DisableCircularReferenceDetect);
        }
    }

    private String buildResultJson(Object result) {
        if (result == null) {
            return "null";
        }

        try {
            if (result.getClass().getName().contains("InputStreamResource") ||
                    result.getClass().getName().contains("FileInputStream") ||
                    result.getClass().getName().contains("ByteArrayInputStream")) {
                return "[StreamResource: 二进制流数据]";
            }

            if (result.getClass().getName().contains("ResponseEntity")) {
                return "[ResponseEntity: HTTP响应对象]";
            }

            String json = JSON.toJSONString(result, SerializerFeature.DisableCircularReferenceDetect);
            return json.length() > 500 ? json.substring(0, 500) + "..." : json;
        } catch (Exception e) {
            log.warn("构建返回结果JSON失败，使用默认序列化", e);
            return "[Result: 序列化失败]";
        }
    }

    @Async
    public void asyncSaveOperateLog(OperateLogPO logPO) {
        try {
            operateLogMapper.insert(logPO);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Data too long for column")) {
                log.warn("日志插入失败（字段超长），自动截断后重试：{}", e.getMessage());

                if (logPO.getPermission() != null && logPO.getPermission().length() > 100) {
                    logPO.setPermission(logPO.getPermission().substring(0, 100));
                }
                if (logPO.getParams() != null && logPO.getParams().length() > 1000) {
                    logPO.setParams(logPO.getParams().substring(0, 1000) + "...");
                }
                if (logPO.getErrorMsg() != null && logPO.getErrorMsg().length() > 500) {
                    logPO.setErrorMsg(logPO.getErrorMsg().substring(0, 500) + "...");
                }

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