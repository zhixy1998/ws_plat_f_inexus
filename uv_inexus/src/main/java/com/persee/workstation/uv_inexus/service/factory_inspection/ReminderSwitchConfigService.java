package com.persee.workstation.uv_inexus.service.factory_inspection;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.systemManageDto.ReminderSwitchConfigDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.ReminderSwitchConfigPO;
import com.persee.workstation.uv_inexus.vo.factory_inspection.ReminderSwitchConfigListVo;
import com.persee.workstation.uv_inexus.vo.factory_inspection.ReminderSwitchConfigVo;

public interface ReminderSwitchConfigService extends IService<ReminderSwitchConfigPO> {

    /**
     * 获取所有提醒配置
     */
    Result<ReminderSwitchConfigListVo> getAllConfigs();

    /**
     * 根据类型获取配置
     */
    Result<ReminderSwitchConfigVo> getConfigByType(Integer reminderType);

    /**
     * 更新单个配置
     */
    Result<Boolean> updateConfig(ReminderSwitchConfigDto dto);

    /**
     * 批量更新配置
     */
    Result<Boolean> batchUpdateConfigs(ReminderSwitchConfigListVo dtos);

    /**
     * 检查提醒功能是否启用（总开关）
     */
    boolean isReminderEnabled();

    /**
     * 检查指定类型的提醒是否启用
     * @param reminderType 1-期间核查提醒 4-标准物质效期提醒
     */
    boolean isReminderTypeEnabled(Integer reminderType);

    /**
     * 初始化默认配置
     */
    void initDefaultConfigs();
}
