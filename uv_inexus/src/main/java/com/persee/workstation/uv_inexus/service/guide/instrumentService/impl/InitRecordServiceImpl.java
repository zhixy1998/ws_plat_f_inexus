/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
            if (i == 0) {
                po.setInitStatus(Constants.INIT_TASK_ING);
            }
            DeviceInitEntryInfo entryInfo = initEntryList.get(i);
            po.setInitCmdIdList(entryInfo.getInitComId());
            po.setInitCmdName(entryInfo.getInitComName());
            po.setInitCmdValue(entryInfo.getInitComValue());
            po.setInitCmdIdList(entryInfo.getInitComId());

            dao.insert(po);
        }
    }


    @Override
    public List<InitRecordPO> searchInitStatus(String deviceSerialNumber, String deviceId) {
        LambdaQueryWrapper<InitRecordPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InitRecordPO::getDeviceSerialNumber, deviceSerialNumber)
                .eq(InitRecordPO::getDeviceInfoId, deviceId)
                .orderByAsc(InitRecordPO::getInitTaskNum);

        return dao.selectList(queryWrapper);
    }

    @Override
    public InitRecordPO selectById(String id) {
        return dao.selectById(id);
    }
}
