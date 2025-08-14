package com.persee.workstation.uv_inexus.service.softwareActivation.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.softwareActivationDao.SoftwareActivationMapper;
import com.persee.workstation.uv_inexus.po.softwareActivation.SoftwareActivationPO;
import com.persee.workstation.uv_inexus.service.softwareActivation.SoftwareActivationService;

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
}