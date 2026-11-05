/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceModelInfoDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceTypeInfoDAO;
import com.persee.workstation.uv_inexus.enums.measurementEnums.EnergyLampEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.ScanSpeedEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInitEntryInfo;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceModelInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceTypeInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.deviceUtil.DeviceConnectionManager;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceConfParam;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


@Service
@Slf4j
public class DeviceInfoServiceImpl implements DeviceInfoService {

    @Autowired
    DeviceInfoDAO dao;

    @Autowired
    DeviceModelInfoDAO deviceModelInfoDAO;

    @Autowired
    DeviceTypeInfoDAO deviceTypeInfoDAO;

    DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

    @Override
    public DeviceInfoPO getInfo(String deviceSerialNumber) {
        LambdaQueryWrapper<DeviceInfoPO> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(DeviceInfoPO::getDeviceSerialNumber, deviceSerialNumber);

        return dao.selectOne(queryWrapper);
    }

    @Override
    public String queryDeviceName(String deviceSerialNumber) {
        DeviceModelInfoPO modelInfo = getDeviceModelInfo(deviceSerialNumber);
        if (modelInfo == null) {
            return null;
        }
//        return modelInfo.getDevName() + modelInfo.getDevModelName();
        return modelInfo.getDevName();
    }

    @Override
    public String queryDeviceModelName(String deviceSerialNumber) {
        DeviceModelInfoPO modelInfo = getDeviceModelInfo(deviceSerialNumber);
        if (modelInfo == null) {
            return null;
        }
        return modelInfo.getDevModelName();
    }

    private DeviceModelInfoPO getDeviceModelInfo(String deviceSerialNumber) {
        if (!StringUtils.hasText(deviceSerialNumber) || deviceSerialNumber.length() < 8) {
            log.info("未传入序列号或序列号有误！");
            return null;
        }
        String devModel = deviceSerialNumber.substring(3, 8);
        LambdaQueryWrapper<DeviceModelInfoPO> modelQueryWrapper = new LambdaQueryWrapper<>();
        modelQueryWrapper.eq(DeviceModelInfoPO::getDevModel, devModel);
        DeviceModelInfoPO modelInfo = deviceModelInfoDAO.selectOne(modelQueryWrapper);
        if (modelInfo == null) {
            log.info("未查询到该型号的仪器信息");
            return null;
        }
        return modelInfo;
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
    public DeviceAttachmentTypeVO getAttachment(Integer attachmentType, String deviceSerialNumber) {
        String s = deviceSerialNumber.substring(3, 8);
        return dao.getAttachmentInfoByAttachmentNum(attachmentType,s);
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

        if (currentStatus.equals(Constants.DEVICE_STATUS_IDLE)) {
            String deviceId = deviceInfoPO.getDeviceId();
             deviceManager.setDevCurrentUseType(deviceId, null);
            deviceManager.setDevCurrentTaskId(deviceId, null);
        }

    }

    @Override
    public void updateDeviceCurrentStatus(String deviceSerialNumber, String useType, Integer currentStatus) {
        LambdaQueryWrapper<DeviceInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceInfoPO::getDeviceSerialNumber, deviceSerialNumber)
                .last("limit 1");
        DeviceInfoPO deviceInfoPO = dao.selectOne(queryWrapper);
        deviceInfoPO.setDeviceCurrentStatus(currentStatus);

        String deviceId = deviceInfoPO.getDeviceId();
        deviceManager.setDevCurrentUseType(deviceId, useType);

        dao.update(deviceInfoPO, queryWrapper);
    }

    @Override
    public void updateDeviceCurrentStatus(String deviceSerialNumber, String useType, String taskId, Integer currentStatus) {
        LambdaQueryWrapper<DeviceInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceInfoPO::getDeviceSerialNumber, deviceSerialNumber)
                .last("limit 1");
        DeviceInfoPO deviceInfoPO = dao.selectOne(queryWrapper);
        deviceInfoPO.setDeviceCurrentStatus(currentStatus);

