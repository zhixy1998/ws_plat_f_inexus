package com.persee.workstation.uv_inexus.service.systemManageService.impl;

import com.persee.workstation.uv_inexus.dao.systemManageDao.SystemDatabaseConfDAO;
import com.persee.workstation.uv_inexus.po.systemManagePo.SystemDatabaseConfPO;
import com.persee.workstation.uv_inexus.service.systemManageService.SystemctlDatabaseConfService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemDataBaseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SystemctlDatabaseConfServiceImpl implements SystemctlDatabaseConfService {

    @Autowired
    SystemDatabaseConfDAO dao;

    @Value("${sys-manage-default.defaultId}")
    String defaultId;

    @Value("${sys-manage-default.userId}")
    String userId;

    @Override
    public Boolean saveDatabaseConf(SystemDatabaseConfPO po) {

        SystemDatabaseConfPO confPO = dao.selectById(userId);

        po.setId(userId);
        if (confPO == null) {
            po.setCreateBy(po.getUpdateBy());
            return dao.insert(po) > 0;
        } else {
            return dao.updateById(po) > 0;
        }
    }

    @Override
    public SystemDataBaseVO queryDatabaseConf() {

        SystemDatabaseConfPO userDataPO = dao.selectById(userId);
        if (userDataPO == null) {
            SystemDatabaseConfPO confPO = dao.selectById(defaultId);
            SystemDataBaseVO vo = new SystemDataBaseVO();
            BeanUtils.copyProperties(confPO, vo);
            return vo;
        } else {
            SystemDataBaseVO vo = new SystemDataBaseVO();
            BeanUtils.copyProperties(userDataPO, vo);
            return vo;
        }
    }
}
