package com.persee.workstation.uv_inexus.service.systemUserManageService.factory;

import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.enums.systemUserManageEnums.SystemRoleEnum;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProductionLineFactory implements UserFactory{

    @Override
    public SystemUserManagePO createUser(CreateSystemUserDTO dto) {

        if (!dto.getSystemIdentityId().equals(SystemRoleEnum.PRODUCTION_LINE.getSid())) {
            System.out.println("用户角色出错");
            throw new IllegalArgumentException("用户角色出错");
        }
        if (!StringUtils.hasText(dto.getGroupNo()) || !StringUtils.hasText(dto.getGroupName())) {
            System.out.println("缺少用户组信息");
            throw new IllegalArgumentException("缺少用户组信息");
        }

        SystemUserManagePO user = new SystemUserManagePO();
        BeanUtils.copyProperties(dto, user);

        if (dto.getUserPermissionList() != null) {
            StringBuilder userPermissionList = new StringBuilder();
            dto.getUserPermissionList().forEach(permission -> userPermissionList.append(permission).append(","));
            user.setUserPermissionList(userPermissionList.toString());
        }

        user.setCreateBy(dto.getUpdateBy());

        return user;
    }
}
