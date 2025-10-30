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

package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserLoginService;
import com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户登录控制器
 */
@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/system")
@Api(tags = "用户登录接口") // Swagger标签（替代@ApiModel，更规范）
@Slf4j
@RequiredArgsConstructor
public class SysUserLoginController {

    private final SysUserLoginService loginService;

    /**
     * 用户登录接口（与原有接口路径、响应格式保持一致）
     * @param loginDTO 登录请求参数（需校验）
     * @return 登录结果（Result包装，兼容原有前端接收逻辑）
     */
    @PostMapping("/login")
    @ApiOperation("用户登录接口")
    @OperateLog(module = "用户登录", description = "用户登录", recordParams = true)
    public Result<LoginUserInfoVO> login(@RequestBody UserLoginDTO loginDTO) {

        if (loginDTO.getIsFastLogin() != null && loginDTO.getIsFastLogin() == 1) {
            log.info("触发快捷登录，使用默认用户");
            // 建议在Constants类中定义默认值，避免硬编码（示例：Constants.DEFAULT_USERNAME）
            loginDTO.setUserName("kh-admin");
            loginDTO.setPassword("123456");
        }

        log.info("接收登录请求，username={}", loginDTO.getUserName());
        // 调用Service层逻辑
        R<LoginUserInfoVO> loginResult = loginService.login(loginDTO);
        // 适配原有Result响应格式（R→Result）
        if (loginResult.getSuccess()) {
            log.info("登录成功，username={}", loginDTO.getUserName());
            return Result.ok(loginResult.getData());
        } else {
            log.info("登录失败，username={}，原因：{}", loginDTO.getUserName(), loginResult.getMessage());
            return Result.fail(loginResult.getMessage());
        }
    }

    @PostMapping("/logout")
    @ApiOperation("用户登出接口")
    @OperateLog(module = "用户登出", description = "用户登出", recordParams = true)
    public Result<Boolean> loginOut(){
        return Result.ok(true);
    }
}