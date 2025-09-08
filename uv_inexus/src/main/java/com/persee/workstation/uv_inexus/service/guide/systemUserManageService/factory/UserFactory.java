package com.persee.workstation.uv_inexus.service.guide.systemUserManageService.factory;

import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;

public interface UserFactory {

    SystemUserManagePO createUser(CreateSystemUserDTO dto);
}
