/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

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