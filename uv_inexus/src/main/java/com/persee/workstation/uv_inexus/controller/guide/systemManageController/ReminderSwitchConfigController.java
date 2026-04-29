package com.persee.workstation.uv_inexus.controller.guide.systemManageController;

import com.persee.workstation.uv_inexus.dto.systemManageDto.ReminderSwitchConfigDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.factory_inspection.ReminderSwitchConfigService;
import com.persee.workstation.uv_inexus.vo.factory_inspection.ReminderSwitchConfigListVo;
import com.persee.workstation.uv_inexus.vo.factory_inspection.ReminderSwitchConfigVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "提醒功能开关配置管理")
@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/reminder-config")
public class ReminderSwitchConfigController {

    @Autowired
    private ReminderSwitchConfigService reminderSwitchConfigService;

    @ApiOperation("获取所有提醒配置")
    @GetMapping("/get-all-configs")
    public Result<ReminderSwitchConfigListVo> getAllConfigs() {
        return reminderSwitchConfigService.getAllConfigs();
    }

    @ApiOperation("根据类型获取配置")
    @GetMapping("/get-config-by-type")
    public Result<ReminderSwitchConfigVo> getConfigByType(@RequestParam Integer reminderType) {
        return reminderSwitchConfigService.getConfigByType(reminderType);
    }

    @ApiOperation("更新单个配置")
    @PostMapping("/update-config")
    public Result<Boolean> updateConfig(@RequestBody @Valid ReminderSwitchConfigDto dto) {
        return reminderSwitchConfigService.updateConfig(dto);
    }

    @ApiOperation("批量更新配置")
    @PostMapping("/batch-update-configs")
    public Result<Boolean> batchUpdateConfigs(@RequestBody List<ReminderSwitchConfigDto> dtos) {
        return reminderSwitchConfigService.batchUpdateConfigs(dtos);
    }
}
