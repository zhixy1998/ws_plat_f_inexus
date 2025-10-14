/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.service.guide.systemUserManageService;

import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.QueryUserVO;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.SystemUserGroupInfo;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.SystemUserInfoVO;
import com.persee.workstation.uv_inexus.vo.userManageVo.LoginUserInfoVO;

import java.util.List;

public interface SystemUserManageService {

    R<Boolean> createUser(CreateSystemUserDTO dto);

    R<Boolean> updateUser(CreateSystemUserDTO dto);

    R<Boolean> deleteUser(String updateBy, String deleteUserName);

    R<List<SystemUserInfoVO>> getUserInfo(String updateBy);

    R<List<QueryUserVO>> queryUserInfo(String updateBy);

    R<List<SystemUserGroupInfo>> getGroupInfo(String updateBy);

    R<LoginUserInfoVO> login(UserLoginDTO loginDTO);

}
