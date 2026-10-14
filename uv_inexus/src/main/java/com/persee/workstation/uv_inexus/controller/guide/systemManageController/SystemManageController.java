/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.systemManageController;

import com.persee.workstation.uv_inexus.dto.systemManageDto.*;
import com.persee.workstation.uv_inexus.i18n.LocaleProvider;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemLogSwitchPO;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.*;
import com.persee.workstation.uv_inexus.vo.systemManageVo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
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
    @Autowired
    private LocaleProvider localeProvider;
    @Autowired
    private SystemctlDatabaseConfService systemctlDatabaseConfService;

    @ApiOperation(value = "系统管理首页--保存系统管理")
    @PostMapping("/saveSystemManage")
    public Result<Boolean> saveSystemManage(@RequestBody SystemManageDTO dto) {
        Boolean b = systemManageService.saveSystemManage(dto);
        if (b) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "登录页面--修改系统语言")
    @GetMapping("/changeLanguage")
    public Result<Boolean> changeLanguage(@RequestParam String language) {
        Boolean b = systemManageService.changeLanguage(language);
        if (b) {
            localeProvider.evictCache();
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "登录页面--查询系统语言")
    @GetMapping("/queryLanguage")
    public Result<String> queryLanguage() {
        String language = systemManageService.queryLanguage();
        if (StringUtils.hasText(language)) {
            return Result.ok(language);
        } else {
            return Result.ok("00C01005");
        }
    }

    @ApiOperation(value = "系统管理首页--获取系统管理")
    @GetMapping("/getSystemManage")
    public Result<SystemManageVO> getSystemManage() {
        SystemManageVO vo = systemManageService.getSystemManage();
        if (vo != null) {
            return Result.ok(vo);
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "系统管理首页--系统管理配置恢复出厂设置")
    @GetMapping("/resetSystemManage")
    public Result<Boolean> resetSystemManage(@RequestParam String updateBy) {
        Boolean flag = systemManageService.resetSystemManage(updateBy);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "数据库配置--保存数据库配置-维修人员")
    @PostMapping("/saveDatabaseConf")
    public Result<Boolean> saveDatabaseConf(@RequestBody SystemDataBaseConfDTO dto) {
        Boolean b = databaseConfService.saveDatabaseConf(dto);
        if (b) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "系统管理配置--保存系统配置-用户")
    @PostMapping("/saveUserDatabaseConf")
    public Result<Boolean> saveUserDatabaseConf(@RequestBody SystemCustomManageDTO dto) {
        Boolean b = databaseConfService.saveCustomDatabaseConf(dto);
        if (b) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "系统管理配置--查询系统管理配置")
    @GetMapping("/querySysManageUserConf")
    public Result<SystemManageUserConfVO> querySysManageUserConf(@RequestParam String updateBy) {
        SystemManageUserConfVO vo = databaseConfService.querySysManageUserConf(updateBy);
        if (vo != null) {
            return Result.ok(vo);
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "数据库配置--查询数据库配置")
    @GetMapping("/queryDatabaseConf")
    public Result<SystemDataBaseVO> queryDatabaseConf(@RequestParam String updateBy) {
        SystemDataBaseVO vo = databaseConfService.queryDatabaseConf(updateBy);
        if (vo != null) {
            return Result.ok(vo);
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "数据库配置和系统管理配置--数据库配置恢复出厂设置")
    @GetMapping("/resetDatabaseConf")
    public Result<Boolean> resetDatabaseConf(@RequestParam String updateBy) {
        Boolean flag = databaseConfService.resetDatabaseConf(updateBy);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "登录成功后--配置登录页面开关")
    @PostMapping("/manageLoginSwitch")
    public Result<Boolean> manageLoginSwitch(@RequestBody SystemLoginConfDTO dto) {
        R<Boolean> r = systemPanelConfService.changeLoginInfo(dto);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "登录页面--查询登录页面开关")
    @GetMapping("/queryLoginSwitch")
    public Result<SystemLoginConfVO> queryLoginSwitch() {
        R<SystemLoginConfVO> r = systemPanelConfService.queryLoginInfo();
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail();
        }

    }

    @ApiOperation(value = "登录成功后--查询默认进入经典模式开关")
    @GetMapping("/queryDefaultMeasureSwitch")
    public Result<Boolean> queryDefaultMeasureSwitch() {
        Boolean defaultClassicSwitch = systemPanelConfService.getDefaultClassicSwitch();
        return Result.ok(defaultClassicSwitch);
    }

    @ApiOperation(value = "登录成功后--查询默认进入经典模式开关")
    @GetMapping("/queryDefaultEnterExperiment")
    public Result<List<String>> queryDefaultEnterExperiment() {
        List<String> defaultClassicSwitch = systemPanelConfService.getDefaultEnterExperiment();
        return Result.ok(defaultClassicSwitch);
    }

    @ApiOperation(value = "系统面板配置--保存系统面板配置")
    @PostMapping("/savePanelConf")
    public Result<Boolean> savePanelConf(@RequestBody SystemPanelConfDTO dto) {
        Boolean flag = systemPanelConfService.savePanelConf(dto);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "系统面板配置--查询系统面板配置")
    @GetMapping("/queryPanelConf")
    public Result<SystemPanelConfVO> queryPanelConf() {
        SystemPanelConfVO vo = systemPanelConfService.queryPanelConf();
        if (vo != null) {
            return Result.ok(vo);
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "系统面板配置--系统面板配置恢复出厂设置")
    @GetMapping("/resetPanelConf")
    public Result<Boolean> resetPanelConf(@RequestParam String updateBy) {
        Boolean flag = systemPanelConfService.resetPanelConf(updateBy);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "日志管理--查询日志开关")
    @GetMapping("/querySystemLogSwitch")
    public Result<SystemLogSwitchVO> querySystemLogSwitch() {
        SystemLogSwitchVO vo = systemLogSwitchService.querySystemLogSwitch();
        if (vo != null) {
            return Result.ok(vo);
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "日志管理--修改日志开关")
    @PostMapping("/changeSystemLogSwitch")
    public Result<Boolean> changeSystemLogSwitch(@RequestBody SystemLogSwitchPO systemLogSwitchPO) {
        Boolean b = systemLogSwitchService.changeSwitch(systemLogSwitchPO);
        if (b) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation("日志管理--日志管理恢复出厂设置")
    @GetMapping("/resetLogSwitch")
    public Result<Boolean> resetLogSwitch(@RequestParam String updateBy) {
        Boolean flag = systemLogSwitchService.resetLogSwitch(updateBy);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "业务参数页面--保存业务参数")
    @PostMapping("/saveBusinessParam")
    public Result<Boolean> saveBusinessParam(@RequestBody SystemBusinessParamDTO list) {
        Boolean b = systemBusinessParamService.saveBusinessParam(list);
        if (b) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "业务参数页面--查询业务参数")
    @GetMapping("/queryBusinessParam")
    public Result<SystemBusinessParamVO> queryBusinessParam() {
        SystemBusinessParamVO vo = systemBusinessParamService.queryBusinessParam();
        if (vo != null) {
            return Result.ok(vo);
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "测量页面--查询测量相关信息")
    @GetMapping("/queryMeasureConf")
    public Result<SystemManageMeasureConfVO> queryMeasureConf() {
        R<SystemManageMeasureConfVO> r = systemBusinessParamService.queryMeasureConf();
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation(value = "定量测定页面--标准曲线相关信息")
    @GetMapping("/queryStandCurveConf")
    public Result<SystemStandCurveConfVO> queryStandCurveConf() {
        R<SystemStandCurveConfVO> r = systemBusinessParamService.queryStandCurveConf();
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation(value = "其他--查询自动登出配置")
    @GetMapping("/queryAutoLogoutConf")
    public Result<SystemAutoLogoutConfVO> queryAutoLogoutConf() {
        R<SystemAutoLogoutConfVO> r = systemBusinessParamService.queryAutoLogoutConf();
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation(value = "业务参数页面--业务参数恢复出厂设置")
    @GetMapping("/resetBusinessParam")
    public Result<Boolean> resetBusinessParam() {
        Boolean flag = systemBusinessParamService.resetBusinessParam();
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "算法配置--算法参数保存")
    @PostMapping("/saveAlgorithmConf")
    public Result<Boolean> saveAlgorithmConf(@RequestBody SystemAlgorithmConfDTO dto) {
        Boolean flag = systemAlgorithmConfService.saveAlgorithmConf(dto);
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "算法配置--算法参数恢复出厂设置")
    @GetMapping("/resetAlgorithmConf")
    public Result<Boolean> resetAlgorithmConf() {
        Boolean flag = systemAlgorithmConfService.resetAlgorithmConf();
        if (flag) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @ApiOperation(value = "算法配置--查询算法配置参数")
    @GetMapping("/queryAlgorithmConf")
    public Result<SystemAlgorithmConfVO> queryAlgorithmConf() {
        SystemAlgorithmConfVO list = systemAlgorithmConfService.queryAlgorithmConf();

        return Result.ok(list);
    }


    @ApiOperation(value = "测量首页--查询面板顺序")
    @GetMapping("/queryPanelOrder")
    public Result<List<String>> queryPanelOrder() {
        List<String> list = systemPanelConfService.queryPanelOrder();
        return Result.ok(list);
    }

    @ApiOperation(value = "测量首页--保存面板顺序", notes = "以数组顺序代表面板顺序，method-方法面板，project-项目面板，task-任务面板")
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
