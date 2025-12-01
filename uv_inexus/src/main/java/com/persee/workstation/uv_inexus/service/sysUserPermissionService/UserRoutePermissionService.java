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

package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import java.util.List;

import com.persee.workstation.uv_inexus.model.Result;

import javax.servlet.http.HttpServletRequest;

public interface UserRoutePermissionService {
    // 根据用户ID查询有权访问的路由结构
    Result<List<String>> getUserRoutePermissions(String userId, HttpServletRequest request);
}