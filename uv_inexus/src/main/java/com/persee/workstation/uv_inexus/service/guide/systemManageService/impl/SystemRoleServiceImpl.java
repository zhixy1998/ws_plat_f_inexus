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
