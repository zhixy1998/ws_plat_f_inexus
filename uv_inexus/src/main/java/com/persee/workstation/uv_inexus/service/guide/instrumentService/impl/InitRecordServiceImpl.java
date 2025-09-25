package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.InitRecordDAO;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceInitInfoEnum;
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
        if (poList.size() > 0) {
            log.info("更新状态值");
            dao.update(po, queryWrapper);
        } else {
            dao.insert(po);
        }

    }

    @Override
    public void initAllTask(String deviceSerialNumber,String deviceInfoId) {

        LambdaQueryWrapper<InitRecordPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InitRecordPO::getDeviceSerialNumber, deviceSerialNumber)
                .eq(InitRecordPO::getDeviceInfoId, deviceInfoId);

        List<InitRecordPO> poList = dao.selectList(queryWrapper);
        if (poList.size() != 0) {
            dao.delete(queryWrapper);
        }

        for (int i = 1; i <= DeviceInitInfoEnum.getLength(); i++) {
            InitRecordPO po = new InitRecordPO();
            po.setInitTaskNum(i);
            po.setDeviceInfoId(deviceInfoId);
            po.setDeviceSerialNumber(deviceSerialNumber);
            po.setInitStatus(Constants.INIT_TASK_STATUS_ING);

            dao.insert(po);
        }

    }


    @Override
    public List<InitRecordPO> searchInitStatus(String deviceSerialNumber, String deviceIp) {
        LambdaQueryWrapper<InitRecordPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InitRecordPO::getDeviceSerialNumber, deviceSerialNumber)
                .eq(InitRecordPO::getDeviceInfoId, deviceIp);

        return dao.selectList(queryWrapper);
    }
}
