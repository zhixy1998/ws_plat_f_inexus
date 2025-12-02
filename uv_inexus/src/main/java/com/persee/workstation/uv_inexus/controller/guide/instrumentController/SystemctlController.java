/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.instrumentController;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.guide.systemUserManageService.SystemUserManageService;
import com.persee.workstation.uv_inexus.util.changeUtil.MessageUtil;
import com.persee.workstation.uv_inexus.vo.userManageVo.LoginUserInfoVO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/system1")
@ApiModel("系统配置")
@Slf4j
public class SystemctlController {

    @Autowired
    SystemUserManageService userManageService;

    @Autowired
    MessageUtil messageUtil;

    @PostMapping("/login")
    @ApiOperation("用户登录接口")
    public Result<LoginUserInfoVO> login(@RequestBody UserLoginDTO loginDTO){

        R<LoginUserInfoVO> login = userManageService.login(loginDTO);
        if (login.getSuccess()) {
            log.info("登陆成功");
            return Result.ok(login.getData());
        } else {
            log.info("登陆失败");
            return Result.fail(login.getMessage());
        }
    }

    @PostMapping("/logout")
    @ApiOperation("用户登出接口")
    public Result<Boolean> loginOut(){
        return Result.ok(true);
    }



}
