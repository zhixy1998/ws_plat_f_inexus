package com.persee.workstation.uv_inexus.service.systemUserManageService.impl;

import com.persee.workstation.uv_inexus.dao.systemUserManageDao.SystemUserGroupManageDAO;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserGroupManagePO;
import com.persee.workstation.uv_inexus.service.systemUserManageService.SystemUserGroupManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemUserGroupManageServiceImpl implements SystemUserGroupManageService {

    @Autowired
    SystemUserGroupManageDAO dao;

    @Override
    public Boolean addNewGroup(SystemUserGroupManagePO po) {


        return dao.insert(po) > 0;
    }

    @Override
    public SystemUserGroupManagePO getAllOrSelfGroup(String updateBy) {
        return null;
    }
}
