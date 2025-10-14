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

package com.persee.workstation.uv_inexus.controller.guide.systemManageController;


import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemRolePO;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemRoleService;
import io.swagger.annotations.ApiModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiModel("系统全局配置接口，开发人员可见的相关接口")
@RequestMapping("/com/persee/workstation/uv-inexus/sysGlobal")
public class SystemGlobalConfController {
    @Autowired
    private SystemRoleService roleService;

    @PostMapping("/addSystemRole")
    public void addSystemRole(@RequestBody SystemRolePO systemRolePO){

        roleService.addSystemRole(systemRolePO);
    }
}
