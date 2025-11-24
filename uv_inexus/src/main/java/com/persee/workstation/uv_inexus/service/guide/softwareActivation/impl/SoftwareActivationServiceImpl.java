/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.service.guide.softwareActivation.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.softwareActivationDao.SoftwareActivationMapper;
import com.persee.workstation.uv_inexus.po.guide.softwareActivation.SoftwareActivationPO;
import com.persee.workstation.uv_inexus.service.guide.softwareActivation.SoftwareActivationService;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 软件激活Service实现类（基于MyBatis-Plus的ServiceImpl）
 */
@Service
public class SoftwareActivationServiceImpl extends ServiceImpl<SoftwareActivationMapper, SoftwareActivationPO> 
        implements SoftwareActivationService {

    @Autowired
    private SoftwareActivationMapper activationMapper;

    @Override
    public SoftwareActivationPO findByDeviceNumber(String deviceNumber) {
        QueryWrapper<SoftwareActivationPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("device_number", deviceNumber);
        return activationMapper.selectOne(queryWrapper);
    }

    /**
     * 分页条件查询实现
     */
    @Override
    public IPage<SoftwareActivationPO> findPage(Page<SoftwareActivationPO> page, Map<String, String> params) {
        // 构建查询条件
        LambdaQueryWrapper<SoftwareActivationPO> queryWrapper = new LambdaQueryWrapper<>();

        // 设备序列号模糊查询
        String deviceNumber = params.get("deviceNumber");
        if (StringUtils.hasText(deviceNumber)) {
            queryWrapper.like(SoftwareActivationPO::getDeviceNumber, deviceNumber);
        }

        // 设备IP精确查询
        String deviceIp = params.get("deviceIp");
        if (StringUtils.hasText(deviceIp)) {
            queryWrapper.eq(SoftwareActivationPO::getDeviceIp, deviceIp);
        }

        // 类型模糊查询
        String type = params.get("type");
        if (StringUtils.hasText(type)) {
            queryWrapper.like(SoftwareActivationPO::getType, type);
        }

        // 激活状态精确查询
        String activationStatus = params.get("activationStatus");
        if (StringUtils.hasText(activationStatus)) {
            queryWrapper.eq(SoftwareActivationPO::getActivationStatus, activationStatus);
        }

        // 按激活时间倒序排序（最新的在前）
        // 先按创建时间降序，再按激活时间降序
        queryWrapper.orderByDesc(SoftwareActivationPO::getCreated);

        // 执行分页查询
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Scheduled(fixedRate = 60000)
    public void expireCheckRealtime() {
        expireOverdueActivations();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void expireCheckDaily() {
        expireOverdueActivations();
    }

    private void expireOverdueActivations() {
        UpdateWrapper<SoftwareActivationPO> wrapper = new UpdateWrapper<>();
        wrapper.eq("activation_status", 1);
        wrapper.apply("COALESCE(STR_TO_DATE(expiration_date,'%Y-%m-%d %H:%i:%s'), STR_TO_DATE(expiration_date,'%Y-%m-%d')) < NOW() ");
        wrapper.set("activation_status", 0);
        baseMapper.update(null, wrapper);
    }
}
