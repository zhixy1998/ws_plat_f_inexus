/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.component.EhcacheUtil;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.*;
import com.persee.workstation.uv_inexus.dto.concise.common.FSampleAttachTypeSaveDTO;
import com.persee.workstation.uv_inexus.dto.factory_inspection.DeviceTaskDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TaskAddDto;
import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.enums.factoryInspection.OutFactoryCheckEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.EnergyLampEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.exception.BusinessException;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskManagement;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.*;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.WorkTimeInfo;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TaskManagementService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TemplateService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceUsageLogService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.InitRecordService;
import com.persee.workstation.uv_inexus.util.DeviceMessageUtils;
import com.persee.workstation.uv_inexus.util.changeUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceConfParam;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import com.persee.workstation.uv_inexus.websocket.liveDetection.LiveDetectionWebSocket;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketOperationEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static com.persee.workstation.uv_inexus.enums.Cache.CacheEnums.DEFAULT_CACHE;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.FACTORY_INSPECTION_TASK;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.OutFactoryCheckEnum.OUT_FACTORY_CHECK_LIST;

@Slf4j
@Service
public class DeviceManageServiceImpl implements DeviceManageService {

    @Autowired
    DevConfigDAO devConfigDAO;
    @Autowired
    TemplateService templateService;
    @Autowired
    EhcacheUtil ehcacheUtil;
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

    @Autowired
    DeviceUsageLogService deviceUsageLogService;

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
    @Autowired
    private TaskManagementService taskManagementService;
    @Autowired
    private ThreadPoolTaskExecutor measurementTaskExecutor;
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

