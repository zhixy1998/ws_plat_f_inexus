/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
