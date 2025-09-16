package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemDataBaseConfDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemDatabaseConfPO;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.impl.SystemctlDatabaseConfServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/system/conf/")
public class SystemctlDatabaseConfController {
    @Autowired
    private SystemctlDatabaseConfServiceImpl systemctlDatabaseConfServiceImpl;

    @PostMapping("update")
    public Result<SystemDatabaseConfPO> updateSystemDatabaseConf(@RequestBody SystemDataBaseConfDTO systemDataBaseConfDTO) {
       return systemctlDatabaseConfServiceImpl.updateSystemDatabaseConf(systemDataBaseConfDTO);
    }
}
