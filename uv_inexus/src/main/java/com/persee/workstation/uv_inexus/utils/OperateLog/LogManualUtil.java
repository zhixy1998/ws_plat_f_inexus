package com.persee.workstation.uv_inexus.utils.OperateLog;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.dao.OperateLog.OperateLogMapper;
import com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Component
public class LogManualUtil {

    private static OperateLogMapper logMapper;

    public LogManualUtil(OperateLogMapper logMapper) {
        LogManualUtil.logMapper = logMapper;
    }

    // 获取当前登录人（仅HTTP可用）
    private static String getDefaultUsername() {
        try {
            LoginUserInfoVO user = UserContext.getCurrentUser();
            if (user != null && StringUtils.hasText(user.getUsername())) {
                return user.getUsername();
            }
        } catch (Exception e) {
        }
        return "系统操作";
    }

    private static String getMethod() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "";
            HttpServletRequest request = attrs.getRequest();
            HandlerMethod handler = (HandlerMethod) request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
            return handler.getBeanType().getName() + "." + handler.getMethod().getName();
        } catch (Exception e) {
            return "";
        }
    }

    private static String getIp() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getRemoteAddr();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private static String safeStr(String s) {
        return s == null ? "" : s;
    }

    // ===================== 【核心】带用户名的方法 =====================
    public static void saveAddLog(String module, String content, String username, Object params) {
        try {
            if (!StringUtils.hasText(content)) return;

            // 如果传了用户名，用传入的；否则用自动获取
            String realUser = StringUtils.hasText(username) ? username : getDefaultUsername();

            OperateLogPO log = new OperateLogPO();
            log.setModule(module);
            log.setDescription("新增操作");
            log.setChangeContent("新增操作：" + content);
            log.setUsername(realUser); // ✅ 使用传入的用户名
            log.setCreateTime(new Date());
            log.setStatus(1);
            log.setOperateTime(System.currentTimeMillis() / 1000);
            log.setIp(getIp());
            log.setMethod(getMethod());
            log.setParams(safeStr(JSON.toJSONString(params)));
            log.setResult("");
            logMapper.insert(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 兼容旧方法
    public static void saveAddLog(String module, String content, Object params) {
        saveAddLog(module, content, null, params);
    }

    // ===================== 修改日志 =====================
    public static void saveUpdateLog(String module, Object oldObj, Object newObj, String username, Object params) {
        try {
            String changeContent = LogCompareUtil.generateCompareContent(oldObj, newObj);
            if (!StringUtils.hasText(changeContent)) return;

            String realUser = StringUtils.hasText(username) ? username : getDefaultUsername();

            OperateLogPO log = new OperateLogPO();
            log.setModule(module);
            log.setDescription("修改操作");
            log.setBeforeData(JSON.toJSONString(oldObj));
            log.setAfterData(JSON.toJSONString(newObj));
            log.setChangeContent(changeContent);
            log.setUsername(realUser);
            log.setCreateTime(new Date());
            log.setStatus(1);
            log.setOperateTime(System.currentTimeMillis() / 1000);
            log.setIp(getIp());
            log.setMethod(getMethod());
            log.setParams(safeStr(JSON.toJSONString(params)));
            log.setResult("");
            logMapper.insert(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveUpdateLog(String module, Object oldObj, Object newObj, Object params) {
        saveUpdateLog(module, oldObj, newObj, null, params);
    }

    // ===================== 删除日志 =====================
    public static void saveDeleteLog(String module, String content, Object oldObj, String username, Object params) {
        try {
            if (!StringUtils.hasText(content)) return;

            String realUser = StringUtils.hasText(username) ? username : getDefaultUsername();

            OperateLogPO log = new OperateLogPO();
            log.setModule(module);
            log.setDescription("删除操作");
            log.setChangeContent(content);
            log.setBeforeData(JSON.toJSONString(oldObj));
            log.setUsername(realUser);
            log.setCreateTime(new Date());
            log.setStatus(1);
            log.setOperateTime(System.currentTimeMillis() / 1000);
            log.setIp(getIp());
            log.setMethod(getMethod());
            log.setParams(safeStr(JSON.toJSONString(params)));
            log.setResult("");
            logMapper.insert(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveDeleteLog(String module, String content, Object oldObj, Object params) {
        saveDeleteLog(module, content, oldObj, null, params);
    }
}