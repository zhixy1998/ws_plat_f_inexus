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

package com.persee.workstation.uv_inexus.service.guide.systemUserManageService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserPermissionDAO;
import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.enums.systemUserManageEnums.SystemGroupEnum;
import com.persee.workstation.uv_inexus.enums.systemUserManageEnums.SystemRoleEnum;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserPermissionPO;
import com.persee.workstation.uv_inexus.service.guide.systemUserManageService.SystemUserPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SystemUserPermissionServiceImpl implements SystemUserPermissionService {

    @Autowired
    SystemUserPermissionDAO dao;

    @Autowired
    SystemUserManageDAO userDao;

    @Override
    public Boolean addNewPermission(SystemUserPermissionPO po) {

        return dao.insert(po) > 0;
    }

    @Override
    public Boolean checkCreateUserPermission(String currentUserName, String systemIdentityId) {
        LambdaQueryWrapper<SystemUserManagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemUserManagePO::getUserName, currentUserName);

        SystemUserManagePO po = userDao.selectOne(queryWrapper);

        if (po.getSystemIdentityId().equals(SystemRoleEnum.DEVELOPER.getSid())) {
            log.info("当前用户是开发人员");
            return true;
        }
        if (po.getGroupNo().equals(SystemGroupEnum.PRODUCTION_LINE_ADMIN.getGroupNo())) {
            log.info("当前用户是产线管理员");
            if (systemIdentityId.equals(SystemRoleEnum.PRODUCTION_LINE.getSid())) {
                log.info("所添加用户是产线人员，产线管理员均可添加");
                return true;
            }
        }

        log.info("当前用户没有添加用户的权限或添加的用户身份超出了权限限制");
        return false;
    }

    @Override
    public Boolean checkDeleteUserPermission(String currentUserName, String deleteUserName) {
        LambdaQueryWrapper<SystemUserManagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemUserManagePO::getUserName, currentUserName);
        SystemUserManagePO currentPo = userDao.selectOne(queryWrapper);

        queryWrapper.clear();
        queryWrapper.eq(SystemUserManagePO::getUserName, deleteUserName);
        SystemUserManagePO deletePo = userDao.selectOne(queryWrapper);

        if (currentPo.getSystemIdentityId().equals(SystemRoleEnum.DEVELOPER.getSid())) {
            log.info("开发人员可操作");
            return true;
        }
        if (currentPo.getGroupNo().equals(SystemGroupEnum.PRODUCTION_LINE_ADMIN.getGroupNo())) {
            log.info("产线人员操作");
            if (deletePo.getGroupNo().equals(SystemRoleEnum.PRODUCTION_LINE.getSid())) {
                log.info("被删除用户是产线人员");
                return true;
            }
        }

        log.info("当前用户没有删除用户的权限");

        return false;
    }

    @Override
    public Boolean checkUpdateUserPermission(String currentUserName, CreateSystemUserDTO updateUserInfo) {
        LambdaQueryWrapper<SystemUserManagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemUserManagePO::getUserName, currentUserName);
        SystemUserManagePO currentPo = userDao.selectOne(queryWrapper);

        queryWrapper.clear();
        queryWrapper.eq(SystemUserManagePO::getUserName, updateUserInfo.getUserName());
        //原有用户
        SystemUserManagePO oldPo  = userDao.selectOne(queryWrapper);

        if (currentPo.getSystemIdentityId().equals(SystemRoleEnum.DEVELOPER.getSid())) {
            log.info("开发人员操作修改");
            return true;
        }
        if (currentPo.getGroupNo().equals(SystemGroupEnum.PRODUCTION_LINE_ADMIN.getGroupNo())) {
            log.info("产线管理员操作中");
            if (oldPo.getGroupNo().equals(SystemRoleEnum.PRODUCTION_LINE.getSid()) && updateUserInfo.getGroupNo().equals(SystemRoleEnum.PRODUCTION_LINE.getSid())) {

                log.info("被修改用户是产线人员且没有越权");
                return true;
            }
        }

        if (currentUserName.equals(updateUserInfo.getUserName())) {
            log.info("当前用户是修改用户自己");
            if (oldPo.getSystemIdentityId().equals(updateUserInfo.getSystemIdentityId()) && oldPo.getGroupNo().equals(updateUserInfo.getGroupNo())) {
                log.info("未涉及到自身组或角色信息");
                return true;
            }
        }
        return false;
    }

    @Override
    public String checkQueryPermission(String systemIdentityId, String groupId) {
        if (systemIdentityId.equals(SystemRoleEnum.DEVELOPER.getSid())) {
            log.info("开发人员可查全部");
            return Constants.GET_ALL_USER;
        }
        if (groupId.equals(SystemGroupEnum.PRODUCTION_LINE_ADMIN.getGroupNo())) {
            log.info("产线管理员可查所有产线人员");
            return Constants.GET_CURRENT_ROLE;
        }
        log.info("普通用户可查当前组");
        return Constants.GET_CURRENT_GROUP;
    }


}
