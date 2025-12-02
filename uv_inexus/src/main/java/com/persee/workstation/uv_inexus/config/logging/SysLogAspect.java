/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.config.logging;

import com.persee.workstation.uv_inexus.dao.log.SysLogsMapper;
import com.persee.workstation.uv_inexus.po.log.SysLogsPO;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FMethodParam;
import com.alibaba.fastjson.JSON;
import java.lang.reflect.Method;
import java.util.Map;
import java.time.LocalDateTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceInitDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceSoftCloseDTO;

/**
 * 系统日志切面
 * 拦截标注了 @SysLog 的方法，采集并写入 t_sys_logs：
 * - 用户信息（user_id/user_name）
 * - 动作/模块（action/module）仅用于生成 message/source
 * - 方法/任务标识（method_id/method_name、task_id/task_name）
 * - 仪器信息（instrument_id/instrument_name）
 * - 类型（type：基于 Result.code 判定成功/失败）
 * 所有字段允许为空；发生异常也会在 finally 中尝试记录日志。
 */
@Aspect
@Component
public class SysLogAspect {

    /** 控制台日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(SysLogAspect.class);

    /** 日志表 Mapper（路由 log 数据源） */
    @Autowired
    private SysLogsMapper sysLogsMapper;

    

    /** 切点：拦截被 @SysLog 标注的方法 */
    @Pointcut("@annotation(com.persee.workstation.uv_inexus.config.logging.SysLog)")
    public void sysLogPointCut() {}

    /**
     * 环绕通知：执行业务方法，并在 finally 中采集信息与入库
     */
    @Around("sysLogPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
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
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                SysLog sysLog = signature.getMethod().getAnnotation(SysLog.class);
                String className = joinPoint.getTarget().getClass().getName();
                String methodName = signature.getName();
                String fullMethodName = className + "." + methodName;

                // 获取用户名：优先 UserContext，其次方法/请求参数，最后匿名
                LoginUserInfoVO current = UserContext.getCurrentUser();
                String userId = current != null ? current.getId() : null;
                String username = (current != null && current.getUsername() != null && !current.getUsername().isEmpty()) ? current.getUsername() : null;
                if (username == null || username.isEmpty()) {
                    try {
                        String[] paramNamesTmp = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
                        Object[] argsTmp = joinPoint.getArgs();
                        if (paramNamesTmp != null && argsTmp != null && paramNamesTmp.length == argsTmp.length) {
                            for (int i = 0; i < paramNamesTmp.length; i++) {
                                String pn = paramNamesTmp[i];
                                if (("userName".equals(pn) || "username".equals(pn) || "updateBy".equals(pn)) && argsTmp[i] != null) {
                                    username = String.valueOf(argsTmp[i]);
                                    break;
                                }
                            }
                        }
                        if (username == null || username.isEmpty()) {
                            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                            if (attrs != null) {
                                HttpServletRequest request = attrs.getRequest();
                                String u1 = request.getParameter("userName");
                                String u2 = request.getParameter("username");
                                String u3 = request.getParameter("updateBy");
                                if (u1 != null && !u1.isEmpty()) username = u1;
                                else if (u2 != null && !u2.isEmpty()) username = u2;
                                else if (u3 != null && !u3.isEmpty()) username = u3;
                            }
                        }
                    } catch (Exception ignore) {}
                }
                if (username == null || username.isEmpty()) {
                    username = "anonymous";
                }

                String action = sysLog != null ? sysLog.action() : "";
                String module = sysLog != null ? sysLog.module() : "";

                // 覆盖 action/module：若请求参数传入则优先生效
                try {
                    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attrs != null) {
                        HttpServletRequest request = attrs.getRequest();
                        String actionParam = request.getParameter("action");
                        String moduleParam = request.getParameter("module");
                        if (actionParam != null && !actionParam.isEmpty()) {
                            action = actionParam;
                        }
                        if (moduleParam != null && !moduleParam.isEmpty()) {
                            module = moduleParam;
                        }
                    }
                } catch (Exception ignore) {
                }

