/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigGrantPermDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysQuickConfigPO;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

/**
 * 快捷配置Service接口
 */
public interface SysQuickConfigService extends IService<SysQuickConfigPO> {

    /**
     * 分页查询快捷配置
     */
    Result<PageView<SysQuickConfigPO>> getQuickConfigPage(Page<SysQuickConfigPO> page, QuickConfigQueryDTO queryDTO);

    /**
     * 新增快捷配置
     */
    Result<String> addQuickConfig(QuickConfigSaveDTO saveDTO);

    /**
     * 修改快捷配置
     */
    Result<String> updateQuickConfig(QuickConfigSaveDTO saveDTO);

    /**
     * 批量删除快捷配置
     */
    Result<String> deleteBatch(List<Long> ids);

    /**
     * 根据ID查询快捷配置
     */
    Result<SysQuickConfigPO> getQuickConfigById(Long id);

    /**
     * 根据ID删除单个快捷配置
     */
    Result<String> deleteById(Long id);

    Result<List<Map<String, Object>>> getQuickConfigTree(String groupId, Integer quickConfigType);

    Result<Boolean> grantQuickConfigToGroup(QuickConfigGrantPermDTO grantDTO);
}