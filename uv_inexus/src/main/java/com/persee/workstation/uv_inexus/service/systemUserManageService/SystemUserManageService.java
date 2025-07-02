package com.persee.workstation.uv_inexus.service.systemUserManageService;

import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.SystemUserInfoVO;

public interface SystemUserManageService {

    R<Boolean> addSystemUser(CreateSystemUserDTO dto);

    R<Boolean> addGroupUser(CreateSystemUserDTO dto);

    R<SystemUserInfoVO> getUserInfo(String userName);

    R<Boolean> deleteSystemUser(String userName);

    R<Boolean> deleteGroupUser(String userName);
}
