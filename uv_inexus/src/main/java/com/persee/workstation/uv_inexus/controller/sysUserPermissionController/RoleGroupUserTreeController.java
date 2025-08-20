package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.RoleGroupUserTreeService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/userTree")
@Api(tags = "角色-组-用户树形结构接口")
public class RoleGroupUserTreeController {

    @Autowired
    private RoleGroupUserTreeService roleGroupUserTreeService;

    @GetMapping("/role-group-user-tree")
    @ApiOperation("获取角色-组-用户树形结构")
    public Result<List<RoleTreeDTO>> getRoleGroupUserTree() {
        return roleGroupUserTreeService.getRoleGroupUserTree();
    }
}