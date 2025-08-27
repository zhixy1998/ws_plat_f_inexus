package com.persee.workstation.uv_inexus.service.guide.systemUserManageService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserGroupManageDAO;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserGroupManagePO;
import com.persee.workstation.uv_inexus.service.guide.systemUserManageService.SystemUserGroupManageService;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.SystemUserGroupInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
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

    @Override
    public List<SystemUserGroupInfo> queryGroupInfo(String systemIdentityId, String groupNo) {

        LambdaQueryWrapper<SystemUserGroupManagePO> queryWrapper = new LambdaQueryWrapper<>();
        List<SystemUserGroupManagePO> poList;
        List<SystemUserGroupInfo> infoList = new ArrayList<>();

        if (systemIdentityId != null) {
            queryWrapper.eq(SystemUserGroupManagePO::getSystemIdentityId, systemIdentityId);
        }
        if (groupNo != null) {
            queryWrapper.eq(SystemUserGroupManagePO::getGroupNo, groupNo);
        }

        poList = dao.selectList(queryWrapper);
        for (SystemUserGroupManagePO po : poList) {
            SystemUserGroupInfo info = new SystemUserGroupInfo();
            BeanUtils.copyProperties(po, info);
            infoList.add(info);
        }
        return infoList;
    }


}