        DeviceAttachmentTypeVO attachmentInfo = infoService.getAttachmentInfo(deviceSerialNumber);
        vo.setAttachmentName(attachmentInfo.getAttachmentName());
        vo.setTotalPoolNum(attachmentInfo.getSamplePoolNum());
        vo.setAttachmentKind(attachmentInfo.getAttachmentKind());


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
            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfoByAttachmentNum(deviceVO.getAttachmentType(), po.getDeviceModel());
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
            boolean a = devConfigDAO.insert(po) > 0;
            if (a) {
                return R.ok();
            }
        } else {

            BeanUtils.copyProperties(dto, po);
            po.setId(userId);

            boolean a = devConfigDAO.updateById(po) > 0;
            if (a) {
                return R.ok();
            }
        }
        return R.fail();
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

                    List<DataNameValue> defaultValue = param.getDefaultValue();
                    if (defaultValue != null && !defaultValue.isEmpty()) {


                        if (paramVO.getParam().equals(DeviceCalibrationParamEnum.Start.getType())) {

                            paramVO.setLastValue(typeInfoPO.getMaxWavelength());

                        } else if (paramVO.getParam().equals(DeviceCalibrationParamEnum.End.getType())) {
                            paramVO.setLastValue(typeInfoPO.getMinWavelength());
                        }else {
                            DataNameValue value = defaultValue.get(0);
                            if (StringUtils.hasText(value.getDataValue())) {
                                paramVO.setLastValue(value.getDataValue());
                            }
                        }
                    }

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

            JSONObject scanSpeed = po.getScanSpeed();
            Object defaultValue = MappingParamEnum.SCAN_SPEED.getDefaultValue();
            String jsonString = JSONObject.toJSONString(defaultValue);
            List<DeviceConfParam> options = JSON.parseArray(jsonString, DeviceConfParam.class);
            Map<String, String> object = new HashMap<>();
            for (DeviceConfParam option : options) {
                object.put(option.getParamValue(), option.getParamName());
            }

            List<DataNameValue> finalAns = new ArrayList<>();
            scanSpeed.forEach((k, v) -> {
                DataNameValue value = new DataNameValue();
                value.setDataKey(object.get(v.toString()));
                value.setDataValue(String.valueOf(v));
                finalAns.add(value);
            });
            ans.addAll(finalAns);
        }
        if (DeviceCalibrationParamEnum.Start.getType().equals(param) || DeviceCalibrationParamEnum.End.getType().equals(param)) {
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

    /**
     * 获取默认参数映射
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public Result<DeviceMeasureVO> queryDeviceTypeInfoWithDefault(String deviceSerialNumber) {
        // 1. 先调用原有接口获取数据库参数，主要是为了获取 connectMode
        R<DeviceMeasureVO> re = queryDeviceTypeInfo(deviceSerialNumber);
        if (!re.getSuccess()) {
            return Result.fail(re.getMessage());
        }

        DeviceMeasureVO dbVo = re.getData();
        Integer connectMode = dbVo != null ? dbVo.getConnectMode() : null;
        List<Integer> filterWavelength = dbVo != null ? dbVo.getFilterWavelength() : null;
        String minWaveLength = dbVo != null ? dbVo.getMinWavelength() : null;
        String maxWaveLength = dbVo != null ? dbVo.getMaxWavelength() : null;
        String minLampWaveLength = dbVo != null ? dbVo.getMinLampWavelength() : null;
        String maxLampWaveLength = dbVo != null ? dbVo.getMaxLampWavelength() : null;

        // 2. 创建新的 VO 对象
        DeviceMeasureVO vo = new DeviceMeasureVO();

        // 3. 遍历枚举，用枚举的默认值填充所有字段
        for (MappingParamEnum paramEnum : MappingParamEnum.values()) {
            Object defaultValue = paramEnum.getDefaultValue();
            String fieldName = paramEnum.getKey();

            // 设置字段值
            setFieldValueSafe(vo, fieldName, defaultValue);
        }

        // 4. 设置 connectMode（从数据库获取）
        vo.setConnectMode(connectMode);
        vo.setFilterWavelength(filterWavelength);
        vo.setMaxWavelength(maxWaveLength);
        vo.setMinWavelength(minWaveLength);
        vo.setMaxLampWavelength(maxLampWaveLength);
        vo.setMinLampWavelength(minLampWaveLength);

        return Result.ok(vo);
    }



    private void setFieldValueSafe(DeviceMeasureVO vo, String fieldName, Object value) {
        try {
            Field field = DeviceMeasureVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            if (value == null) {
                field.set(vo, null);
                return;
            }

            field.set(vo, value);

        } catch (NoSuchFieldException e) {
            // 字段不存在，忽略
            System.out.println("字段不存在: " + fieldName);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过反射获取字段值
     */
    private Object getFieldValue(DeviceMeasureVO vo, String fieldName) {
        try {
            Field field = DeviceMeasureVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(vo);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }


    @Autowired
    RestUtil restUtil;

    @Override
    public void test() {

//        String formula = "(A1*m0)/(A0*m1)*w0*100";
//        String identity = "A1, m0, A0, m1, w0";
//        String values = "2.0,3,1.5,1,8";
//        String upperCase = formula.toUpperCase();
//        identity = identity.toUpperCase();
//        String result = restUtil.sum(upperCase, identity, values);
//        System.out.println(result);

//        String json = "[\n" +
//                "  {\n" +
//                "    \"type\": \"仪器\",\n" +
//                "    \"workTime\": \"GetDeviceCurrentTime\",\n" +
//                "    \"totalWorkTime\": \"GetDeviceTotalTime\",\n" +
//                "    \"updateTime\": \"\"\n" +
//                "  },\n" +
//                "  {\n" +
//                "    \"type\": \"钨灯\",\n" +
//                "    \"workTime\": \"GetWLampCurrentTime\",\n" +
//                "    \"totalWorkTime\": \"GetWLampTotalTime\",\n" +
//                "    \"updateTime\": \"\"\n" +
//                "  },\n" +
//                "  {\n" +
//                "    \"type\": \"氘灯\",\n" +
//                "    \"workTime\": \"GetDLampCurrentTime\",\n" +
//                "    \"totalWorkTime\": \"GetDLampTotalTime\",\n" +
//                "    \"updateTime\": \"\"\n" +
//                "  }\n" +
//                "]\n";
//        List<WorkTimeInfo> workTimeInfos = JSON.parseArray(json, WorkTimeInfo.class);
//        LambdaQueryWrapper<DeviceTypeInfoPO> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(DeviceTypeInfoPO::getDevType, "T800");
//        DeviceTypeInfoPO po = deviceTypeInfoDAO.selectOne(wrapper);
//        po.setWorkTimeCount(workTimeInfos);
//        deviceTypeInfoDAO.update(po, wrapper);


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
            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfoByAttachmentNum(vo.getAttachmentType(), deviceModel);
            LambdaQueryWrapper<InsConfigPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(InsConfigPO::getDeviceSerialNumber, vo.getDeviceSerialNumber());

            attachmentInfo.setDeviceSerialNumber(vo.getDeviceSerialNumber());
            FSampleAttachTypeSaveDTO dto = new FSampleAttachTypeSaveDTO();

            BeanUtils.copyProperties(vo, dto);
            dto.setAttachmentTypeName(attachmentInfo.getAttachmentName());
            samplePoolConfigService.saveSamplePoolInfo(dto);

            info.setAttachmentName(attachmentInfo.getAttachmentName());
            info.setUpdateBy(param.getUpdateBy());

            LambdaQueryWrapper<DeviceInfoPO> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DeviceInfoPO::getConnectStatus, Constants.CONNECT_DEVICE_SUCCESS)
                    .eq(DeviceInfoPO::getUpdateBy, param.getUpdateBy())
                    .orderByDesc(DeviceInfoPO::getUpdated);
            //TODO 校验用户权限，是否可以连多个
            int max = 1;
            List<DeviceInfoPO> pos = deviceInfoDAO.selectList(lambdaQueryWrapper);

            if (pos != null && !pos.isEmpty()) {
                for (DeviceInfoPO po : pos) {
                    disconnectDevice(po.getDeviceSerialNumber(), param.getUpdateBy());
                }
            }

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

    public R<FindDeviceVO> factoryConnectBatch(SearchInfo param) {
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
            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfoByAttachmentNum(vo.getAttachmentType(), deviceModel);
            LambdaQueryWrapper<InsConfigPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(InsConfigPO::getDeviceSerialNumber, vo.getDeviceSerialNumber());

            attachmentInfo.setDeviceSerialNumber(vo.getDeviceSerialNumber());
            FSampleAttachTypeSaveDTO dto = new FSampleAttachTypeSaveDTO();

            BeanUtils.copyProperties(vo, dto);
            dto.setAttachmentTypeName(attachmentInfo.getAttachmentName());
            samplePoolConfigService.saveSamplePoolInfo(dto);

            info.setAttachmentName(attachmentInfo.getAttachmentName());
            info.setUpdateBy(param.getUpdateBy());

            LambdaQueryWrapper<DeviceInfoPO> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DeviceInfoPO::getConnectStatus, Constants.CONNECT_DEVICE_SUCCESS)
                    .eq(DeviceInfoPO::getUpdateBy, param.getUpdateBy())
                    .orderByDesc(DeviceInfoPO::getUpdated);
            //TODO 校验用户权限，是否可以连多个
            int max = 1;


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

            return R.ok(vo);
        }
    }

    @Override
    public R<Boolean> disconnectDevice(String deviceSerialNumber, String updateBy) {

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null || info.getConnectStatus().equals(Constants.CONNECT_DEVICE_STOP)) {
            log.info("仪器未连接");
            return R.ok(true);
        }

        if (!StringUtils.hasText(info.getDeviceId())) {
            info.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
            infoService.saveOrUpdateInfo(info);
            return R.ok(true);
        }

