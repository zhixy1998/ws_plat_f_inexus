/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.*;
import com.persee.workstation.uv_inexus.dto.concise.common.FSampleAttachTypeSaveDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.EnergyLampEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.*;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.InitRecordService;
import com.persee.workstation.uv_inexus.util.DeviceMessageUtils;
import com.persee.workstation.uv_inexus.util.changeUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import com.persee.workstation.uv_inexus.websocket.liveDetection.LiveDetectionWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
public class DeviceManageServiceImpl implements DeviceManageService {

    @Autowired
    DevConfigDAO devConfigDAO;

    @Autowired
    CalibrationTempDAO tempDao;

    @Autowired
    DeviceInfoDAO deviceInfoDAO;

    @Autowired
    InitRecordService initRecordService;

    @Autowired
    DeviceInfoService infoService;

    @Autowired
    DeviceMessageUtils messageUtils;

    @Autowired
    DevMessageUtil devMessageUtil;

    @Autowired
    FormulaUtil formulaUtil;

    @Autowired
    SamplePoolConfigService samplePoolConfigService;

    @Autowired
    DeviceModelInfoDAO deviceModelInfoDAO;

    @Autowired
    DeviceTypeInfoDAO deviceTypeInfoDAO;

    @Autowired
    DeviceCalibrationConfigDAO calibrationConfigDAO;

    @Autowired
    DeviceCalibrationParamDAO calibrationParamDAO;

//    @Autowired
//    LiveDetectionDevicesWebSocket liveDetectionManyDevicesWebSocket;

    @Autowired
    LiveDetectionWebSocket liveDetectionWebSocket;

    @Autowired
    DevOperationUtilService operationUtilService;

    @Value("${sys-manage-default.defaultId}")
    String defaultId;

    @Value("${sys-manage-default.userId}")
    String userId;


    @Value("${device.default.sendPort}")
    Integer sendPort;

    @Value("${device.default.receivePort}")
    Integer receivePort;

    private static volatile Map<String, Boolean> stopInit = new HashMap<>();

    @Override
    public R<DeviceInfoVO> queryDeviceInfo(String deviceSerialNumber, String updateBy) {
//        log.info("查询仪器信息");
        DeviceInfoVO vo = new DeviceInfoVO();

        List<DeviceInitInfo> infoList = new ArrayList<>();

        DeviceInfoPO deviceInfoPO = infoService.getInfo(deviceSerialNumber);
        if (deviceInfoPO == null || deviceInfoPO.getId() == null) {
            log.info("未找到连接仪器信息！");
            //未查找到仪器信息
            return R.fail(YamlMessageSource.getMessageStatic("00203001"));
        }

        List<InitRecordPO> initList = initRecordService.searchInitStatus(deviceSerialNumber, deviceInfoPO.getId());
        if (initList.isEmpty()) {
            log.info("查询初始化状态异常");
            //查询初始化状态异常
            return R.fail(false, YamlMessageSource.getMessageStatic("00203002"));
        }

        vo.setIsInited(deviceInfoPO.getIsInited());
        vo.setConnectStatus(deviceInfoPO.getConnectStatus());
        vo.setUserName(updateBy);
        vo.setIsDevOpened(deviceInfoPO.getIsDevOpened());
        vo.setDeviceSerialNumber(deviceSerialNumber);
        vo.setDeviceName(deviceInfoPO.getDeviceName());
        vo.setDeviceModelName(deviceInfoPO.getDeviceModelName());
        vo.setIsActivated(deviceInfoPO.getIsActivated() == null ? 0 : deviceInfoPO.getIsActivated());

        String connect = DeviceConnectMessageEnum.getName(deviceInfoPO.getConnectStatus()) + " IP="
                + deviceInfoPO.getIp() + " port=" + sendPort;

        vo.setConnectMessage(connect);

        String attachmentType = infoService.getAttachmentType(deviceSerialNumber);
        vo.setAppendMessage(attachmentType);

        for (InitRecordPO recordPO : initList) {
            DeviceInitInfo info = new DeviceInitInfo();
            info.setInitTaskNum(recordPO.getInitTaskNum());
            info.setInitProject(recordPO.getInitCmdName());
            info.setInitStatus(recordPO.getInitStatus());

            infoList.add(info);
        }
        vo.setIntMessage(infoList);


        return R.ok(vo);
    }

    @Override
    public R<Integer> queryDeviceStatus(String deviceSerialNumber) {

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null || info.getDeviceCurrentStatus() == null || !StringUtils.hasText(info.getDeviceId())) {
            log.info("未找到连接仪器信息！");
            //未查找到仪器信息
            return R.fail(YamlMessageSource.getMessageStatic("00203001"));
        }

//        DeviceConnectionManager manager = DeviceConnectionManager.getInstance();
//        Integer first = null;
//        Integer two = null;
//        first = manager.getDevCurrentStatus(info.getDeviceId());
//
//        try {
//            Thread.sleep(1000);
//        } catch (Exception e) {
//            log.error("查询仪器状态异常！", e);
//        }
//        two = manager.getDevCurrentStatus(info.getDeviceId());
//
//        if (first == null || two == null){
//            return R.fail("获取当前状态值失败");
//        }
//
//        boolean flag = first.equals(Constants.DEVICE_STATUS_IDLE) || first.equals(Constants.DEVICE_STATUS_ECHO);
//        boolean flag2 = two.equals(Constants.DEVICE_STATUS_IDLE) || two.equals(Constants.DEVICE_STATUS_ECHO);
//
//
//        if (flag && flag2 && !info.getDeviceCurrentStatus().equals(Constants.DEVICE_STATUS_IDLE)) {
//            infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
//            return R.ok(Constants.DEVICE_STATUS_IDLE);
//        } else {
//            return R.ok(info.getDeviceCurrentStatus());
//        }

