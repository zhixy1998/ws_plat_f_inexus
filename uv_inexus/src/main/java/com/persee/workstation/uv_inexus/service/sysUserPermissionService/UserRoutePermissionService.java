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

import com.persee.workstation.uv_inexus.model.Result;

import javax.servlet.http.HttpServletRequest;

public interface UserRoutePermissionService {
    // 根据用户ID查询有权访问的路由结构
    Result<List<String>> getUserRoutePermissions(String userId, HttpServletRequest request);
    
    // 获取所有菜单路由结构（perm_type=1）
    Result<List<?>> getMenuRouteStructure();
}