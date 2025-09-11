package com.persee.workstation.uv_inexus.service.guide.OperateLog;

import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO;

/**
 * 操作日志服务接口
 */
public interface OperateLogService {

    /**
     * 日志分页查询（支持模糊查询）
     * @param page 分页对象（页码、页大小）
     * @param params 模糊查询参数（如username、module、description等）
     * @return 分页结果（总数、当前页数据）
     */
    IPage<OperateLogPO> findLogPage(Page<OperateLogPO> page, Map<String, String> params);
}