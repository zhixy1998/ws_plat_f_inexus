package com.persee.workstation.uv_inexus.service.systemUserManageService.impl;

import com.persee.workstation.uv_inexus.dao.systemUserManageDao.SystemUserPermissionDAO;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserPermissionPO;
import com.persee.workstation.uv_inexus.service.systemUserManageService.SystemUserPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemUserPermissionServiceImpl implements SystemUserPermissionService {

    @Autowired
    SystemUserPermissionDAO dao;
    @Override
    public Boolean addNewPermission(SystemUserPermissionPO po) {

        return dao.insert(po) > 0;
    }
}
