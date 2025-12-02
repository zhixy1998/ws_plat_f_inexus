/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
