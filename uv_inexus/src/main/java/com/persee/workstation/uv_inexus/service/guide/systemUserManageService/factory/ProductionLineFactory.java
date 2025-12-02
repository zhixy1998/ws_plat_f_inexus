/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
public class ProductionLineFactory implements UserFactory{

    @Override
    public SystemUserManagePO createUser(CreateSystemUserDTO dto) {

        if (!dto.getSystemIdentityId().equals(SystemRoleEnum.PRODUCTION_LINE.getSid())) {
            System.out.println("用户角色出错");
            throw new IllegalArgumentException("用户角色出错");
        }
        if (!StringUtils.hasText(dto.getGroupNo())) {
            System.out.println("缺少用户组信息");
            throw new IllegalArgumentException("缺少用户组信息");
        }

        SystemUserManagePO user = new SystemUserManagePO();
        BeanUtils.copyProperties(dto, user);
        user.setGroupName(SystemGroupEnum.getName(dto.getGroupNo()));


//        if (!StringUtils.hasText(dto.getGroupNo()) || !StringUtils.hasText(dto.getGroupName())) {
//            System.out.println("缺少用户组信息");
//            throw new IllegalArgumentException("缺少用户组信息");
//        }

        if (dto.getUserPermissionList() != null) {
            StringBuilder userPermissionList = new StringBuilder();
            dto.getUserPermissionList().forEach(permission -> userPermissionList.append(permission).append(","));
            user.setUserPermissionList(userPermissionList.toString());
        }

        user.setCreateBy(dto.getUpdateBy());

        return user;
    }
}
