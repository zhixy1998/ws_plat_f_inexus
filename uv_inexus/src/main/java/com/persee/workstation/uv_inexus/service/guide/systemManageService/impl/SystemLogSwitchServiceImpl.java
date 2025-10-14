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

import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemLogSwitchDAO;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemLogSwitchPO;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemLogSwitchService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemLogSwitchVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SystemLogSwitchServiceImpl implements SystemLogSwitchService {

    @Autowired
    SystemLogSwitchDAO dao;

    @Value("${sys-manage-default.defaultId}")
    String defaultId;

    @Value("${sys-manage-default.userId}")
    String userId;

    @Override
    public SystemLogSwitchVO querySystemLogSwitch() {
        SystemLogSwitchPO po = dao.selectById(userId);
        SystemLogSwitchVO vo = new SystemLogSwitchVO();
        if (po == null) {
            po = dao.selectById(defaultId);
        }
        BeanUtils.copyProperties(po, vo);
        return vo;
    }

    @Override
    public Boolean changeSwitch(SystemLogSwitchPO po) {
        SystemLogSwitchPO userPO = dao.selectById(userId);
        po.setId(userId);
        if (!po.getStatUsageTotalSwitch()) {
            log.info("使用情况统计总开关是关闭的，关闭其他开关");
            po.setStatItemEnabled(false);
            po.setStatTypeEnabled(false);
            po.setStatDetailEnabled(false);
            po.setStatUserEnabled(false);
            po.setStatUsageEnabled(false);
        }

        if (userPO == null) {
            po.setCreateBy(po.getUpdateBy());
            return dao.insert(po) > 0;
        } else {
            return dao.updateById(po) > 0;
        }
    }

    @Override
    public Boolean resetLogSwitch(String updateBy) {
        SystemLogSwitchPO po = dao.selectById(userId);
        if (po != null) {
            return dao.deleteById(po) > 0;
        } else {
            log.info("没有用户记录");
        }
        return true;
    }
}
