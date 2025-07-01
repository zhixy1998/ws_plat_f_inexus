package com.persee.workstation.uv_inexus.controller.systemManageController;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemAlgorithmConfDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemBusinessParamDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemDataBaseConfDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemPanelConfDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemManagePo.SystemLogSwitchPO;
import com.persee.workstation.uv_inexus.service.systemManageService.*;
import com.persee.workstation.uv_inexus.vo.systemManageVo.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    SystemPanelConfService systemPanelConfService;

    @ApiOperation(value = "保存数据库配置")
    @PostMapping("/saveDatabaseConf")
    public Result<Boolean> saveDatabaseConf(@RequestBody SystemDataBaseConfDTO dto) {
        Boolean b = databaseConfService.saveDatabaseConf(dto);
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

    @ApiOperation(value = "数据库配置恢复出厂设置")
    @PostMapping("/resetDatabaseConf")
    public Result<Boolean> resetDatabaseConf(@RequestParam String updateBy) {
        Boolean flag = databaseConfService.resetDatabaseConf(updateBy);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "快捷登录开关保存")
    @PostMapping("/changeFastLoginSwitch")
    public Result<Boolean> changeFastLoginSwitch(@RequestParam Boolean switchStatus, @RequestParam String updateBy) {
        Boolean flag = databaseConfService.changeFastLoginSwitch(switchStatus, updateBy);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "保存系统面板配置")
    @PostMapping("/savePanelConf")
    public Result<Boolean> savePanelConf(@RequestBody SystemPanelConfDTO dto) {
        Boolean flag = systemPanelConfService.savePanelConf(dto);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "查询系统面板配置")
    @GetMapping("/queryPanelConf")
    public Result<SystemPanelConfVO> queryPanelConf() {
        SystemPanelConfVO vo = systemPanelConfService.queryPanelConf();
        if (vo != null) {
            return Result.ok(vo);
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "系统面板配置恢复出厂设置")
    @PostMapping("/resetPanelConf")
    public Result<Boolean> resetPanelConf(@RequestParam String updateBy) {
        Boolean flag = systemPanelConfService.resetPanelConf(updateBy);
        if (flag) {
            return Result.ok();
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
    public Result<Boolean> saveBusinessParam(@RequestBody SystemBusinessParamDTO list) {
        Boolean b = systemBusinessParamService.saveBusinessParam(list);
        if (b) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "查询业务参数")
    @GetMapping("/queryBusinessParam")
    public Result<SystemBusinessParamVO> queryBusinessParam() {
        SystemBusinessParamVO vo = systemBusinessParamService.queryBusinessParam();
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
    public Result<Boolean> saveAlgorithmConf(@RequestBody SystemAlgorithmConfDTO dto) {
        Boolean flag = systemAlgorithmConfService.saveAlgorithmConf(dto);
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
    public Result<SystemAlgorithmConfVO> queryAlgorithmConf() {
        SystemAlgorithmConfVO list = systemAlgorithmConfService.queryAlgorithmConf();

        return Result.ok(list);
    }


}