        return R.ok(info.getDeviceCurrentStatus());
    }

    @Override
    public R<Boolean> init(DeviceInitDTO initDTO) {
        if (initDTO.getDeviceSerialNumber().isEmpty()) {
            log.info("未传仪器序列号！");
            //缺少序列号
            return R.fail(YamlMessageSource.getMessageStatic("00203003"));
        }

        DeviceInfoPO infoPO = infoService.getInfo(initDTO.getDeviceSerialNumber());
        if (infoPO == null || !StringUtils.hasText(infoPO.getId()) || !StringUtils.hasText(infoPO.getDeviceModel())) {
            log.info("未找到连接仪器信息！");
            //未查找到仪器信息
            return R.fail(YamlMessageSource.getMessageStatic("00203001"));
        }
        log.info("调用机器初始化接口");

        R<Boolean> r = infoService.useDevice(initDTO.getDeviceSerialNumber(), Constants.DEVICE_STATUS_INIT);
        if (!r.getSuccess()) {
            log.info(r.getMessage());
            return R.fail(r.getMessage());
        }
        infoPO = infoService.getInfo(initDTO.getDeviceSerialNumber());

        LambdaQueryWrapper<DeviceModelInfoPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceModelInfoPO::getDevModel, infoPO.getDeviceModel());
        DeviceModelInfoPO modelInfoPO = deviceModelInfoDAO.selectOne(wrapper);

        List<DeviceInitEntryInfo> initEntry = modelInfoPO.getInitEntry();

        DeviceAttachmentTypeVO attachmentInfo = infoService.getAttachmentInfo(initDTO.getDeviceSerialNumber());
        Integer kind = attachmentInfo.getAttachmentKind();

        if (kind.equals(DeviceAttachmentKindEnum.FIVE_POOL.getType()) || kind.equals(DeviceAttachmentKindEnum.EIGHT_POOL.getType())) {
            List<DeviceInitEntryInfo> allInitEntry = addInitEntry(initEntry);
            initRecordService.initAllTask(initDTO.getDeviceSerialNumber(), infoPO.getId(), allInitEntry);
        } else {
            initRecordService.initAllTask(initDTO.getDeviceSerialNumber(), infoPO.getId(), initEntry);
        }

        infoPO.setIsInited(Constants.INIT_STATUS_RUNNING);
        infoService.saveOrUpdateInfo(infoPO);


        Thread initThread = new Thread(() -> {
            try {
                DeviceInfoPO finalInfoPO = infoService.getInfo(initDTO.getDeviceSerialNumber());
                stopInit.put(initDTO.getDeviceSerialNumber(), false);
                newStartInit(finalInfoPO, attachmentInfo.getAttachmentKind());
            } catch (Exception e) {
                DetectionConstants.isDeviceIdle = true;
                throw new RuntimeException(e);
            }
        });
        initThread.start();

        return R.ok(true);
    }

    private void newStartInit(DeviceInfoPO infoPO, Integer attachmentType) throws Exception {
        log.info("自检开始");
        String ip = infoPO.getIp();

        List<InitRecordPO> list = initRecordService.searchInitStatus(infoPO.getDeviceSerialNumber(), infoPO.getId());
        for (InitRecordPO recordPO : list) {
            updateRunning(recordPO);
            recordPO = initRecordService.selectById(recordPO.getId());

            List<String> cmdIdList = recordPO.getInitCmdIdList();

            int count = 0;
            while (count < cmdIdList.size() && !stopInit.get(infoPO.getDeviceSerialNumber())) {
                String cmdId = cmdIdList.get(count);
                Command command = dealCommand(attachmentType, cmdId);
                try {
                    R<String> initDevice = devMessageUtil.initDevice(command, ip);
                    if (!initDevice.getSuccess()) {
                        log.info("{}指令初始化异常", cmdId);
                        failDeal(infoPO, recordPO);
                        devMessageUtil.setDeviceInitStatus(ip, 1);
                        infoService.releaseDevice(infoPO.getDeviceSerialNumber());
                        return;
                    }
                    count++;
                    log.info("{}指令初始化成功", cmdId);
                } catch (Exception e) {
                    log.info("{}指令初始化异常", cmdId);
                    failDeal(infoPO, recordPO);
                    devMessageUtil.setDeviceInitStatus(ip, 1);
                    infoService.releaseDevice(infoPO.getDeviceSerialNumber());
                    return;

                }


            }
            if (count != cmdIdList.size()) {
                log.info("自检已停止");
                updateStop(recordPO, infoPO);
                infoService.releaseDevice(infoPO.getDeviceSerialNumber());
                return;
            }
            log.info("{}成功", recordPO.getInitCmdName());
            updateSuccess(recordPO);

        }


        infoPO.setIsInited(Constants.INIT_STATUS_SUCCESS);
        infoService.saveOrUpdateInfo(infoPO);
        synchronized (infoPO) {
            Command mode = new Command("SetPhotoMode", new Object[]{WaveLengthEnum.abs.getCode()});
            Boolean b = devMessageUtil.wavelengthLocate(mode, ip);

            Double step = formulaUtil.calculateStep(660.0, infoPO.getDeviceSerialNumber());
            Command command = new Command("SetWl", new Object[]{step});
            Boolean locate = devMessageUtil.wavelengthLocate(command, ip);
            DetectionConstants.currentWavelength = String.valueOf(660);

            R<String> stringR = devMessageUtil.setDeviceInitStatus(ip, 1);

            infoService.releaseDevice(infoPO.getDeviceSerialNumber());

            Constants.devConnectInfo.put(infoPO.getDeviceSerialNumber(), true);
            liveDetectionWebSocket.startMeasure(infoPO.getDeviceSerialNumber());
        }

    }

    private static Command dealCommand(Integer attachmentType, String cmdId) {
        Command command;
        if (cmdId.equals("SetSlit1")) {
            if (attachmentType.equals(DeviceAttachmentKindEnum.INTEGRAL_BALL.getType())) {
                command = new Command(cmdId, new Object[]{5});
            } else {
                command = new Command(cmdId, new Object[]{2});
            }
        } else if (cmdId.equals("SetCellPos")) {
            command = new Command(cmdId, new Object[]{1});
        } else {
            command = new Command(cmdId, new Object[]{});
        }
        return command;
    }

    private void updateSuccess(InitRecordPO recordPO) {
        recordPO.setInitStatus(Constants.INIT_TASK_SUCCESS);
        initRecordService.saveOrUpdateInitTask(recordPO);
    }

    private void updateStop(InitRecordPO recordPO, DeviceInfoPO po) {
        recordPO.setInitStatus(Constants.INIT_TASK_STOP);
        initRecordService.saveOrUpdateInitTask(recordPO);

        po.setIsInited(Constants.INIT_STATUS_STOP);
        infoService.saveOrUpdateInfo(po);
    }

    private void updateRunning(InitRecordPO recordPO) {
        recordPO.setInitStatus(Constants.INIT_TASK_ING);
        initRecordService.saveOrUpdateInitTask(recordPO);
    }

    private void failDeal(DeviceInfoPO po, InitRecordPO recordPO) {

        recordPO.setInitStatus(Constants.INIT_TASK_FALSE);
        initRecordService.saveOrUpdateInitTask(recordPO);

        po.setIsInited(Constants.INIT_STATUS_FALSE);
        infoService.saveOrUpdateInfo(po);
    }

    @Override
    public R<Boolean> stopInit(String deviceSerialNumber, String updateBy) {
        stopInit.put(deviceSerialNumber, true);

        return R.ok();
    }


    @Override
    public Boolean startDevice(DeviceStartDTO startDTO) {
        return null;
    }

    @Override
    public Boolean softClose(DeviceSoftCloseDTO closeDTO) {
        return null;
    }



    @Override
    public R<Boolean> closeOrOpenLamp(String lampType, Integer status, String deviceSerialNumber) {

//        SetWLampStatus  （设置钨灯开关：1开0关）
//        SetDLampStatus  （设置氘灯开关：1开0关）
//        SetLampPos （设置灯位置：0钨灯1氘灯）

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
        SetParam param = new SetParam();
        if (lampType.equals(EnergyLampEnums.Tungsten.getIdentity())) {
            param.setWLampStatus(status);
        } else if (lampType.equals(EnergyLampEnums.Deuterium.getIdentity())) {
            param.setDLampStatus(status);
        } else {
            log.info("灯类型出错");
            return R.fail("灯类型出错！");
        }

        inputParam.setParam(param);

        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        }

        return R.ok(true);
    }

    @Override
    public R<Boolean> changeLamp(String currentLampType, String deviceSerialNumber) {

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
        SetParam param = new SetParam();

        if (currentLampType.equals(EnergyLampEnums.Tungsten.getIdentity())) {
            param.setLampPos(1);
        } else if (currentLampType.equals(EnergyLampEnums.Deuterium.getIdentity())) {
            param.setLampPos(0);
        } else {
            log.info("当前灯类型出错");
            return R.fail("灯类型出错！");
        }
        inputParam.setParam(param);

        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        }

        return R.ok(true);
    }

    @Override
    public Boolean disConnect(String deviceSerialNumber, String updateBy) {
        DeviceInfoPO infoPO = infoService.getInfo(deviceSerialNumber);
        Constants.devConnectInfo.remove(deviceSerialNumber);
        devMessageUtil.disConnect(infoPO.getIp());

        infoPO.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
        infoPO.setUpdateBy(updateBy);
        infoService.saveOrUpdateInfo(infoPO);
        ResponseDeal.Sn = null;
        return true;
    }

    @Override
    @DS("guide")
    public R<List<FindDeviceVO>> findDevice() throws Exception {


        List<Command> list = new ArrayList<>();
        list.add(new Command(FindDevEnum.FindDevice.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.NewIP.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.Connect.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetSn.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetAccessoryType.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetInitStatus.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetCellPos.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetWlList.getComId(), new Object[]{}));

        List<FindDeviceVO> devices = new ArrayList<>();

        R<FindDeviceVO> connected = devMessageUtil.connectDevice(list);
        DetectionConstants.isDeviceIdle = true;
        if (connected.getSuccess()) {
            log.info("找到设备");
            FindDeviceVO deviceVO = connected.getData();
            String ip = deviceVO.getIp();

            DeviceInfoPO po = infoService.getInfo(deviceVO.getDeviceSerialNumber());
            if (po == null) {
                po = new DeviceInfoPO();
            }

            if (deviceVO.getIsInited() == null || deviceVO.getIsInited() == 0) {
                po.setIsInited(Constants.INIT_STATUS_NO);
            }

            if (po.getIsInited() == null) {
                po.setIsInited(Constants.INIT_STATUS_NO);
            }
            Constants.devConnectInfo.put(deviceVO.getDeviceSerialNumber(), true);
            if (po.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                liveDetectionWebSocket.startMeasure(deviceVO.getDeviceSerialNumber());

                Command mode = new Command("SetPhotoMode", new Object[]{WaveLengthEnum.abs.getCode()});
                Boolean b = devMessageUtil.wavelengthLocate(mode, ip);

                Double step = formulaUtil.calculateStep(660.0, deviceVO.getLinearSegment());
                Command command = new Command("SetWl", new Object[]{step});
                Boolean locate = devMessageUtil.wavelengthLocate(command, ip);

                DetectionConstants.currentWavelength = "660";
            }

            po.setIp(ip);
            po.setDeviceSerialNumber(deviceVO.getDeviceSerialNumber());
            po.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
            po.setIsDevOpened(1);
            po.setAttachmentType(deviceVO.getAttachmentType());
            po.setDevicePort(sendPort);
            po.setDeviceCurrentStatus(Constants.DEVICE_STATUS_IDLE);
            po.setLinearSegment(deviceVO.getLinearSegment());

            //TODO 根据仪器序列号，获取型号信息，还需要调整
            String deviceModel = deviceVO.getDeviceSerialNumber().substring(3, 8);
            po.setDeviceModel(deviceModel);

            LambdaQueryWrapper<DeviceModelInfoPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceModelInfoPO::getDevModel, deviceModel);
            DeviceModelInfoPO modelInfoPO = deviceModelInfoDAO.selectOne(wrapper);

            deviceVO.setDeviceModelName(modelInfoPO.getDevName());
            po.setDeviceModelName(modelInfoPO.getDevName());

            if (StringUtils.hasText(po.getDeviceName())) {
                deviceVO.setDeviceName(po.getDeviceName());
            } else {
                deviceVO.setDeviceName(modelInfoPO.getDevName());
                po.setDeviceName(modelInfoPO.getDevName());
            }

            Integer i = infoService.queryDeviceActivationStatus(deviceVO.getDeviceSerialNumber());
            po.setIsActivated(i);

            List<DeviceInitEntryInfo> initEntry = modelInfoPO.getInitEntry();

            //附件处理
            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfoByAttachmentNum(deviceVO.getAttachmentType());
            LambdaQueryWrapper<InsConfigPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(InsConfigPO::getDeviceSerialNumber, deviceVO.getDeviceSerialNumber());
            if (attachmentInfo == null) {
                attachmentInfo = new DeviceAttachmentTypeVO();
                attachmentInfo.setAttachmentNum(12);
                attachmentInfo.setAttachmentName("未识别");
            }

            attachmentInfo.setDeviceSerialNumber(deviceVO.getDeviceSerialNumber());
            FSampleAttachTypeSaveDTO dto = new FSampleAttachTypeSaveDTO();

            BeanUtils.copyProperties(deviceVO, dto);
            dto.setAttachmentTypeName(attachmentInfo.getAttachmentName());
            samplePoolConfigService.saveSamplePoolInfo(dto);

            po.setAttachmentName(attachmentInfo.getAttachmentName());
            infoService.saveOrUpdateInfo(po);
            boolean initRecord = true;

            List<InitRecordPO> initList = initRecordService.searchInitStatus(deviceVO.getDeviceSerialNumber(), po.getId());
            if (initList == null || initList.isEmpty()) {
                initRecord = false;
            }

            if (deviceVO.getIsInited() == 0 || !initRecord) {
                if (attachmentInfo.getAttachmentKind().equals(1)) {
                    List<DeviceInitEntryInfo> allInitEntry = addInitEntry(initEntry);
                    initRecordService.initAllTask(deviceVO.getDeviceSerialNumber(), po.getId(), allInitEntry);
                }
                initRecordService.initAllTask(deviceVO.getDeviceSerialNumber(), po.getId(), initEntry);
            }

            BeanUtils.copyProperties(po, deviceVO);
            deviceVO.setAttachmentKind(attachmentInfo.getAttachmentKind());
            deviceVO.setLinearSegment(null);
            devices.add(deviceVO);

            return R.ok(devices);
        }
        return R.fail(connected.getMessage());
    }

    public List<DeviceInitEntryInfo> addInitEntry(List<DeviceInitEntryInfo> initEntry) {
        List<DeviceInitEntryInfo> ans = new ArrayList<>();

        for (int i = 0; i < DeviceInitPreCommandEnum.getLength(); i++) {
            DeviceInitEntryInfo entry = new DeviceInitEntryInfo();
            DeviceInitPreCommandEnum anEnum = DeviceInitPreCommandEnum.getEnum(i);
            entry.setInitComId(anEnum.getComIdList());
            entry.setInitComName(anEnum.getDescription());
            entry.setInitComValue(anEnum.getComValue());
            ans.add(entry);
        }
        ans.addAll(initEntry);
        return ans;
    }


    @Override
    public Boolean deviceActivation(String name) {
        return null;
    }

    /**
     * 查询仪器配置
     */
    @Override
    public R<DeviceConfigVO> newQueryDeviceConfig() {

        DevConfigPO po = devConfigDAO.selectById(userId);
        DevConfigPO defPo = devConfigDAO.selectById(defaultId);
        DeviceConfigVO vo = new DeviceConfigVO();
        BeanUtils.copyProperties(defPo, vo);
        if (po != null) {
            BeanUtils.copyProperties(po, vo, getNullPropertyNames(po));
        }
        return R.ok(vo);
    }

    @Override
    public R<Boolean> newDevConfig(DevConfigDTO dto) {

        DevConfigPO po = devConfigDAO.selectById(userId);
        if (po == null) {
            po = new DevConfigPO();
            BeanUtils.copyProperties(dto, po);
            po.setId(userId);

            po.setCreateBy(dto.getUpdateBy());
            po.setUpdateBy(dto.getUpdateBy());
            boolean a = devConfigDAO.insert(po) > 0 ;
            if (a) {
                return R.ok();
            }
            return R.fail();
        } else {

            BeanUtils.copyProperties(dto, po);
            po.setId(userId);

            boolean a = devConfigDAO.updateById(po) > 0 ;
            if (a) {
                return R.ok();
            }
            return R.fail();
        }
    }

    @Override
    public R<Boolean> setDevMessagePort(Integer messagePort, String updateBy) {

        DevConfigDTO dto = new DevConfigDTO();
        dto.setMessagePort(messagePort);
        dto.setUpdateBy(updateBy);
        return newDevConfig(dto);
    }


    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        return emptyNames.toArray(new String[0]);
    }




    @Override
    public List<Double> getBandWidth(String deviceSerialNumber) {
        List<Double> result = new ArrayList<>();
        R<DeviceMeasureVO> r = queryDeviceTypeInfo(deviceSerialNumber);
        if (!r.getSuccess()) {
            result.add(2.0);
            return result;
        }
        DeviceMeasureVO vo = r.getData();
        if (vo == null || vo.getBandWidth() == null || vo.getBandWidth().isEmpty()) {
            result.add(2.0);
            return result;
        }
        List<String> bandWidth = vo.getBandWidth();
        for (String s : bandWidth) {
            result.add(Double.parseDouble(s));
        }
        return result;
    }

    @Override
    public Boolean timedStart(String deviceSerialNumber, String closeTime) {
        log.info("定时开机");
        return null;
    }

    @Override
    public R<Boolean> deviceCalibration(DeviceCalibrationDTO dto) {

        if (!StringUtils.hasText(dto.getDeviceSerialNumber())) {
            log.info("缺少序列号");
            //缺少序列号
            return R.fail(YamlMessageSource.getMessageStatic("00203003"));
        }

        if (dto.getCalibrationType() == null || DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()) == null) {
            log.info("缺少校准类型");
            //缺少校准类型
            return R.fail(YamlMessageSource.getMessageStatic("00203004"));
        }

        String ip = infoService.getIp(dto.getDeviceSerialNumber());
        log.info("调用校准");

        R<Boolean> r = infoService.useDevice(dto.getDeviceSerialNumber(), Constants.DEVICE_STATUS_OTHER);
        if (!r.getSuccess()) {
            log.info("设备被占用");
            return R.fail(r.getMessage());
        }

        if (dto.getCalibrationType() == 1) {
            //波长校正
            Command command = new Command(DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()), new Object[]{});
            try {
                R<String> stringR = devMessageUtil.deviceCalibration(command, ip);
                if (!stringR.getSuccess()) {
                    return R.fail("波长校正失败，请检查仪器!");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                infoService.releaseDevice(dto.getDeviceSerialNumber());
            }
        } else if (dto.getCalibrationType() == 2) {
            //暗电流校正
            Command command = new Command(DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()), new Object[]{});
            try {
                R<String> stringR = devMessageUtil.deviceCalibration(command, ip);
                if (!stringR.getSuccess()) {
                    return R.fail("暗电流校正失败，请检查仪器!");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                infoService.releaseDevice(dto.getDeviceSerialNumber());
            }
        }

        LambdaQueryWrapper<CalibrationTempPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());

        List<CalibrationTempPO> poList = tempDao.selectList(queryWrapper);
        if (poList.isEmpty()) {
            for (DataNameValue data : dto.getData()) {
                CalibrationTempPO po = new CalibrationTempPO();
                po.setDeviceSerialNumber(dto.getDeviceSerialNumber());
                po.setCreateBy(dto.getUpdateBy());
                po.setCalibrationType(dto.getCalibrationType());
                po.setParam(data.getDataKey());
                po.setParamValue(data.getDataValue());
                tempDao.insert(po);
            }
        } else {
            for (DataNameValue data : dto.getData()) {
                LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                        .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());

                wrapper.eq(CalibrationTempPO::getParam, data.getDataKey());
                CalibrationTempPO calibrationTempPO = tempDao.selectOne(wrapper);
                if (calibrationTempPO == null) {
                    CalibrationTempPO po = new CalibrationTempPO();
                    po.setDeviceSerialNumber(dto.getDeviceSerialNumber());
                    po.setCreateBy(dto.getUpdateBy());
                    po.setCalibrationType(dto.getCalibrationType());
                    po.setParam(data.getDataKey());
                    po.setParamValue(data.getDataValue());
                    tempDao.insert(po);
                } else {
                    calibrationTempPO.setUpdateBy(dto.getUpdateBy());
                    calibrationTempPO.setParamValue(data.getDataValue());
                    tempDao.update(calibrationTempPO, wrapper);
                }
            }
        }

        return R.ok();
    }

    public List<DeviceCalibrationVO> newQueryCalibrationInfo(String deviceSerialNumber) {
        String deviceType = "T600/T700";

        DeviceTypeInfoPO typeInfoPO = new DeviceTypeInfoPO();
        LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
        DeviceModelInfoPO modelInfoPO = null;


        if (!StringUtils.hasText(deviceSerialNumber)) {
            LambdaQueryWrapper<DeviceTypeInfoPO> query = new LambdaQueryWrapper<>();
            query.eq(DeviceTypeInfoPO::getDevType, deviceType);
            typeInfoPO = deviceTypeInfoDAO.selectOne(query);

        } else {
            //根据序列号，去查找仪器类型，根据仪器类型，去获取参数列表
            log.info("检测仪器类型");
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info != null) {
                LambdaQueryWrapper<DeviceModelInfoPO> query = new LambdaQueryWrapper<>();
                query.eq(DeviceModelInfoPO::getDevModel, info.getDeviceModel());

                modelInfoPO = deviceModelInfoDAO.selectOne(query);
                if (modelInfoPO != null) {
                    typeInfoPO = deviceTypeInfoDAO.selectById(modelInfoPO.getDevTypeId());
                }
            }

        }

        List<Integer> calibrationType = typeInfoPO.getCalibrationType();

        List<DeviceCalibrationVO> ans = new ArrayList<>();


        if (calibrationType != null && !calibrationType.isEmpty()) {
            for (Integer type : calibrationType) {


                DeviceCalibrationVO vo = new DeviceCalibrationVO();

                DeviceCalibrationConfigPO configPO = calibrationConfigDAO.selectById(type);
                List<Integer> paramList = configPO.getParamList();
                List<DeviceCalibrationParams> params = new ArrayList<>();

                vo.setCalibrationType(type);
                vo.setCalibrationName(configPO.getCalibrationName());

                for (Integer paramId : paramList) {

                    DeviceCalibrationParamPO param = calibrationParamDAO.selectById(paramId);
                    DeviceCalibrationParams paramVO = new DeviceCalibrationParams();
                    paramVO.setId(param.getId());
                    paramVO.setParamName(param.getParamName());
                    paramVO.setParam(param.getParam());
                    paramVO.setParamType(param.getParamType());


                    wrapper.eq(CalibrationTempPO::getCalibrationType, type);
                    wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, deviceSerialNumber);
                    wrapper.eq(CalibrationTempPO::getParam, param.getParam());
                    wrapper.orderByDesc(CalibrationTempPO::getUpdated).last("limit 1");
                    CalibrationTempPO tempPO = tempDao.selectOne(wrapper);
                    wrapper.clear();
                    if (tempPO != null) {
                        paramVO.setLastValue(tempPO.getParamValue());
                    }
                    List<DataNameValue> dataNameValues = dealData(param, typeInfoPO, modelInfoPO);
                    paramVO.setData(dataNameValues);
                    params.add(paramVO);
                }
                vo.setParams(params);

                ans.add(vo);
            }

        }

        return ans;
    }

    public List<DataNameValue> dealData(DeviceCalibrationParamPO paramPO, DeviceTypeInfoPO po, DeviceModelInfoPO modelInfoPO) {

        if (po == null) {
            return paramPO.getDefaultValue();
        }

        String param = paramPO.getParam();
        List<DataNameValue> ans = new ArrayList<>();
        if (DeviceCalibrationParamEnum.ScanSpeed.getType().equals(param)) {
            int i = 0;
            for (String speed : po.getScanSpeed()) {
                DataNameValue value = new DataNameValue();
                value.setDataKey(speed);
                value.setDataValue(String.valueOf(i));
                ans.add(value);
                i++;
            }
        }
        if (DeviceCalibrationParamEnum.Start.getType().equals( param) || DeviceCalibrationParamEnum.End.getType().equals(param)){
            DataNameValue min = new DataNameValue();
            min.setDataKey("min");
            min.setDataValue(po.getMinWavelength());
            ans.add(min);

            DataNameValue max = new DataNameValue();
            max.setDataKey("max");
            max.setDataValue(po.getMaxWavelength());
            ans.add(max);
        }

        if (DeviceCalibrationParamEnum.WaveBrand.getType().equals(param)) {

            if (modelInfoPO != null && modelInfoPO.getSpacingWidth() != null) {
                for (String s : modelInfoPO.getSpacingWidth()) {
                    DataNameValue value = new DataNameValue();

                    value.setDataValue(s);
                    ans.add(value);
                }
            }
        }
        if (DeviceCalibrationParamEnum.Step.getType().equals(param)) {

            for (String s : po.getScanInterval()) {
                DataNameValue value = new DataNameValue();
                value.setDataValue(s);
                ans.add(value);
            }
        }

        if (ans.isEmpty()) {
            ans = paramPO.getDefaultValue();
        }
        return ans;
    }

    @Override
    public R<Boolean> checkDeviceStatus(String deviceSerialNumber) {
        return null;
    }

    @Override
    public R<Boolean> updateDeviceStatus(String deviceSerialNumber, String status) {

        return null;
    }

    @Override
    public R<DeviceMeasureVO> queryDeviceTypeInfo(String deviceSerialNumber) {

        return infoService.queryDeviceTypeInfo(deviceSerialNumber);
    }

    @Override
    public void test() {
//        List<DeviceTypeInfoPO> deviceTypeInfoPOS = deviceTypeInfoDAO.selectList(null);
//        for (DeviceTypeInfoPO po : deviceTypeInfoPOS) {
//            log.info("{}", po);
//
//            LambdaQueryWrapper<DeviceModelInfoPO> query = new LambdaQueryWrapper<>();
//            query.eq(DeviceModelInfoPO::getDevTypeId, po.getId());
//            List<DeviceModelInfoPO> modelInfoPOS = deviceModelInfoDAO.selectList(query);
//            for (DeviceModelInfoPO modelInfoPO : modelInfoPOS) {
//                log.info("{}", modelInfoPO);
//            }
//
//        }
//
//        try {
//            devMessageUtil.zero(new Command("GetWlList", null), "169.254.180.253");
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

//        ChangeParam(wl=500.0, r=491765, s=402424, d=1716)


//        ChangeParam(wl=500, r=128337, s=103479, d=128337)
//        ChangeParam or = new ChangeParam("500", "128337", "103479", "128337");
//        ChangeParam cu = new ChangeParam("500", "491765", "402424", "1716");

////        ChangeParam changeParam = new ChangeParam("0.01", "123456", "9852", "123456");
//        String s = DataCountUtil.countTPercent(or, cu, 1.0);
//        String s1 = DataCountUtil.changeTPercentOrAbs(null, s);
//        System.out.println("+++" + s + "+++" + s1);


    }

    @Override
    public R<List<SearchInfo>> searchDevice(Integer connectType) {

        if (connectType == null) {
            R<DeviceConfigVO> r = newQueryDeviceConfig();
            if (r.getSuccess()) {
                DeviceConfigVO data = r.getData();
                connectType = data.getMessagePort();
            }
        }

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Find.getType());
        inputParam.setConnectType(connectType);
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            OperationResultEntity data = operation.getData();
            List<SearchInfo> searchInfo = data.getSearchInfo();
            return R.ok(searchInfo);
        }
    }

    @Override
    public R<List<SearchInfo>> searchDevice() {

        Integer connectType = 1;
        R<DeviceConfigVO> r = newQueryDeviceConfig();
        if (r.getSuccess()) {
            DeviceConfigVO data = r.getData();
            connectType = data.getMessagePort();
        }

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Find.getType());
        inputParam.setConnectType(connectType);
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            OperationResultEntity data = operation.getData();
            List<SearchInfo> searchInfo = data.getSearchInfo();
            return R.ok(searchInfo);
        }
    }

    @Override
    public R<FindDeviceVO> connect(SearchInfo param) {
        Integer connectType = param.getConnectType();

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Connect.getType());
        inputParam.setConnectType(connectType);
        inputParam.setDeviceInfo(param);

        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            OperationResultEntity data = operation.getData();
            FindDeviceVO vo = new FindDeviceVO();

            DeviceInfoPO info = infoService.getInfo(data.getDeviceSerialNumber());
            if (info == null) {
                info = new DeviceInfoPO();
            }

            BeanUtils.copyProperties(data, vo);
            BeanUtils.copyProperties(data, info);

            info.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
            info.setIsDevOpened(1);

            info.setDevicePort(data.getConnectPort());
            info.setDeviceCurrentStatus(Constants.DEVICE_STATUS_IDLE);

            String deviceModel = vo.getDeviceSerialNumber().substring(3, 8);
            info.setDeviceModel(deviceModel);

            LambdaQueryWrapper<DeviceModelInfoPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceModelInfoPO::getDevModel, deviceModel);
            DeviceModelInfoPO modelInfoPO = deviceModelInfoDAO.selectOne(wrapper);

            vo.setDeviceModelName(modelInfoPO.getDevName());
            info.setDeviceModelName(modelInfoPO.getDevName());

            if (StringUtils.hasText(info.getDeviceName())) {
                vo.setDeviceName(info.getDeviceName());
            } else {
                vo.setDeviceName(modelInfoPO.getDevName());
                info.setDeviceName(modelInfoPO.getDevName());
            }

            Integer i = infoService.queryDeviceActivationStatus(vo.getDeviceSerialNumber());
            info.setIsActivated(i);

            List<DeviceInitEntryInfo> initEntry = modelInfoPO.getInitEntry();

            //附件处理
            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfoByAttachmentNum(vo.getAttachmentType());
            LambdaQueryWrapper<InsConfigPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(InsConfigPO::getDeviceSerialNumber, vo.getDeviceSerialNumber());

            attachmentInfo.setDeviceSerialNumber(vo.getDeviceSerialNumber());
            FSampleAttachTypeSaveDTO dto = new FSampleAttachTypeSaveDTO();

            BeanUtils.copyProperties(vo, dto);
            dto.setAttachmentTypeName(attachmentInfo.getAttachmentName());
            samplePoolConfigService.saveSamplePoolInfo(dto);

            info.setAttachmentName(attachmentInfo.getAttachmentName());
            info.setUpdateBy(param.getUpdateBy());
            infoService.saveOrUpdateInfo(info);
            boolean initRecord = true;

            List<InitRecordPO> initList = initRecordService.searchInitStatus(vo.getDeviceSerialNumber(), info.getId());
            if (initList == null || initList.isEmpty()) {
                initRecord = false;
            }

            if (vo.getIsInited() == 0 || !initRecord) {
                if (attachmentInfo.getAttachmentKind().equals(1)) {
                    List<DeviceInitEntryInfo> allInitEntry = addInitEntry(initEntry);
                    initRecordService.initAllTask(vo.getDeviceSerialNumber(), info.getId(), allInitEntry);
                }
                initRecordService.initAllTask(vo.getDeviceSerialNumber(), info.getId(), initEntry);
            }

            BeanUtils.copyProperties(info, vo);
            vo.setAttachmentKind(attachmentInfo.getAttachmentKind());
            vo.setLinearSegment(null);

