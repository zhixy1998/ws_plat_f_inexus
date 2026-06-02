package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.ReminderSwitchConfigMapper;
import com.persee.workstation.uv_inexus.dto.systemManageDto.ReminderSwitchConfigDto;
import com.persee.workstation.uv_inexus.enums.factoryInspection.ReminderTypeEnum;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.ReminderSwitchConfigPO;
import com.persee.workstation.uv_inexus.service.factory_inspection.ReminderSwitchConfigService;
import com.persee.workstation.uv_inexus.vo.factory_inspection.ReminderSwitchConfigListVo;
import com.persee.workstation.uv_inexus.vo.factory_inspection.ReminderSwitchConfigVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ReminderSwitchConfigServiceImpl extends ServiceImpl<ReminderSwitchConfigMapper, ReminderSwitchConfigPO>
        implements ReminderSwitchConfigService {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 总开关类型
    private static final int GLOBAL_SWITCH_TYPE = 0;

    @Override
    public Result<ReminderSwitchConfigListVo> getAllConfigs() {
        try {
            LambdaQueryWrapper<ReminderSwitchConfigPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByAsc(ReminderSwitchConfigPO::getSortOrder);
            List<ReminderSwitchConfigPO> configs = this.list(wrapper);

            if (CollectionUtils.isEmpty(configs)) {
                log.warn("未找到提醒配置，初始化默认配置");
                initDefaultConfigs();
                configs = this.list(wrapper);
            }

            ReminderSwitchConfigListVo listVo = new ReminderSwitchConfigListVo();
            
            // 分离总开关和分项配置
            for (ReminderSwitchConfigPO config : configs) {
                ReminderSwitchConfigVo vo = convertToVo(config);
                
                if (config.getReminderType() == GLOBAL_SWITCH_TYPE) {
                    listVo.setGlobalSwitch(vo);
                } else {
                    if (listVo.getItemConfigs() == null) {
                        listVo.setItemConfigs(new ArrayList<>());
                    }
                    listVo.getItemConfigs().add(vo);
                }
            }

            return Result.ok(listVo);
        } catch (Exception e) {
            log.error("获取提醒配置列表异常", e);
            return Result.fail("00615001", e.getMessage());
        }
    }

    @Override
    public Result<ReminderSwitchConfigVo> getConfigByType(Integer reminderType) {
        try {
            LambdaQueryWrapper<ReminderSwitchConfigPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReminderSwitchConfigPO::getReminderType, reminderType);
            ReminderSwitchConfigPO config = this.getOne(wrapper);

            if (config == null) {
                return Result.fail("00615002");
            }

            return Result.ok(convertToVo(config));
        } catch (Exception e) {
            log.error("获取提醒配置异常", e);
            return Result.fail("00615003", e.getMessage());
        }
    }
    @Override
    public Integer getNotificationMode(Integer reminderType) {
        LambdaQueryWrapper<ReminderSwitchConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderSwitchConfigPO::getReminderType, reminderType);
        ReminderSwitchConfigPO config = this.getOne(wrapper);

        if (config == null) {
            return 0;
        }

        return config.getNotificationMode() != null ? config.getNotificationMode() : 0;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> updateConfig(ReminderSwitchConfigDto dto) {
        try {
            LambdaQueryWrapper<ReminderSwitchConfigPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReminderSwitchConfigPO::getReminderType, dto.getReminderType());
            ReminderSwitchConfigPO config = this.getOne(wrapper);

            if (config == null) {
                log.error("未找到提醒配置，type: {}", dto.getReminderType());
                return Result.fail("00615002");
            }

            // 更新字段
            if (dto.getEnabled() != null) {
                config.setEnabled(dto.getEnabled());
            }
            if (dto.getReminderName() != null) {
                config.setReminderName(dto.getReminderName());
            }
            if (dto.getSortOrder() != null) {
                config.setSortOrder(dto.getSortOrder());
            }
            if (dto.getDescription() != null) {
                config.setDescription(dto.getDescription());
            }

            boolean updated = this.updateById(config);
            
            if (updated) {
                log.info("提醒配置更新成功: type={}, enabled={}", dto.getReminderType(), dto.getEnabled());
                return Result.ok(true);
            } else {
                log.error("提醒配置更新失败");
                return Result.fail("00615004");
            }
        } catch (Exception e) {
            log.error("更新提醒配置异常", e);
            return Result.fail("00615005", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> batchUpdateConfigs(ReminderSwitchConfigListVo configList) {
        try {
            if (configList == null) {
                return Result.fail("00615006");
            }

            int updateCount = 0;

            if (configList.getGlobalSwitch() != null && configList.getGlobalSwitch().getEnabled() != null) {
                LambdaQueryWrapper<ReminderSwitchConfigPO> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ReminderSwitchConfigPO::getReminderType, GLOBAL_SWITCH_TYPE);
                ReminderSwitchConfigPO config = this.getOne(wrapper);

                if (config != null) {
                    config.setEnabled(configList.getGlobalSwitch().getIsEnabled()?1:0);
                    this.updateById(config);
                    updateCount++;
                    log.info("更新总开关配置: enabled={}", config.getEnabled());
                }
            }

            if (!CollectionUtils.isEmpty(configList.getItemConfigs())) {
                for (ReminderSwitchConfigVo vo : configList.getItemConfigs()) {
                    if (vo.getReminderType() != null && vo.getEnabled() != null) {
                        LambdaQueryWrapper<ReminderSwitchConfigPO> wrapper = new LambdaQueryWrapper<>();
                        wrapper.eq(ReminderSwitchConfigPO::getReminderType, vo.getReminderType());
                        ReminderSwitchConfigPO config = this.getOne(wrapper);

                        if (config != null) {
                            config.setEnabled(vo.getIsEnabled()?1:0);
                            this.updateById(config);
                            updateCount++;
                            log.info("更新配置项: type={}, enabled={}", vo.getReminderType(), config.getEnabled());
                        }
                    }
                }
            }

            log.info("批量更新提醒配置成功，共更新 {} 条配置", updateCount);
            return Result.ok(true);
        } catch (Exception e) {
            log.error("批量更新提醒配置异常", e);
            return Result.fail("00615007", e.getMessage());
        }
    }


    @Override
    public boolean isReminderEnabled() {
        try {
            LambdaQueryWrapper<ReminderSwitchConfigPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReminderSwitchConfigPO::getReminderType, GLOBAL_SWITCH_TYPE);
            ReminderSwitchConfigPO config = this.getOne(wrapper);

            if (config == null) {
                log.warn("未找到总开关配置，默认启用");
                return true;
            }

            boolean enabled = config.getEnabled() == null || config.getEnabled() == 1;
            if (!enabled) {
                log.debug("提醒功能总开关已关闭");
            }
            return enabled;
        } catch (Exception e) {
            log.error("检查提醒总开关状态异常，默认启用", e);
            return true;
        }
    }

    @Override
    public boolean isReminderTypeEnabled(Integer reminderType) {
        try {
            // 先检查总开关
            if (!isReminderEnabled()) {
                return false;
            }

            LambdaQueryWrapper<ReminderSwitchConfigPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReminderSwitchConfigPO::getReminderType, reminderType);
            ReminderSwitchConfigPO config = this.getOne(wrapper);

            if (config == null) {
                log.warn("未找到类型 {} 的配置，默认启用", reminderType);
                return true;
            }

            boolean enabled = config.getEnabled() == null || config.getEnabled() == 1;
            if (!enabled) {
                log.debug("提醒类型 {} 已关闭", reminderType);
            }
            return enabled;
        } catch (Exception e) {
            log.error("检查提醒类型 {} 开关状态异常，默认启用", reminderType, e);
            return true;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultConfigs() {
        try {
            // 检查是否已有配置
            long count = this.count();
            if (count > 0) {
                log.info("提醒配置已存在，无需初始化");
                return;
            }

            List<ReminderSwitchConfigPO> defaultConfigs = new ArrayList<>();
            
            // 总开关
            ReminderSwitchConfigPO globalSwitch = new ReminderSwitchConfigPO();
            globalSwitch.setId(UUID.randomUUID().toString().replace("-", ""));
            globalSwitch.setReminderType(GLOBAL_SWITCH_TYPE);
            globalSwitch.setReminderName("总开关");
            globalSwitch.setEnabled(1);
            globalSwitch.setSortOrder(0);
            globalSwitch.setDescription("提醒功能总开关，关闭后所有提醒将停止");
            globalSwitch.setCreateBy("system");
            globalSwitch.setUpdateBy("system");
            defaultConfigs.add(globalSwitch);

            // 期间核查提醒
            ReminderSwitchConfigPO periodicInspection = new ReminderSwitchConfigPO();
            periodicInspection.setId(UUID.randomUUID().toString().replace("-", ""));
            periodicInspection.setReminderType(ReminderTypeEnum.PERIODIC_INSPECTION.getCode());
            periodicInspection.setReminderName(ReminderTypeEnum.PERIODIC_INSPECTION.getDescription());
            periodicInspection.setEnabled(1);
            periodicInspection.setSortOrder(3);
            periodicInspection.setDescription("期间核查任务提醒");
            periodicInspection.setCreateBy("system");
            periodicInspection.setUpdateBy("system");
            defaultConfigs.add(periodicInspection);

            // 标准物质效期提醒
            ReminderSwitchConfigPO expiryReminder = new ReminderSwitchConfigPO();
            expiryReminder.setId(UUID.randomUUID().toString().replace("-", ""));
            expiryReminder.setReminderType(ReminderTypeEnum.EXPIRY_REMINDER.getCode());
            expiryReminder.setReminderName(ReminderTypeEnum.EXPIRY_REMINDER.getDescription());
            expiryReminder.setEnabled(1);
            expiryReminder.setSortOrder(4);
            expiryReminder.setDescription("标准物质有效期到期提醒");
            expiryReminder.setCreateBy("system");
            expiryReminder.setUpdateBy("system");
            defaultConfigs.add(expiryReminder);

            this.saveBatch(defaultConfigs);
            log.info("提醒配置初始化成功，共 {} 条配置", defaultConfigs.size());
        } catch (Exception e) {
            log.error("初始化提醒配置失败", e);
        }
    }

    /**
     * 转换为VO
     */
    private ReminderSwitchConfigVo convertToVo(ReminderSwitchConfigPO po) {
        ReminderSwitchConfigVo vo = new ReminderSwitchConfigVo();
        BeanUtils.copyProperties(po, vo);

        if (po.getCreated() != null) {
            vo.setCreateTime(po.getCreated().format(dateTimeFormatter));
        }
        if (po.getUpdated() != null) {
            vo.setUpdateTime(po.getUpdated().format(dateTimeFormatter));
        }

        return vo;
    }
}

