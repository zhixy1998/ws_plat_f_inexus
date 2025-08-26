package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.RoleGroupUserTreeService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/userTree")
@Api(tags = "角色-组-用户树形结构接口")
public class RoleGroupUserTreeController {

    @Autowired
    private RoleGroupUserTreeService roleGroupUserTreeService;

    // 原接口修改：添加userId请求参数
    @GetMapping("/role-group-user-tree")
    @ApiOperation("根据用户ID动态返回树形结构（SUPER_ADMIN返回所有角色，普通用户返回自身角色）")
    public Result<List<RoleTreeDTO>> getRoleGroupUserTree( 
            @ApiParam(value = "用户ID", required = true, example = "1001")
            @RequestParam String userId) { 
        return roleGroupUserTreeService.getRoleGroupUserTree(userId);
    }
}