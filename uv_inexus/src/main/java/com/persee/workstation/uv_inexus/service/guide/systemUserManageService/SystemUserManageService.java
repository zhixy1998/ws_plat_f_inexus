/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
