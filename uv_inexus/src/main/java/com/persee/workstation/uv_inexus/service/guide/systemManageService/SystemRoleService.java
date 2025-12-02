/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
