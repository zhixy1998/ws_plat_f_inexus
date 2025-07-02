package com.persee.workstation.uv_inexus.service.systemUserManageService;

import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserGroupManagePO;

public interface SystemUserGroupManageService {

    Boolean addNewGroup(SystemUserGroupManagePO po);

    SystemUserGroupManagePO getAllOrSelfGroup(String updateBy);

}
