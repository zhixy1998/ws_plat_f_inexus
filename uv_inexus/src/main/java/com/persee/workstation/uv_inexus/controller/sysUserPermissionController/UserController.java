/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.config.ButtonPermission;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PasswordUpdateDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserDetailDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

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
    @OperateLog(module = "用户管理-用户权限管理-新增用户", description = "执行用户管理-用户权限管理-新增用户操作", recordParams = true)
    @PostMapping("/addUser")
    @ApiOperation("新增用户")
    public Result<String> addUser(@RequestBody @Valid UserSaveDTO saveDTO) {
        return sysUserService.addUser(saveDTO);
    }

//    @ButtonPermission("catEditUserDeleteGroup")
    @OperateLog(module = "用户管理-用户权限管理-新增角色", description = "执行用户管理-用户权限管理-新增角色操作", recordParams = true)
    @PutMapping("/updateUser")
    @ApiOperation("修改用户")
    public Result<String> updateUser(@RequestBody @Valid UserSaveDTO saveDTO) {
        return sysUserService.updateUser(saveDTO);
    }

    /**
     * 根据ID删除单个用户
     */
//    @ButtonPermission("catEditUserDeleteOrDisableUser")
    @OperateLog(module = "用户管理-用户权限管理-删除用户", description = "执行用户管理-用户权限管理-删除用户操作", recordParams = true)
    @DeleteMapping("/{id}")  // 使用HTTP DELETE方法，符合RESTful风格
    @ApiOperation("根据ID删除单个用户")
    public Result<String> deleteById(@PathVariable String id, 
                                     @ApiParam(value = "操作人ID", required = true)
                                     @RequestParam String updateById) {
        // 调用Service的删除方法（需在Service中新增实现）
        return sysUserService.deleteById(id, updateById);
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