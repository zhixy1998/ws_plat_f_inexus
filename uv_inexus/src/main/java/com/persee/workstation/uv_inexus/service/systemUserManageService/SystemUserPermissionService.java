package com.persee.workstation.uv_inexus.service.systemUserManageService;

import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserPermissionPO;

public interface SystemUserPermissionService {

    Boolean addNewPermission(SystemUserPermissionPO po);

    Boolean checkCreateUserPermission(String currentUserName, String systemIdentityId);

    Boolean checkDeleteUserPermission(String currentUserName, String deleteUserName);

    Boolean checkUpdateUserPermission(String currentUserName, CreateSystemUserDTO updateUserInfo);

    String checkQueryPermission(String systemIdentityId, String groupId);

}
