package com.persee.workstation.uv_inexus.service.instrumentService;


import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.systemctlConfigPo.UserConfigPO;

public interface UserManagerService {

    R<UserConfigPO> login(UserLoginDTO loginDTO);

}
