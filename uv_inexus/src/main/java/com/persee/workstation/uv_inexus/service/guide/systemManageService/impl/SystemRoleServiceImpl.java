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

package com.persee.workstation.uv_inexus.service.guide.systemManageService.impl;

import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemRoleDAO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemRolePO;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SystemRoleServiceImpl implements SystemRoleService {
    @Autowired
    private SystemRoleDAO roleDAO;


    @Override
    public R<Boolean> addSystemRole(SystemRolePO systemRolePO) {
        systemRolePO.setCreateBy(systemRolePO.getUpdateBy());
        boolean b = roleDAO.insert(systemRolePO) > 0;
        return b ? R.ok() : R.fail("添加失败");
    }

    @Override
    public R<Boolean> updateSystemRole(SystemRolePO po) {
        if (StringUtils.hasText(po.getId())){
            String updateBy = po.getUpdateBy();


        } else {

        }

        return null;
    }

    @Override
    public R<Boolean> deleteSystemRole(SystemRolePO po) {
        return null;
    }

    @Override
    public R<List<SystemRolePO>> querySystemRole(String updateBy) {
        return null;
    }
}
