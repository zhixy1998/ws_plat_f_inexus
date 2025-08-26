package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleSaveDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysRoleService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/role")
@Api(tags = "角色管理")
public class RoleController {

    @Autowired
    private SysRoleService sysRoleService;

    // 分页查询：返回 Result<PageView>
    @GetMapping("/")
    @ApiOperation("分页查询角色")
    public Result<PageView<SysRolePO>> getRolePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            RoleQueryDTO queryDTO) {
        Page<SysRolePO> page = new Page<>(pageNum, pageSize);
        return sysRoleService.getRolePage(page, queryDTO);  // 直接返回 Service 的 Result 结果
    }

    // 其他接口保持不变（已返回 Result 格式）
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询角色")
    public Result<SysRolePO> getRoleById(@PathVariable String id) {
        return sysRoleService.getRoleById(id);
    }

    @PostMapping("/addRole")
    @ApiOperation("新增角色")
    public Result<String> addRole(@RequestBody @Valid RoleSaveDTO saveDTO) {
        return sysRoleService.addRole(saveDTO);
    }

    @PutMapping("/updateRole")
    @ApiOperation("修改角色")
    public Result<String> updateRole(@RequestBody @Valid RoleSaveDTO saveDTO) {
        return sysRoleService.updateRole(saveDTO);
    }

    @PostMapping("/deleteBatch")
    @ApiOperation("批量删除角色")
    public Result<String> deleteBatch(@RequestBody List<String> ids) {
        return sysRoleService.deleteBatch(ids);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("根据ID删除单个角色")
    public Result<String> deleteById(@PathVariable String id) {
        return sysRoleService.deleteById(id);
    }
    
    @GetMapping("/user-role-tree")
    @ApiOperation("根据用户ID查询角色及下属组和用户树形结构")
    public Result<RoleTreeDTO> getUserRoleGroupTree(
            @ApiParam(value = "用户ID", required = true) 
            @RequestParam String userId) {
        return sysRoleService.getUserRoleGroupTree(userId);
    }

    // 新增接口：根据角色ID查询下属所有组信息
    @GetMapping("/groups")
    @ApiOperation("根据角色ID查询下属所有启用组")
    @ApiParam(name = "id", value = "角色ID", required = true, example = "1001")
    public Result<List<SysGroupPO>> getRoleGroups(
            @RequestParam(required = true) String id // 角色ID，参数名与你现有接口保持一致（如/getRoleById的id）
    ) {
        // 直接调用Service层方法，返回结果（与现有接口风格一致）
        return sysRoleService.getGroupsByRoleId(id);
    }
}