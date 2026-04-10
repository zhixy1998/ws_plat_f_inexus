package com.persee.workstation.uv_inexus.utils.OperateLog;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.dao.OperateLog.OperateLogMapper;
import com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Component
public class LogManualUtil {

    private static OperateLogMapper logMapper;

    // 注入Mapper
    public LogManualUtil(OperateLogMapper logMapper) {
        LogManualUtil.logMapper = logMapper;
    }

    // ===================== 工具方法：统一截断超长字段，避免数据库报错 =====================
    private static String safeStr(String str) {
        if (str == null) return "";
        // 按数据库字段最大长度截断（params/result等text字段，统一2000字符）
        return str.length() > 2000 ? str.substring(0, 2000) + "..." : str;
    }

    // ===================== 工具方法：稳定获取客户端真实IP =====================
    private static String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return "127.0.0.1"; // 兜底
            HttpServletRequest request = attributes.getRequest();

            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // 多级代理取第一个有效IP
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "127.0.0.1"; // 异常兜底
        }
    }

    // ===================== 新增日志（全字段非空） =====================
    public static void saveAddLog(String module, String content, Object params) {
        if (!StringUtils.hasText(content)) return;
        OperateLogPO log = new OperateLogPO();

        // 核心业务字段
        log.setModule(module);
        log.setDescription("新增操作");
        log.setChangeContent(safeStr("新增操作：" + content));
        log.setBeforeData("");
        log.setAfterData("");

        // 通用字段（全非空）
        log.setUsername(UserContext.getCurrentUser().getUsername());
        log.setCreateTime(new Date());
        log.setStatus(1);
        log.setOperateTime(System.currentTimeMillis() / 1000); // 秒级时间戳，适配int(11)
        log.setIp(safeStr(getClientIp()));
        log.setLogType(1);
        log.setErrorMsg("");
        log.setPermission("");
        log.setMethod(""); // 手动日志不自动获取method，避免上下文问题，保持空字符串（非null）
        log.setParams(safeStr(JSON.toJSONString(params))); // 入参直接序列化，永不为null
        log.setResult(""); // 占位，永不为null
        log.setChangeContent(safeStr("新增操作：" + content));

        logMapper.insert(log);
    }

    // ===================== 修改日志（全字段非空） =====================
    public static void saveUpdateLog(String module, Object oldObj, Object newObj, Object params) {
        String changeContent = LogCompareUtil.generateCompareContent(oldObj, newObj);
        if (!StringUtils.hasText(changeContent)) return;

        OperateLogPO log = new OperateLogPO();

        // 核心业务字段
        log.setModule(module);
        log.setDescription("修改操作");
        log.setBeforeData(safeStr(JSON.toJSONString(oldObj)));
        log.setAfterData(safeStr(JSON.toJSONString(newObj)));
        log.setChangeContent(safeStr("修改操作：" + changeContent));

        // 通用字段（全非空）
        log.setUsername(UserContext.getCurrentUser().getUsername());
        log.setCreateTime(new Date());
        log.setStatus(1);
        log.setOperateTime(System.currentTimeMillis() / 1000);
        log.setIp(safeStr(getClientIp()));
        log.setLogType(1);
        log.setErrorMsg("");
        log.setPermission("");
        log.setMethod("");
        log.setParams(safeStr(JSON.toJSONString(params)));
        log.setResult("");

        logMapper.insert(log);
    }

    // ===================== 删除日志（全字段非空） =====================
    public static void saveDeleteLog(String module, String content, Object oldObj, Object params) {
        if (!StringUtils.hasText(content)) return;
        OperateLogPO log = new OperateLogPO();

        // 核心业务字段
        log.setModule(module);
        log.setDescription("删除操作");
        log.setChangeContent(safeStr("删除操作：" + content));
        log.setBeforeData(safeStr(JSON.toJSONString(oldObj)));
        log.setAfterData("");

        // 通用字段（全非空）
        log.setUsername(UserContext.getCurrentUser().getUsername());
        log.setCreateTime(new Date());
        log.setStatus(1);
        log.setOperateTime(System.currentTimeMillis() / 1000);
        log.setIp(safeStr(getClientIp()));
        log.setLogType(1);
        log.setErrorMsg("");
        log.setPermission("");
        log.setMethod("");
        log.setParams(safeStr(JSON.toJSONString(params)));
        log.setResult("");

        logMapper.insert(log);
    }

    // ===================== 兼容老调用（不传params也永不为null） =====================
    public static void saveAddLog(String module, String content) {
        saveAddLog(module, content, "");
    }
    public static void saveUpdateLog(String module, Object oldObj, Object newObj) {
        saveUpdateLog(module, oldObj, newObj, "");
    }
    public static void saveDeleteLog(String module, String content, Object oldObj) {
        saveDeleteLog(module, content, oldObj, "");
    }
    public static void saveDeleteLog(String module, Object oldObj) {
        saveDeleteLog(module, JSON.toJSONString(oldObj), oldObj, "");
    }
}