        String deviceId = deviceInfoPO.getDeviceId();
        deviceManager.setDevCurrentUseType(deviceId, useType);
        if (taskId != null) {
            deviceManager.setDevCurrentTaskId(deviceId, taskId);
        }

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
            return R.fail("00208008");
        }
        //

        DeviceInfoPO info = getInfo(deviceSerialNumber);
        Integer isInited = info.getIsInited();

        if (!info.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS)) {
            //仪器已断开！
            return R.fail("00202007");
        }

        Integer isActivated = info.getIsActivated();
        if (isActivated == null || isActivated == 0) {
            //请先激活仪器
            return R.fail("00202001");
        }

        Integer connectStatus = info.getDeviceCurrentStatus();
        if (connectStatus.equals(Constants.DEVICE_STATUS_INIT) && !DetectionConstants.isDeviceIdle){
            //设备正在初始化，请等待
            return R.fail("00202002");
        }
        if (connectStatus.equals(Constants.DEVICE_STATUS_MEASURE) && !DetectionConstants.isDeviceIdle){
            //设备正在测量，请等待
            return R.fail("00202003");
        }

        if (!status.equals(Constants.DEVICE_STATUS_INIT)) {
            if (isInited == null || isInited == 0) {
                log.info("未初始化");
                //未初始化
                return R.fail("00202004");
            }

            if (!info.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                return R.fail("00202004");
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
                return R.fail("00202005");
            }
        }
        //设备正在使用中，请等待
        return R.fail("00202006");
    }

    @Override
    public R<Boolean> newUseDevice(String deviceSerialNumber, Integer status) {
        if (!StringUtils.hasText(deviceSerialNumber)){
            return R.fail("00208008");
        }
        //

        DeviceInfoPO info = getInfo(deviceSerialNumber);
        Integer isInited = info.getIsInited();
        String deviceId = info.getDeviceId();

        if (!info.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS)) {
            //仪器已断开！
            return R.fail("00202007");
        }

        if (!status.equals(Constants.DEVICE_STATUS_DIS_CONNECT)) {
            Integer isActivated = info.getIsActivated();
            if (isActivated == null || isActivated == 0) {
                //请先激活仪器
                return R.fail("00202001");
            }

            if (!status.equals(Constants.DEVICE_STATUS_INIT) && !info.getDeviceCurrentStatus().equals(Constants.CONNECT_DEVICE_STOP)) {
                if (isInited == null || isInited.equals(Constants.INIT_STATUS_NO)) {
                    log.info("未初始化");
                    //未初始化
                    return R.fail("00202004");
                }

                if (!info.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                    return R.fail("00202004");
                }
            }
        }

        DeviceConnectInfo device = deviceManager.getDevice(deviceId);

        Integer currentStatus = device.getDevCurrentStatus();
        Boolean isIdle = device.getIsIdle();

        if (currentStatus.equals(Constants.DEVICE_STATUS_INIT)){
            //设备正在初始化，请等待
            return R.fail("00202002");
        }
        if (currentStatus.equals(Constants.DEVICE_STATUS_MEASURE)){
            //设备正在测量，请等待
            return R.fail("00202003");
        }


        int count = 0;
        while (count < 40) {

            device = deviceManager.getDevice(deviceId);

            if (device.getIsIdle()){
                deviceManager.setDeviceIdleStatus(deviceId, false);
                deviceManager.setDevCurrentStatus(deviceId, status);
//
                log.info("[通知线程] 成功抢占线程!!!");
                return R.ok();
            }
            try {
                Thread.sleep(50);
                count ++;
            } catch (InterruptedException e) {
                log.error(e.getMessage());
                //系统异常
                return R.fail("00202005");
            }
        }
        //设备正在使用中，请等待
        return R.fail("00202006");
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

        if(!StringUtils.hasText(deviceId)) {
            deviceId = deviceManager.getDeviceId(deviceSerialNumber);
        }
        deviceManager.setDeviceIdleStatus(deviceId, true);
        deviceManager.setDevCurrentStatus(deviceId, Constants.DEVICE_STATUS_IDLE);
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
        return Constants.DEVICE_ACTIVE_NO;
    }

    @Override
    public Boolean isNeedSetSit(String deviceSerialNumber) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            return false;
        }

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

    @Override
    public List<DeviceInfoPO> getUserLastConnectDevice(String updateBy) {
        LambdaQueryWrapper<DeviceInfoPO> query = new LambdaQueryWrapper<>();
        query.eq(DeviceInfoPO::getUpdateBy, updateBy)
                .eq(DeviceInfoPO::getConnectStatus, Constants.CONNECT_DEVICE_STOP)
                .isNotNull(DeviceInfoPO::getDeviceId).ne(DeviceInfoPO::getDeviceId, "")
                .isNotNull(DeviceInfoPO::getDeviceSerialNumber).ne(DeviceInfoPO::getDeviceSerialNumber, "")
                .isNotNull(DeviceInfoPO::getDevicePort).ne(DeviceInfoPO::getDevicePort, "")
                .isNotNull(DeviceInfoPO::getDeviceMac).ne(DeviceInfoPO::getDeviceMac, "")
                .orderByDesc(DeviceInfoPO::getUpdated);
        List<DeviceInfoPO> pos = dao.selectList(query);

        return pos;
    }

    //    @PostConstruct        需注释
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

    @Override
    public R<DeviceMeasureVO> queryDeviceTypeInfo(String deviceSerialNumber) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            DeviceMeasureVO ans = new DeviceMeasureVO();
            ans.setConnectMode(1);
            ans.setMinWavelength("190");
            ans.setMaxWavelength("1100");
            ans.setMaxLampWavelength("390");
            ans.setMinLampWavelength("330");
            ans.setBandWidth(Collections.singletonList("2"));
            ans.setGain(Arrays.asList(1, 2, 3, 4));
            ans.setScanInterval(Arrays.asList("0.1", "0.2", "0.5", "1.0", "2.0", "5.0"));
            ans.setResponseTime(Arrays.asList("0.01", "0.02", "0.05", "0.1", "0.2", "0.5", "1", "2"));
            List<DeviceConfParam> list = new ArrayList<>();
            list.add(new DeviceConfParam(WaveLengthEnum.abs.getName(), WaveLengthEnum.abs.getCode()));
            list.add(new DeviceConfParam(WaveLengthEnum.tPercent.getName(), WaveLengthEnum.tPercent.getCode()));
            list.add(new DeviceConfParam(WaveLengthEnum.es.getName(), WaveLengthEnum.es.getCode()));
            list.add(new DeviceConfParam(WaveLengthEnum.er.getName(), WaveLengthEnum.er.getCode()));
            ans.setLuminosity(list);
            list = new ArrayList<>();
            list.add(new DeviceConfParam(EnergyLampEnums.Tungsten.getName(), EnergyLampEnums.Tungsten.getIdentity()));
            list.add(new DeviceConfParam(EnergyLampEnums.Deuterium.getName(), EnergyLampEnums.Deuterium.getIdentity()));
            ans.setLampType(list);
            list = new ArrayList<>();
            list.add(new DeviceConfParam(ScanSpeedEnum.SLOW.getName(), ScanSpeedEnum.SLOW.getCode()));
            list.add(new DeviceConfParam(ScanSpeedEnum.MEDIUM.getName(), ScanSpeedEnum.MEDIUM.getCode()));
            list.add(new DeviceConfParam(ScanSpeedEnum.FAST.getName(), ScanSpeedEnum.FAST.getCode()));
            list.add(new DeviceConfParam(ScanSpeedEnum.SUPER_FAST.getName(), ScanSpeedEnum.SUPER_FAST.getCode()));
            ans.setScanSpeed(list);
            ans.setFilterWavelength(Arrays.asList(762, 555, 359));
            sortScanInterval(ans);
            return R.ok(ans);
        }

        DeviceInfoPO info = getInfo(deviceSerialNumber);
        if (info == null) {
            //未找到该设备
            return R.fail("00203005");
        }

        String devModel = deviceSerialNumber.substring(3, 8);

        LambdaQueryWrapper<DeviceModelInfoPO> query = new LambdaQueryWrapper<>();
        query.eq(DeviceModelInfoPO::getDevModel, devModel);

        DeviceModelInfoPO deviceModelInfoPO = deviceModelInfoDAO.selectOne(query);
        if (deviceModelInfoPO == null) {
            //未识别该型号
            return R.fail("00203006");
        }

        DeviceMeasureVO ans = new DeviceMeasureVO();

        Integer devTypeId = deviceModelInfoPO.getDevTypeId();

        DeviceTypeInfoPO deviceTypeInfoPO = deviceTypeInfoDAO.selectById(devTypeId);
        ans = changeType(deviceTypeInfoPO);
        BeanUtils.copyProperties(deviceModelInfoPO, ans);

        ans.setBandWidth(deviceModelInfoPO.getSpacingWidth() == null ? null : deviceModelInfoPO.getSpacingWidth());
        ans.setFilterWavelength(deviceModelInfoPO.getFilterWavelength() == null ? null : deviceModelInfoPO.getFilterWavelength());

        JSONObject scanSpeedJson = deviceModelInfoPO.getScanSpeed();
        if (scanSpeedJson != null) {
            List<DeviceConfParam> scanSpeedList = new ArrayList<>();
            scanSpeedJson.forEach((k, v) -> {
                DeviceConfParam param = new DeviceConfParam();
                param.setParamName(v.toString());
                param.setParamValue(k);
                scanSpeedList.add(param);
            });
            ans.setScanSpeed(scanSpeedList);
        } else {
            ans.setScanSpeed(null);
        }

        sortScanInterval(ans);

        return R.ok(ans);
    }

    /**
     * 波长间隔按数值从小到大排序后回填（非数值项排在最后）
     *
     * @param vo 设备测量参数
     */
    private void sortScanInterval(DeviceMeasureVO vo) {
        if (vo == null) {
            return;
        }
        List<String> scanInterval = vo.getScanInterval();
        if (scanInterval == null || scanInterval.size() < 2) {
            return;
        }
        List<String> sorted = new ArrayList<>(scanInterval);
        sorted.sort(Comparator.comparingDouble(item -> {
            try {
                return Double.parseDouble(item);
            } catch (NumberFormatException ignored) {
                return Double.MAX_VALUE;
            }
        }));
        vo.setScanInterval(sorted);
    }

    @Override
    public Double queryCoefficient(String deviceSerialNumber) {

        String devModel = deviceSerialNumber.substring(3, 8);

        LambdaQueryWrapper<DeviceModelInfoPO> query = new LambdaQueryWrapper<>();
        query.eq(DeviceModelInfoPO::getDevModel, devModel);

        DeviceModelInfoPO deviceModelInfoPo = deviceModelInfoDAO.selectOne(query);

        if (deviceModelInfoPo == null) {
            //未识别该型号
            return 50.0;
        }

        Integer devTypeId = deviceModelInfoPo.getDevTypeId();

        DeviceTypeInfoPO deviceTypeInfoPO = deviceTypeInfoDAO.selectById(devTypeId);
        if (deviceTypeInfoPO == null) {
            //未识别该型号
            return 50.0;
        }
        return deviceTypeInfoPO.getCoefficient() == null ? 50.0 : deviceTypeInfoPO.getCoefficient();
    }

    @Override
    public String queryDeviceTypeBySerialNumber(String deviceSerialNumber) {
        List<String> list = deviceTypeInfoDAO.queryDeviceTypeBySerialNumber(deviceSerialNumber);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return "";
    }

    private DeviceMeasureVO changeType(DeviceTypeInfoPO po) {

        DeviceMeasureVO ans = new DeviceMeasureVO();
        List<DeviceConfParam> middle = new ArrayList<>();
        BeanUtils.copyProperties(po, ans);
        po.getLuminosity().forEach(i -> {
            DeviceConfParam param = new DeviceConfParam();
            param.setParamValue(String.valueOf(i + 1));
            param.setParamName(WaveLengthEnum.getName(String.valueOf(i + 1)));
            middle.add(param);
        });
        ans.setLuminosity(middle);

        List<DeviceConfParam> lamp = new ArrayList<>();
        po.getLampType().forEach(i -> {
            DeviceConfParam param = new DeviceConfParam();
            param.setParamValue(i.toString());
            param.setParamName(EnergyLampEnums.getName(i.toString()));
            lamp.add(param);
        });
        ans.setLampType(lamp);

        return ans;

    }

    @Override
    public  List<DeviceAttachmentTypeVO> getAttachmentListByName(String keyword){
        if (!StringUtils.hasText(keyword)) {
            log.info("未传入附件名称关键字");
            return null;
        }
        return dao.getAttachmentListByName(keyword);
    }



}