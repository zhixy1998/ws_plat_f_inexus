package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.PermissionService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/permission")
@Api(tags = "权限管理")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping("/tree/all")
    @ApiOperation("查询所有权限的树形结构")
    public Result<List<PermissionTreeDTO>> getAllPermissionTree() {
        return permissionService.getAllPermissionTree();
    }
}
