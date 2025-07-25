package com.persee.workstation.uv_inexus.service.systemUserManageService.factory;

import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;

public interface UserFactory {

    SystemUserManagePO createUser(CreateSystemUserDTO dto);
}
