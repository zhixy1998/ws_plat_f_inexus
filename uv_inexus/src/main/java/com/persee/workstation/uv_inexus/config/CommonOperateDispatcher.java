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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用操作分发器
 * 封装所有操作类型的分发逻辑（只写一次，所有业务复用）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonOperateDispatcher {

    // 缓存所有业务处理器：key=bizType，value=对应的处理器
    private final Map<String, CommonOperateHandler<?>> handlerMap = new ConcurrentHashMap<>();

    // 初始化：把所有实现了CommonOperateHandler的Bean放入缓存
    public CommonOperateDispatcher(List<CommonOperateHandler<?>> handlers) {
        handlers.forEach(handler -> {
            handlerMap.put(handler.getBizType(), handler);
            log.info("注册业务处理器：{}", handler.getBizType());
        });
    }

    /**
     * 统一分发操作
     * @param bizType 业务类型（如：softwareActivation）
     * @param dto 通用操作DTO
     */
    @SuppressWarnings("unchecked")
    public <T> Result<Void> dispatch(String bizType, CommonOperateDTO<T> dto) {
        // 1. 获取当前业务的处理器
        CommonOperateHandler<T> handler = (CommonOperateHandler<T>) Optional.ofNullable(handlerMap.get(bizType))
                .orElseThrow(() -> new IllegalArgumentException("未找到业务类型[" + bizType + "]的处理器"));

        // 2. 统一处理操作类型分发（只写一次，所有业务复用）
        String operateType = dto.getOperateType();
        if (operateType == null || operateType.isEmpty()) {
            return Result.fail(400, "操作类型不能为空");
        }

        log.info("业务类型[{}]执行操作类型[{}]", bizType, operateType);
        switch (operateType) {
            case "add":
                return handler.handleAdd(dto);
            case "edit":
                return handler.handleEdit(dto);
            case "delete":
                return handler.handleDelete(dto);
            case "batchDelete":
                return handler.handleBatchDelete(dto);
            case "export":
                return handler.handleExport(dto);
            default:
                return Result.fail(400, "不支持的操作类型：" + operateType);
        }
    }
}