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

package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
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

        return dao.selectOne(queryWrapper);
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

    @Override
    public String getAttachmentType(String deviceSerialNumber) {
        if (StringUtils.hasText(deviceSerialNumber)) {
            DeviceAttachmentTypeVO attachmentInfo = dao.getAttachmentInfo(deviceSerialNumber);
            return attachmentInfo.getAttachmentName();
        } else {
            log.info("未传入序列号");
            return null;
        }
    }

    @Override
    public void updateDeviceCurrentStatus(String deviceSerialNumber, Integer currentStatus) {
        LambdaQueryWrapper<DeviceInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceInfoPO::getDeviceSerialNumber, deviceSerialNumber)
                .last("limit 1");
        DeviceInfoPO deviceInfoPO = dao.selectOne(queryWrapper);
        deviceInfoPO.setDeviceCurrentStatus(currentStatus);
        dao.update(deviceInfoPO, queryWrapper);
    }

    @Override
    public Boolean changeDeviceName(String deviceSerialNumber, String newDeviceName, String updateBy) {
        LambdaQueryWrapper<DeviceInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceInfoPO::getDeviceSerialNumber, deviceSerialNumber)
                .last("limit 1");
        DeviceInfoPO deviceInfoPO = dao.selectOne(queryWrapper);
        deviceInfoPO.setDeviceName(newDeviceName);
        deviceInfoPO.setUpdateBy(updateBy);
        return dao.update(deviceInfoPO, queryWrapper) > 0;
    }



    @Override
    public R<Boolean> useDevice(String deviceSerialNumber, Integer status) {

        DeviceInfoPO info = getInfo(deviceSerialNumber);
        Integer isInited = info.getIsInited();

        Integer isActivated = info.getIsActivated();
        if (isActivated == null || isActivated == 0) {
            return R.fail("请先激活仪器");
        }

        Integer connectStatus = info.getDeviceCurrentStatus();
        if (connectStatus.equals(Constants.DEVICE_STATUS_INIT) && !DetectionConstants.isDeviceIdle){
            return R.fail( "设备正在初始化，请等待");
        }
        if (connectStatus.equals(Constants.DEVICE_STATUS_MEASURE) && !DetectionConstants.isDeviceIdle){
            return R.fail( "设备正在测量，请等待");
        }

        if (!status.equals(Constants.DEVICE_STATUS_INIT)) {
            if (isInited == null || isInited == 0) {
                log.info("未初始化");
                return R.fail("未初始化");
            }
        }


        int count = 0;
        while (count < 40) {
            if (DetectionConstants.isDeviceIdle){
                DetectionConstants.isDeviceIdle = false;
                updateDeviceCurrentStatus(deviceSerialNumber, status);
                return R.ok();
            }
            try {
                Thread.sleep(50);
                count ++;
            } catch (InterruptedException e) {
                log.error(e.getMessage());
                return R.fail("系统异常");
            }
        }

        return R.fail("设备正在使用中，请等待");
    }

    @Override
    public void releaseDevice(String deviceSerialNumber) {
        DetectionConstants.isDeviceIdle = true;
        updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
    }

    @Override
    public void activeDevice(String deviceSerialNumber) {
        dao.updateDeviceActivateStatus(deviceSerialNumber, 1);
    }

    @Override
    public Integer queryDeviceActivationStatus(String deviceSerialNumber) {
        Integer i = dao.queryDeviceActivateStatus(deviceSerialNumber);
        if (i != null) {
            return i;
        }
        return 0;
    }


}
