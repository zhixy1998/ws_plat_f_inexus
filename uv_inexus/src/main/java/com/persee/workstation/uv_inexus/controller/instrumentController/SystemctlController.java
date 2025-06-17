package com.persee.workstation.uv_inexus.controller.instrumentController;


import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.instrumentService.UserManagerService;
import com.persee.workstation.uv_inexus.vo.systemConfigVo.LoginUserInfoVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/system")
@ApiModel("系统配置")
@Slf4j
public class SystemctlController {

    @Autowired
    UserManagerService userManagerService;

    @PostMapping("/login")
    @ApiModelProperty("用户登录接口")
    public Result<LoginUserInfoVO> login(@RequestBody UserLoginDTO loginDTO){

        R<LoginUserInfoVO> login = userManagerService.login(loginDTO);
        if (login.getSuccess()) {
            log.info("登陆成功");
            return Result.ok(login.getData());
        } else {
            log.info("登陆失败");
            return Result.fail(login.getMessage());
        }
    }


}
