package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FSamplePoolConfigDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.dto.concise.common.FSampleAttachTypeSaveDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.FSamplePoolInfoChangeDto;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceSamplePoolPurpose;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FSamplePoolConfigPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FSamplePoolConfigService;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
    public Boolean saveSamplePoolInfo(FSampleAttachTypeSaveDTO dto) {

        if (!StringUtils.hasText(dto.getDeviceSerialNumber())) {
            log.info("缺少序列号信息");
            return false;
        }

        LambdaQueryWrapper<FSamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(FSamplePoolConfigPO::getDeviceSerialNumber, dto.getDeviceSerialNumber());
        FSamplePoolConfigPO po = dao.selectOne(query);
        if (po == null) {
            po = new FSamplePoolConfigPO();
        }
        BeanUtils.copyProperties(dto, po);

        String deviceSerialNumber = dto.getDeviceSerialNumber();

        DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfo(deviceSerialNumber);
        List<FSamplePoolStatus> list = createFSamplePoolStatuses(attachmentInfo.getSamplePoolNum());
        po.setPoolInfo(JSON.toJSONString(list));
        po.setIsAutoChangePool(false);

        po.setCreateBy(dto.getUpdateBy());
        po.setUpdateBy(dto.getUpdateBy());
        if (StringUtils.hasText(po.getId())) {
            return dao.updateById(po) > 0;
        } else {
            return dao.insert(po) > 0;
        }
    }

    @Override
    public R<Boolean> updateSamplePoolInfo(FSamplePoolInfoChangeDto changeDto) {

        if (!StringUtils.hasText(changeDto.getDeviceSerialNumber())) {
            log.info("未连接仪器");
            return R.fail("未连接仪器");
        }

        if (!StringUtils.hasText(changeDto.getUpdateBy())) {
            log.info("缺少更新人信息");
            return R.fail("未登录");
        }

        LambdaQueryWrapper<FSamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(FSamplePoolConfigPO::getDeviceSerialNumber, changeDto.getDeviceSerialNumber());
        FSamplePoolConfigPO po = dao.selectOne(query);
        if (po == null) {
            return R.fail("未找到该仪器附件配置");
        }

        po.setUpdateBy(changeDto.getUpdateBy());
        if (changeDto.getIsAutoChangePool() != null) {
            po.setIsAutoChangePool(changeDto.getIsAutoChangePool());
        }

        if (changeDto.getPoolNum() != null) {
            List<FSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), FSamplePoolStatus.class);
            for (FSamplePoolStatus status : list) {
                if (status.getPoolNum().equals(changeDto.getPoolNum())) {
                    if (changeDto.getPoolStatus() != null) {
                        status.setPoolStatus(changeDto.getPoolStatus());
                    }
                    if (changeDto.getPoolPurpose() != null) {
                        status.setPoolPurpose(changeDto.getPoolPurpose());
                        status.setPoolPurposeName(DeviceSamplePoolPurpose.getName(changeDto.getPoolPurpose()));
                    }
                }
            }
            po.setPoolInfo(JSON.toJSONString(list));
        } else {
            if (changeDto.getPoolStatus() != null || changeDto.getPoolPurpose() != null) {
                return R.fail("请选择要操作的样品池");
            }
        }
        return R.ok(dao.updateById(po) > 0);
    }

    @Override
    public R<FSamplePoolInfoVO> querySamplePoolInfo(String deviceSerialNumber) {

        LambdaQueryWrapper<FSamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(FSamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        FSamplePoolConfigPO po = dao.selectOne(query);
        if (po == null || !StringUtils.hasText(po.getPoolInfo())) {
            return R.fail("未找到该仪器附件配置");
        }

        FSamplePoolInfoVO vo = new FSamplePoolInfoVO();
        BeanUtils.copyProperties(po, vo);
        List<FSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), FSamplePoolStatus.class);
        list.forEach(status-> status.setPoolName(status.getPoolNum() + "号"));
        vo.setPoolStatusList(list);
        return R.ok(vo);
    }


}