//            if (info.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
//                liveDetectionManyDevicesWebSocket.startMeasure(info.getDeviceSerialNumber());
//            }

            return R.ok(vo);
        }
    }

    @Override
    public R<Boolean> disconnectDevice(String deviceSerialNumber, String updateBy) {

        DeviceInfoPO info1 = infoService.getInfo(deviceSerialNumber);
        if (info1 == null || info1.getConnectStatus().equals(Constants.CONNECT_DEVICE_STOP)) {
            log.info("仪器未连接");
            return R.ok(true);
        }

        if (!StringUtils.hasText(info1.getDeviceId())) {
            info1.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
            infoService.saveOrUpdateInfo(info1);
            return R.ok(true);
        }

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.Disconnect.getType());
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            OperationResultEntity data = operation.getData();
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            info.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
            info.setDeviceId("");
            info.setConnectType(-1);
            info.setIp("");
            info.setIsInited(0);
            info.setMachineIp("");
            info.setDevicePort(-1);
            info.setUpdateBy(updateBy);
            infoService.saveOrUpdateInfo(info);
            return R.ok(true);
        }
    }

    @Override
    public R<Boolean> startInitDevice(String deviceSerialNumber, String updateBy) {
        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Init.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setUpdateBy(updateBy);
        inputParam.setIsInitFromHead(true);
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        info.setIsInited(Constants.INIT_STATUS_RUNNING);

        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            log.info("初始化启动失败: {}", operation.getMessage());
            info.setIsInited(Constants.INIT_STATUS_FALSE);
            infoService.saveOrUpdateInfo(info);
            return R.fail(operation.getMessage());
        } else {
//            liveDetectionWebSocket.startMeasure(deviceSerialNumber);

            synchronized (info) {
                LambdaQueryWrapper<DeviceModelInfoPO> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DeviceModelInfoPO::getDevModel, info.getDeviceModel());
                DeviceModelInfoPO modelInfoPO = deviceModelInfoDAO.selectOne(wrapper);

                List<DeviceInitEntryInfo> initEntry = modelInfoPO.getInitEntry();

                DeviceAttachmentTypeVO attachmentInfo = infoService.getAttachmentInfo(deviceSerialNumber);
                Integer kind = attachmentInfo.getAttachmentKind();

                if (kind.equals(DeviceAttachmentKindEnum.FIVE_POOL.getType()) || kind.equals(DeviceAttachmentKindEnum.EIGHT_POOL.getType())) {
                    List<DeviceInitEntryInfo> allInitEntry = addInitEntry(initEntry);
                    initRecordService.initAllTask(deviceSerialNumber, info.getId(), allInitEntry);
                } else {
                    initRecordService.initAllTask(deviceSerialNumber, info.getId(), initEntry);
                }

                infoService.saveOrUpdateInfo(info);
            }

            return R.ok(true);
        }
    }

    @Override
    public R<Boolean> continueInitDevice(String deviceSerialNumber, String updateBy) {

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Init.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setIsInitFromHead(false);
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        info.setIsInited(Constants.INIT_STATUS_RUNNING);
        infoService.saveOrUpdateInfo(info);

        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            log.info("继续初始化启动失败: {}", operation.getMessage());

            info.setIsInited(Constants.INIT_STATUS_FALSE);
            infoService.saveOrUpdateInfo(info);
            return R.fail(operation.getMessage());
        } else {
            return R.ok(true);
        }
    }

    @Override
    public R<String> getConnectDevice(String updateBy) {

        LambdaQueryWrapper<DeviceInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceInfoPO::getConnectStatus, Constants.CONNECT_DEVICE_SUCCESS)
                .eq(DeviceInfoPO::getUpdateBy, updateBy)
                .orderByDesc(DeviceInfoPO::getUpdated);
        //TODO 校验用户权限，是否可以连多个
        int max = 1;
        List<DeviceInfoPO> pos = deviceInfoDAO.selectList(queryWrapper);
        if (pos != null && !pos.isEmpty()) {
            DeviceInfoPO infoPO = pos.remove(0);

            try {
                pos.forEach(po -> {
                    try {
                        disconnectDevice(po.getDeviceSerialNumber(), updateBy);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                log.error("断开连接失败: {}", e.getMessage());
            }

            if (infoPO.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                DeviceConnectionManager connectionManager = DeviceConnectionManager.getInstance();
                DeviceConnectInfo device = connectionManager.getDevice(infoPO.getDeviceId());
//                if (device != null) {
//                    liveDetectionManyDevicesWebSocket.startMeasure(infoPO.getDeviceSerialNumber());
//                }
            }
            return R.ok(infoPO.getDeviceSerialNumber());
        }
        return R.ok(null);
    }

    @Override
    public R<Boolean> stop(
            String deviceSerialNumber
    ) {

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.Stop.getType());
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            return R.ok(true);
        }
    }

    @Override
    public R<OperationResultEntity> setParamOneDevice(
            DevOperationInputParam param
    ) {

        param.setOperationType(OperationTypeEnums.SetParam.getType());
//        param.setDeviceSerialNumber(deviceSerialNumber);

        R<OperationResultEntity> operation =
                operationUtilService.operation(param);

        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            return R.ok(operation.getData());
        }
    }

    @Override
    public R<OperationResultEntity> getParamOneDevice(
            DevOperationInputParam param
    ) {

        // 设置操作类型为“读取参数”
        param.setOperationType(OperationTypeEnums.GetValue.getType());

        // 调用统一操作入口
        R<OperationResultEntity> operation =
                operationUtilService.operation(param);

        // 返回结果
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            return R.ok(operation.getData());
        }
    }


    @Override
    public R<OperationResultEntity> Action(
            DevOperationInputParam param
    ) {

        // 明确这是 Action 类型
        param.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());

        R<OperationResultEntity> operation =
                operationUtilService.operation(param);

        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            new Thread(() -> {
                UnknowCountMessageQueue queue = UnknowCountMessageQueue.getInstance();
                while (true) {
                    if (queue.getQueueSize(param.getDeviceSerialNumber(), "Measure") <= 0) {
                        continue;
                    } else {
                        Object scan = queue.pollMessage(param.getDeviceSerialNumber(), "Measure");
                        System.out.println(scan.toString());
                    }
                }
            }).start();
            return R.ok(operation.getData());
        }
    }

    @Override
    public R<Boolean> newStopInit(String deviceSerialNumber, String updateBy) {
        DeviceConstants.isStopInit.put(deviceSerialNumber, true);

        return R.ok();
    }

    @Override
    public R<Boolean> deviceNewCalibration(DeviceCalibrationDTO dto) {

        if (!StringUtils.hasText(dto.getDeviceSerialNumber())) {
            log.info("缺少序列号");
            //缺少序列号
            return R.fail(YamlMessageSource.getMessageStatic("00203003"));
        }

        if (dto.getCalibrationType() == null || DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()) == null) {
            log.info("缺少校准类型");
            //缺少校准类型
            return R.fail(YamlMessageSource.getMessageStatic("00203004"));
        }

        if (Objects.equals(dto.getCalibrationType(), DeviceCalibrationInfoEnum.T800Dark.getType())) {
            double start = 900.0;
            double end = 190.0;
            double step = 0.5;

            int scanSpeed = 3;
            double darkBand = 2.0;

            for (DataNameValue datum : dto.getData()) {
                try {

                    if (datum.getDataKey().equals(DeviceCalibrationParamEnum.Start.getType())) {
                        start = Double.parseDouble(datum.getDataValue());
                    }
                    if (datum.getDataKey().equals(DeviceCalibrationParamEnum.End.getType())) {
                        end = Double.parseDouble(datum.getDataValue());
                    }
                    if (datum.getDataKey().equals(DeviceCalibrationParamEnum.ScanSpeed.getType())) {
                        scanSpeed = Integer.parseInt(datum.getDataValue());
                    }

                    if (datum.getDataKey().equals(DeviceCalibrationParamEnum.WaveBrand.getType())) {
                        darkBand = Double.parseDouble(datum.getDataValue());
                    }
                    if (datum.getDataKey().equals(DeviceCalibrationParamEnum.Step.getType())) {
                        step = Double.parseDouble(datum.getDataValue());
                    }
                } catch (Exception e) {
                    log.info("参数转换失败");
                }
            }

            if (end >= start) {
                return R.fail("终止波长不能大于起始波长");
            }

            start = start + step;
            end = end - step;
            DevOperationInputParam inputParam = new DevOperationInputParam();
            inputParam.setOperationType(OperationTypeEnums.SetParam.getType());

            SetParam param = new SetParam();
            param.setScanWlInterval(step);
            param.setStartWl(start);
            param.setEndWl(end);
            param.setScanSpeed(scanSpeed);
            param.setPhotoMode(Integer.valueOf(WaveLengthEnum.abs.getCode()));
            inputParam.setDeviceSerialNumber(dto.getDeviceSerialNumber());

            Boolean needSetSit = infoService.isNeedSetSit(dto.getDeviceSerialNumber());
            if (needSetSit) {
                param.setSlit1(darkBand);
            }
            inputParam.setParam(param);
            R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
            if (!operation.getSuccess()) {
                return R.fail(operation.getMessage());
            }

            inputParam.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());
        }

        log.info("调用校准");

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Calibration.getType());
        inputParam.setCalibrationType(dto.getCalibrationType());
        inputParam.setDeviceSerialNumber(dto.getDeviceSerialNumber());
        inputParam.setUpdateBy(dto.getUpdateBy());
        if (dto.getData() != null) {
            inputParam.setCalibrationData(dto.getData());
        }

        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        }
        log.info("校准成功");
        LambdaQueryWrapper<CalibrationTempPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());

        List<CalibrationTempPO> poList = tempDao.selectList(queryWrapper);
        if (poList.isEmpty()) {
            for (DataNameValue data : dto.getData()) {
                CalibrationTempPO po = new CalibrationTempPO();
                po.setDeviceSerialNumber(dto.getDeviceSerialNumber());
                po.setCreateBy(dto.getUpdateBy());
                po.setCalibrationType(dto.getCalibrationType());
                po.setParam(data.getDataKey());
                po.setParamValue(data.getDataValue());
                tempDao.insert(po);
            }
        } else {
            for (DataNameValue data : dto.getData()) {
                LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                        .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());

                wrapper.eq(CalibrationTempPO::getParam, data.getDataKey());
                CalibrationTempPO calibrationTempPO = tempDao.selectOne(wrapper);
                if (calibrationTempPO == null) {
                    CalibrationTempPO po = new CalibrationTempPO();
                    po.setDeviceSerialNumber(dto.getDeviceSerialNumber());
                    po.setCreateBy(dto.getUpdateBy());
                    po.setCalibrationType(dto.getCalibrationType());
                    po.setParam(data.getDataKey());
                    po.setParamValue(data.getDataValue());
                    tempDao.insert(po);
                } else {
                    calibrationTempPO.setUpdateBy(dto.getUpdateBy());
                    calibrationTempPO.setParamValue(data.getDataValue());
                    tempDao.update(calibrationTempPO, wrapper);
                }
            }
        }
        return R.ok();
    }

    @Override
    public R<OperationResultEntity> TimeMeasure(DevOperationInputParam param) {
        param.setOperationType(OperationTypeEnums.TimeMeasure.getType());

        R<OperationResultEntity> operation =
                operationUtilService.operation(param);

        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            new Thread(() -> {
                UnknowCountMessageQueue queue = UnknowCountMessageQueue.getInstance();
//                while (true) {
//                    if (queue.getQueueSize(param.getDeviceSerialNumber(), "Measure") <= 0) {
//                        continue;
//                    } else {
//                        Object scan = queue.pollMessage(param.getDeviceSerialNumber(), "Measure");
//                        System.out.println(scan.toString());
//                    }
//                }
            }).start();
            return R.ok(operation.getData());
        }
    }


    @Override
    public R<Boolean> zero(
            String deviceSerialNumber,
            List<String> param
    ) {

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setWavelength(param);
        inputParam.setOperationType(OperationTypeEnums.Zero.getType());
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            return R.ok(true);
        }
    }

    @Override
    public R<Boolean> addDevice(SearchInfo info) {
        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceInfo(info);
        inputParam.setOperationType(OperationTypeEnums.T800Connect.getType());
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            return R.ok(true);
        }
    }
}
