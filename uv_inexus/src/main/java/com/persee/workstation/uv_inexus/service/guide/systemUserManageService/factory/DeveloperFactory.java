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

package com.persee.workstation.uv_inexus.service.guide.systemUserManageService.factory;

import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.enums.systemUserManageEnums.SystemGroupEnum;
import com.persee.workstation.uv_inexus.enums.systemUserManageEnums.SystemRoleEnum;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DeveloperFactory implements UserFactory{

    @Override
    public SystemUserManagePO createUser(CreateSystemUserDTO dto) {
        if (!dto.getSystemIdentityId().equals(SystemRoleEnum.DEVELOPER.getSid())) {
            System.out.println("用户角色出错");
            throw new IllegalArgumentException("用户角色出错");
        }
        if (!StringUtils.hasText(dto.getGroupNo())) {
            dto.setGroupNo(SystemGroupEnum.DEV_USER.getGroupNo());
        }
        dto.setGroupName(SystemGroupEnum.DEV_USER.getName());

        SystemUserManagePO user = new SystemUserManagePO();
        BeanUtils.copyProperties(dto, user);
        user.setGroupName(SystemGroupEnum.getName(dto.getGroupNo()));


        if (dto.getUserPermissionList() != null) {
            StringBuilder userPermissionList = new StringBuilder();
            dto.getUserPermissionList().forEach(permission -> userPermissionList.append(permission).append(","));
            user.setUserPermissionList(userPermissionList.toString());
        }


        user.setCreateBy(dto.getUpdateBy());

        return user;
    }
}
