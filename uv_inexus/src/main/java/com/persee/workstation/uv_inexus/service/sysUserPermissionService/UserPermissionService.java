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

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;

/**
 * 用户权限查询服务
 */
public interface UserPermissionService {
    // 根据用户ID查询所有权限
    Result<List<PermissionDTO>> getUserPermissions(String userId);

    // 新增方法：查询parent_id为0的所有权限的path值
    Result<List<String>> getRootPermissionPaths(String userId);

    // 根据权限类型和角色ID查询树形权限结构
    Result<List<PermissionTreeDTO>> getPermissionTreeByTypeAndUser(
            List<Integer> permTypeList, String groupId, Integer quickConfigType, Integer hidden);

    /**
     * 更新小组权限关系
     * @param groupId 小组ID
     * @param targetPermIds 目标权限ID列表
     * @return 操作结果
     */
    Result<Boolean> updateGroupPermissions(String groupId, List<String> targetPermIds);
}