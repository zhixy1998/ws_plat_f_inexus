package com.persee.workstation.uv_inexus.service.systemUserManageService;

import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.SystemUserGroupInfo;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.SystemUserInfoVO;
import com.persee.workstation.uv_inexus.vo.userManageVo.LoginUserInfoVO;

import java.util.List;

public interface SystemUserManageService {

    R<Boolean> createUser(CreateSystemUserDTO dto);

    R<Boolean> updateUser(CreateSystemUserDTO dto);

    R<Boolean> deleteUser(String updateBy, String deleteUserName);

    R<List<SystemUserInfoVO>> getUserInfo(String currentUserName);

    R<List<SystemUserGroupInfo>> getGroupInfo(String currentUserName);

    R<LoginUserInfoVO> login(UserLoginDTO loginDTO);

}
