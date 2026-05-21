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
import java.util.Map;

import javax.validation.Valid;

import com.persee.workstation.uv_inexus.utils.OperateLog.LogManualUtil;
import org.apache.ibatis.annotations.Param;
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

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PasswordUpdateDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserDetailDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.utils.OperateLog.LogContext;
import com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/user")
@Api(tags = "用户管理")
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/")
    @ApiOperation("分页查询用户")
    public Result<PageView<SysUserPO>> getUserPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            UserQueryDTO queryDTO) {
        Page<SysUserPO> page = new Page<>(pageNum, pageSize);
        // System.out.println("222222222222222222"+page);
        // 直接返回封装后的 Result<PageView<SysUserPO>>
        return sysUserService.getUserPage(page, queryDTO); 
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询用户（含组、角色信息）")
    public Result<UserDetailDTO> getUserById(@PathVariable String id) { // 返回类型改为UserDetailDTO
        return sysUserService.getUserById(id);
    }

//    @ButtonPermission("catEditUserAddUser")
    @PostMapping("/addUser")
    @ApiOperation("新增用户")
    public Result<String> addUser(@RequestBody @Valid UserSaveDTO saveDTO) {
        // 执行新增
        Result<String> result = sysUserService.addUser(saveDTO);

        // 如果新增成功，记录日志
        if (result != null && result.getCode() != null && result.getCode() == 200) {
            String username = saveDTO.getUsername() != null ? saveDTO.getUsername() : "未命名用户";
            String realName = saveDTO.getRealName() != null ? saveDTO.getRealName() : "";
            String userDesc = realName.isEmpty() ? username : (username + "(" + realName + ")");
            LogManualUtil.saveAddLog(
                    "用户管理-用户权限管理",
                    "用户名：" + userDesc,
                    saveDTO
            );
        }

        return result;
    }

//    @ButtonPermission("catEditUserDeleteGroup")
    @PutMapping("/updateUser")
    @ApiOperation("修改用户")
    public Result<String> updateUser(@RequestBody @Valid UserSaveDTO saveDTO) {
        // 1. 查询【修改前】数据
        SysUserPO oldUser = sysUserService.getById(saveDTO.getId());

        // 2. 执行修改
        Result<String> result = sysUserService.updateUser(saveDTO);

        // 3. 如果修改成功，记录日志
        if (result != null && result.getCode() != null && result.getCode() == 200 && oldUser != null) {
            LogManualUtil.saveUpdateLog(
                    "用户管理-用户权限管理",
                    oldUser,
                    saveDTO,
                    saveDTO
            );
        }

        return result;
    }

    /**
     * 根据ID删除单个用户
     */
//    @ButtonPermission("catEditUserDeleteOrDisableUser")
    @DeleteMapping("/{id}")  // 使用HTTP DELETE方法，符合RESTful风格
    @ApiOperation("根据ID删除单个用户")
    public Result<String> deleteById(@PathVariable String id,
                                     @ApiParam(value = "操作人ID", required = true)
                                     @RequestParam String updateById) {
        // 1. 查询要删除的用户数据（用于日志记录）
        SysUserPO oldUser = sysUserService.getById(id);

        // 2. 执行删除
        Result<String> result = sysUserService.deleteById(id, updateById);

        // 3. 如果删除成功，记录日志
        if (result != null && result.getCode() != null && result.getCode() == 200 && oldUser != null) {
            String username = oldUser.getUsername() != null ? oldUser.getUsername() : "未命名用户";
            String realName = oldUser.getRealName() != null ? oldUser.getRealName() : "";
            String userDesc = realName.isEmpty() ? username : (username + "(" + realName + ")");
            LogManualUtil.saveDeleteLog(
                    "用户管理-用户权限管理",
                    "用户：" + userDesc,
                    oldUser,
                    id
            );
        }

        return result;
    }

    @PostMapping("/deleteBatch")
    @ApiOperation("批量删除用户")
    public Result<String> deleteBatch(@RequestBody List<String> ids) {
        return sysUserService.deleteBatch(ids);
    }

    @PostMapping("/updatePassword")
    @ApiOperation("修改密码")
    @OperateLog(module = "用户管理", description = "修改密码", recordParams = true)
    public Result<String> updatePassword(@RequestBody @Valid PasswordUpdateDTO updateDTO) {
        return sysUserService.updatePassword(updateDTO);
    }

    @PostMapping("/resetPassword")
    @ApiOperation("重置密码为默认值123456")
    @OperateLog(module = "用户管理", description = "重置用户密码", recordParams = true)
    public Result<String> resetPassword(@RequestParam String userId) {
        return sysUserService.resetPassword(userId);
    }

    @PostMapping("/addQuickNavigation")
    @ApiOperation("添加快捷导航")
    public Result<String> addQuickNavigation(@RequestBody Map<String, Object> quickNavigation) {
        if (sysUserService.addQuickNavigation(quickNavigation)) {
            return Result.ok("快捷导航添加成功");
        } else {
            return Result.fail("快捷导航添加失败");
        }
    }

    //查找快捷导航
    @GetMapping("/getQuickNavigation")
    @ApiOperation("查找快捷导航")
    public Result<JSONArray> getQuickNavigation(@Param("updateBy") String updateBy) {
        return sysUserService.getQuickNavigation(updateBy);
    }
}