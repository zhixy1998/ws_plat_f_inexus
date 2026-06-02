/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.OperateLog.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.OperateLog.OperateLogMapper;
import com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO;
import com.persee.workstation.uv_inexus.service.guide.OperateLog.OperateLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 操作日志服务实现类（已修复无效字段问题）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OperateLogServiceImpl implements OperateLogService {

    private final OperateLogMapper operateLogMapper;

    // 表t_sys_operate_log中存在的有效查询字段（白名单）
    // 请根据你的实际表结构修改此列表
    private static final List<String> VALID_FIELDS = Arrays.asList(
            "username",    // 操作用户名
            "module",      // 操作模块
            "description", // 操作描述
            "change_content", // 变更内容
            "ip",          // 操作IP
            "status",      // 操作状态
            "log_type",
            "method"       // 调用方法
    );

    // 参数名到数据库字段名的映射（支持驼峰转下划线）
    private static final Map<String, String> FIELD_MAPPING = new HashMap<>();
    static {
        FIELD_MAPPING.put("changeContent", "change_content");
        FIELD_MAPPING.put("logType", "log_type");
    }

    @Override
    public IPage<OperateLogPO> findLogPage(Page<OperateLogPO> page, Map<String, String> params) {
        QueryWrapper<OperateLogPO> queryWrapper = new QueryWrapper<>();

        // 遍历参数，只处理白名单中的字段
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 1. 跳过分页参数和无效字段
            if ("pageNum".equals(key) || "pageSize".equals(key)) {
                continue;
            }

            // 2. 处理时间区间查询
            if ("startTime".equals(key)) {
                if (StringUtils.isNotBlank(value)) {
                    queryWrapper.ge("create_time", value);
                    log.info("添加时间区间查询: create_time >= {}", value);
                }
                continue;
            }

            if ("endTime".equals(key)) {
                if (StringUtils.isNotBlank(value)) {
                    queryWrapper.le("create_time", value);
                    log.info("添加时间区间查询: create_time <= {}", value);
                }
                continue;
            }

            // 3. 检查是否有字段映射（驼峰转下划线）
            String dbField = FIELD_MAPPING.getOrDefault(key, key);

            // 4. 处理白名单中的字段（模糊查询）
            if (VALID_FIELDS.contains(dbField)) {
                if (StringUtils.isNotBlank(value)) {
                    queryWrapper.like(dbField, value);
                    log.info("添加查询条件: {} LIKE %{}%", dbField, value);
                }
            } else {
                // 记录无效字段（便于调试）
                log.warn("忽略无效查询字段: {}", key);
            }
        }

        // 4. 按创建时间倒序
        queryWrapper.orderByDesc("create_time");

        // 5. 执行分页查询
        IPage<OperateLogPO> resultPage = operateLogMapper.selectPage(page, queryWrapper);

        log.info("日志查询结果: 总记录数={}, 当前页={}",
                resultPage.getTotal(), resultPage.getCurrent());

        return resultPage;
    }
}
