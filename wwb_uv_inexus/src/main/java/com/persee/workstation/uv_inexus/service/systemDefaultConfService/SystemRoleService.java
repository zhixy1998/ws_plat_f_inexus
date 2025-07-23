package com.persee.workstation.uv_inexus.service.systemDefaultConfService;


import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.systemDefaultConfPo.SystemRolePO;

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
