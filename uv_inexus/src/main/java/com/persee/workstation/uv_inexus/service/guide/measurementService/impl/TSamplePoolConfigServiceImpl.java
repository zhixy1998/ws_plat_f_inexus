package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TSamplePoolConfigDAO;
import com.persee.workstation.uv_inexus.dto.guide.TSampleAttachTypeSaveDTO;
import com.persee.workstation.uv_inexus.dto.guide.TSamplePoolInfoChangeDto;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceSamplePoolPurpose;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TSamplePoolConfigPO;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TSamplePoolStatus;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.PublicUtilService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TSamplePoolConfigService;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.vo.factory_inspection.SamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.factory_inspection.SamplePoolStatus;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.TSamplePoolInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service("TSamplePoolConfig")
@Slf4j
public class TSamplePoolConfigServiceImpl extends ServiceImpl<TSamplePoolConfigDAO, TSamplePoolConfigPO> implements TSamplePoolConfigService {

    @Autowired
    TSamplePoolConfigDAO dao;

    @Autowired
    DeviceInfoDAO deviceInfoDAO;

    @Autowired
    DevMessageUtil devMessageUtil;

    @Autowired
    DeviceInfoService deviceInfoService;

    @Autowired
    PublicUtilService publicUtilService;

    @Override
    public Boolean updateCurrentPoolNum(String deviceSerialNumber, Integer currentPoolNum, String updateBy) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info("缺少序列号");
            return false;
        }
        LambdaQueryWrapper<TSamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(TSamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        TSamplePoolConfigPO po = dao.selectOne(query);
        if (po==null){
            po= new TSamplePoolConfigPO();
            DeviceAttachmentTypeVO attachmentInfo=deviceInfoDAO.getAttachmentInfo(deviceSerialNumber);
            po.setAttachmentType(attachmentInfo.getAttachmentNum());
            po.setDeviceSerialNumber(deviceSerialNumber);
            po.setCurrentPoolNum(currentPoolNum);
            po.setUpdateBy(updateBy);
            List<TSamplePoolStatus> poolInfo=createTSamplePoolStatuses(attachmentInfo.getSamplePoolNum());
            po.setPoolInfo(JSON.toJSONString(poolInfo));
            return dao.insert(po)>0;
        }
        else {
            po.setCurrentPoolNum(currentPoolNum);
            po.setUpdateBy(updateBy);
            return dao.updateById(po) > 0;
        }
    }

    private static List<TSamplePoolStatus> createTSamplePoolStatuses(Integer totalPoolNum) {
        List<TSamplePoolStatus> poolInfo = new ArrayList<>();

        for (int i = 1; i < totalPoolNum + 1; i++) {
            TSamplePoolStatus status = new TSamplePoolStatus();
            status.setPoolNum(i);
            status.setPoolStatus(false);
            status.setPoolPurpose(DeviceSamplePoolPurpose.MEASURE.getCode());
            status.setPoolPurposeName(DeviceSamplePoolPurpose.MEASURE.getName());
            poolInfo.add(status);
        }
        return poolInfo;
    }

    @Override
    public Boolean saveSamplePoolInfo(TSampleAttachTypeSaveDTO dto) {
        if (!StringUtils.hasText(dto.getDeviceSerialNumber())) {
            log.info("缺少序列号信息");
            return false;
        }

        LambdaQueryWrapper<TSamplePoolConfigPO> query=new LambdaQueryWrapper<>();
        query.eq(TSamplePoolConfigPO::getDeviceSerialNumber,dto.getDeviceSerialNumber());
        TSamplePoolConfigPO po=dao.selectOne(query);
        if (po==null){
            po=new TSamplePoolConfigPO();
        }
        BeanUtils.copyProperties(dto,po);
        DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfoByAttachmentNum(dto.getAttachmentType());
        List<TSamplePoolStatus> list=list = createTSamplePoolStatuses(attachmentInfo.getSamplePoolNum());
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
    public R<Boolean> updateSamplePoolInfo(TSamplePoolInfoChangeDto changeDto) {
        if (!StringUtils.hasText(changeDto.getDeviceSerialNumber())) {
            log.info("未连接仪器");
            //未连接仪器
            return R.fail(YamlMessageSource.getMessageStatic("0000001D"));
        }

        if (!StringUtils.hasText(changeDto.getUpdateBy())) {
            log.info("缺少更新人信息");
            //请登录后操作！
            return R.fail(YamlMessageSource.getMessageStatic("00307004"));
        }

        LambdaQueryWrapper<TSamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(TSamplePoolConfigPO::getDeviceSerialNumber, changeDto.getDeviceSerialNumber());
        TSamplePoolConfigPO po = dao.selectOne(query);
        if (po == null) {
            //未找到该仪器附件配置
            return R.fail(YamlMessageSource.getMessageStatic("00106001"));
        }
        if (changeDto.getIsAutoChangePool() == null && changeDto.getPoolNum() == null && changeDto.getPoolStatus() != null) {
            changeDto.setIsAutoChangePool(changeDto.getPoolStatus());
        }
        if (changeDto.getIsAutoChangePool() != null) {
            if (changeDto.getIsAutoChangePool()) {
                if (StringUtils.hasText(po.getPoolInfo())) {
                    List<TSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), TSamplePoolStatus.class);
                    if (list != null && !list.isEmpty()) {
                        for (TSamplePoolStatus status : list) {
                            status.setPoolStatus(true);
                        }
                        po.setPoolInfo(JSON.toJSONString(list));
                    }
                }
            }
            else{
                if (StringUtils.hasText(po.getPoolInfo())) {
                    List<TSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), TSamplePoolStatus.class);
                    if (list != null && !list.isEmpty()) {
                        for (TSamplePoolStatus status : list) {
                            status.setPoolStatus(false);
                        }
                        po.setPoolInfo(JSON.toJSONString(list));
                    }
                }
            }
        }
        po.setUpdateBy(changeDto.getUpdateBy());
        if (changeDto.getIsAutoChangePool() != null) {
            if (StringUtils.hasText(po.getPoolInfo())) {
                List<TSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), TSamplePoolStatus.class);
                if (list == null || list.size() <= 1) {
                    //没有切换的样品池
                    return R.fail(YamlMessageSource.getMessageStatic("00106002"));
                }
            }
            po.setIsAutoChangePool(changeDto.getIsAutoChangePool());
        }
        if (changeDto.getPoolNum() != null) {
            List<TSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), TSamplePoolStatus.class);
            for (TSamplePoolStatus status : list){
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
        }
        else {
            if (changeDto.getPoolStatus() != null || changeDto.getPoolPurpose() != null) {
                //请选择要操作的样品池
                return R.fail(YamlMessageSource.getMessageStatic("00106003"));
            }
        }
        boolean b = dao.updateById(po) > 0;
        if (!b) {
            //更新失败
            return R.fail(YamlMessageSource.getMessageStatic("00000048"));
        }
        if (StringUtils.hasText(changeDto.getTaskId()) && po.getIsAutoChangePool()) {
            List<Integer> usefulLocation = getUsefulLocation(changeDto.getDeviceSerialNumber());
            publicUtilService.changeSampleLocation(changeDto.getTaskId(), usefulLocation);
        }
        return R.ok();
    }

    @Override
    public R<TSamplePoolInfoVO> querySamplePoolInfo(String deviceSerialNumber) {
        LambdaQueryWrapper<TSamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(TSamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        TSamplePoolConfigPO po = dao.selectOne(query);
        if (po == null || !StringUtils.hasText(po.getPoolInfo())) {
            //未找到该仪器附件配置
            return R.fail(YamlMessageSource.getMessageStatic("00106001"));
        }
        TSamplePoolInfoVO vo = new TSamplePoolInfoVO();
        BeanUtils.copyProperties(po, vo);
        List<TSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), TSamplePoolStatus.class);
        list.forEach(status -> status.setPoolName(status.getPoolNum() + "号"));
        vo.setPoolStatusList(list);
        return R.ok(vo);
    }

    @Override
    public List<Integer> getUsefulLocation(String deviceSerialNumber) {
        List<Integer> list = new ArrayList<>();
        LambdaQueryWrapper<TSamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(TSamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        TSamplePoolConfigPO po = dao.selectOne(query);
        if (po == null || !StringUtils.hasText(po.getPoolInfo())) {
            return list;
        }
        List<TSamplePoolStatus> poolStatuses = JSON.parseArray(po.getPoolInfo(), TSamplePoolStatus.class);
        for (TSamplePoolStatus status : poolStatuses) {
            if (status.getPoolStatus() && status.getPoolPurpose().equals(DeviceSamplePoolPurpose.MEASURE.getCode())) {
                list.add(status.getPoolNum());
            }
        }
        list.sort(Integer::compareTo);
        return list;
    }

    @Override
    public R<Boolean> setCurrentPool(String deviceSerialNumber, Integer poolNum, String updateBy) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info("未连接仪器");
            //未连接仪器
            return R.fail(YamlMessageSource.getMessageStatic("0000001D"));
        }
        if (!StringUtils.hasText(updateBy)) {
            log.info("缺少更新人信息");
            //请登录后操作
            return R.fail(YamlMessageSource.getMessageStatic("00307004"));
        }
        LambdaQueryWrapper<TSamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(TSamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        TSamplePoolConfigPO po = dao.selectOne(query);
        if (po == null) {
            po = new TSamplePoolConfigPO();
            po.setCreateBy(updateBy);
            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfo(deviceSerialNumber);
            po.setAttachmentType(attachmentInfo.getAttachmentNum());
            po.setDeviceSerialNumber(deviceSerialNumber);
            List<TSamplePoolStatus> poolInfo = createTSamplePoolStatuses(attachmentInfo.getAttachmentNum());
            po.setPoolInfo(JSON.toJSONString(poolInfo));
        }
        String ip = deviceInfoDAO.getIp(deviceSerialNumber);
        R<Boolean> r = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_OTHER);
        if (!r.getSuccess()) {
            log.info("设备被占用");
            return R.fail(r.getMessage());
        }
        R<String> stringR = devMessageUtil.setCurrentCellPos(poolNum, ip);
        if (!stringR.getSuccess()) {
            log.info("设置出错");
            deviceInfoService.releaseDevice(deviceSerialNumber);
            //设置出错
            return R.fail(YamlMessageSource.getMessageStatic("00106004"));
        }
        deviceInfoService.releaseDevice(deviceSerialNumber);
        po.setCurrentPoolNum(poolNum);
        po.setUpdateBy(updateBy);
        boolean b = dao.updateById(po) > 0;
        if (b) {
            return R.ok(true);
        } else {
            //更新失败
            return R.fail(YamlMessageSource.getMessageStatic("00000048"));
        }
    }

    @Override
    public R<TSamplePoolInfoVO> queryCalibrationSampleInfo(String deviceSerialNumber) {
        LambdaQueryWrapper<TSamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(TSamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        TSamplePoolConfigPO po = dao.selectOne(query);
        if (po == null || !StringUtils.hasText(po.getPoolInfo())) {
            //未找到该仪器附件配置
            return R.fail(YamlMessageSource.getMessageStatic("00106001"));
        }

        TSamplePoolInfoVO vo = new TSamplePoolInfoVO();
        BeanUtils.copyProperties(po, vo);
        List<TSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), TSamplePoolStatus.class);
        list.forEach(status -> status.setPoolName(status.getPoolNum() + "号"));
        vo.setPoolStatusList(list);
        return R.ok(vo);
    }
}
