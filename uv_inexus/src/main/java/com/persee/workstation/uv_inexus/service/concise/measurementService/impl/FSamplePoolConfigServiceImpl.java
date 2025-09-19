package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FSamplePoolConfigDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.dto.concise.common.FSamplePoolInfoChangeDto;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceSamplePoolPurpose;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FSamplePoolConfigPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FSamplePoolConfigService;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service("FSamplePoolConfig")
@DS("concise")
@Slf4j
public class FSamplePoolConfigServiceImpl extends ServiceImpl<FSamplePoolConfigDAO, FSamplePoolConfigPO> implements FSamplePoolConfigService {

    @Autowired
    private FSamplePoolConfigDAO dao;

    @Autowired
    DeviceInfoDAO deviceInfoDAO;

    @Override
    public Boolean updateCurrentPoolNum(String deviceSerialNumber, Integer currentPoolNum, String updateBy) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info("缺少序列号");
            return false;
        }

        LambdaQueryWrapper<FSamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(FSamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        FSamplePoolConfigPO po = dao.selectOne(query);
        if (po == null) {
            po = new FSamplePoolConfigPO();
            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfo(deviceSerialNumber);
            po.setAttachmentType(attachmentInfo.getAttachmentNum());
            po.setDeviceSerialNumber(deviceSerialNumber);
            po.setCurrentPoolNum(currentPoolNum);
            po.setUpdateBy(updateBy);
            List<FSamplePoolStatus> poolInfo = createFSamplePoolStatuses(attachmentInfo.getSamplePoolNum());
            po.setPoolInfo(JSON.toJSONString(poolInfo));
            return dao.insert(po) > 0;
        } else {
            po.setCurrentPoolNum(currentPoolNum);
            po.setUpdateBy(updateBy);
            return dao.updateById(po) > 0;
        }
    }

    private static List<FSamplePoolStatus> createFSamplePoolStatuses(Integer totalPoolNum) {
        List<FSamplePoolStatus> poolInfo = new ArrayList<>();

        for (int i = 1; i < totalPoolNum + 1; i++) {
            FSamplePoolStatus status = new FSamplePoolStatus();
            status.setPoolNum(i);
            status.setPoolStatus(false);
            status.setPoolPurpose(DeviceSamplePoolPurpose.MEASURE.getCode());
            status.setPoolPurposeName(DeviceSamplePoolPurpose.MEASURE.getName());
            poolInfo.add(status);
        }
        return poolInfo;
    }

    @Override
    public Boolean saveSamplePoolInfo(String deviceSerialNumber, List<FSamplePoolStatus> poolInfo) {
        FSamplePoolConfigPO po = new FSamplePoolConfigPO();




        return null;
    }

    @Override
    public Boolean updateSamplePoolInfo(FSamplePoolInfoChangeDto samplePoolInfoChangeDto) {
        return null;
    }


}
