package com.persee.workstation.uv_inexus.service.systemUserManageService.factory;

import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.enums.systemUserManageEnums.SystemRoleEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserFactoryRoute {

    public R<SystemUserManagePO> createUser(CreateSystemUserDTO dto) {

        if (!StringUtils.hasText(dto.getSystemIdentityId())) {
            System.out.println("缺少用户角色");
            return R.fail("缺少用户角色");
        }

        SystemUserManagePO po;
        try {
            if (dto.getSystemIdentityId().equals(SystemRoleEnum.DEVELOPER.getSid())) {
                po = new DeveloperFactory().createUser(dto);
                return R.ok(po);
            } else if (dto.getSystemIdentityId().equals(SystemRoleEnum.PRODUCTION_LINE.getSid())) {
                po = new ProductionLineFactory().createUser(dto);
                return R.ok(po);
            } else if (dto.getSystemIdentityId().equals(SystemRoleEnum.REPAIR_PERSON.getSid())) {
                po = new RepairLineFactory().createUser(dto);
                return R.ok(po);
            } else if (dto.getSystemIdentityId().equals(SystemRoleEnum.CUSTOMER.getSid())) {
                po = new CustomerFactory().createUser(dto);
                return R.ok(po);
            } else {
                return R.fail("用户角色出错");
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
