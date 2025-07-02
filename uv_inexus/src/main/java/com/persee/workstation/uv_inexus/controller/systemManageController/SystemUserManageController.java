package com.persee.workstation.uv_inexus.controller.systemManageController;

import com.persee.workstation.uv_inexus.service.systemUserManageService.SystemUserManageService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/userManage")
@Api("系统用户管理接口")
public class SystemUserManageController {

    @Autowired
    SystemUserManageService UserManageService;



}
