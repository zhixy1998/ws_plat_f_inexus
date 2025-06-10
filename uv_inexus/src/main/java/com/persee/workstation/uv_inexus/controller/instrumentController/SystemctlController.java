package com.persee.workstation.uv_inexus.controller.instrumentController;


import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.instrumentService.UserManagerService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/system")
@ApiModel("系统配置")
public class SystemctlController {

    @Autowired
    UserManagerService userManagerService;

    @PostMapping("/login")
    @ApiModelProperty("用户登录接口")
    public Result<String> login(@RequestBody UserLoginDTO loginDTO){

        userManagerService.login(loginDTO);
        System.out.println("1111");
        return Result.ok("成功");
    }


}
