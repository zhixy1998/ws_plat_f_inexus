package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
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
    /**
     * 新增：按权限类型筛选+角色权限标记，返回权限树形结构
     * @param permType 权限类型（1-菜单路由，2-操作权限，3-数据权限，为空查全部）
     * @param roleId 角色ID（用于标记该角色是否拥有权限）
     * @return 权限树形列表（根节点为parent_id=0的权限）
     */
    @GetMapping("/tree")
    @ApiOperation("根据权限类型和角色ID查询树形权限结构")
    public Result<List<PermissionTreeDTO>> getPermissionTree(
            @ApiParam(value = "权限类型：1-菜单路由权限，2-操作权限，3-数据权限，多个用逗号分隔，如1,2,3，null表示所有类型")
            @RequestParam(required = false) String permType,  // 修改为String类型接收多个值
            @ApiParam(value = "角色ID", required = true)
            @RequestParam String userId) {
        // 将字符串转换为Integer列表
        List<Integer> permTypeList = null;
        if (permType != null && !permType.isEmpty()) {
            permTypeList = Arrays.stream(permType.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }
        return userPermissionService.getPermissionTreeByTypeAndUser(permTypeList, userId);
    }

    @PostMapping("/update-group-perm")
    @ApiOperation("更新小组权限关系")
    public Result<Boolean> updateGroupPermissions(
            @ApiParam(value = "小组ID", required = true) @RequestParam String groupId,
            @ApiParam(value = "权限ID列表（逗号分隔）", required = true) @RequestParam String permIds) {
        // 分割字符串为List
        List<String> permIdList = permIds.split(",").length > 0 
                ? Arrays.asList(permIds.split(",")) 
                : new ArrayList<>();
        return userPermissionService.updateGroupPermissions(groupId, permIdList);
    }
}