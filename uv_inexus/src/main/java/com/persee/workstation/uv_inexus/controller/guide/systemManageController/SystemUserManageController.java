package com.persee.workstation.uv_inexus.controller.guide.systemManageController;

import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.guide.systemUserManageService.SystemUserManageService;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.QueryUserVO;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.SystemUserGroupInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/userManage")
@Api("系统用户管理接口")
public class SystemUserManageController {

    @Autowired
    SystemUserManageService userManageService;

    @PostMapping("/createNewUser")
    @ApiOperation("添加用户")
    public Result<Boolean> createNewUser(@RequestBody CreateSystemUserDTO dto){

        R<Boolean> user = userManageService.createUser(dto);
        if (user.getSuccess()) {
            return Result.ok(user.getData());
        } else {
            return Result.fail(user.getMessage());
        }
    }

    @PostMapping("/updateSystemUser")
    @ApiOperation("修改用户信息")
    public Result<Boolean> updateSystemUser(@RequestBody CreateSystemUserDTO dto){

        R<Boolean> user = userManageService.updateUser(dto);
        if (user.getSuccess()) {
            return Result.ok(user.getData());
        } else {
            return Result.fail(user.getMessage());
        }
    }

    @GetMapping("/deleteSystemUser")
    @ApiOperation("删除用户")
    public Result<Boolean> deleteSystemUser(@RequestParam String updateBy, @RequestParam String deleteUserName){

        R<Boolean> user = userManageService.deleteUser(updateBy, deleteUserName);
        if (user.getSuccess()) {
            return Result.ok(user.getData());
        } else {
            return Result.fail(user.getMessage());
        }
    }

//    @GetMapping("/getUserInfo")
//    @ApiOperation("获取用户信息")
//    public Result<List<SystemUserInfoVO>> getUserInfo(@RequestParam String updateBy){
//
//        R<List<SystemUserInfoVO>> userInfo = userManageService.getUserInfo(updateBy);
//        if(userInfo.getSuccess()) {
//            return Result.ok(userInfo.getData());
//        } else {
//            return Result.fail(userInfo.getMessage());
//        }
//    }

    @GetMapping("/getUserInfo")
    @ApiOperation("获取用户信息")
    public Result<List<QueryUserVO>> getUserInfo(@RequestParam String updateBy){

        R<List<QueryUserVO>> userInfo = userManageService.queryUserInfo(updateBy);
        if(userInfo.getSuccess()) {
            return Result.ok(userInfo.getData());
        } else {
            return Result.fail(userInfo.getMessage());
        }
    }

    @GetMapping("/getGroupInfo")
    @ApiOperation("获取用户组信息")
    public Result<List<SystemUserGroupInfo>> getGroupInfo(@RequestParam String updateBy){

        R<List<SystemUserGroupInfo>> userInfo = userManageService.getGroupInfo(updateBy);
        if(userInfo.getSuccess()) {
            return Result.ok(userInfo.getData());
        } else {
            return Result.fail(userInfo.getMessage());
        }
    }

}
