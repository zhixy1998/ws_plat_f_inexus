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
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.InitRecordDAO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInitEntryInfo;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InitRecordPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.InitRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class InitRecordServiceImpl implements InitRecordService {

    @Autowired
    InitRecordDAO dao;

    @Override
    public void saveOrUpdateInitTask(InitRecordPO po) {
        LambdaQueryWrapper<InitRecordPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InitRecordPO::getDeviceSerialNumber, po.getDeviceSerialNumber())
                .eq(InitRecordPO::getInitTaskNum, po.getInitTaskNum())
                .eq(InitRecordPO::getDeviceInfoId, po.getDeviceInfoId());
        List<InitRecordPO> poList = dao.selectList(queryWrapper);
        if (!poList.isEmpty()) {
            log.info("更新状态值");
            dao.update(po, queryWrapper);
        } else {
            dao.insert(po);
        }

    }

    @Override
    public void initAllTask(String deviceSerialNumber, String deviceInfoId, List<DeviceInitEntryInfo> initEntryList) {
        LambdaQueryWrapper<InitRecordPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InitRecordPO::getDeviceSerialNumber, deviceSerialNumber)
                .eq(InitRecordPO::getDeviceInfoId, deviceInfoId);

        List<InitRecordPO> poList = dao.selectList(queryWrapper);
        if (!poList.isEmpty()) {
            dao.delete(queryWrapper);
        }

        for (int i = 0; i < initEntryList.size(); i++) {     //我在这去了个  =
            InitRecordPO po = new InitRecordPO();
            po.setInitTaskNum(i);
            po.setDeviceInfoId(deviceInfoId);
            po.setDeviceSerialNumber(deviceSerialNumber);
            po.setInitStatus(Constants.INIT_TASK_NO);
            DeviceInitEntryInfo entryInfo = initEntryList.get(i);
            po.setInitCmdIdList(entryInfo.getInitComId());
            po.setInitCmdName(entryInfo.getInitComName());
            po.setInitCmdValue(entryInfo.getInitComValue());
            po.setInitCmdIdList(entryInfo.getInitComId());

            dao.insert(po);
        }
    }


    @Override
    public List<InitRecordPO> searchInitStatus(String deviceSerialNumber, String deviceIp) {
        LambdaQueryWrapper<InitRecordPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InitRecordPO::getDeviceSerialNumber, deviceSerialNumber)
                .eq(InitRecordPO::getDeviceInfoId, deviceIp)
                .orderByAsc(InitRecordPO::getInitTaskNum);

        return dao.selectList(queryWrapper);
    }

    @Override
    public InitRecordPO selectById(String id) {
        return dao.selectById(id);
    }
}
