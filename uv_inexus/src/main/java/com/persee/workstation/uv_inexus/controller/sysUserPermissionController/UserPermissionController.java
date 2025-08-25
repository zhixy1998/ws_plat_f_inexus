package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.UserPermissionService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/permission")
@Api(tags = "用户权限接口")
public class UserPermissionController {

    @Autowired
    private UserPermissionService userPermissionService;

    @GetMapping("/user")
    @ApiOperation("根据用户ID查询所有权限")
    public Result<List<PermissionDTO>> getUserPermissions(
            @ApiParam(value = "用户ID", required = true)
            @RequestParam String userId) {
        return userPermissionService.getUserPermissions(userId);
    }

    @GetMapping("/role-path")
    @ApiOperation("根据用户ID查询parent_id为0的权限path列表")
    public Result<List<String>> getRootPermissionPaths( // 返回类型改为 List<String>
            @ApiParam(value = "用户ID", required = true)
            @RequestParam String userId) {
        // 调用Service层，直接返回path列表
        return userPermissionService.getRootPermissionPaths(userId);
    }
}