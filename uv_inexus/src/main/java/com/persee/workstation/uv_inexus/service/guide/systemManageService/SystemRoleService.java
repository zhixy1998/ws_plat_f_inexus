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

package com.persee.workstation.uv_inexus.service.guide.systemManageService;


import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemRolePO;

import java.util.List;

public interface SystemRoleService {

    R<Boolean> addSystemRole(SystemRolePO systemRolePO);

    /**
     * 新增系统角色
     * 查询角色权限
     * 修改角色权限
     * 删除系统角色
     */

    R<Boolean> updateSystemRole(SystemRolePO systemRolePO);

    R<Boolean> deleteSystemRole(SystemRolePO systemRolePO);

    R<List<SystemRolePO>> querySystemRole(String updateBy);

}
