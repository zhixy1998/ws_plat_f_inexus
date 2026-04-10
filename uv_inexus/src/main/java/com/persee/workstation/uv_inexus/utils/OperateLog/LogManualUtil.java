package com.persee.workstation.uv_inexus.utils.OperateLog;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.dao.OperateLog.OperateLogMapper;
import com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class LogManualUtil {

    private static OperateLogMapper logMapper;

    // 注入Mapper
    public LogManualUtil(OperateLogMapper logMapper) {
        LogManualUtil.logMapper = logMapper;
    }

    // 新增日志
    public static void saveAddLog(String module, String content) {
        OperateLogPO log = new OperateLogPO();
        log.setModule(module);
        log.setDescription("新增操作：" + content);
        log.setCreateTime(new Date());
        log.setUsername(UserContext.getCurrentUser().getUsername());
        log.setStatus(1);
        logMapper.insert(log);
    }

    // 修改日志（自动对比）
    public static void saveUpdateLog(String module, Object oldObj, Object newObj) {
        String changeContent = LogCompareUtil.generateCompareContent(oldObj, newObj);

        OperateLogPO log = new OperateLogPO();
        log.setModule(module);
        log.setDescription("修改操作：" + changeContent);
        log.setBeforeData(JSON.toJSONString(oldObj));
        log.setAfterData(JSON.toJSONString(newObj));
        log.setChangeContent(changeContent);
        log.setCreateTime(new Date());
        log.setUsername(UserContext.getCurrentUser().getUsername());
        log.setStatus(1);
        logMapper.insert(log);
    }

    // 删除日志
    public static void saveDeleteLog(String module, Object oldObj) {
        OperateLogPO log = new OperateLogPO();
        log.setModule(module);
        log.setDescription("删除操作：" + JSON.toJSONString(oldObj));
        log.setBeforeData(JSON.toJSONString(oldObj));
        log.setCreateTime(new Date());
        log.setUsername(UserContext.getCurrentUser().getUsername());
        log.setStatus(1);
        logMapper.insert(log);
    }
}
