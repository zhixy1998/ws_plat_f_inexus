package com.persee.workstation.uv_inexus.controller.systemManageController;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemAlgorithmConfDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemBusinessParamDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemManagePo.SystemDatabaseConfPO;
import com.persee.workstation.uv_inexus.po.systemManagePo.SystemLogSwitchPO;
import com.persee.workstation.uv_inexus.service.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.service.systemManageService.SystemBusinessParamService;
import com.persee.workstation.uv_inexus.service.systemManageService.SystemLogSwitchService;
import com.persee.workstation.uv_inexus.service.systemManageService.SystemctlDatabaseConfService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemBusinessParamVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemDataBaseVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemLogSwitchVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/systemManage")
@ApiModel("系统管理接口")
public class SystemManageController {

    @Autowired
    SystemctlDatabaseConfService databaseConfService;

    @Autowired
    SystemLogSwitchService systemLogSwitchService;

    @Autowired
    SystemBusinessParamService systemBusinessParamService;

    @Autowired
    SystemAlgorithmConfService systemAlgorithmConfService;

    @ApiOperation(value = "保存数据库配置")
    @PostMapping("/saveDatabaseConf")
    public Result<Boolean> saveDatabaseConf(@RequestBody SystemDatabaseConfPO systemDatabaseConfPO) {
        Boolean b = databaseConfService.saveDatabaseConf(systemDatabaseConfPO);
        if (b) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "查询数据库配置")
    @GetMapping("/queryDatabaseConf")
    public Result<SystemDataBaseVO> queryDatabaseConf() {
        SystemDataBaseVO vo = databaseConfService.queryDatabaseConf();
        if (vo != null) {
            return Result.ok(vo);
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "查询日志开关")
    @GetMapping("/querySystemLogSwitch")
    public Result<SystemLogSwitchVO> querySystemLogSwitch() {
        SystemLogSwitchVO vo = systemLogSwitchService.querySystemLogSwitch();
        if (vo != null) {
            return Result.ok(vo);
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "修改日志开关")
    @PostMapping("/changeSystemLogSwitch")
    public Result<Boolean> changeSystemLogSwitch(@RequestBody SystemLogSwitchPO systemLogSwitchPO) {
        Boolean b = systemLogSwitchService.changeSwitch(systemLogSwitchPO);
        if (b) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "保存业务参数")
    @PostMapping("/saveBusinessParam")
    public Result<Boolean> saveBusinessParam(@RequestBody List<SystemBusinessParamDTO> list) {
        Boolean b = systemBusinessParamService.saveBusinessParam(list);
        if (b) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "查询业务参数")
    @GetMapping("/queryBusinessParam")
    public Result<List<SystemBusinessParamVO>> queryBusinessParam() {
        List<SystemBusinessParamVO> vo = systemBusinessParamService.queryBusinessParam();
        if (vo != null) {
            return Result.ok(vo);
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "业务参数恢复出厂设置")
    @PostMapping("/resetBusinessParam")
    public Result<Boolean> resetBusinessParam() {
        Boolean flag = systemBusinessParamService.resetBusinessParam();
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "算法参数保存")
    @PostMapping("/saveAlgorithmConf")
    public Result<Boolean> saveAlgorithmConf(@RequestBody List<SystemAlgorithmConfDTO> list) {
        Boolean flag = systemAlgorithmConfService.saveAlgorithmConf(list);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "算法参数重置")
    @PostMapping("/resetAlgorithmConf")
    public Result<Boolean> resetAlgorithmConf() {
        Boolean flag = systemAlgorithmConfService.resetAlgorithmConf();
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }


    @ApiOperation(value = "查询算法配置参数")
    @GetMapping("/queryAlgorithmConf")
    public Result<List<SystemAlgorithmConfVO>> queryAlgorithmConf() {
        List<SystemAlgorithmConfVO> list = systemAlgorithmConfService.queryAlgorithmConf();

        return Result.ok(list);
    }


}
