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
import com.persee.workstation.uv_inexus.enums.T600ComIdEnums;
import com.persee.workstation.uv_inexus.enums.factoryInspection.OutFactoryCheckEnum;
import com.persee.workstation.uv_inexus.enums.factoryInspection.TemplateTypeEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.EnergyLampEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.exception.BusinessException;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskManagement;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.*;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.StepPoint;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.WorkTimeInfo;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TaskManagementService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TemplateService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceUsageLogService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.InitRecordService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.DeviceMessageUtils;
import com.persee.workstation.uv_inexus.util.changeUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.ActionCmdEnum;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.util.pythonUtil.PeakReceiveInfo;
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
import java.util.stream.Collectors;

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

    @Autowired
    RestUtil restUtil;

    @Autowired
    SystemAlgorithmConfService systemAlgorithmConfService;


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
            return R.fail("00203001");
        }

        List<InitRecordPO> initList = initRecordService.searchInitStatus(deviceSerialNumber, deviceInfoPO.getId());
        if (initList.isEmpty()) {
            log.info("查询初始化状态异常");
            //查询初始化状态异常
            return R.fail(false, "00203002");
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
            return R.fail("00203001");
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
            return R.fail("00203003");
        }

        DeviceInfoPO infoPO = infoService.getInfo(initDTO.getDeviceSerialNumber());
        if (infoPO == null || !StringUtils.hasText(infoPO.getId()) || !StringUtils.hasText(infoPO.getDeviceModel())) {
            log.info("未找到连接仪器信息！");
            //未查找到仪器信息
            return R.fail("00203001");
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
//        list.add(new Command(FindDevEnum.FindDevice.getComId(), new Object[]{}));
//        list.add(new Command(FindDevEnum.NewIP.getComId(), new Object[]{}));
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
            return R.fail("00203003");
        }

        if (dto.getCalibrationType() == null || DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()) == null) {
            log.info("缺少校准类型");
            //缺少校准类型
            return R.fail("00203004");
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

                if (type.equals(DeviceCalibrationInfoEnum.MultiWavelength.getType())) {
                    log.info("多波长校正，跳过");
                    continue;
                }


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

                    if (param.getParam().equals(DeviceCalibrationParamEnum.Table.getType()) || param.getParam().equals(DeviceCalibrationParamEnum.Graph.getType())) {
                        paramVO.setTableData(param.getTableData());

                        wrapper.eq(CalibrationTempPO::getCalibrationType, type);
                        wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, deviceSerialNumber);
                        wrapper.eq(CalibrationTempPO::getParam, param.getParam());
                        wrapper.orderByDesc(CalibrationTempPO::getUpdated).last("limit 1");
                        CalibrationTempPO tempPO = tempDao.selectOne(wrapper);
                        wrapper.clear();
                        if (tempPO != null) {
                            paramVO.setLastValue(tempPO.getParamValue());

                            if(param.getParam().equals(DeviceCalibrationParamEnum.Table.getType())) {
                                tempPO.getTableData().setHead(param.getTableData().getHead());

                                List<CalibrationTableHeadVO> headVOList = param.getTableData().getHead();

                                for (CalibrationTableHeadVO headVO : headVOList) {
                                    String key = headVO.getKey();
                                    List<Integer> values = null;
                                    MappingParamEnum paramEnum = null;

                                    if (key.equals(DeviceCalibrationParamEnum.Mode.getType())) {

                                        values = typeInfoPO.getLuminosity();
                                        if (values != null && !values.isEmpty()) {
                                            values = values.stream()
                                                    .map(v -> v + 1)
                                                    .collect(Collectors.toList());
                                        }
                                        paramEnum = MappingParamEnum.LUMINOSITY;
                                    } else if (key.equals(DeviceCalibrationParamEnum.Lamp.getType())) {
                                        values = typeInfoPO.getLampType();
                                        paramEnum = MappingParamEnum.LAMP_TYPE;
                                        List<DataNameValue> result = new ArrayList<>();
                                        for (Integer value : values) {
                                            DataNameValue dataNameValue = new DataNameValue();
                                            dataNameValue.setDataValue(String.valueOf(value));

                                            String name = EnergyLampEnums.getName(String.valueOf(value));

                                            dataNameValue.setDataKey(name != null ? name : String.valueOf(value));

                                            result.add(dataNameValue);
                                        }
                                        headVO.setData(result);
                                        continue;

                                    } else if (key.equals(DeviceCalibrationParamEnum.Gain.getType())) {
                                        values = typeInfoPO.getGain();
                                    } else if (key.equals(DeviceCalibrationParamEnum.RealWavelength.getType()) || key.equals(DeviceCalibrationParamEnum.StandWavelength.getType())) {
                                        List<DataNameValue> result = dealMaxAndMinLength(typeInfoPO);
                                        headVO.setData(result);
                                        continue;
                                    }

                                    if (values != null && !values.isEmpty()) {
                                        List<DataNameValue> data = buildDataNameValues(values, paramEnum);
                                        headVO.setData(data);
                                    }
                                }
                                tempPO.getTableData().setHead(headVOList);
                            }
                            paramVO.setTableData(tempPO.getTableData());
                        }

                        params.add(paramVO);
                        continue;
                    } else {
                        List<DataNameValue> defaultValue = param.getDefaultValue();
                        if (defaultValue != null && !defaultValue.isEmpty()) {


                            if (paramVO.getParam().equals(DeviceCalibrationParamEnum.Start.getType())) {

                                paramVO.setLastValue(typeInfoPO.getMaxWavelength());

                            } else if (paramVO.getParam().equals(DeviceCalibrationParamEnum.End.getType())) {
                                paramVO.setLastValue(typeInfoPO.getMinWavelength());
                            } else if (paramVO.getParam().equals(DeviceCalibrationParamEnum.ScanSpeed.getType())) {
                                Object speed = MappingParamEnum.SCAN_SPEED.getDefaultValue();
                                String jsonString = JSONObject.toJSONString(speed);
                                List<DeviceConfParam> options = JSON.parseArray(jsonString, DeviceConfParam.class);
                                DeviceConfParam deviceConfParam = options.get(0);

                                paramVO.setLastValue(deviceConfParam.getParamValue());
                            }else {
                                DataNameValue value = defaultValue.get(0);
                                if (StringUtils.hasText(value.getDataValue())) {
                                    paramVO.setLastValue(value.getDataValue());
                                }
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

    @Override
    public List<DeviceCalibrationVO> queryMultiCalibration(String deviceSerialNumber) {
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
        Integer type = DeviceCalibrationInfoEnum.MultiWavelength.getType();

        if (!calibrationType.contains(DeviceCalibrationInfoEnum.MultiWavelength.getType())) {
            log.info("当前设备不支持多波长校准");
            return null;
        }

        List<DeviceCalibrationVO> ans = new ArrayList<>();

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

            if (param.getParam().equals(DeviceCalibrationParamEnum.Table.getType()) || param.getParam().equals(DeviceCalibrationParamEnum.Graph.getType())) {
                paramVO.setTableData(param.getTableData());

                wrapper.eq(CalibrationTempPO::getCalibrationType, type);
                wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, deviceSerialNumber);
                wrapper.eq(CalibrationTempPO::getParam, param.getParam());
                wrapper.orderByDesc(CalibrationTempPO::getUpdated).last("limit 1");
                CalibrationTempPO tempPO = tempDao.selectOne(wrapper);
                wrapper.clear();
                if (tempPO != null) {
                    paramVO.setLastValue(tempPO.getParamValue());

                    if(param.getParam().equals(DeviceCalibrationParamEnum.Table.getType())) {
                        tempPO.getTableData().setHead(param.getTableData().getHead());

                        List<CalibrationTableHeadVO> headVOList = param.getTableData().getHead();

                        for (CalibrationTableHeadVO headVO : headVOList) {
                            String key = headVO.getKey();
                            List<Integer> values = null;
                            MappingParamEnum paramEnum = null;

                            if (key.equals(DeviceCalibrationParamEnum.Mode.getType())) {

                                values = typeInfoPO.getLuminosity();
                                if (values != null && !values.isEmpty()) {
                                    values = values.stream()
                                            .map(v -> v + 1)
                                            .collect(Collectors.toList());
                                }
                                paramEnum = MappingParamEnum.LUMINOSITY;
                            } else if (key.equals(DeviceCalibrationParamEnum.Lamp.getType())) {
                                values = typeInfoPO.getLampType();
                                paramEnum = MappingParamEnum.LAMP_TYPE;
                                List<DataNameValue> result = new ArrayList<>();
                                for (Integer value : values) {
                                    DataNameValue dataNameValue = new DataNameValue();
                                    dataNameValue.setDataValue(String.valueOf(value));

                                    String name = EnergyLampEnums.getName(String.valueOf(value));

                                    dataNameValue.setDataKey(name != null ? name : String.valueOf(value));

                                    result.add(dataNameValue);
                                }
                                headVO.setData(result);
                                continue;

                            } else if (key.equals(DeviceCalibrationParamEnum.Gain.getType())) {
                                values = typeInfoPO.getGain();
                            } else if (key.equals(DeviceCalibrationParamEnum.RealWavelength.getType()) || key.equals(DeviceCalibrationParamEnum.StandWavelength.getType())) {
                                List<DataNameValue> result = dealMaxAndMinLength(typeInfoPO);
                                headVO.setData(result);
                                continue;
                            }

                            if (values != null && !values.isEmpty()) {
                                List<DataNameValue> data = buildDataNameValues(values, paramEnum);
                                headVO.setData(data);
                            }
                        }
                        tempPO.getTableData().setHead(headVOList);
                    }
                    paramVO.setTableData(tempPO.getTableData());
                }

                params.add(paramVO);
                continue;
            } else {
                List<DataNameValue> defaultValue = param.getDefaultValue();
                if (defaultValue != null && !defaultValue.isEmpty()) {


                    if (paramVO.getParam().equals(DeviceCalibrationParamEnum.Start.getType())) {

                        paramVO.setLastValue(typeInfoPO.getMaxWavelength());

                    } else if (paramVO.getParam().equals(DeviceCalibrationParamEnum.End.getType())) {
                        paramVO.setLastValue(typeInfoPO.getMinWavelength());
                    } else if (paramVO.getParam().equals(DeviceCalibrationParamEnum.ScanSpeed.getType())) {
                        Object speed = MappingParamEnum.SCAN_SPEED.getDefaultValue();
                        String jsonString = JSONObject.toJSONString(speed);
                        List<DeviceConfParam> options = JSON.parseArray(jsonString, DeviceConfParam.class);
                        DeviceConfParam deviceConfParam = options.get(0);

                        paramVO.setLastValue(deviceConfParam.getParamValue());
                    }else {
                        DataNameValue value = defaultValue.get(0);
                        if (StringUtils.hasText(value.getDataValue())) {
                            paramVO.setLastValue(value.getDataValue());
                        }
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




        return ans;
    }

    private static List<DataNameValue> dealMaxAndMinLength(DeviceTypeInfoPO typeInfoPO) {
        List<DataNameValue> result = new ArrayList<>();

        String minWavelength = typeInfoPO.getMinWavelength();
        String maxWavelength = typeInfoPO.getMaxWavelength();
        DataNameValue min = new DataNameValue();
        min.setDataKey("min");
        min.setDataValue(minWavelength);
        result.add(min);

        DataNameValue max = new DataNameValue();
        max.setDataKey("max");
        max.setDataValue(maxWavelength);
        result.add(max);
        return result;
    }

    // ... existing code ...

    /**
     * 构建DataNameValue列表
     * @param values 值列表
     * @param paramEnum 参数枚举（可为null，表示直接使用值作为key）
     * @return DataNameValue列表
     */
    private List<DataNameValue> buildDataNameValues(List<Integer> values, MappingParamEnum paramEnum) {
        List<DataNameValue> result = new ArrayList<>();

        Map<Integer, String> nameMap = new HashMap<>();
        if (paramEnum != null) {
            Object defaultValue = paramEnum.getDefaultValue();
            String jsonString = JSONObject.toJSONString(defaultValue);
            List<DeviceConfParam> options = JSON.parseArray(jsonString, DeviceConfParam.class);
            for (DeviceConfParam option : options) {
                try {
                    Integer paramValue = Integer.valueOf(option.getParamValue());
                    nameMap.put(paramValue, option.getParamName());
                } catch (NumberFormatException e) {
                    // 忽略无法转换的值
                }
            }
        }

        for (Integer value : values) {
            DataNameValue dataNameValue = new DataNameValue();
            dataNameValue.setDataValue(String.valueOf(value));

            if (paramEnum != null) {
                String name = nameMap.get(value);
                dataNameValue.setDataKey(name != null ? name : String.valueOf(value));
            } else {
                dataNameValue.setDataKey(String.valueOf(value));
            }

            result.add(dataNameValue);
        }

        return result;
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
    public R<List<SearchInfo>> searchDevice(Integer connectType, String updateBy) {

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
        inputParam.setUpdateBy(updateBy);
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
            saveWlList(data.getDeviceSerialNumber(), data.getStepPoints(), param.getUpdateBy());
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

    private void saveWlList(String deviceSerialNumber, List<StepPoint> stepPoints, String updateBy) {

        if(stepPoints ==null || stepPoints.isEmpty()) {
            log.info("没有要保存的校正信息");
            return;

        }
        // 1. 查询是否存在当前设备的校准记录
        LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, deviceSerialNumber)
                .eq(CalibrationTempPO::getCalibrationType, DeviceCalibrationInfoEnum.MultiWavelength.getType())
                .eq(CalibrationTempPO::getParam, DeviceCalibrationParamEnum.Table.getType());
        CalibrationTempPO existingPo = tempDao.selectOne(wrapper);

        // 2. 获取系数
        Double coefficient = infoService.queryCoefficient(deviceSerialNumber);

        if (existingPo == null) {
            // 新增模式：从模板获取表结构并填充数据
            LambdaQueryWrapper<DeviceCalibrationParamPO> paramWrapper = new LambdaQueryWrapper<>();
            paramWrapper.eq(DeviceCalibrationParamPO::getParam, DeviceCalibrationParamEnum.Table.getType());

            DeviceCalibrationParamPO templateParam = calibrationParamDAO.selectOne(paramWrapper);
            CalibrationTableVO tableVO = templateParam.getTableData();
            List<CalibrationTableData> content = buildTableDataList(stepPoints, coefficient, 0);
            tableVO.setContent(content);

            CalibrationTempPO newPo = new CalibrationTempPO();
            newPo.setDeviceSerialNumber(deviceSerialNumber);
            newPo.setCreateBy(updateBy);
            newPo.setCalibrationType(DeviceCalibrationInfoEnum.MultiWavelength.getType());
            newPo.setParam(DeviceCalibrationParamEnum.Table.getType());
            newPo.setTableData(tableVO);

            tempDao.insert(newPo);
        } else {
            // 更新模式：在现有数据基础上追加新点
            CalibrationTableVO existingTableVO = existingPo.getTableData();
            List<CalibrationTableData> existingContent = existingTableVO.getContent();



            // 构建需要新增的数据（过滤掉重复且目标条目标识缺失的）
            List<CalibrationTableData> newContent = new ArrayList<>();
            int currentNum = 0;
            for (StepPoint point : stepPoints) {
                String standLength = String.valueOf(point.getTheoryStep() / coefficient);
                String realLength = String.valueOf(point.getActualStep() / coefficient);
                //去掉过滤，直接使用仪器的覆盖
//                // 检查是否与已有数据冲突且冲突记录的 name 或 identifier 为空
//                boolean shouldSkip = existingContent.stream()
//                        .filter(existing -> existing.getStandLength().equals(standLength) && existing.getRealLength() != null && existing.getRealLength().equals(realLength))
//                        .anyMatch(existing -> isNullOrEmpty(existing.getName()) || isNullOrEmpty(existing.getIdentifier()));
//
//                if (shouldSkip) {
//                    continue; // 不添加该项
//                }

                CalibrationTableData data = new CalibrationTableData();
                data.setStandLength(standLength);
                data.setRealLength(realLength);
                data.setNum(String.valueOf(++currentNum));
                data.setIsOriginData(true);
                newContent.add(data);
            }

//            existingContent.addAll(newContent);

            existingTableVO.setContent(newContent);
            // 更新实体
            existingPo.setTableData(existingTableVO);
            existingPo.setUpdateBy(updateBy);
            tempDao.updateById(existingPo);
        }

        wrapper.clear();
        wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, deviceSerialNumber)
                .eq(CalibrationTempPO::getCalibrationType, DeviceCalibrationInfoEnum.MultiWavelength.getType())
                .eq(CalibrationTempPO::getParam, DeviceCalibrationParamEnum.Graph.getType());
        tempDao.delete(wrapper);
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 根据步进点列表构建校准表数据，自动分配序号
     * @param stepPoints  步进点列表
     * @param coefficient 系数
     * @param startNum    起始序号（已存在的最大序号）
     * @return 校准表数据列表
     */
    private List<CalibrationTableData> buildTableDataList(List<StepPoint> stepPoints, Double coefficient, int startNum) {
        List<CalibrationTableData> list = new ArrayList<>();
        int currentNum = startNum;
        for (StepPoint point : stepPoints) {
            CalibrationTableData data = new CalibrationTableData();
            data.setStandLength(String.valueOf(point.getTheoryStep() / coefficient));
            data.setRealLength(String.valueOf(point.getActualStep() / coefficient));
            data.setNum(String.valueOf(++currentNum));
            data.setIsOriginData(true);
            list.add(data);
        }
        return list;
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
//                info.setConnectType(-1);
//                info.setIp("");
//                info.setIsInited(0);
//                info.setMachineIp("");
//                info.setDevicePort(-1);
                info.setUpdateBy(updateBy);
                infoService.saveOrUpdateInfo(info);
                deviceUsageLogService.saveDeviceUsageLog(logPO);
                return R.ok(true);
            } else {
                return R.fail(operation.getMessage());
            }

        } else {

            info.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
            info.setDeviceId("");
//            info.setConnectType(-1);
//            info.setIp("");
//            info.setIsInited(0);
//            info.setMachineIp("");
//            info.setDevicePort(-1);
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
            return R.fail("00203003");
        }

        if (dto.getCalibrationType() == null || DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()) == null) {
            log.info("缺少校准类型");
            //缺少校准类型
            return R.fail("00203004");
        }


        infoService.updateDeviceCurrentStatus(deviceSerialNumber, WebSocketOperationEnum.Calibration.getOperation(), Constants.DEVICE_STATUS_CALIBRATION);
        if (Objects.equals(dto.getCalibrationType(), DeviceCalibrationInfoEnum.T800Dark.getType())) {
            double start = 900.0;
            double end = 190.0;
            double step = 1.0;

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

//                    if (datum.getDataKey().equals(DeviceCalibrationParamEnum.WaveBrand.getType())) {
//                        darkBand = Double.parseDouble(datum.getDataValue());
//                    }
//                    if (datum.getDataKey().equals(DeviceCalibrationParamEnum.Step.getType())) {
//                        step = Double.parseDouble(datum.getDataValue());
//                    }
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

//            List<String> bandWidth = vo.getBandWidth();
//            if (!bandWidth.contains(String.valueOf(darkBand))) {
//                log.info("当前仪器不支持该光谱带宽！");
//                infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
//                return R.fail("当前仪器不支持该光谱带宽！");
//            }
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
            param.setPhotoMode(Integer.valueOf(WaveLengthEnum.tPercent.getCode()));
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
    public R<Boolean> deleteMultiCalibrationInfo(DeviceCalibrationDTO dto) {
        if (!StringUtils.hasText(dto.getDeviceSerialNumber())) {
            return R.fail("请连接仪器！");
        }

        List<CalibrationTableData> deleteData = dto.getCalibrationTableData();
        if (deleteData == null || deleteData.isEmpty()) {
            return R.fail("没有需要删除的波长信息！");
        }

        Map<String, List<String>> toDeleteInfo = new HashMap<>();
        for (CalibrationTableData de : deleteData) {
            String identifier = de.getIdentifier();
            String num = de.getNum();
            toDeleteInfo.computeIfAbsent(identifier, k -> new ArrayList<>()).add(num);
        }

        LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType())
                .eq(CalibrationTempPO::getParam, DeviceCalibrationParamEnum.Table.getType());

        CalibrationTempPO tablePo = tempDao.selectOne(wrapper);
        if (tablePo != null && tablePo.getTableData() != null) {
            CalibrationTableVO tableData = tablePo.getTableData();
            List<CalibrationTableData> content = tableData.getContent();
            if (content != null) {
                List<CalibrationTableData> newContent = content.stream()
                        .filter(data -> {
                            List<String> numToDelete = toDeleteInfo.get(data.getIdentifier());
                            return numToDelete == null || !numToDelete.contains(data.getNum());
                        })
                        .collect(Collectors.toList());
                tableData.setContent(newContent);
                tablePo.setTableData(tableData);
                tempDao.update(tablePo, wrapper);
            }
        }

        wrapper.clear();
        wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType())
                .eq(CalibrationTempPO::getParam, DeviceCalibrationParamEnum.Graph.getType());

        CalibrationTempPO graphPo = tempDao.selectOne(wrapper);
        if (graphPo == null || graphPo.getTableData() == null) {
            return R.ok();
        }

        CalibrationTableVO graphData = graphPo.getTableData();
        Map<String, CalibrationGraphDataVO> graph = graphData.getGraph();
        List<CalibrationTableHeadVO> graphHead = graphData.getHead();

        if (graph == null || graph.isEmpty()) {
            return R.ok();
        }

        List<String> removeKeys = new ArrayList<>();
        Set<String> processedIdentifiers = new HashSet<>();

        toDeleteInfo.forEach((identifier, nums) -> {
            if (nums == null || nums.isEmpty()) {
                return;
            }

            CalibrationGraphDataVO vo = graph.get(identifier);
            if (vo == null) {
                return;
            }

            Map<String, List<GraphData>> voData = vo.getData();
            if (voData == null || voData.isEmpty()) {
                return;
            }

            Map<String, List<GraphData>> newData = new HashMap<>();
            voData.forEach((key, value) -> {
                if (!nums.contains(key)) {
                    newData.put(key, value);
                }
            });

            if (newData.isEmpty()) {
                removeKeys.add(identifier);
            } else {
                vo.setData(newData);
                graph.put(identifier, vo);
            }

            processedIdentifiers.add(identifier);
        });

        if (!removeKeys.isEmpty()) {
            removeKeys.forEach(graph::remove);
        }

        if (graphHead != null) {
            List<CalibrationTableHeadVO> newGraphHead = graphHead.stream()
                    .filter(headVO -> !removeKeys.contains(headVO.getKey()))
                    .collect(Collectors.toList());
            graphData.setHead(newGraphHead);
        }

        graphData.setGraph(graph);
        graphPo.setTableData(graphData);
        tempDao.update(graphPo, wrapper);

        return R.ok();
    }

    @Override
    public R<Boolean> saveAllData(DeviceCalibrationDTO dto) {

        if (!StringUtils.hasText(dto.getDeviceSerialNumber())) {
            return R.fail("请连接仪器！");
        }
        if (dto.getAllTableData() == null || dto.getAllTableData().isEmpty()) {
            return R.fail("没有需要保存信息！");
        }

        LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                .eq(CalibrationTempPO::getCalibrationType, DeviceCalibrationInfoEnum.MultiWavelength.getType());
        wrapper.eq(CalibrationTempPO::getParam, DeviceCalibrationParamEnum.Table.getType());

        CalibrationTempPO tablePo = tempDao.selectOne(wrapper);

        LambdaQueryWrapper<DeviceCalibrationParamPO> paramWrapper = new LambdaQueryWrapper<>();
        paramWrapper.eq(DeviceCalibrationParamPO::getParam, DeviceCalibrationParamEnum.Table.getType());

        DeviceCalibrationParamPO paramPO = calibrationParamDAO.selectOne(paramWrapper);

        CalibrationTableVO tableVO = paramPO.getTableData();

        List<CalibrationTableData> allTableData = dto.getAllTableData();
        allTableData.forEach(data -> data.setIsOriginData(true));

        if (tablePo == null) {

            CalibrationTempPO po = new CalibrationTempPO();
            po.setDeviceSerialNumber(dto.getDeviceSerialNumber());
            po.setCreateBy(dto.getUpdateBy());
            po.setCalibrationType(dto.getCalibrationType());
            po.setParam(DeviceCalibrationParamEnum.Table.getType());
            tableVO.setContent(allTableData);
            po.setTableData(tableVO);

            tempDao.insert(po);
        } else {

            tablePo.setUpdateBy(dto.getUpdateBy());
            CalibrationTableVO tableData = tablePo.getTableData();
            if (tableData == null) {
                tableData = new CalibrationTableVO();
                tableData.setHead(tableVO.getHead());
            }

            tableData.setContent(allTableData);

            tablePo.setTableData(tableData);
            tempDao.update(tablePo, wrapper);
        }


        return R.ok();
    }

    /**
     *
     * 下发所有的参数给仪器
     */
    @Override
    public R<Boolean> sendMultiCalibrationToDevice(DeviceCalibrationDTO dto) {
        if (!StringUtils.hasText(dto.getDeviceSerialNumber())) {
            return R.fail("请连接仪器！");
        }

        String deviceSerialNumber = dto.getDeviceSerialNumber();
        List<CalibrationTableData> allTableData = dto.getAllTableData();

        if (allTableData == null || allTableData.isEmpty()) {
            return R.fail("没有可以下发给仪器的参数！");
        }

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.SetWlList.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);

        List<StepPoint> setWlList = new ArrayList<>();

        for (CalibrationTableData data : allTableData) {
            if (StringUtils.hasText(data.getStandLength()) && StringUtils.hasText(data.getRealLength())) {
                setWlList.add(new StepPoint(Double.parseDouble(data.getRealLength()), Double.parseDouble(data.getStandLength()), 1));
            }
        }

        if (setWlList.isEmpty()) {
            return R.fail("没有可以下发给仪器的参数！");
        }
        inputParam.setSetWlList(setWlList);

        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        }

        allTableData.forEach(data -> data.setIsSendToDevice(true));

        LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());
        wrapper.eq(CalibrationTempPO::getParam, DeviceCalibrationParamEnum.Table.getType());

        CalibrationTempPO tablePo = tempDao.selectOne(wrapper);

        LambdaQueryWrapper<DeviceCalibrationParamPO> paramWrapper = new LambdaQueryWrapper<>();
        paramWrapper.eq(DeviceCalibrationParamPO::getParam, DeviceCalibrationParamEnum.Table.getType());

        DeviceCalibrationParamPO paramPO = calibrationParamDAO.selectOne(paramWrapper);

        CalibrationTableVO tableVO = paramPO.getTableData();

        if (tablePo == null) {

            CalibrationTempPO po = new CalibrationTempPO();
            po.setDeviceSerialNumber(dto.getDeviceSerialNumber());
            po.setCreateBy(dto.getUpdateBy());
            po.setCalibrationType(dto.getCalibrationType());
            po.setParam(DeviceCalibrationParamEnum.Table.getType());
            tableVO.setContent(allTableData);
            po.setTableData(tableVO);

            tempDao.insert(po);
        } else {

            tablePo.setUpdateBy(dto.getUpdateBy());
            CalibrationTableVO tableData = tablePo.getTableData();
            if (tableData == null) {
                tableData = new CalibrationTableVO();
                tableData.setHead(tableVO.getHead());
            }

            tableData.setContent(allTableData);

            tablePo.setTableData(tableData);
            tempDao.update(tablePo, wrapper);
        }


        return R.ok();
    }

    @Override
    public R<Boolean> multiCalibration(DeviceCalibrationDTO dto, String multiType) {
        infoService.updateDeviceCurrentStatus(dto.getDeviceSerialNumber(), WebSocketOperationEnum.Calibration.getOperation(), Constants.DEVICE_STATUS_CALIBRATION);

        R<SetParam> r = multiCheckAndSendParam(dto, multiType);
        if (!r.getSuccess()) {
            infoService.updateDeviceCurrentStatus(dto.getDeviceSerialNumber(), Constants.DEVICE_STATUS_IDLE);
            return R.fail(r.getMessage());
        }
        SetParam param = r.getData();

        if (param.getMlLamp() != null) {
            DevOperationInputParam inputParam = new DevOperationInputParam();
            inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
            inputParam.setDeviceSerialNumber(dto.getDeviceSerialNumber());

            SetParam closeParam = new SetParam();

            closeParam.setLampPos(Integer.valueOf(EnergyLampEnums.Mercury.getIdentity()));

            inputParam.setParam(closeParam);
            R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
            if (!operation.getSuccess()) {
                log.info("设置灯位置为汞灯失败：{}", operation.getMessage());
                infoService.updateDeviceCurrentStatus(dto.getDeviceSerialNumber(), Constants.DEVICE_STATUS_IDLE);

                return R.fail(operation.getMessage());
            }

        }

        //表格数据保存
        LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());
        wrapper.eq(CalibrationTempPO::getParam, DeviceCalibrationParamEnum.Table.getType());

        CalibrationTempPO tablePo = tempDao.selectOne(wrapper);
        LambdaQueryWrapper<DeviceCalibrationParamPO> paramWrapper = new LambdaQueryWrapper<>();
        paramWrapper.eq(DeviceCalibrationParamPO::getParam, DeviceCalibrationParamEnum.Table.getType());

        DeviceCalibrationParamPO paramPO = calibrationParamDAO.selectOne(paramWrapper);

        CalibrationTableVO tableVO = paramPO.getTableData();

        List<CalibrationTableData> allTableData = dto.getAllTableData();
        dto.getCalibrationTableData().forEach(data -> {
            data.setIdentifier(data.getName());
            data.setIsOriginData(true);
        });

        if (allTableData == null) {
            allTableData = dto.getCalibrationTableData();
        }

        allTableData.forEach(data -> {
            data.setIdentifier(data.getName());
            data.setIsOriginData(true);
        });


        tableVO.setContent(allTableData);
        dto.setAllTableData(allTableData);

        if (tablePo == null) {
            CalibrationTempPO po = new CalibrationTempPO();
            po.setDeviceSerialNumber(dto.getDeviceSerialNumber());
            po.setCreateBy(dto.getUpdateBy());
            po.setCalibrationType(dto.getCalibrationType());
            po.setParam(DeviceCalibrationParamEnum.Table.getType());
            po.setTableData(tableVO);

            tempDao.insert(po);
        } else {

            tablePo.setUpdateBy(dto.getUpdateBy());
            List<CalibrationTableData> content = tablePo.getTableData().getContent();
            if (content == null) {
                content = new ArrayList<>();
            }

            List<CalibrationTableData> ans = new ArrayList<>();
            Map<String, CalibrationTableData> map = new HashMap<>();
            for (CalibrationTableData a : content) {
                map.put(a.getNum(), a);
            }

            for (CalibrationTableData cont : allTableData) {
                cont.setIsOriginData(true);
                map.put(cont.getNum(), cont);
            }
            ans = new ArrayList<>(map.values());
            tableVO.setContent(ans);

            tablePo.setTableData(tableVO);
            tempDao.update(tablePo, wrapper);
        }

        baselineOrScan(dto, multiType, param);
        return R.ok();
    }

    @Override
    public R<Boolean> reFindPeak(String deviceSerialNumber, String updateBy) {

        LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
        //图数据保存
        wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, deviceSerialNumber)
                .eq(CalibrationTempPO::getCalibrationType, DeviceCalibrationInfoEnum.MultiWavelength.getType());
        wrapper.eq(CalibrationTempPO::getParam, DeviceCalibrationParamEnum.Graph.getType());

        CalibrationTempPO originGraphPO = tempDao.selectOne(wrapper);

        if (originGraphPO == null) {
            return R.fail("没有数据可以寻峰！");
        } else {
            originGraphPO.setUpdateBy(updateBy);

            CalibrationTableVO originTableData = originGraphPO.getTableData();
            Map<String, CalibrationGraphDataVO> graph = originTableData.getGraph();
            double height = Constants.CALIBRATION_PARAM_PEAK_VALLEY_DIFF / 100;

            for(String key : graph.keySet()) {

                CalibrationGraphDataVO vo = graph.get(key);
                Map<String, List<GraphData>> voData = vo.getData();
                for(String key1 : voData.keySet()) {
                    List<Double> xList = new ArrayList<>();
                    List<Double> yList = new ArrayList<>();
                    List<GraphData> list = voData.get(key1);
                    for (GraphData graData : list) {
                        xList.add(Double.valueOf(graData.getX()));
                        yList.add(Double.valueOf(graData.getY()));
                        graData.setFeng(null);
                    }
                    String s = restUtil.peakSearching(xList, yList, height, Constants.CALIBRATION_PARAM_PEAK_INTERVAL);

                    PeakReceiveInfo info = JSON.parseObject(s, PeakReceiveInfo.class);
                    if (info != null && ResultCodeEnum.SUCCESS.getCode().equals(info.getCode()) && info.getPeakX() != null && !info.getPeakX().isEmpty()) {
                        List<Double> peakX = info.getPeakX();

                        for (GraphData gd : list) {
                            Double x = Double.parseDouble(gd.getX());
                            if (peakX.contains(x)) {
                                gd.setFeng(DeviceCalibrationParamEnum.Peak.getType());
                            }
                        }
                    }
                    voData.put(key1, list);
                }
                vo.setData(voData);
                graph.put(key, vo);
            }
            originGraphPO.setTableData(originTableData);
            tempDao.update(originGraphPO, wrapper);

        }
        return R.ok();
    }

    private R<Boolean> baselineOrScan(DeviceCalibrationDTO dto, String multiType, SetParam setParam) {


        String deviceSerialNumber = dto.getDeviceSerialNumber();
        ActionCmdEnum action = ActionCmdEnum.BaseLine;
        String key;
        if (multiType.equals(DeviceCalibrationParamEnum.Scan.getType())) {
            action = ActionCmdEnum.Scan;
            key = T600ComIdEnums.Scan.getComId();
        } else {
            key = T600ComIdEnums.BaseLine.getComId();
        }

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        // 发送基线命令
        inputParam.setAction(action);

        R<OperationResultEntity> r = operationUtilService.operation(inputParam);
        if (!r.getSuccess()) {
            return R.fail(r.getMessage());
        }
        new Thread(()-> {
            UnknowCountMessageQueue messageQueue = UnknowCountMessageQueue.getInstance();
            List<SpectrumPoint> list = new ArrayList<>();
            while (Constants.isRunning) {
                Object result;

                result = messageQueue.pollMessage(deviceSerialNumber, key);

                // 如果队列为空（可选：根据你的 pollMessage 实现决定是否需要）
                if (result == null) {
                    // 可选：短暂休眠避免 CPU 空转
                    try {
                        Thread.sleep(10); // 比如 10ms
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }
                // 判断是否是结束标记
                if (result instanceof EndMarker) {
                    EndMarker endMarker = (EndMarker) result;
                    // 可选：记录结束原因或打日志
                    switch (endMarker.getReason()) {
                        case TIMEOUT:
                            log.warn("设备 {} 的测量超时", deviceSerialNumber);
                            break;
                        case UN_NORMAL:
                            log.error("设备 {} 的测量异常结束", deviceSerialNumber);
                            break;
                        case NORMAL:
                            log.info("设备 {} 的测量正常完成", deviceSerialNumber);
                            break;
                        case NORMAL_STOP:
                            log.info("设备 {} 的测量已停止", deviceSerialNumber);
                            break;
                    }
                    // 结束循环
                    break;
                }
                SpectrumPoint message = (SpectrumPoint) result;
                DeviceConnectionManager manager = DeviceConnectionManager.getInstance();
                DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
                manager.setDarkWavelength(info.getDeviceId(), String.valueOf(message.getWavelength()));
                list.add(message);
            }

            if (setParam.getMlLamp() != null) {
                DevOperationInputParam param = new DevOperationInputParam();
                param.setOperationType(OperationTypeEnums.SetParam.getType());
                param.setDeviceSerialNumber(dto.getDeviceSerialNumber());

                SetParam closeParam = new SetParam();

                closeParam.setMlLamp(0);
                closeParam.setLampPos(0);

                param.setParam(closeParam);
                R<OperationResultEntity> operation = operationUtilService.operation(param);
                if (!operation.getSuccess()) {
                    log.error("关闭汞灯失败{}", operation.getMessage());
                }
            }

            saveMultiValue(dto, list, multiType, setParam);

            infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);

        }).start();


        return R.ok();
    }

    private void saveMultiValue(DeviceCalibrationDTO dto, List<SpectrumPoint> data, String multiType, SetParam setParam)  {

        CalibrationTableVO vo = new CalibrationTableVO();

        Map<String, CalibrationGraphDataVO> graph = new HashMap<>();
        List<CalibrationTableHeadVO> headList = new ArrayList<>();

        LambdaQueryWrapper<DeviceCalibrationParamPO> paramWrapper = new LambdaQueryWrapper<>();
        paramWrapper.eq(DeviceCalibrationParamPO::getParam, DeviceCalibrationParamEnum.Table.getType());

        DeviceCalibrationParamPO param = calibrationParamDAO.selectOne(paramWrapper);

        CalibrationTableVO tableVO = param.getTableData();

        List<CalibrationTableHeadVO> h = tableVO.getHead();
        Map<String, String> nameMap = new HashMap<>();
        for (CalibrationTableHeadVO headVO : h) {
            if (headVO.getKey().equals(DeviceCalibrationParamEnum.Name.getType())) {
                for (DataNameValue value : headVO.getData()) {
                    nameMap.put(value.getDataValue(), value.getDataKey());
                }
            }
        }

        if (multiType.equals(DeviceCalibrationParamEnum.Baseline.getType())) {


            CalibrationGraphDataVO graphDataVO = new CalibrationGraphDataVO();
            List<ChangeParam> originalData = new ArrayList<>();

            for (SpectrumPoint point : data) {
                originalData.add(point.getBaselineRsd());
            }
            CalibrationTableHeadVO head = new CalibrationTableHeadVO();

            CalibrationTableData tableData = dto.getCalibrationTableData().get(0);
            String identifier = tableData.getIdentifier();
            String name = nameMap.get(identifier);

            head.setName(name);
            head.setKey(identifier);
            headList.add(head);

            String num = tableData.getNum();
            Map<String, List<ChangeParam>> baseLine = new HashMap<>();
            baseLine.put(num, originalData);

            graphDataVO.setBaseLine(baseLine);
            graph.put(identifier, graphDataVO);
            vo.setGraph(graph);
            vo.setContent(dto.getCalibrationTableData());
            vo.setHead(headList);

        } else if (multiType.equals(DeviceCalibrationParamEnum.Scan.getType())) {

            List<Double> xList = new ArrayList<>();
            List<Double> yList = new ArrayList<>();
            List<GraphData> graphData = new ArrayList<>();

            for (SpectrumPoint point : data) {

                GraphData scanGraph = new GraphData();
                scanGraph.setX(String.valueOf(point.getWavelength()));

                scanGraph.setChangeParam(point.getScanRsd());

                String y;

                String photoMode = setParam.getPhotoMode().toString();
                if (photoMode.equals(WaveLengthEnum.abs.getCode())) {
                    y = point.getAbsResult().getAbs();
                } else if (photoMode.equals(WaveLengthEnum.tPercent.getCode())) {
                    y = point.getTResult().getTPercent();
                } else if (photoMode.equals(WaveLengthEnum.er.getCode())) {
                    y = point.getErResult().getEr();
                } else if (photoMode.equals(WaveLengthEnum.es.getCode())) {
                    y = point.getEsResult().getEs();
                } else {
                    log.error("光度模式出错");
                    return;
                }
                scanGraph.setY(DataCountUtil.formatValueWithBigDecimal(Constants.DEFAULT_DECIMAL_NUM, y));
                xList.add(point.getWavelength());
                yList.add(Double.parseDouble(y));

                graphData.add(scanGraph);
            }


            double height = Constants.CALIBRATION_PARAM_PEAK_VALLEY_DIFF / 100;

            String s = restUtil.peakSearching(xList, yList, height, Constants.CALIBRATION_PARAM_PEAK_INTERVAL);

            PeakReceiveInfo info = JSON.parseObject(s, PeakReceiveInfo.class);

            List<CalibrationTableData> tableDataList = dto.getCalibrationTableData();
            if (info != null && ResultCodeEnum.SUCCESS.getCode().equals(info.getCode()) && info.getPeakX() != null && !info.getPeakX().isEmpty()) {
                List<Double> peakX = info.getPeakX();

                for (GraphData gd : graphData) {
                    Double x = Double.parseDouble(gd.getX());
                    if (peakX.contains(x)) {
                        gd.setFeng(DeviceCalibrationParamEnum.Peak.getType());
                    }
                }

                for (CalibrationTableData datum : tableDataList) {

                    double wavelength = Double.parseDouble(datum.getStandLength());

                    double closestPeak = peakX.get(0);
                    double minDiff = Math.abs(wavelength - closestPeak);

                    for (int i = 1; i < peakX.size(); i++) {
                        double currentDiff = Math.abs(wavelength - peakX.get(i));
                        if (currentDiff < minDiff) {
                            minDiff = currentDiff;
                            closestPeak = peakX.get(i);
                        }
                    }
                    datum.setRealLength(String.valueOf(closestPeak));
                }
            }


            Map<String, List<GraphData>> scanData = new HashMap<>();

            CalibrationGraphDataVO dataVO = new CalibrationGraphDataVO();

            CalibrationTableData tableData = tableDataList.get(0);
            String identifier = tableData.getIdentifier();
            String name = nameMap.get(identifier);
            String num = tableData.getNum();
            scanData.put(num, graphData);
            dataVO.setData(scanData);
            String luminosity = tableData.getLuminosity();
            dataVO.setYName(WaveLengthEnum.getName(luminosity));
            dataVO.setXName(DeviceCalibrationParamEnum.XName.getType());

            graph.put(identifier, dataVO);

            CalibrationTableHeadVO head = new CalibrationTableHeadVO();
            head.setName(name);
            head.setKey(identifier);
            headList.add(head);


            vo.setGraph(graph);
            vo.setContent(tableDataList);
            vo.setHead(headList);
        }
        List<CalibrationTableData> allTableData = dto.getAllTableData();
        if (allTableData == null) {
            allTableData = vo.getContent();
        }

        for (CalibrationTableData content : vo.getContent()) {
            content.setIsSendToDevice(false);
            allTableData.removeIf(existing -> existing.getNum().equals(content.getNum()) && existing.getIdentifier().equals(content.getIdentifier()));
            allTableData.add(content);
        }
        vo.setContent(allTableData);

        LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
        //图数据保存
        wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());
        wrapper.eq(CalibrationTempPO::getParam, DeviceCalibrationParamEnum.Graph.getType());

        CalibrationTempPO originGraphPO = tempDao.selectOne(wrapper);

        if (originGraphPO == null) {
            CalibrationTempPO po = new CalibrationTempPO();
            po.setDeviceSerialNumber(dto.getDeviceSerialNumber());
            po.setCreateBy(dto.getUpdateBy());
            po.setCalibrationType(dto.getCalibrationType());
            po.setParam(DeviceCalibrationParamEnum.Graph.getType());
            po.setTableData(vo);

            tempDao.insert(po);
        } else {
            originGraphPO.setUpdateBy(dto.getUpdateBy());

            CalibrationTableVO originTableData = originGraphPO.getTableData();
            List<CalibrationTableHeadVO> originHead = originTableData.getHead();
            CalibrationTableVO currentTableData = new CalibrationTableVO();
            BeanUtils.copyProperties(vo, currentTableData);

            for (CalibrationTableHeadVO newHead : currentTableData.getHead()) {
                originHead.removeIf(existing -> existing.getKey().equals(newHead.getKey()));
                originHead.add(newHead);
            }

            Map<String, CalibrationGraphDataVO> origin = originTableData.getGraph() == null ? new HashMap<>() : originTableData.getGraph();

            Map<String, CalibrationGraphDataVO> current = currentTableData.getGraph();

            // 遍历current中的所有key，查找origin中相同的key
            for (Map.Entry<String, CalibrationGraphDataVO> entry : current.entrySet()) {
                String key = entry.getKey();
                CalibrationGraphDataVO currentVO = entry.getValue();

                // 检查origin中是否存在相同的key
                if (!origin.isEmpty() && origin.containsKey(key)) {
                    CalibrationGraphDataVO originVO = origin.get(key);

                    // 处理data字段：key一致则替换，不一致则追加
                    Map<String, List<GraphData>> originData = originVO.getData();
                    Map<String, List<GraphData>> currentData = currentVO.getData();

                    if (originData != null && currentData != null) {
                        for (Map.Entry<String, List<GraphData>> dataEntry : currentData.entrySet()) {
                            String dataKey = dataEntry.getKey();
                            List<GraphData> newDataList = dataEntry.getValue();

                            // key一致则替换，不一致则添加
                            originData.put(dataKey, new ArrayList<>(newDataList));
                        }
                        originVO.setData(originData);
                    } else if (currentData != null) {
                        originVO.setData(new HashMap<>(currentData));
                    }

                    // 处理baseLine字段：key一致则替换，不一致则追加
                    Map<String, List<ChangeParam>> originBaseLine = originVO.getBaseLine();
                    Map<String, List<ChangeParam>> currentBaseLine = currentVO.getBaseLine();

                    if (originBaseLine != null && currentBaseLine != null) {
                        for (Map.Entry<String, List<ChangeParam>> baseLineEntry : currentBaseLine.entrySet()) {
                            String baseLineKey = baseLineEntry.getKey();
                            List<ChangeParam> newBaseLineList = baseLineEntry.getValue();

                            // key一致则替换，不一致则添加
                            originBaseLine.put(baseLineKey, new ArrayList<>(newBaseLineList));
                        }
                    } else if (currentBaseLine != null) {
                        originVO.setBaseLine(new HashMap<>(currentBaseLine));
                    }
                } else {
                    // 如果origin中没有这个key，直接添加
                    origin.put(key, currentVO);
                }
            }

            currentTableData.setGraph(origin);
            currentTableData.setHead(originHead);

            originGraphPO.setTableData(currentTableData);
            tempDao.update(originGraphPO, wrapper);
        }

        //表格数据保存
        wrapper.clear();
        wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());
        wrapper.eq(CalibrationTempPO::getParam, DeviceCalibrationParamEnum.Table.getType());

        CalibrationTempPO tablePo = tempDao.selectOne(wrapper);

        tableVO.setContent(vo.getContent());

        if (tablePo == null) {
            CalibrationTempPO po = new CalibrationTempPO();
            po.setDeviceSerialNumber(dto.getDeviceSerialNumber());
            po.setCreateBy(dto.getUpdateBy());
            po.setCalibrationType(dto.getCalibrationType());
            po.setParam(DeviceCalibrationParamEnum.Table.getType());
            po.setTableData(tableVO);

            tempDao.insert(po);
        } else {
            tablePo.setUpdateBy(dto.getUpdateBy());

            List<CalibrationTableData> originTable = tablePo.getTableData().getContent();

            Map<String, CalibrationTableData> map = new HashMap<>();
            for (CalibrationTableData a : originTable) {
                map.put(a.getNum(), a);
            }

            for (CalibrationTableData cont : allTableData) {
                map.put(cont.getNum(), cont);
            }

            vo.setContent(new ArrayList<>(map.values()));
            tableVO.setContent(new ArrayList<>(map.values()));

            tablePo.setTableData(tableVO);
            tempDao.update(tablePo, wrapper);
        }
    }

    //基线和扫描设置参数
    private R<SetParam> multiCheckAndSendParam(DeviceCalibrationDTO dto, String operateType){

        String identifier = null;
        double end = Double.MAX_VALUE;
        double start = Double.MIN_VALUE;

        String lamp = null;
        int gain = -1;
        String mode = null;
        //获取一组测试的样品,获取设置的起始波长和结束波长，以及是否需要基线

        for (CalibrationTableData data : dto.getCalibrationTableData()) {
            String num = data.getNum();

            if (!StringUtils.hasText(data.getName())) {
                return R.fail("第"+  num +"号请选择物质！");
            }
            if (!StringUtils.hasText(data.getLuminosity())) {
                return R.fail("第"+  num +"号请选择光度模式！");
            }
            if (data.getLuminosity().equals(WaveLengthEnum.er.getCode()) || data.getLuminosity().equals(WaveLengthEnum.es.getCode())) {
                if (!StringUtils.hasText(data.getGain())) {
                    return R.fail("第"+  num +"号请选择增益！");
                }

                if (!StringUtils.hasText(data.getLamp())) {
                    return R.fail("第"+  num +"号请选择灯类型！");
                }
            }
            if (!StringUtils.hasText(data.getStandLength())) {
                return R.fail("第"+  num +"号请选择标准波长！");
            }

            if (identifier == null) {
                identifier = data.getIdentifier();
            } else if (!identifier.equals(data.getIdentifier())) {
                return R.fail("请选择同一物质");
            }

            if (mode == null) {
                mode = data.getLuminosity();
            } else if (!mode.equals(data.getLuminosity())) {
                return R.fail("请选择同一光度模式");
            }
            if (mode.equals(WaveLengthEnum.er.getCode()) || mode.equals(WaveLengthEnum.es.getCode())) {
                int v = Integer.parseInt(data.getGain());
                if (gain == -1) {
                    gain = v;
                } else if (gain != v) {
                    return R.fail("请选择同一增益");
                }

                String object = data.getLamp();
                if (lamp == null) {
                    lamp = object;
                } else if (!lamp.equals(object)) {
                    return R.fail("请选择同一灯类型");
                }
            }

            String standWavelength = data.getStandLength();

            if (standWavelength != null && end > Double.parseDouble(standWavelength)) {
                end = Double.parseDouble(standWavelength);
            }
            if (standWavelength != null && start < Double.parseDouble(standWavelength)) {
                start = Double.parseDouble(standWavelength);
            }

        }

        if (mode == null) {
            log.error("光度模式缺失");
            return R.fail("光度模式缺失");
        }

        if (Objects.equals(operateType, DeviceCalibrationParamEnum.Baseline.getType()) && (mode.equals(WaveLengthEnum.es.getCode()) || mode.equals(WaveLengthEnum.er.getCode()))) {
            log.error("基线不能选择光度模式为ES、ER");
            return R.fail("基线不能选择光度模式为ES、ER");
        }


        start = start + 5;
        end = end - 5;

        R<DeviceMeasureVO> r = infoService.queryDeviceTypeInfo(dto.getDeviceSerialNumber());
        if (!r.getSuccess()) {
            log.error("未识别当前仪器");
            return R.fail("未识别当前仪器");
        }
        DeviceMeasureVO measureVO = r.getData();
        String maxWavelength = measureVO.getMaxWavelength();
        String minWavelength = measureVO.getMinWavelength();

        List<String> scanInterval = measureVO.getScanInterval();
        double minInterval = scanInterval.stream()
                .mapToDouble(Double::parseDouble)
                .min()
                .orElse(0.0);

        if (start < Double.parseDouble(minWavelength) || end > Double.parseDouble(maxWavelength)) {
            log.error("波段超出范围");
            return R.fail("波段超出范围");
        }

        if (start > Double.parseDouble(maxWavelength)) {
            start = Double.parseDouble(maxWavelength);
        }
        if (end < Double.parseDouble(minWavelength)) {
            end = Double.parseDouble(minWavelength);
        }

        DevOperationInputParam param = new DevOperationInputParam();
        param.setOperationType(OperationTypeEnums.SetParam.getType());
        param.setDeviceSerialNumber(dto.getDeviceSerialNumber());

        SetParam setParam = new SetParam();
        setParam.setPhotoMode(Integer.valueOf(mode));
        setParam.setSlit1(2.0);
        setParam.setScanWlInterval(minInterval);

        setParam.setCorrStatus(0);
        if (mode.equals(WaveLengthEnum.er.getCode()) || mode.equals(WaveLengthEnum.es.getCode())) {

            setParam.setGain(gain);

            if (lamp.equals(EnergyLampEnums.Mercury.getIdentity())) {
                setParam.setMlLamp(1);
            } else {
                setParam.setLampPos(Integer.valueOf(lamp));
            }
        }
        setParam.setStartWl(start);
        setParam.setEndWl(end);

        param.setParam(setParam);
        R<OperationResultEntity> operation = operationUtilService.operation(param);
        if (!operation.getSuccess()) {
            log.error("设置参数失败{}", operation.getMessage());
            return R.fail(operation.getMessage());
        }

        if (lamp !=  null && lamp.equals(EnergyLampEnums.Mercury.getIdentity())) {
            param.setOperationType(OperationTypeEnums.MercuryLocate.getType());
            operation = operationUtilService.operation(param);
            if (!operation.getSuccess()) {
                log.error(" MercuryLocate失败{}", operation.getMessage());
                return R.fail(operation.getMessage());
            }
        }

        return R.ok(setParam);

    }

    //基线和扫描设置参数
    private R<Boolean> checkAndSendParam(DeviceCalibrationDTO dto, String operateType){

        List<CalibrationTableData> tableData = dto.getCalibrationTableData();
        if (tableData == null || tableData.size() != 1) {
            log.error("基线和扫描只能选择一个物质");
            return R.fail("基线和扫描只能选择一个物质");
        }
        CalibrationTableData data = tableData.get(0);

        String mode = data.getLuminosity();

        if (mode == null) {
            log.error("光度模式缺失");
            return R.fail("光度模式缺失");
        }

        if (Objects.equals(operateType, DeviceCalibrationParamEnum.Baseline.getType()) && (mode.equals(WaveLengthEnum.es.getCode()) || mode.equals(WaveLengthEnum.er.getCode()))) {
            log.error("基线不能选择光度模式为ES、ER");
            return R.fail("基线不能选择光度模式为ES、ER");
        }

        double standWavelength = Double.parseDouble(data.getStandLength());


        double start = standWavelength + 5;
        double end = standWavelength - 5;

        R<DeviceMeasureVO> r = infoService.queryDeviceTypeInfo(dto.getOperateType());
        if (!r.getSuccess()) {
            log.error("未识别当前仪器");
            return R.fail("未识别当前仪器");
        }
        DeviceMeasureVO measureVO = r.getData();
        String maxWavelength = measureVO.getMaxWavelength();
        String minWavelength = measureVO.getMinWavelength();

        if (standWavelength < Double.parseDouble(minWavelength) || standWavelength > Double.parseDouble(maxWavelength)) {
            log.error("波段超出范围");
            return R.fail("波段超出范围");
        }

        if (start > Double.parseDouble(maxWavelength)) {
            start = Double.parseDouble(maxWavelength);
        }
        if (end < Double.parseDouble(minWavelength)) {
            end = Double.parseDouble(minWavelength);
        }


        DevOperationInputParam param = new DevOperationInputParam();
        param.setOperationType(OperationTypeEnums.SetParam.getType());
        param.setDeviceSerialNumber(dto.getDeviceSerialNumber());

        SetParam setParam = new SetParam();
        setParam.setPhotoMode(Integer.valueOf(mode));

        setParam.setStartWl(start);
        setParam.setEndWl(end);

        if (mode.equals(WaveLengthEnum.er.getCode()) || mode.equals(WaveLengthEnum.es.getCode())) {

            int gain = Integer.parseInt(data.getGain());

            String lamp = data.getLamp();

            setParam.setGain(gain);
            setParam.setLampPos(Integer.valueOf(lamp));
        }

        param.setParam(setParam);
        R<OperationResultEntity> operation = operationUtilService.operation(param);
        if (!operation.getSuccess()) {
            log.error("设置参数失败{}", operation.getMessage());
            return R.fail(operation.getMessage());
        }
        return R.ok();

    }

    @Override
    public R<List<WorkTimeInfo>> queryLampLifeInfo(String deviceSerialNumber, Boolean isUpdate) {

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null) {
            return R.fail("未找到该设备");
        }

        if (info.getWorkTimeInfo() == null) {
            info.setWorkTimeInfo(new ArrayList<>());
        }

        if (isUpdate) {
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
            throw new BusinessException("未找到连接仪器信息！");
        }

        // 先将初始化中的状态添加到缓存
        DeviceTaskDto initDto = new DeviceTaskDto();
        initDto.setDeviceSerialNumber(deviceSerialNumber);
        initDto.setStatus(OutFactoryCheckEnum.INIT_ING.getDesc());
        addToCache(initDto);

        // 异步执行初始化监控和任务创建
        asyncExecuteInitMonitor(deviceSerialNumber, templateCode, updateBy);

        // 立即返回成功响应
        return Result.ok(initDto);
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
            LambdaQueryWrapper<TaskManagement> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(TaskManagement::getOperationInstrument, deviceSerialNumber)
                            .eq(TaskManagement::getTaskType, FACTORY_INSPECTION_TASK.getCode());
            long count = taskManagementService.count(lambdaQueryWrapper);
            if (count > 0) {
                log.info("该设备已存在任务，请勿重复创建！");
                break;
            }


            //先根据仪器序列号找出这个仪器的型号
            List<String> list1 = deviceTypeInfoDAO.queryDeviceTypeBySerialNumber(deviceSerialNumber);
            LambdaQueryWrapper<ExcelTemplate> queryWrapper = new LambdaQueryWrapper<>();

            if (!CollectionUtils.isEmpty(list1)){
                //找到了仪器型号
                String deviceType = list1.get(0);


                //找出该型号的出厂检验模板
                queryWrapper.eq(ExcelTemplate::getStatus, TemplateTypeEnum.OUT_FACTORY_CHECK.getCode())
                        .like(ExcelTemplate::getModel, deviceType);
                List<ExcelTemplate> list2 = templateService.list(queryWrapper);
                
                // 如果精确匹配失败，尝试查找通用模板（model为空或包含"通用"的模板）
                if (CollectionUtils.isEmpty(list2)) {
                    log.warn("未找到仪器型号 [{}] 的专用出厂模板，尝试查找通用模板", deviceType);
                    LambdaQueryWrapper<ExcelTemplate> genericWrapper = new LambdaQueryWrapper<>();
                    genericWrapper.eq(ExcelTemplate::getStatus, TemplateTypeEnum.OUT_FACTORY_CHECK.getCode());
                    List<ExcelTemplate> genericTemplates = templateService.list(genericWrapper);
                    
                    // 查找通用模板（model为空、null或标记为"通用"）
                    if (!CollectionUtils.isEmpty(genericTemplates)) {
                        for (ExcelTemplate template : genericTemplates) {
                            if (template.getModel() == null || template.getModel().trim().isEmpty() || 
                                "通用".equals(template.getModel()) || "ALL".equalsIgnoreCase(template.getModel())) {
                                list2 = new ArrayList<>();
                                list2.add(template);
                                log.info("找到通用出厂模板，templateCode={}", template.getTemplateCode());
                                break;
                            }
                        }
                    }
                }
                
                if (!CollectionUtils.isEmpty(list2)){
                    templateCode=list2.get(0).getTemplateCode();
                    log.info("成功找到出厂模板，deviceType={}, templateCode={}", deviceType, templateCode);
                }else{
                    String errorMsg = String.format("未找到仪器型号 [%s] 的出厂检验模板！请确认：\n1. 模板管理中已创建状态为'出厂检验模板'的记录\n2. 模板的model字段包含该仪器型号或设置为通用模板", deviceType);
                    log.error(errorMsg);
                    throw new BusinessException(errorMsg);
                }
            }else {
                log.info("未找到该序列号对应的仪器型号!");
                throw new BusinessException("未找到该序列号对应的仪器型号!");
            }

            LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaskManagement::getOperationInstrument, deviceSerialNumber)
                    .eq(TaskManagement::getTaskType, FACTORY_INSPECTION_TASK.getCode());
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


    /**
     * 更新缓存中指定仪器的信息
     *
     * @param deviceSerialNumber 仪器序列号
     * @param updatedData        需要更新的数据项
     */
    private void updateInCache(String deviceSerialNumber, DeviceTaskDto updatedData) {
        try {
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

            boolean found = false;
            for (int i = 0; i < dtoList.size(); i++) {
                DeviceTaskDto item = dtoList.get(i);
                if (item != null && deviceSerialNumber.equals(item.getDeviceSerialNumber())) {
                    // 创建新的 DTO 对象，避免引用问题
                    DeviceTaskDto newItem = new DeviceTaskDto();
                    newItem.setDeviceSerialNumber(item.getDeviceSerialNumber());
                    newItem.setTaskId(updatedData.getTaskId() != null ? updatedData.getTaskId() : item.getTaskId());
                    newItem.setTemplateCode(updatedData.getTemplateCode() != null ? updatedData.getTemplateCode() : item.getTemplateCode());
                    newItem.setStatus(updatedData.getStatus());

                    dtoList.set(i, newItem);
                    found = true;
                    log.info("已更新缓存中设备信息：deviceSerialNumber={}, status={}, taskId={}",
                            deviceSerialNumber, updatedData.getStatus(), newItem.getTaskId());
                    break;
                }
            }

            if (!found) {
                log.info("缓存中未找到设备信息，添加新项：deviceSerialNumber={}", deviceSerialNumber);
                DeviceTaskDto newDto = new DeviceTaskDto();
                newDto.setDeviceSerialNumber(deviceSerialNumber);
                newDto.setTaskId(updatedData.getTaskId());
                newDto.setTemplateCode(updatedData.getTemplateCode());
                newDto.setStatus(updatedData.getStatus());
                dtoList.add(newDto);
            }

            ehcacheUtil.put(DEFAULT_CACHE.getCacheName(),
                    OUT_FACTORY_CHECK_LIST.getCode(), dtoList, String.class, Object.class);
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
            if (dto == null || dto.getDeviceSerialNumber() == null) {
                log.warn("添加到缓存的 DTO 为空或缺少设备序列号");
                return;
            }

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

            boolean exists = dtoList.stream()
                    .anyMatch(item -> item != null && dto.getDeviceSerialNumber().equals(item.getDeviceSerialNumber()));

            if (!exists) {
                dtoList.add(dto);
                ehcacheUtil.put(DEFAULT_CACHE.getCacheName(),
                        OUT_FACTORY_CHECK_LIST.getCode(), dtoList, String.class, Object.class);
                log.info("已添加设备到缓存：deviceSerialNumber={}", dto.getDeviceSerialNumber());
            } else {
                log.info("设备已存在于缓存中，跳过添加：deviceSerialNumber={}", dto.getDeviceSerialNumber());
            }
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
