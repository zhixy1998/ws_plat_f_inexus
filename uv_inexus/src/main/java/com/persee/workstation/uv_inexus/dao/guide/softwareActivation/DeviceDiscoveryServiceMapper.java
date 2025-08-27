package com.persee.workstation.uv_inexus.dao.guide.softwareActivation;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.softwareActivation.SoftwareActivationPO;

// 仅继承BaseMapper，使用其自带的insert方法
@Mapper
public interface DeviceDiscoveryServiceMapper extends BaseMapper<SoftwareActivationPO> {
    // 移除insertBatch方法，无需额外定义
}