package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.UserRoutePermissionService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/route-permission")
@Api(tags = "用户路由权限接口")
public class UserRoutePermissionController {

    @Autowired
    private UserRoutePermissionService userRoutePermissionService;

    @GetMapping("/user")
    @ApiOperation("根据用户ID查询有权访问的路由path列表")
    public Result<List<String>> getUserRoutePermissions(
            @ApiParam(value = "用户ID", required = true)
            @RequestParam String userId) {
        // 返回值改为 List<String>
        return userRoutePermissionService.getUserRoutePermissions(userId);
    }
}