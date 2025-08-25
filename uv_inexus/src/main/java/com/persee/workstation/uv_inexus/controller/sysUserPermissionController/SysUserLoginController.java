package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result; // 原有统一响应类（与R对应）
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserLoginService;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户登录控制器
 */
@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/system1")
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
    public Result<LoginUserInfoVO> login(@Validated @RequestBody UserLoginDTO loginDTO) {
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
}