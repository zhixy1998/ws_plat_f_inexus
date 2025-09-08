package com.persee.workstation.uv_inexus.service.guide.systemUserManageService;

import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserGroupManagePO;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.SystemUserGroupInfo;

import java.util.List;

public interface SystemUserGroupManageService {

    Boolean addNewGroup(SystemUserGroupManagePO po);

    SystemUserGroupManagePO getAllOrSelfGroup(String updateBy);

    List<SystemUserGroupInfo> queryGroupInfo(String systemIdentityId, String groupNo);

}
