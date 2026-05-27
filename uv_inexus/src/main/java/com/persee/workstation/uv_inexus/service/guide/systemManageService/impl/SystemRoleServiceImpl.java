/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.systemManageService.impl;

import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemRoleDAO;
import com.persee.workstation.uv_inexus.i18n.I18nMessageSource;
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
        //添加失败
        return b ? R.ok() : R.fail("00501001");
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