//        Boolean saveDeviceUsageLog(DeviceUsageLogPO po);

        DeviceUsageLogPO logPO = new DeviceUsageLogPO();
        logPO.setDeviceSerialNumber(deviceSerialNumber);
        logPO.setUpdateBy(updateBy);
        DeviceConnectionManager manager = DeviceConnectionManager.getInstance();
        DeviceConnectInfo device = manager.getDevice(info.getDeviceId());
        LocalDateTime connectTime = device.getConnectTime();
        logPO.setStartTime(connectTime);

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.Disconnect.getType());
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            if(info.getIsActivated().equals(Constants.DEVICE_ACTIVE_NO) || !info.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                info.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
                info.setDeviceId("");
                info.setConnectType(-1);
                info.setIp("");
                info.setIsInited(0);
                info.setMachineIp("");
                info.setDevicePort(-1);
                info.setUpdateBy(updateBy);
                infoService.saveOrUpdateInfo(info);
                deviceUsageLogService.saveDeviceUsageLog(logPO);
                return R.ok(true);
            } else {
                return R.fail(operation.getMessage());
            }

        } else {
            OperationResultEntity data = operation.getData();

            info.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
            info.setDeviceId("");
            info.setConnectType(-1);
            info.setIp("");
            info.setIsInited(0);
            info.setMachineIp("");
            info.setDevicePort(-1);
            info.setUpdateBy(updateBy);
            infoService.saveOrUpdateInfo(info);
            deviceUsageLogService.saveDeviceUsageLog(logPO);
            return R.ok(true);
        }
    }

    @Override
    public R<Boolean> startInitDevice(String deviceSerialNumber, String updateBy) {

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);

        if (info == null) {
            return R.fail("未找到该设备");
        }

        if (!info.getDeviceCurrentStatus().equals(Constants.DEVICE_STATUS_IDLE) && !info.getDeviceCurrentStatus().equals(Constants.DEVICE_STATUS_ECHO)) {
            return R.fail("设备正在使用中，请等待！");
        }

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Init.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setUpdateBy(updateBy);
        inputParam.setIsInitFromHead(true);

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
                pos.forEach(po -> disconnectDevice(po.getDeviceSerialNumber(), updateBy));
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

        String deviceSerialNumber = dto.getDeviceSerialNumber();
        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info("缺少序列号");
            //缺少序列号
            return R.fail(YamlMessageSource.getMessageStatic("00203003"));
        }

        if (dto.getCalibrationType() == null || DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()) == null) {
            log.info("缺少校准类型");
            //缺少校准类型
            return R.fail(YamlMessageSource.getMessageStatic("00203004"));
        }


        infoService.updateDeviceCurrentStatus(deviceSerialNumber, WebSocketOperationEnum.Calibration.getOperation(), Constants.DEVICE_STATUS_CALIBRATION);
        if (Objects.equals(dto.getCalibrationType(), DeviceCalibrationInfoEnum.T800Dark.getType())) {
            double start = 900.0;
            double end = 190.0;
            double step = 0.5;

            String scanSpeed = "medium";
            double darkBand = 2.0;

            double minWaveLength = 190.0;
            double maxWaveLength = 1100.0;

            if (StringUtils.hasText(deviceSerialNumber)) {
                R<DeviceMeasureVO> typeInfo = infoService.queryDeviceTypeInfo(deviceSerialNumber);
                if (typeInfo.getSuccess()) {
                    DeviceMeasureVO vo = typeInfo.getData();
                    if (vo != null) {
                        minWaveLength = StringUtils.hasText(vo.getMinWavelength()) ? Double.parseDouble(vo.getMinWavelength()) : minWaveLength;
                        maxWaveLength = StringUtils.hasText(vo.getMaxWavelength()) ? Double.parseDouble(vo.getMaxWavelength()) : maxWaveLength;
                    }

                }
            }

            for (DataNameValue datum : dto.getData()) {
                try {

                    if (datum.getDataKey().equals(DeviceCalibrationParamEnum.Start.getType())) {
                        start = Double.parseDouble(datum.getDataValue());
                    }
                    if (datum.getDataKey().equals(DeviceCalibrationParamEnum.End.getType())) {
                        end = Double.parseDouble(datum.getDataValue());
                    }
                    if (datum.getDataKey().equals(DeviceCalibrationParamEnum.ScanSpeed.getType())) {
                        scanSpeed = datum.getDataValue();
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

            if (start < minWaveLength || start > maxWaveLength) {
                infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
                return R.fail(String.format("起始波长范围在 %s ~ %s 内", minWaveLength, maxWaveLength));
            }
            if (end < minWaveLength || end > maxWaveLength) {
                infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
                return R.fail(String.format("终止波长范围在 %s ~ %s 内", minWaveLength, maxWaveLength));
            }


            if (end >= start) {
                infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
                return R.fail("终止波长不能大于起始波长");
            }

            R<DeviceMeasureVO> r = infoService.queryDeviceTypeInfo(deviceSerialNumber);
            if (!r.getSuccess()) {
                infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
                return R.fail(r.getMessage());
            }
            DeviceMeasureVO vo = r.getData();

            List<String> bandWidth = vo.getBandWidth();
            if (!bandWidth.contains(String.valueOf(darkBand))) {
                log.info("当前仪器不支持该光谱带宽！");
                infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
                return R.fail("当前仪器不支持该光谱带宽！");
            }
            List<DeviceConfParam> speed = vo.getScanSpeed();
            int deviceScanSpeed = -1;

            for (DeviceConfParam s : speed) {
                if (s.getParamName().equals(scanSpeed)) {
                    deviceScanSpeed = Integer.parseInt(s.getParamValue());
                    break;
                }
            }


            if (deviceScanSpeed == -1) {
                log.info("缺少扫描速度");
                infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
                return R.fail("当前仪器不支持该扫描速度");
            }

            start = start + step;
            end = end - step;
            DevOperationInputParam inputParam = new DevOperationInputParam();
            inputParam.setOperationType(OperationTypeEnums.SetParam.getType());

            SetParam param = new SetParam();
            param.setScanWlInterval(step);
            param.setStartWl(start);
            param.setEndWl(end);
            param.setScanSpeed(deviceScanSpeed);
            param.setPhotoMode(Integer.valueOf(WaveLengthEnum.abs.getCode()));
            inputParam.setDeviceSerialNumber(deviceSerialNumber);

            Boolean needSetSit = infoService.isNeedSetSit(deviceSerialNumber);
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
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setUpdateBy(dto.getUpdateBy());
        if (dto.getData() != null) {
            inputParam.setCalibrationData(dto.getData());
        }

        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
            return R.fail(operation.getMessage());
        }
        log.info("校准成功");

        LambdaQueryWrapper<CalibrationTempPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CalibrationTempPO::getDeviceSerialNumber, deviceSerialNumber)
                .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());

        List<CalibrationTempPO> poList = tempDao.selectList(queryWrapper);
        if (poList.isEmpty()) {
            for (DataNameValue data : dto.getData()) {
                CalibrationTempPO po = new CalibrationTempPO();
                po.setDeviceSerialNumber(deviceSerialNumber);
                po.setCreateBy(dto.getUpdateBy());
                po.setCalibrationType(dto.getCalibrationType());
                po.setParam(data.getDataKey());
                po.setParamValue(data.getDataValue());
                tempDao.insert(po);
            }
        } else {
            for (DataNameValue data : dto.getData()) {
                LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, deviceSerialNumber)
                        .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());

                wrapper.eq(CalibrationTempPO::getParam, data.getDataKey());
                CalibrationTempPO calibrationTempPO = tempDao.selectOne(wrapper);
                if (calibrationTempPO == null) {
                    CalibrationTempPO po = new CalibrationTempPO();
                    po.setDeviceSerialNumber(deviceSerialNumber);
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

        if (Objects.equals(dto.getCalibrationType(), DeviceCalibrationInfoEnum.T800Dark.getType())) {
            new Thread(() -> {
                DeviceConnectionManager deviceConnectionManager = DeviceConnectionManager.getInstance();
                DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
                String deviceId = info.getDeviceId();
                DeviceConnectInfo device = deviceConnectionManager.getDevice(deviceId);
                Boolean isIdle = device.getIsIdle();
                while (!isIdle) {
                    try {
                        Thread.sleep(200);
                        device = deviceConnectionManager.getDevice(deviceId);
                        isIdle = device.getIsIdle();
                    } catch (Exception e) {
                        log.info("线程异常");
                    }
                }
                infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
            }).start();
        } else {
            infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
        }

        return R.ok();
    }

    @Override
    public R<List<WorkTimeInfo>> queryLampLifeInfo(String deviceSerialNumber, Boolean isUpdate) {

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null) {
            return R.fail("未找到该设备");
        }

        if (info.getWorkTimeInfo() == null || info.getWorkTimeInfo().isEmpty() || isUpdate) {
            //查询信息
            String type = deviceModelInfoDAO.getDevTypeByDevModel(info.getDeviceModel());

            if (type == null) {
                log.info("该设备不支持查询使用寿命信息");
                return R.fail("该设备不支持查询使用寿命");
            }

            LambdaQueryWrapper<DeviceTypeInfoPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceTypeInfoPO::getDevType, type);
            DeviceTypeInfoPO deviceTypeInfoPO = deviceTypeInfoDAO.selectOne(wrapper);

            if (deviceTypeInfoPO == null || deviceTypeInfoPO.getWorkTimeCount() == null || deviceTypeInfoPO.getWorkTimeCount().isEmpty()) {
                log.info("该设备不支持查询使用寿命信息");
                return R.fail("该设备不支持查询使用寿命");
            }

            DevOperationInputParam inputParam = new DevOperationInputParam();
            inputParam.setOperationType(OperationTypeEnums.QueryLifeTime.getType());
            inputParam.setDeviceSerialNumber(deviceSerialNumber);

            inputParam.setWorkTimeInfo(deviceTypeInfoPO.getWorkTimeCount());


            R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
            if (!operation.getSuccess()) {
                return R.fail(operation.getMessage());
            }
            OperationResultEntity data = operation.getData();
            List<WorkTimeInfo> workTimeInfo = data.getWorkTimeInfo();

            info.setWorkTimeInfo(workTimeInfo);
            infoService.saveOrUpdateInfo(info);

            return R.ok(workTimeInfo);

        } else {
            return R.ok(info.getWorkTimeInfo());
        }
    }

    @Override
    public R<Boolean> hasWorkTime(String deviceSerialNumber) {

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null) {
            return R.ok(false);
        }


        String type = deviceModelInfoDAO.getDevTypeByDevModel(info.getDeviceModel());

        if (type == null) {
            log.info("该设备不支持查询使用寿命信息");
            return R.ok(false);
        }

        LambdaQueryWrapper<DeviceTypeInfoPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceTypeInfoPO::getDevType, type);
        DeviceTypeInfoPO typeInfoPO = deviceTypeInfoDAO.selectOne(wrapper);
        if (typeInfoPO == null || typeInfoPO.getWorkTimeCount() == null || typeInfoPO.getWorkTimeCount().isEmpty()) {
            log.info("该设备不支持查询使用寿命信息");
            return R.ok(false);
        }


        return R.ok(true);
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

    @Override
    public R connect(List<SearchInfo> params, String updateBy, String templateCode) {
        if (params == null || params.isEmpty()) {
            return R.fail("参数为空");
        }

        try {
            String userId = params.get(0).getUpdateBy();
            Integer connectType = params.get(0).getConnectType();

            log.info("批量连接设备：userId={}, 设备数量={}", userId, params.size());

//            List<FindDeviceVO> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            ArrayList<R<FindDeviceVO>> vos = new ArrayList<>();
            for (SearchInfo param : params) {
                try {
                    // 确保每个参数都有正确的 userId 和 connectType
                    param.setUpdateBy(userId);
                    param.setConnectType(connectType);

                    // 调用单台设备连接方法
                    R<FindDeviceVO> result = factoryConnectBatch(param);
                    if (result.getSuccess()) {
                        vos.add(result);
                        //                        R<Boolean> booleanR = startInitDevice(deviceSerialNumber, updateBy);
//                        //这里初始化之后还要创建新的任务，根据当前选定的模板创建任务，其实就是检定校准的任务
//                        if (booleanR.getSuccess()) {
//                            TaskAddDto taskAddDto = new TaskAddDto();
//                            String randomSuffix = generateRandomSuffix();
//                            taskAddDto.setDetectName(deviceSerialNumber + "_" + randomSuffix);
//                            taskAddDto.setUseTemplate(templateCode);
//                            taskAddDto.setCreateBy(updateBy);
//                            taskAddDto.setCreated(LocalDateTime.now());
//                            taskAddDto.setSerialNumber(deviceSerialNumber);
//                            Result<HashMap<String, String>> task = taskManagementService.newTask(taskAddDto);
//                            list.add(task.getData());
//                        }
                        successCount++;
                    } else {
                        vos.add(result);
                        failCount++;
                        log.warn("批量连接中部分失败：device={}, error={}",
                                param.getDeviceIp(), result.getMessage());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("批量连接中异常：device={}", param.getDeviceIp(), e);
                }
            }
            ArrayList<DeviceTaskDto> list = new ArrayList<>();
            for (R<FindDeviceVO> vo : vos) {
                if (vo.getSuccess()) {
                    log.info("批量连接成功：device={}", vo.getData().getDeviceSerialNumber());
                    DeviceTaskDto dto = new DeviceTaskDto();
                    dto.setDeviceSerialNumber(vo.getData().getDeviceSerialNumber());
                    dto.setStatus(OutFactoryCheckEnum.CONNECTED.getDesc());
                    list.add(dto);
                }
            }
            addToCache(list);
            log.info("批量连接完成：总数={}, 成功={}, 失败={}",
                    params.size(), successCount, failCount);
            return R.ok(vos);

        } catch (Exception e) {
            log.error("批量连接异常", e);
            return R.fail();
        }
    }

    @Override
    public Result<DeviceTaskDto> createTaskAfterInstrumentInit(String deviceSerialNumber, String updateBy, String templateCode) throws BusinessException {
        R<Boolean> booleanR = startInitDevice(deviceSerialNumber, updateBy);

        DeviceInfoPO deviceInfoPO = infoService.getInfo(deviceSerialNumber);
        if (deviceInfoPO == null || deviceInfoPO.getId() == null) {
            log.info("未找到连接仪器信息！");
            //未查找到仪器信息
            throw new BusinessException("未找到连接仪器信息！");
        }

//        List<InitRecordPO> initList = initRecordService.searchInitStatus(deviceSerialNumber, deviceInfoPO.getId());


        //这里要异步执行操作，前面返回，这里开异步线程执行
        // 异步执行初始化监控和任务创建
        asyncExecuteInitMonitor(deviceSerialNumber, templateCode, updateBy);

        // 立即返回成功响应
        DeviceTaskDto dto = new DeviceTaskDto();
        dto.setDeviceSerialNumber(deviceSerialNumber);
        dto.setStatus(OutFactoryCheckEnum.INIT_ING.getDesc());
        return Result.ok(dto);
//
//        while (true) {
//            //这里初始化之后还要创建新的任务，根据当前选定的模板创建任务，其实就是检定校准的任务
//            if (deviceInfoPO.getIsInited() == 1) {
//                LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
//                wrapper.eq(TaskManagement::getOperationInstrument, deviceSerialNumber);
//                List<TaskManagement> list = taskManagementService.list(wrapper);
//                if (!CollectionUtils.isEmpty(list)) {
//                    TaskManagement taskManagement = list.get(0);
//                    DeviceTaskDto dto = new DeviceTaskDto();
//                    dto.setTaskId(taskManagement.getId());
//                    ExcelTemplate excelTemplate = templateService.getById(taskManagement.getUseTemplate());
//                    if (ObjectUtils.isEmpty(excelTemplate)) {
//                        log.info("未找到模板！");
//                        throw new BusinessException("未找到模板！");
//                    }
//                    dto.setDeviceSerialNumber(deviceSerialNumber);
//                    dto.setTemplateCode(excelTemplate.getTemplateCode());
//                    dto.setStatus(OutFactoryCheckEnum.INIT_SUCCESS.getDesc());
////                map.put("templateCode", );
////                map.put("deviceSerialNumber",deviceSerialNumber);
////                Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(), OUT_FACTORY_CHECK_LIST.getCode(), String.class, Object.class);
////
////                List<DeviceTaskDto> dtoList;
////                if (cachedObject instanceof List) {
////                    @SuppressWarnings("unchecked")
////                    List<DeviceTaskDto> checkedList = (List<DeviceTaskDto>) cachedObject;
////                    dtoList = checkedList;
////                } else {
////                    dtoList = new ArrayList<DeviceTaskDto>();
////                }
////                dtoList.add(dto);
////                ehcacheUtil.put(DEFAULT_CACHE.getCacheName(), OUT_FACTORY_CHECK_LIST.getCode(), dtoList, String.class, Object.class);
//                    updateInCache(deviceSerialNumber, dto);
//                    return Result.ok(dto);
//                }
//                TaskAddDto taskAddDto = new TaskAddDto();
//                String randomSuffix = generateRandomSuffix();
//                taskAddDto.setDetectName(deviceSerialNumber + "_" + randomSuffix);
//                taskAddDto.setUseTemplate(templateCode);
//                taskAddDto.setCreateBy(updateBy);
//                taskAddDto.setCreated(LocalDateTime.now());
//                taskAddDto.setSerialNumber(deviceSerialNumber);
//                Result<HashMap<String, String>> task = taskManagementService.newTask(taskAddDto);
//                if (ObjectUtils.isEmpty(task)) {
//                    throw new BusinessException("创建任务失败！");
//                }
//
//                HashMap<String, String> map = task.getData();
//                DeviceTaskDto dto = new DeviceTaskDto();
//                dto.setTaskId(MapUtils.getString(map, "taskId"));
//                dto.setDeviceSerialNumber(deviceSerialNumber);
//                dto.setTemplateCode(MapUtils.getString(map, "templateCode"));
//                dto.setStatus(OutFactoryCheckEnum.INIT_SUCCESS.getDesc());
//                updateInCache(deviceSerialNumber, dto);
//                return Result.ok(dto);
//            } else if (deviceInfoPO.getIsInited() == 2) {
//                DeviceTaskDto dto = new DeviceTaskDto();
//                dto.setDeviceSerialNumber(deviceSerialNumber);
//                dto.setStatus(OutFactoryCheckEnum.INIT_FAIL.getDesc());
//                updateInCache(deviceSerialNumber, dto);
//                return Result.ok(dto);
//            }
//            //在这里等待一秒
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//
//        }
    }


    /**
     * 异步执行初始化监控和任务创建
     */
    private void asyncExecuteInitMonitor(String deviceSerialNumber, String templateCode, String updateBy) {
        measurementTaskExecutor.execute(() -> {
            try {
                monitorDeviceInitAndCreateTask(deviceSerialNumber, templateCode, updateBy);
            } catch (Exception e) {
                log.error("异步监控设备初始化失败:deviceSerialNumber={}", deviceSerialNumber, e);
                // 更新缓存为失败状态
                DeviceTaskDto failDto = new DeviceTaskDto();
                failDto.setDeviceSerialNumber(deviceSerialNumber);
                failDto.setStatus(OutFactoryCheckEnum.INIT_FAIL.getDesc());
                updateInCache(deviceSerialNumber, failDto);
            }
        });
    }

/**
 * 监控设备初始化状态并创建任务
 */
private void monitorDeviceInitAndCreateTask(String deviceSerialNumber, String templateCode, String updateBy) throws BusinessException {
    while (true) {
        DeviceInfoPO deviceInfoPO = infoService.getInfo(deviceSerialNumber);
        if (deviceInfoPO == null) {
            log.error("监控过程中设备信息丢失:deviceSerialNumber={}", deviceSerialNumber);
            break;
        }

        //这里初始化之后还要创建新的任务，根据当前选定的模板创建任务，其实就是检定校准的任务
        if (deviceInfoPO.getIsInited() == 1) {
            LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaskManagement::getOperationInstrument, deviceSerialNumber);
            List<TaskManagement> list = taskManagementService.list(wrapper);
            if (!CollectionUtils.isEmpty(list)) {
                TaskManagement taskManagement = list.get(0);
                DeviceTaskDto dto = new DeviceTaskDto();
                dto.setTaskId(taskManagement.getId());
                ExcelTemplate excelTemplate = templateService.getById(taskManagement.getUseTemplate());
                if (ObjectUtils.isEmpty(excelTemplate)) {
                    log.info("未找到模板!");
                    throw new BusinessException("未找到模板!");
                }
                dto.setDeviceSerialNumber(deviceSerialNumber);
                dto.setTemplateCode(excelTemplate.getTemplateCode());
                dto.setStatus(OutFactoryCheckEnum.INIT_SUCCESS.getDesc());
                updateInCache(deviceSerialNumber, dto);
                break;
            }
            TaskAddDto taskAddDto = new TaskAddDto();
            String randomSuffix = generateRandomSuffix();
            taskAddDto.setDetectName(deviceSerialNumber + "_" + randomSuffix);
            taskAddDto.setUseTemplate(templateCode);
            taskAddDto.setCreateBy(updateBy);
            taskAddDto.setCreated(LocalDateTime.now());
            taskAddDto.setSerialNumber(deviceSerialNumber);
            taskAddDto.setTaskType(FACTORY_INSPECTION_TASK.getCode());
            Result<HashMap<String, String>> task = taskManagementService.newTask(taskAddDto);
            if (ObjectUtils.isEmpty(task)) {
                throw new BusinessException("创建任务失败!");
            }

            HashMap<String, String> map = task.getData();
            DeviceTaskDto dto = new DeviceTaskDto();
            dto.setTaskId(MapUtils.getString(map, "taskId"));
            dto.setDeviceSerialNumber(deviceSerialNumber);
            dto.setTemplateCode(MapUtils.getString(map, "templateCode"));
            dto.setStatus(OutFactoryCheckEnum.INIT_SUCCESS.getDesc());
            updateInCache(deviceSerialNumber, dto);
            break;
        } else if (deviceInfoPO.getIsInited() == 2) {
            DeviceTaskDto dto = new DeviceTaskDto();
            dto.setDeviceSerialNumber(deviceSerialNumber);
            dto.setStatus(OutFactoryCheckEnum.INIT_FAIL.getDesc());
            updateInCache(deviceSerialNumber, dto);
            break;
        }
        //在这里等待一秒
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("异步监控线程被中断:deviceSerialNumber={}", deviceSerialNumber);
            break;
        }
    }
}

