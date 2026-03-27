/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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

import javax.servlet.http.HttpServletRequest;

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
            @RequestParam String userId, HttpServletRequest request) {
        // 返回值改为 List<String>
        return userRoutePermissionService.getUserRoutePermissions(userId, request);
    }
    
    @GetMapping("/menu-structure")
    @ApiOperation("获取所有菜单路由结构（perm_type=1）")
    public Result<List<?>> getMenuRouteStructure() {
        return userRoutePermissionService.getMenuRouteStructure();
    }
}