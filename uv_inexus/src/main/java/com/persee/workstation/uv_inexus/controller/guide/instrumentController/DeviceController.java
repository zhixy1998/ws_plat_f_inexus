/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.instrumentController;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.config.LogPoint;
import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceCalibrationParamEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceAutoInjectorPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InjectionTrayParamPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.WorkTimeInfo;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationInputParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationResultEntity;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/dev/ins")
@Api("仪器管理接口")
public class DeviceController {

    @Autowired
    private DeviceManageService deviceManageService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private DeviceMonitorService monitorService;

    @Autowired
    private DeviceAutoInjectorService injectorService;

    @Autowired
    private DeviceUsageLogService deviceUsageLogService;

    @Autowired
    SamplePoolConfigService samplePoolConfigService;

    @GetMapping("/stop")
    @ApiOperation("新驱动-停止扫描")
    public Result<Boolean> stop(@RequestParam String deviceSerialNumber) throws InterruptedException {
        Boolean r = deviceManageService.stop(deviceSerialNumber).getSuccess();
        if (r) {
            return Result.ok(true);
        } else {
            return Result.fail(false);
        }
    }

    @PostMapping("/zero")
    @ApiOperation("新驱动-停止扫描")
    public Result<Boolean> zero(@RequestBody DevOperationInputParam inputParam) throws InterruptedException {
        Boolean r = deviceManageService.zero(inputParam.getDeviceSerialNumber(), inputParam.getWavelength()).getSuccess();
        if (r) {
            return Result.ok(true);
        } else {
            return Result.fail(false);
        }
    }

    @PostMapping("/scan")
    @ApiOperation("扫描测试")
    @LogPoint(value = "扫描测试", module = "设备管理", recordParams = true, recordResult = true)
    public Result<OperationResultEntity> scan(
            @RequestBody DevOperationInputParam inputParam
    ) throws InterruptedException {
        R<OperationResultEntity> result =
                deviceManageService.Action(inputParam);

        if (!result.getSuccess()) {
            return Result.fail(result.getMessage());
        } else {
            return Result.ok(result.getData());
        }
    }

    @PostMapping("/TimeMeasure")
    @ApiOperation("时间扫描测试")
    @LogPoint(value = "扫描测试", module = "设备管理", recordParams = true, recordResult = true)
    public Result<OperationResultEntity> TimeMeasure(
            @RequestBody DevOperationInputParam inputParam
    ) throws InterruptedException {
        R<OperationResultEntity> result =
                deviceManageService.TimeMeasure(inputParam);

        if (!result.getSuccess()) {
            return Result.fail(result.getMessage());
        } else {
            return Result.ok(result.getData());
        }
    }


    @PostMapping("/setParam")
    @ApiOperation("设置仪器参数")
    @LogPoint(value = "设置仪器参数", module = "设备管理", recordParams = true, recordResult = true)
    public Result<OperationResultEntity> setParam(
            @RequestBody DevOperationInputParam inputParam
    ) throws InterruptedException {

        R<OperationResultEntity> result =
                deviceManageService.setParamOneDevice(inputParam);

        if (!result.getSuccess()) {
            return Result.fail(result.getMessage());
        } else {
            return Result.ok(result.getData());
        }
    }

    @PostMapping("/getValue")
    @ApiOperation("获取仪器参数")
    @LogPoint(value = "获取仪器参数", module = "设备管理", recordParams = true, recordResult = true)
    public Result<OperationResultEntity> getValue(
            @RequestBody DevOperationInputParam inputParam
    ) throws InterruptedException {

        R<OperationResultEntity> result =
                deviceManageService.getParamOneDevice(inputParam);

        if (!result.getSuccess()) {
            return Result.fail(result.getMessage());
        } else {
            return Result.ok(result.getData());
        }
    }


