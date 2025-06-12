package com.persee.workstation.uv_inexus.service.instrumentService;


import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.systemctlConfigPo.UserConfigPO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.LoginUserInfoVO;

public interface UserManagerService {

    R<LoginUserInfoVO> login(UserLoginDTO loginDTO);

}
