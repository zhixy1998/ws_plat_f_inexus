package com.persee.workstation.uv_inexus.service.instrumentService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dao.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.po.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Service
@Slf4j
public class DeviceInfoServiceImpl implements DeviceInfoService {

    @Autowired
    DeviceInfoDAO dao;

    @Override
    public DeviceInfoPO getInfo(String deviceSerialNumber) {
        LambdaQueryWrapper<DeviceInfoPO> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(DeviceInfoPO::getDeviceSerialNumber, deviceSerialNumber);

        DeviceInfoPO po = dao.selectOne(queryWrapper);

        return po;
    }

    @Override
    public Boolean saveOrUpdateInfo(DeviceInfoPO po) {
        LambdaQueryWrapper<DeviceInfoPO> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(DeviceInfoPO::getDeviceSerialNumber, po.getDeviceSerialNumber());

        Long count = dao.selectCount(queryWrapper);
        if (count != null && count > 0) {
            log.info("该序列号已有数据");
            return dao.update(po, queryWrapper) > 0;
        } else {
            return dao.insert(po) > 0;
        }
    }

    @Override
    public String getIp(String deviceSerialNumber) {
        if (StringUtils.hasText(deviceSerialNumber)) {
            return dao.getIp(deviceSerialNumber);
        } else {
            log.info("未传入序列号");
            return null;
        }
    }


}
