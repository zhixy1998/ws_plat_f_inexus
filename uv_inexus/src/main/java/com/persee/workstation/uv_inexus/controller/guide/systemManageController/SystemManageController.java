package com.persee.workstation.uv_inexus.controller.guide.systemManageController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.persee.workstation.uv_inexus.dto.systemManageDto.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemLogSwitchPO;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemManagerConf;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.*;
import com.persee.workstation.uv_inexus.vo.systemManageVo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/systemManage")
@Api("系统管理接口")
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

    @Autowired
    SystemManageService systemManageService;

    @ApiOperation("修改系统管理配置")
    @PostMapping("/changeSystemConfig")
    public Result<SystemManagerConf> changeSystemManagerConf(@RequestBody SystemManagerConf systemManagerConf) throws JsonProcessingException {
        return systemManageService.changeSystemManagerConf(systemManagerConf);
    }

    @ApiOperation("获取系统管理配置")
    @PostMapping("/getSystemConfig")
    public Result<SystemManagerConf> getSystemManagerConf() throws JsonProcessingException {
        return systemManageService.getSystemManagerConf();
    }
//
//    @ApiOperation("认证物联代理配置信息")
//    @PostMapping("/setConfiguration")
//    public Result<Boolean> setConfiguration(@RequestBody ConfigVO configVO) throws JsonProcessingException {
//        return systemManageService.setConfiguration(configVO);
//    }
//
//    @ApiOperation("远程访问配置")
//    @PostMapping("/longDistanceConfig")
//    public Result<Boolean> longDistanceConfig(@RequestBody LongDistanceConfigVO longDistanceConfigVO) {
//        return systemManageService.longDistanceConfig(longDistanceConfigVO);
//    }


    @ApiOperation(value = "保存系统管理")
    @PostMapping("/saveSystemManage")
    public Result<Boolean> saveSystemManage(@RequestBody SystemManageDTO dto) {
        Boolean b = systemManageService.saveSystemManage(dto);
        if (b) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "获取系统管理")
    @GetMapping("/getSystemManage")
    public Result<SystemManageVO> getSystemManage() {
        SystemManageVO vo = systemManageService.getSystemManage();
        if (vo != null) {
            return Result.ok(vo);
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "系统管理配置恢复出厂设置")
    @GetMapping("/resetSystemManage")
    public Result<Boolean> resetSystemManage(@RequestParam String updateBy) {
        Boolean flag = systemManageService.resetSystemManage(updateBy);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

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
    @GetMapping("/resetDatabaseConf")
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
    @GetMapping("/resetPanelConf")
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

    @ApiOperation("日志管理恢复出厂设置")
    @GetMapping("/resetLogSwitch")
    public Result<Boolean> resetLogSwitch(@RequestParam String updateBy) {
        Boolean flag = systemLogSwitchService.resetLogSwitch(updateBy);
        if (flag) {
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
    @GetMapping("/resetBusinessParam")
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

    @ApiOperation(value = "算法参数恢复出厂设置")
    @GetMapping("/resetAlgorithmConf")
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


    @ApiOperation(value = "查询面板顺序")
    @GetMapping("/queryPanelOrder")
    public Result<List<String>> queryPanelOrder() {
        List<String> list = systemPanelConfService.queryPanelOrder();
        return Result.ok(list);
    }

    @ApiOperation(value = "保存面板顺序", notes = "以数组顺序代表面板顺序，method-方法面板，project-项目面板，task-任务面板")
    @PostMapping("/savePanelOrder")
    public Result<Boolean> savePanelOrder(@RequestBody SystemPanelOrderDTO dto) {
        R<Boolean> flag = systemPanelConfService.savePanelOrder(dto);
        if (flag.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(flag.getMessage());
        }
    }

}
