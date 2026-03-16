/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.config;

import com.persee.workstation.uv_inexus.model.Result;

/**
 * 通用操作处理器接口（策略模式）
 * 每个业务实现自己的处理器，处理add/edit/delete等操作
 */
public interface CommonOperateHandler<T> {

    /**
     * 处理新增操作
     */
    Result<Void> handleAdd(CommonOperateDTO<T> dto);

    /**
     * 处理修改操作
     */
    Result<Void> handleEdit(CommonOperateDTO<T> dto);

    /**
     * 处理单条删除操作
     */
    Result<Void> handleDelete(CommonOperateDTO<T> dto);

    /**
     * 处理批量删除操作
     */
    Result<Void> handleBatchDelete(CommonOperateDTO<T> dto);

    /**
     * 处理导出操作（可选）
     */
    default Result<Void> handleExport(CommonOperateDTO<T> dto) {
        return Result.fail(400, "该业务不支持导出操作");
    }

    /**
     * 获取当前处理器对应的业务类型（如：softwareActivation、sysUser）
     */
    String getBizType();
}