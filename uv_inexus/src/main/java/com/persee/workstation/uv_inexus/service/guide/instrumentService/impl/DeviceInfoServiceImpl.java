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
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceModelInfoDAO;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInitEntryInfo;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceModelInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.deviceUtil.DeviceConnectionManager;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;


@Service
@Slf4j
public class DeviceInfoServiceImpl implements DeviceInfoService {

    @Autowired
    DeviceInfoDAO dao;

    @Autowired
    DeviceModelInfoDAO deviceModelInfoDAO;

    DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

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
    public DeviceAttachmentTypeVO getAttachment(Integer attachmentType) {
        return dao.getAttachmentInfoByAttachmentNum(attachmentType);
    }

    @Override
    public DeviceAttachmentTypeVO getAttachmentInfo(String deviceSerialNumber) {

        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info("未传入序列号");
            return null;
        }

        return dao.getAttachmentInfo(deviceSerialNumber);
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
        if (!StringUtils.hasText(deviceSerialNumber)){
            return R.fail("仪器未连接");
        }
        //

        DeviceInfoPO info = getInfo(deviceSerialNumber);
        Integer isInited = info.getIsInited();

        if (!info.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS)) {
            //仪器已断开！
            return R.fail(YamlMessageSource.getMessageStatic("00202007"));
        }

        Integer isActivated = info.getIsActivated();
        if (isActivated == null || isActivated == 0) {
            //请先激活仪器
            return R.fail(YamlMessageSource.getMessageStatic("00202001"));
        }

        Integer connectStatus = info.getDeviceCurrentStatus();
        if (connectStatus.equals(Constants.DEVICE_STATUS_INIT) && !DetectionConstants.isDeviceIdle){
            //设备正在初始化，请等待
            return R.fail(YamlMessageSource.getMessageStatic("00202002"));
        }
        if (connectStatus.equals(Constants.DEVICE_STATUS_MEASURE) && !DetectionConstants.isDeviceIdle){
            //设备正在测量，请等待
            return R.fail(YamlMessageSource.getMessageStatic("00202003"));
        }

        if (!status.equals(Constants.DEVICE_STATUS_INIT)) {
            if (isInited == null || isInited == 0) {
                log.info("未初始化");
                //未初始化
                return R.fail(YamlMessageSource.getMessageStatic("00202004"));
            }

            if (!info.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                return R.fail(YamlMessageSource.getMessageStatic("00202004"));
            }
        }


        int count = 0;
        while (count < 40) {
            if (DetectionConstants.isDeviceIdle){
                DetectionConstants.isDeviceIdle = false;
                updateDeviceCurrentStatus(deviceSerialNumber, status);
                log.info("[通知线程] 成功抢占线程!!!");
                return R.ok();
            }
            try {
                Thread.sleep(50);
                count ++;
            } catch (InterruptedException e) {
                log.error(e.getMessage());
                //系统异常
                return R.fail(YamlMessageSource.getMessageStatic("00202005"));
            }
        }
        //设备正在使用中，请等待
        return R.fail(YamlMessageSource.getMessageStatic("00202006"));
    }

    @Override
    public R<Boolean> newUseDevice(String deviceSerialNumber, Integer status) {
        if (!StringUtils.hasText(deviceSerialNumber)){
            return R.fail("仪器未连接");
        }
        //

        DeviceInfoPO info = getInfo(deviceSerialNumber);
        Integer isInited = info.getIsInited();
        String deviceId = info.getDeviceId();

        if (!info.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS)) {
            //仪器已断开！
            return R.fail(YamlMessageSource.getMessageStatic("00202007"));
        }

        Integer isActivated = info.getIsActivated();
        if (isActivated == null || isActivated == 0) {
            //请先激活仪器
            return R.fail(YamlMessageSource.getMessageStatic("00202001"));
        }

        Integer connectStatus = info.getDeviceCurrentStatus();
        if (connectStatus.equals(Constants.DEVICE_STATUS_INIT) && !DetectionConstants.isDeviceIdle){
            //设备正在初始化，请等待
            return R.fail(YamlMessageSource.getMessageStatic("00202002"));
        }
        if (connectStatus.equals(Constants.DEVICE_STATUS_MEASURE) && !DetectionConstants.isDeviceIdle){
            //设备正在测量，请等待
            return R.fail(YamlMessageSource.getMessageStatic("00202003"));
        }

        if (!status.equals(Constants.DEVICE_STATUS_INIT)) {
            if (isInited == null || isInited == 0) {
                log.info("未初始化");
                //未初始化
                return R.fail(YamlMessageSource.getMessageStatic("00202004"));
            }

            if (!info.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                return R.fail(YamlMessageSource.getMessageStatic("00202004"));
            }
        }

        int count = 0;
        while (count < 40) {

            DeviceConnectInfo device = deviceManager.getDevice(deviceId);

            if (device.getIsIdle()){
                deviceManager.setDeviceIdleStatus(deviceId, false);
                updateDeviceCurrentStatus(deviceSerialNumber, status);
                log.info("[通知线程] 成功抢占线程!!!");
                return R.ok();
            }
            try {
                Thread.sleep(50);
                count ++;
            } catch (InterruptedException e) {
                log.error(e.getMessage());
                //系统异常
                return R.fail(YamlMessageSource.getMessageStatic("00202005"));
            }
        }
        //设备正在使用中，请等待
        return R.fail(YamlMessageSource.getMessageStatic("00202006"));
    }

    @Override
    public void releaseDevice(String deviceSerialNumber) {
        DetectionConstants.isDeviceIdle = true;
        if (StringUtils.hasText(deviceSerialNumber)){
            updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
        }
    }


    @Override
    public void newReleaseDevice(String deviceSerialNumber) {


        DeviceInfoPO info = getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        deviceManager.setDeviceIdleStatus(deviceId, true);

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

    @Override
    public Boolean isNeedSetSit(String deviceSerialNumber) {

        String devModel = deviceSerialNumber.substring(3, 8);

        LambdaQueryWrapper<DeviceModelInfoPO> query = new LambdaQueryWrapper<>();
        query.eq(DeviceModelInfoPO::getDevModel, devModel);

        DeviceModelInfoPO po = deviceModelInfoDAO.selectOne(query);
        if (po != null && po.getInitEntry() != null) {
            for (DeviceInitEntryInfo entry : po.getInitEntry()) {
                if (entry.getInitComId().contains("SetSlit1")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<DeviceInfoPO> getLastConnectDevice() {
        LambdaQueryWrapper<DeviceInfoPO> query = new LambdaQueryWrapper<>();
        query.eq(DeviceInfoPO::getConnectStatus, Constants.CONNECT_DEVICE_SUCCESS)
                .isNotNull(DeviceInfoPO::getDeviceId).ne(DeviceInfoPO::getDeviceId, "")
                .isNotNull(DeviceInfoPO::getDeviceSerialNumber).ne(DeviceInfoPO::getDeviceSerialNumber, "")
                .isNotNull(DeviceInfoPO::getDevicePort).ne(DeviceInfoPO::getDevicePort, "")
                .isNotNull(DeviceInfoPO::getIp).ne(DeviceInfoPO::getIp, "");
        return dao.selectList(query);
    }

    @PostConstruct
    @Override
    public void checkDeviceStatus() {
        log.info("[检查设备状态] 开始检查设备状态");
        LambdaQueryWrapper<DeviceInfoPO> query = new LambdaQueryWrapper<>();
        List<DeviceInfoPO> pos = dao.selectList(query);
        if (pos == null || pos.isEmpty()) {
            return;
        }
        for (DeviceInfoPO po : pos) {
            log.info("初始化设备：{}", po.getDeviceSerialNumber());

//            po.setIsInited(Constants.INIT_STATUS_NO);
            po.setConnectStatus(Constants.CONNECT_DEVICE_NO);
            po.setDeviceCurrentStatus(Constants.DEVICE_STATUS_IDLE);
            dao.updateById(po);

        }
    }


}
