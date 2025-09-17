package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import com.persee.workstation.uv_inexus.dto.systemManageDto.InitSystemConf;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.impl.SystemctlDatabaseConfServiceImpl;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
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
    @ApiOperation("更改系统配置")
    public Result<InitSystemConf> updateSystemDatabaseConf(@RequestBody InitSystemConf initSystemConf) {
       return systemctlDatabaseConfServiceImpl.updateSystemDatabaseConf(initSystemConf);
    }

    @PostMapping("initConf")
    @ApiOperation("获取系统初始化配置")
    public Result<InitSystemConf> initSystemConf() {
        return systemctlDatabaseConfServiceImpl.initSystemConf();
    }
}
