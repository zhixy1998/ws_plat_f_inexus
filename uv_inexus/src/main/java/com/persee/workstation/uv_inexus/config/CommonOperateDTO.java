/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.config;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 通用操作DTO
 * 统一所有增删改查/批量操作/特殊操作的入参格式，适配动态权限校验
 */
@Data
@ApiModel(value = "CommonOperateDTO", description = "通用操作入参DTO")
public class CommonOperateDTO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 操作类型（标准化）：add-新增 | edit-修改 | delete-删除 | batchDelete-批量删除
     * export-导出 | import-导入 | enable-启用 | disable-禁用
     */
    @ApiModelProperty(value = "操作类型", required = true, example = "add")
    private String operateType;

    /**
     * 单条业务ID（适用于：修改/删除单条数据）
     */
    @ApiModelProperty(value = "单条业务ID", example = "1001")
    private Long id;

    /**
     * 批量ID列表（适用于：批量删除/批量启用/批量禁用）
     */
    @ApiModelProperty(value = "批量业务ID列表", example = "[1001,1002,1003]")
    private List<Long> ids;

    /**
     * 具体业务数据（泛型，存放不同业务的差异化字段）
     */
    @ApiModelProperty(value = "具体业务数据（新增/修改用）")
    private T businessData;

    /**
     * 扩展参数（可选，存放非结构化的临时参数）
     */
    @ApiModelProperty(value = "扩展参数（可选）")
    private Map<String, Object> extParams;
}