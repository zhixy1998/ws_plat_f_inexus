/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import com.persee.workstation.uv_inexus.utils.OperateLog.LogManualUtil;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

import java.util.Map;

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
//    @OperateLog(module = "用户登录", description = "用户登录", recordParams = true, logType = 1)
//    @OperateLog(module = "用户登录", description = "用户登录", recordParams = true) // 默认logType = 2
    public Result<LoginUserInfoVO> login(@RequestBody UserLoginDTO loginDTO) {

        if (loginDTO.getIsFastLogin() != null && loginDTO.getIsFastLogin() == 1) {
            log.info("触发快捷登录，使用默认用户");

            loginDTO.setUserName("kh-admin");
            loginDTO.setPassword("123456");
        }

        log.info("接收登录请求，username={}", loginDTO.getUserName());
        // 调用Service层逻辑
        R<LoginUserInfoVO> loginResult = loginService.login(loginDTO);
        // 适配原有Result响应格式（R→Result）
        if (loginResult.getSuccess()) {
            log.info("登录成功，username={}", loginDTO.getUserName());
            LogManualUtil.saveLoginLog(
                    "用户登录成功：" + loginDTO.getUserName(),
                    loginDTO.getUserName(),
                    loginDTO
            );
            return Result.ok(loginResult.getData());
        } else {
            log.info("登录失败，username={}，原因：{}", loginDTO.getUserName(), loginResult.getMessage());
            return Result.fail(loginResult.getMessage());
        }
    }

    @PostMapping("/logout")
    @ApiOperation("用户登出接口")
// @OperateLog(module = "用户登出", description = "用户登出", recordParams = true)
    public Result<Boolean> loginOut(@RequestBody Map<String, String> params){

        String username = params.get("updateBy");

        LogManualUtil.saveLogoutLog(
                "用户登出成功",
                username,
                params
        );

        return Result.ok(true);
    }
}