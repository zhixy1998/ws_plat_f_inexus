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
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/user")
@Api(tags = "用户管理")
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    // UserController.java
    @GetMapping("/")
    @ApiOperation("分页查询用户")
    public Result<PageView<SysUserPO>> getUserPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            UserQueryDTO queryDTO) {
        Page<SysUserPO> page = new Page<>(pageNum, pageSize);
        System.out.println("222222222222222222"+page);
        // 直接返回封装后的 Result<PageView<SysUserPO>>
        return sysUserService.getUserPage(page, queryDTO); 
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询用户")
    public Result<SysUserPO> getUserById(@PathVariable String id) {
        return sysUserService.getUserById(id);
    }

    @PostMapping("/addUser")
    @ApiOperation("新增用户")
    public Result<String> addUser(@RequestBody @Valid UserSaveDTO saveDTO) {
        return sysUserService.addUser(saveDTO);
    }

    @PutMapping("/updateUser")
    @ApiOperation("修改用户")
    public Result<String> updateUser(@RequestBody @Valid UserSaveDTO saveDTO) {
        return sysUserService.updateUser(saveDTO);
    }

    /**
     * 根据ID删除单个用户
     */
    @DeleteMapping("/{id}")  // 使用HTTP DELETE方法，符合RESTful风格
    @ApiOperation("根据ID删除单个用户")
    public Result<String> deleteById(@PathVariable String id) {
        // 调用Service的删除方法（需在Service中新增实现）
        return sysUserService.deleteById(id);
    }

    @PostMapping("/deleteBatch")
    @ApiOperation("批量删除用户")
    public Result<String> deleteBatch(@RequestBody List<String> ids) {
        return sysUserService.deleteBatch(ids);
    }
}