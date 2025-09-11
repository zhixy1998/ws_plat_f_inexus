package com.persee.workstation.uv_inexus.service.guide.OperateLog.impl;

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
 * 操作日志服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor // 构造器注入，替代@Autowired
public class OperateLogServiceImpl implements OperateLogService {

    // 注入日志DAO（继承MyBatis-Plus的BaseMapper，自带selectPage等方法）
    private final OperateLogMapper operateLogMapper;

    @Override
    public IPage<OperateLogPO> findLogPage(Page<OperateLogPO> page, Map<String, String> params) {
        // 1. 构建查询条件（动态模糊查询）
        QueryWrapper<OperateLogPO> queryWrapper = new QueryWrapper<>();
        
        // 2. 遍历参数，忽略分页相关参数（pageNum、pageSize），其他参数做模糊匹配
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey(); // 查询字段名（需与OperateLogPO的属性名一致）
            String value = entry.getValue(); // 查询关键词
            
            // 跳过分页参数，避免错误查询
            if ("pageNum".equals(key) || "pageSize".equals(key)) {
                continue;
            }
            
            // 关键词非空时，添加模糊查询条件（like '%value%'）
            if (StringUtils.isNotBlank(value)) {
                queryWrapper.like(key, value);
                log.info("添加模糊查询条件：{} LIKE %{}%", key, value);
            }
        }
        
        // 3. 按日志记录时间倒序（最新的日志排在前面，符合业务习惯）
        queryWrapper.orderByDesc("create_time");
        
        // 4. 调用MyBatis-Plus分页查询方法
        IPage<OperateLogPO> resultPage = operateLogMapper.selectPage(page, queryWrapper);
        
        // 5. 打印分页日志（调试用）
        log.info("日志分页查询结果：总记录数={}, 当前页={}, 页大小={}, 当前页记录数={}",
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getRecords().size());
        
        return resultPage;
    }
}