                // 生成 message：仅保留 action，长度截断 255
                String message = (action != null ? action : "");
                if (message.length() > 255) {
                    message = message.substring(0, 255);
                }

                // 获取主机名（Windows 优先使用 COMPUTERNAME，失败回退 HOSTNAME）
                String pcName = System.getenv("COMPUTERNAME");
                if (pcName == null || pcName.isEmpty()) {
                    pcName = System.getenv("HOSTNAME");
                }
                if (pcName == null || pcName.isEmpty()) {
                    pcName = "unknown";
                }

                // 捕获 methodId：方法参数或请求参数（允许为空）
                String capturedMethodId = null;
                try {
                    String[] paramNames = signature.getParameterNames();
                    Object[] args = joinPoint.getArgs();
                    if (paramNames != null && args != null && paramNames.length == args.length) {
                        for (int i = 0; i < paramNames.length; i++) {
                            if ("methodId".equals(paramNames[i]) && args[i] != null) {
                                capturedMethodId = String.valueOf(args[i]);
                                break;
                            }
                        }
                    }
                    if (capturedMethodId == null) {
                        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                        if (attrs != null) {
                            HttpServletRequest request = attrs.getRequest();
                            String reqMid = request.getParameter("methodId");
                            if (reqMid != null && !reqMid.isEmpty()) {
                                capturedMethodId = reqMid;
                            }
                        }
                    }
                } catch (Exception ignore) {
                }

                // 路径A：仅在已捕获 methodId 时，从返回体提取 methodName（FMethodParam）
                String capturedMethodName = null;
                if (capturedMethodId != null) {
                    if (result instanceof Result) {
                        Object data = ((Result<?>) result).getData();
                        if (data instanceof FMethodParam) {
                            capturedMethodName = ((FMethodParam) data).getMethodName();
                        }
                    }
                }

                // 捕获 taskId，并尝试从返回体提取 taskName（允许为空）
                String capturedTaskId = null;
                try {
                    String[] paramNames = signature.getParameterNames();
                    Object[] args = joinPoint.getArgs();
                    if (paramNames != null && args != null && paramNames.length == args.length) {
                        for (int i = 0; i < paramNames.length; i++) {
                            if ("taskId".equals(paramNames[i]) && args[i] != null) {
                                capturedTaskId = String.valueOf(args[i]);
                                break;
                            }
                        }
                    }
                    if (capturedTaskId == null) {
                        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                        if (attrs != null) {
                            HttpServletRequest request = attrs.getRequest();
                            String reqTid = request.getParameter("taskId");
                            if (reqTid != null && !reqTid.isEmpty()) {
                                capturedTaskId = reqTid;
                            }
                        }
                    }
                } catch (Exception ignore) {
                }

                String capturedTaskName = null;
                if (capturedTaskId != null) {
                    if (result instanceof Result) {
                        Object data = ((Result<?>) result).getData();
                        if (data != null) {
                            try {
                                Method m = data.getClass().getMethod("getTaskName");
                                Object tn = m.invoke(data);
                                if (tn != null) {
                                    capturedTaskName = String.valueOf(tn);
                                }
                            } catch (Exception ignore) {
                                if (data instanceof Map) {
                                    Object tn = ((Map<?, ?>) data).get("taskName");
                                    if (tn != null) {
                                        capturedTaskName = String.valueOf(tn);
                                    }
                                }
                            }
                        }
                    }
                }

                try {
                    String resultSummary;
                    if (result instanceof Result) {
                        resultSummary = JSON.toJSONString(result);
                    } else {
                        resultSummary = (result == null) ? "null" : String.valueOf(result);
                    }
                    log.info("[SysLogAspect] 接收到信息：userName={}, methodId={}, taskId={}, taskName={}, 返回值={}"
                            , username, capturedMethodId, capturedTaskId, capturedTaskName, resultSummary);
                } catch (Exception ignore) {
                }