// ... existing code ...

    /**
     * 更新缓存中指定仪器的信息
     *
     * @param deviceSerialNumber 仪器序列号
     * @param updatedData        需要更新的数据项
     */
    private void updateInCache(String deviceSerialNumber, DeviceTaskDto updatedData) {
        try {
            // 获取缓存中的列表
            Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(),
                    OUT_FACTORY_CHECK_LIST.getCode(), String.class, Object.class);

            if (cachedObject instanceof List) {
                @SuppressWarnings("unchecked")
                List<DeviceTaskDto> dtoList = (List<DeviceTaskDto>) cachedObject;

                if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(dtoList)) {
                    boolean found = false;
                    // 找到并更新包含该设备序列号的项
                    for (int i = 0; i < dtoList.size(); i++) {
                        DeviceTaskDto item = dtoList.get(i);
                        if (item != null && deviceSerialNumber.equals(item.getDeviceSerialNumber())) {
                            // 更新该项的属性
                            if (updatedData.getTaskId() != null) {
                                item.setTaskId(updatedData.getTaskId());
                            }
                            if (updatedData.getTemplateCode() != null) {
                                item.setTemplateCode(updatedData.getTemplateCode());
                            }
                            // 确保设备序列号不变
                            item.setDeviceSerialNumber(deviceSerialNumber);
                            dtoList.set(i, item);
                            found = true;
                            log.info("已更新缓存中设备信息：deviceSerialNumber={}", deviceSerialNumber);
                            break;
                        }
                    }

                    // 如果未找到，可以选择添加新项（根据业务需求决定）
                    if (!found) {
                        log.warn("缓存中未找到设备信息，是否要添加？deviceSerialNumber={}", deviceSerialNumber);
                        // 如果需要添加新项，取消下面这行的注释
                        dtoList.add(updatedData);
                    }

                    // 更新缓存
                    ehcacheUtil.put(DEFAULT_CACHE.getCacheName(),
                            OUT_FACTORY_CHECK_LIST.getCode(), dtoList, String.class, Object.class);
                }
            }
        } catch (Exception e) {
            log.error("更新缓存中设备信息失败：deviceSerialNumber={}", deviceSerialNumber, e);
        }
    }

    /**
     * 将设备任务信息添加到缓存
     *
     * @param dto 设备任务 DTO
     */
    private void addToCache(DeviceTaskDto dto) {
        try {
            // 获取缓存中的列表
            Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(),
                    OUT_FACTORY_CHECK_LIST.getCode(), String.class, Object.class);

            List<DeviceTaskDto> dtoList;
            if (cachedObject instanceof List) {
                @SuppressWarnings("unchecked")
                List<DeviceTaskDto> checkedList = (List<DeviceTaskDto>) cachedObject;
                dtoList = checkedList;
            } else {
                dtoList = new ArrayList<DeviceTaskDto>();
            }

            dtoList.add(dto);
            ehcacheUtil.put(DEFAULT_CACHE.getCacheName(),
                    OUT_FACTORY_CHECK_LIST.getCode(), dtoList, String.class, Object.class);

            log.info("已添加设备到缓存：deviceSerialNumber={}", dto.getDeviceSerialNumber());
        } catch (Exception e) {
            log.error("添加设备到缓存失败：deviceSerialNumber={}", dto.getDeviceSerialNumber(), e);
        }
    }

    /**
     * 批量将设备任务信息添加到缓存
     *
     * @param dtoList 设备任务 DTO 列表
     */
    private void addToCache(List<DeviceTaskDto> dtoList) {
        if (CollectionUtils.isEmpty(dtoList)) {
            log.warn("批量添加到缓存的 DTO 列表为空");
            return;
        }

        try {
            // 获取缓存中的列表
            Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(),
                    OUT_FACTORY_CHECK_LIST.getCode(), String.class, Object.class);

            List<DeviceTaskDto> cacheList;
            if (cachedObject instanceof List) {
                @SuppressWarnings("unchecked")
                List<DeviceTaskDto> checkedList = (List<DeviceTaskDto>) cachedObject;
                cacheList = checkedList;
            } else {
                cacheList = new ArrayList<DeviceTaskDto>();
            }

            // 批量添加（去重）
            for (DeviceTaskDto dto : dtoList) {
                if (dto != null) {
                    // 检查是否已存在相同设备序列号
                    boolean exists = cacheList.stream()
                            .anyMatch(item -> dto.getDeviceSerialNumber().equals(item.getDeviceSerialNumber()));

                    if (!exists) {
                        cacheList.add(dto);
                        log.info("已添加设备到缓存：deviceSerialNumber={}", dto.getDeviceSerialNumber());
                    } else {
                        log.warn("设备已存在于缓存中，跳过添加：deviceSerialNumber={}", dto.getDeviceSerialNumber());
                    }
                }
            }

            ehcacheUtil.put(DEFAULT_CACHE.getCacheName(),
                    OUT_FACTORY_CHECK_LIST.getCode(), cacheList, String.class, Object.class);

            log.info("批量添加设备到缓存完成，共添加 {} 个设备（去重后）", cacheList.size());
        } catch (Exception e) {
            log.error("批量添加设备到缓存失败", e);
        }
    }


    private String generateRandomSuffix() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String nums = "23456789";
        StringBuilder suffix = new StringBuilder();

        // 第 1 位：大写字母
        suffix.append(chars.charAt((int) (Math.random() * chars.length())));

        // 第 2-4 位：数字或字母混合
        for (int i = 0; i < 3; i++) {
            if (Math.random() > 0.5) {
                suffix.append(chars.charAt((int) (Math.random() * chars.length())));
            } else {
                suffix.append(nums.charAt((int) (Math.random() * nums.length())));
            }
        }

        return suffix.toString();
    }
}