    @GetMapping("/findDevice")
    @ApiOperation("查找仪器的接口")
    @LogPoint(value = "查找仪器", module = "设备管理", recordParams = true, recordResult = true)
    public Result<List<FindDeviceVO>> findDevice() throws Exception {
        R<List<FindDeviceVO>> device = deviceManageService.findDevice();
        if (!device.getSuccess()) {
            return Result.fail(device.getMessage());
        } else {
            return Result.ok(device.getData());
        }

    }

    @GetMapping("/searchDevice")
    @ApiOperation(value = "新驱动-搜索仪器接口", notes = "连接方式(connectType)：0-串口，1-网口，2-Wifi")
    @LogPoint(value = "搜索仪器", module = "设备管理", recordParams = true, recordResult = true)
    @OperateLog(module = "仪器管理-搜索仪器", description = "执行仪器管理-搜索仪器操作", recordParams = true, ignore = true)
    public Result<List<SearchInfo>> searchDevice(@RequestParam(required = false) Integer connectType, @RequestParam String updateBy) {
        R<List<SearchInfo>> r = deviceManageService.searchDevice(connectType, updateBy);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        } else {
            return Result.ok(r.getData());
        }
    }

//    @GetMapping("/searchDevice")
//    @ApiOperation(value = "新驱动-搜索仪器接口", notes = "连接方式(connectType)：0-串口，1-网口，2-Wifi")
//    @LogPoint(value = "搜索仪器", module = "设备管理", recordParams = true,recordResult = true)
//    public Result<List<SearchInfo>> searchDevice() throws Exception {
//        R<List<SearchInfo>> r = deviceManageService.searchDevice();
//        if (!r.getSuccess()) {
//            return Result.fail(r.getMessage());
//        } else {
//            return Result.ok(r.getData());
//        }
//    }

    @PostMapping("/connect")
    @ApiOperation("新驱动-连接仪器")
    @LogPoint(value = "连接仪器", module = "设备管理", recordParams = true, recordResult = true)
    public Result<FindDeviceVO> connect(@RequestBody SearchInfo param) throws Exception {

        R<FindDeviceVO> r = deviceManageService.connect(param);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        } else {
            return Result.ok(r.getData());
        }
    }

    @GetMapping("/disconnectDevice")
    @ApiOperation("新驱动-断开连接")
    @OperateLog(module = "仪器管理-断开连接", description = "执行仪器管理-断开连接操作", recordParams = true, ignore = true)
    public Result<Boolean> disconnectDevice(@RequestParam String deviceSerialNumber, @RequestParam String updateBy) throws InterruptedException {
        R<Boolean> r = deviceManageService.disconnectDevice(deviceSerialNumber, updateBy);
        if (r.getSuccess()) {
            return Result.ok(true);
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/initOneDevice")
    @ApiOperation("新驱动-初始化")
    @OperateLog(module = "仪器管理-初始化", description = "执行仪器管理-初始化操作", recordParams = true, ignore = true)
    public Result<Boolean> initOneDevice(@RequestParam String deviceSerialNumber, @RequestParam String updateBy) throws InterruptedException {
        R<Boolean> r = deviceManageService.startInitDevice(deviceSerialNumber, updateBy);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/queryDeviceTypeInfo")
    @ApiOperation("查询设备类型信息")
    public Result<DeviceMeasureVO> queryDeviceTypeInfo(@RequestParam(required = false) String deviceSerialNumber) {
        R<DeviceMeasureVO> r = deviceManageService.queryDeviceTypeInfo(deviceSerialNumber);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        } else {
            return Result.ok(r.getData());
        }
    }

    @GetMapping("queryDeviceTypeInfoWithDefault")
    @ApiOperation("查询设备类型信息")
    public Result<DeviceMeasureVO> queryDeviceTypeInfoWithDefault(@RequestParam(required = false) String deviceSerialNumber) {
        return deviceManageService.queryDeviceTypeInfoWithDefault(deviceSerialNumber);
    }

    @GetMapping("/continueInit")
    @ApiOperation("新驱动-继续初始化")
    @OperateLog(module = "仪器管理-继续初始化", description = "执行仪器管理-继续初始化操作", recordParams = true, ignore = true)
    public Result<Boolean> continueInit(@RequestParam String deviceSerialNumber, @RequestParam String updateBy) throws InterruptedException {
        R<Boolean> r = deviceManageService.continueInitDevice(deviceSerialNumber, updateBy);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/getConnectDevice")
    @ApiOperation("新驱动-获取当前连接的仪器")
    public Result<String> getConnectDevice(@RequestParam String updateBy) {
        R<String> r = deviceManageService.getConnectDevice(updateBy);
        return Result.ok(r.getData());
    }

    @GetMapping("/query")
    @ApiOperation("仪器管理查询接口")
    public Result<DeviceInfoVO> query(@RequestParam String deviceSerialNumber, @RequestParam String updateBy) {
        R<DeviceInfoVO> infoVO = deviceManageService.queryDeviceInfo(deviceSerialNumber, updateBy);
        if (infoVO.getSuccess()) {
            return Result.ok(infoVO.getData());
        } else {
            return Result.fail(infoVO.getMessage());
        }
    }

    @PostMapping("/init")
    @ApiOperation("仪器初始化接口")
    public Result<Boolean> deviceInit(@RequestBody DeviceInitDTO initDTO) {
        R<Boolean> init = deviceManageService.init(initDTO);
        if (init.getSuccess()) {
            return Result.ok(init.getData());
        } else {
            return Result.fail(init.getMessage());
        }
    }

    @GetMapping("/stopInit")
    @ApiOperation("停止初始化")
    @OperateLog(module = "仪器管理-仪器配置-停止初始化", description = "执行仪器管理-仪器配置-停止初始化记录", recordParams = true, recordResult = true, compare = true)
    public Result<Boolean> stopInit(@RequestParam String deviceSerialNumber, @RequestParam String updateBy) {
        R<Boolean> r = deviceManageService.newStopInit(deviceSerialNumber, updateBy);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/changeDeviceName")
    @OperateLog(module = "仪器管理-仪器配置-仪器命名", description = "执行仪器管理-仪器配置-仪器命名记录", recordParams = true, recordResult = true, compare = true)
    @ApiOperation("更改仪器名称")
    public Result<Boolean> updateDeviceName(@RequestParam String deviceSerialNumber,
                                            @RequestParam String updateBy,
                                            @RequestParam String deviceName) {
        Boolean r = deviceInfoService.changeDeviceName(deviceSerialNumber, deviceName, updateBy);
        if (r) {
            return Result.ok(true);
        } else {
            return Result.fail("00208020");
        }
    }

    @GetMapping("/changeLamp")
    @ApiOperation("切换灯接口,currentLampType: 当前灯类型， 0-钨灯，1-氘灯")
    public Result<Boolean> changeLamp(@RequestParam String currentLampType, @RequestParam String deviceSerialNumber) {
        R<Boolean> r = deviceManageService.changeLamp(currentLampType, deviceSerialNumber);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/closeOrOpenLamp")
    @ApiOperation("仪器开关灯接口,lampType 灯类型 0-钨灯，1-氘灯;  status 状态：1-开，0-关")
    public Result<Boolean> closeOrOpenLamp(@RequestParam String lampType,
                                           @RequestParam Integer status,
                                           @RequestParam String deviceSerialNumber) {
        R<Boolean> r = deviceManageService.closeOrOpenLamp(lampType, status, deviceSerialNumber);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }


    @PostMapping("/start")
    @ApiOperation("仪器开机接口")
    public Result<Boolean> deviceStart(@RequestBody DeviceStartDTO startDTO) {
        deviceManageService.startDevice(startDTO);
        return null;
    }

    @PostMapping("/softClose")
    @ApiOperation("仪器软关机接口")
    public Result<Boolean> softClose(@RequestBody DeviceSoftCloseDTO softCloseDTO) {
        deviceManageService.softClose(softCloseDTO);
        return null;
    }

    @GetMapping("/devMonitor")
    @ApiOperation("查询仪器监控信息")
    public Result<List<DeviceMonitorVO>> deviceMonitor(@RequestBody DeviceQueryDTO queryDTO) {
        monitorService.queryStatus(queryDTO);
        return null;
    }

    @GetMapping("/componentMonitor")
    @ApiOperation("查询器件监控信息")
    public Result<DeviceComponentMonitorVO> componentMonitor(@RequestBody DeviceQueryDTO dto) {
        monitorService.componentQuery(dto);
        return null;
    }

    @GetMapping("/getDevConfig")
    @ApiOperation("新--查询仪器配置")
    public Result<DeviceConfigVO> getDevConfig() {
        R<DeviceConfigVO> r = deviceManageService.newQueryDeviceConfig();
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

//    @ButtonPermission("instrumentConfigSave")
    @OperateLog(module = "仪器管理-仪器配置-保存修改的仪器配置", description = "执行仪器管理-仪器配置-保存修改的仪器配置记录", recordParams = true)
    @PostMapping("/devConfig")
    @ApiOperation("新--仪器配置接口")
    public Result<Boolean> devConfig(@RequestBody DevConfigDTO dto) {
        R<Boolean> r = deviceManageService.newDevConfig(dto);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/getWorkTime")
    @ApiOperation("新--查询仪器以及各灯工作时长")
    public Result<List<WorkTimeInfo>> getWorkTime(@RequestParam String deviceSerialNumber, @RequestParam Boolean isUpdate) {
        R<List<WorkTimeInfo>> r = deviceManageService.queryLampLifeInfo(deviceSerialNumber, isUpdate);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/hasWorkTime")
    @ApiOperation("新--仪器是否具有查询灯时长的功能")
    public Result<Boolean> hasWorkTime(@RequestParam String deviceSerialNumber) {
        R<Boolean> r = deviceManageService.hasWorkTime(deviceSerialNumber);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }



    @GetMapping("/setMessagePort")
    @ApiOperation("设置仪器通讯端口")
    public Result<Boolean> setMessagePort(@RequestParam Integer messagePort, @RequestParam String updateBy) {
        R<Boolean> r = deviceManageService.setDevMessagePort(messagePort, updateBy);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }


    @GetMapping("/getBandWidth")
    @ApiOperation("查询光谱带宽")
    public Result<List<Double>> getBandWidth(@RequestParam String deviceSerialNumber) {
        List<Double> bandWidth = deviceManageService.getBandWidth(deviceSerialNumber);
        return Result.ok(bandWidth);
    }


    @ApiOperation("设置进样器参数配置")
    @PostMapping("/setInjectorConf")
    @OperateLog(module = "仪器管理-{source}", description = "执行仪器管理-{source}操作", recordParams = true)
    public Result<Boolean> setInjectorConf(@RequestBody DeviceAutoInjectorPO injectorPO) {
        if (injectorPO.getSource() == null || injectorPO.getSource().trim().isEmpty()) {
            injectorPO.setSource("自动进样器配置保存");
        }
        R<Boolean> r = injectorService.setInjectorConf(injectorPO);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("设置进样器托盘参数配置")
    @PostMapping("/setInjectorTray")
    @OperateLog(module = "仪器管理-进样盘参数配置保存", description = "执行仪器管理-进样盘参数配置保存", recordParams = true)
    public Result<Boolean> setInjectorTray(@RequestBody List<InjectionTrayParamPO> poList) {
        R<Boolean> r = injectorService.setInjectorTray(poList);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/getInjectorConf")
    @ApiOperation("查询进样器参数配置")
    public Result<DeviceAutoInjectorPO> getInjectorConf(@RequestParam String deviceSerialNumber) {
        R<DeviceAutoInjectorPO> r = injectorService.queryInjectorConf(deviceSerialNumber);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/checkConnect")
    @ApiOperation("联机")
    public Result<Integer> checkConnect(@RequestParam String deviceSerialNumber) {
        R<Integer> r = injectorService.checkConnectStatus(deviceSerialNumber);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }


    @GetMapping("/getCalibrationInfo")
    @ApiOperation("查询仪器校准信息")
    public Result<List<DeviceCalibrationVO>> getCalibrationInfo(@RequestParam(required = false) String deviceSerialNumber) {
        List<DeviceCalibrationVO> list = deviceManageService.newQueryCalibrationInfo(deviceSerialNumber);
        return Result.ok(list);

    }

    @GetMapping("/queryMultiCalibration")
    @ApiOperation("查询仪器校准信息")
    public Result<List<DeviceCalibrationVO>> queryMultiCalibration(@RequestParam(required = false) String deviceSerialNumber) {
        List<DeviceCalibrationVO> list = deviceManageService.queryMultiCalibration(deviceSerialNumber);
        return Result.ok(list);

    }



    //    @PostMapping("/calibration")
//    @ApiOperation("仪器校准")
//    public Result<Boolean> deviceCalibration(@RequestBody DeviceCalibrationDTO dto){
//        R<Boolean> r = deviceManageService.deviceCalibration(dto);
//        if (r.getSuccess()) {
//            return Result.ok(r.getData());
//        } else {
//            return Result.fail(r.getMessage());
//        }
//    }
//    @ButtonPermission("instrumentCalibrationWavelengthCorrection")
//    @OperateLog(module = "仪器管理-仪器校正-波长校正", description = "执行仪器管理-仪器校正-波长校正记录", recordParams = true)
//    @ButtonPermission(
//            description = "实验测试-经典模式-删除历史文件数据", // 操作描述
//            permissionMapping = "instrumentCalibrationWavelengthCorrection=instrumentCalibrationWavelengthCorrection,instrumentCalibrationDarkCurrentCorrection=instrumentCalibrationDarkCurrentCorrection" // 自定义操作类型→驼峰权限标识
//    )
    @OperateLog(
            module = "仪器管理-仪器校正-{source}",
            description = "执行仪器管理-仪器校正-{source}操作",
            recordParams = true
    )
    @PostMapping("/calibration")
    @ApiOperation("仪器校准")
    public Result<Boolean> deviceCalibration(@RequestBody DeviceCalibrationDTO dto) {
        R<Boolean> r = deviceManageService.deviceNewCalibration(dto);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @PostMapping("/deleteMultiCalibrationInfo")
    @ApiOperation("多波长校正-批量删除")
    public Result<Boolean> deleteMultiCalibrationInfo(@RequestBody DeviceCalibrationDTO dto) {
        R<Boolean> r = deviceManageService.deleteMultiCalibrationInfo(dto);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @PostMapping("/saveMultiCalibrationInfo")
    @ApiOperation("多波长校正-保存用户填入的信息")
    public Result<Boolean> saveMultiCalibrationInfo(@RequestBody DeviceCalibrationDTO dto) {
        R<Boolean> r = deviceManageService.saveAllData(dto);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }


    @GetMapping("/reFindPeak")
    @ApiOperation("多波长校正-重新寻峰")
    public Result<Boolean> reFindPeak(@RequestParam String deviceSerialNumber, @RequestParam String updateBy) {
        R<Boolean> r = deviceManageService.reFindPeak(deviceSerialNumber, updateBy);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }




    @OperateLog(
            module = "实验测试-仪器管理",
            description = "执行仪器管理-仪器校正-{operateType}",
            recordParams = true
    )
    @PostMapping("/calibrationBaseline")
    @ApiOperation("多波长校正-基线")
    public Result<Boolean> calibrationBaseline(@RequestBody DeviceCalibrationDTO dto) {
        R<Boolean> r = deviceManageService.multiCalibration(dto, DeviceCalibrationParamEnum.Baseline.getType());
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @OperateLog(
            module = "实验测试-仪器管理",
            description = "执行仪器管理-仪器校正-{operateType}",
            recordParams = true
    )
    @PostMapping("/calibrationScan")
    @ApiOperation("多波长校正-扫描")
    public Result<Boolean> calibrationScan(@RequestBody DeviceCalibrationDTO dto) {
        R<Boolean> r = deviceManageService.multiCalibration(dto, DeviceCalibrationParamEnum.Scan.getType());
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    //sendMultiCalibrationToDevice
    @PostMapping("/sendMultiCalibrationToDevice")
    @ApiOperation("多波长校正-发送给仪器")
    public Result<Boolean> sendMultiCalibrationToDevice(@RequestBody DeviceCalibrationDTO dto) {
        R<Boolean> r = deviceManageService.sendMultiCalibrationToDevice(dto);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    //样品池校正
    @PostMapping("/samplePoolCalibration")
    @ApiOperation("样品池校正")
    public Result<Boolean> samplePoolCalibration(@RequestBody SamplePoolCalibrationDTO dto) {
        R<Boolean> r = deviceManageService.samplePoolCalibration(dto);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    //检查是否有汞灯
    @GetMapping("/checkMercuryLamp")
    @ApiOperation("检查是否有汞灯")
    public Result<Boolean> checkMercuryLamp(@RequestParam(required = false) String deviceSerialNumber) {
        R<Boolean> r = deviceManageService.checkMercuryLamp(deviceSerialNumber);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    //汞灯开关
    @GetMapping("/setMercuryLampStatus")
    @ApiOperation("汞灯开关，1-开汞灯，2-关汞灯")
    public Result<Boolean> setMercuryLampStatus(@RequestParam String deviceSerialNumber, @RequestParam Integer status) {
        R<Boolean> r = deviceManageService.switchHgLamp(deviceSerialNumber, status);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/clearAllPoolCorrectionData")
    @ApiOperation("清除设备所有样品池校正数据")
    public Result<Boolean> clearAllPoolCorrectionData(@RequestParam String deviceSerialNumber) {
        Boolean result = samplePoolConfigService.clearAllPoolCorrectionData(deviceSerialNumber);
        if (result) {
            return Result.ok(true);
        } else {
            return Result.fail("0025000C");
        }
    }


    @GetMapping("/usageStatistics")
    @ApiOperation("使用情况统计")
    public Result<List<DeviceUsageStatisticsVO>> usageStatistics(@RequestBody DeviceUsageCountDTO dto) {

        monitorService.countUsageInfo(dto);
        return null;

    }

    @GetMapping("/deviceUsageStatistics")
    @ApiOperation("仪器使用情况统计")
    public Result<List<DeviceUsageLogVO> > deviceUsageStatistics(@RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime){
        List<DeviceUsageLogVO> list = deviceUsageLogService.deviceUsageLogStatistics(startTime, endTime);
        return Result.ok(list);
    }

    @GetMapping("/userUsageStatistics")
    @ApiOperation("用户使用情况统计")
    public Result<JSONObject> userUsageStatistics(@RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime){
        JSONObject list = deviceUsageLogService.userUsageLogStatistics(startTime, endTime);
        return Result.ok(list);
    }

    @PostMapping("/addDevice")
    @ApiOperation("添加仪器接口")
    public Result<Boolean> addDevice(@RequestBody SearchInfo info) {
        R<Boolean> r = deviceManageService.addDevice(info);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @PostMapping("/test")
    public void test() {
        deviceManageService.test();
    }

    @PostMapping("/queryHistoryTask")
    @ApiOperation("查询历史任务")
    public Result<Page<HistoryTaskVO>> queryHistoryTask(@RequestBody DeviceHistoryTaskDTO queryDto) {
        try {
            Page<HistoryTaskVO> page = monitorService.getHistoryTask(queryDto);
            return Result.ok(page);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/getTypeStatistics")
    @ApiOperation("检测类型分项统计")
    public Result<Map<String, Object>> getTypeStatistics(
            @RequestParam String updateBy,
            @RequestParam String deviceSerialNumber,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            return monitorService.getTypeStatistics(updateBy, startTime, endTime, deviceSerialNumber);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/getProjectStatistics")
    @ApiOperation("检测项目分项统计")
    public Result<Map<String, Object>> getProjectStatistics(
            @RequestParam String updateBy,
            @RequestParam String deviceSerialNumber,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            return monitorService.getProjectStatistics(updateBy, startTime, endTime, deviceSerialNumber);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }


}