                // 捕获 instrumentId：方法参数/DTO/请求参数（允许为空）
                String capturedInstrumentId = null;
                try {
                    String[] paramNames = signature.getParameterNames();
                    Object[] args = joinPoint.getArgs();
                    if (paramNames != null && args != null && paramNames.length == args.length) {
                        for (int i = 0; i < paramNames.length; i++) {
                            if (("instrumentId".equals(paramNames[i]) || "deviceSerialNumber".equals(paramNames[i])) && args[i] != null) {
                                capturedInstrumentId = String.valueOf(args[i]);
                                break;
                            }
                            if (args[i] instanceof DeviceInitDTO) {
                                String v = ((DeviceInitDTO) args[i]).getDeviceSerialNumber();
                                if (v != null && !v.isEmpty()) {
                                    capturedInstrumentId = v;
                                    break;
                                }
                            }
                            if (args[i] instanceof DeviceSoftCloseDTO) {
                                String v = ((DeviceSoftCloseDTO) args[i]).getDeviceSerialNumber();
                                if (v != null && !v.isEmpty()) {
                                    capturedInstrumentId = v;
                                    break;
                                }
                            }
                        }
                    }
                    if (capturedInstrumentId == null) {
                        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                        if (attrs != null) {
                            HttpServletRequest request = attrs.getRequest();
                            String reqIns = request.getParameter("instrumentId");
                            if (reqIns != null && !reqIns.isEmpty()) {
                                capturedInstrumentId = reqIns;
                            } else {
                                String reqSn = request.getParameter("deviceSerialNumber");
                                if (reqSn != null && !reqSn.isEmpty()) {
                                    capturedInstrumentId = reqSn;
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {
                }

                // 从返回体提取 instrumentName 或 deviceName（允许为空）
                String capturedInstrumentName = null;
                try {
                    if (result instanceof Result) {
                        Object data = ((Result<?>) result).getData();
                        if (data != null) {
                            try {
                                Method gm = data.getClass().getMethod("getInstrumentName");
                                Object name = gm.invoke(data);
                                if (name != null) {
                                    capturedInstrumentName = String.valueOf(name);
                                }
                            } catch (Exception ignore) {
                                try {
                                    Method gm2 = data.getClass().getMethod("getDeviceName");
                                    Object name2 = gm2.invoke(data);
                                    if (name2 != null) {
                                        capturedInstrumentName = String.valueOf(name2);
                                    }
                                } catch (Exception ignore2) {
                                    if (data instanceof Map) {
                                        Object val = ((Map<?, ?>) data).get("instrumentName");
                                        if (val == null) {
                                            val = ((Map<?, ?>) data).get("deviceName");
                                        }
                                        if (val != null) {
                                            capturedInstrumentName = String.valueOf(val);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {
                }

                // 控制台打印：概要信息
                try {
                    log.info("[SysLogAspect] 前端传入：instrumentId={}, 返回体：instrumentName={}", capturedInstrumentId, capturedInstrumentName);
                } catch (Exception ignore) {
                }

                

                // 根据 Result.code 判定类型（成功/失败）
                String typeValue;
                if (result instanceof Result) {
                    Integer codeVal = ((Result<?>) result).getCode();
                    typeValue = (codeVal != null && codeVal.equals(ResultCodeEnum.SUCCESS.getCode())) ? "成功" : "失败";
                } else {
                    typeValue = (ex == null) ? "成功" : "失败";
                }

                // 构造 SysLogsPO 并写入日志库
                SysLogsPO po = new SysLogsPO();
                po.setTime(LocalDateTime.now());
                po.setType(typeValue);
                po.setSource(module);
                po.setUserId(userId);
                po.setUserName(username);
                po.setMethodId(capturedMethodId);
                po.setMethodName(capturedMethodName);
                po.setTaskId(capturedTaskId);
                po.setTaskName(capturedTaskName);
                po.setInstrumentId(capturedInstrumentId);
                po.setInstrumentName(capturedInstrumentName);
                po.setMessage(message);
                po.setPcName(pcName);

                sysLogsMapper.insert(po);
            } catch (Exception ignore) {
            }
        }
    }
}
