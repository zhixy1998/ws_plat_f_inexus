package com.persee.workstation.uv_inexus.service.guide.userManageService;


import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.userManageVo.LoginUserInfoVO;

public interface UserManagerService {

    R<LoginUserInfoVO> login(UserLoginDTO loginDTO);

}
