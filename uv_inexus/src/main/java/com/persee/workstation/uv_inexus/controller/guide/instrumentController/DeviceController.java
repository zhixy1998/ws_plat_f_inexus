/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.controller.guide.instrumentController;


import com.persee.workstation.uv_inexus.config.LogPoint;
import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceAutoInjectorPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InjectionTrayParamPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InsConfigPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceAutoInjectorService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceMonitorService;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/findDevice")
    @ApiOperation("查找仪器的接口")
    @LogPoint(value = "查找仪器", module = "设备管理", recordParams = true,recordResult = true)
    public Result<List<FindDeviceVO>> findDevice() throws Exception {
        R<List<FindDeviceVO>> device = deviceManageService.findDevice();
        if (!device.getSuccess()) {
            return Result.fail(device.getMessage());
        } else {
            return Result.ok(device.getData());
        }

    }

    @GetMapping("/disConnect")
    @ApiOperation("断开连接")
    @LogPoint(value = "断开连接", module = "设备管理", recordParams = true,recordResult = true)
    public Result<Boolean> disConnect(@RequestParam String deviceSerialNumber, @RequestParam String updateBy) {
        Boolean r = deviceManageService.disConnect(deviceSerialNumber, updateBy);
        if (r) {
            return Result.ok(true);
        } else {
            return Result.fail(false);
        }
    }

    @GetMapping("/query")
    @ApiOperation("仪器管理查询接口")
    public Result<DeviceInfoVO> query(@RequestParam String deviceSerialNumber, @RequestParam String updateBy){
        R<DeviceInfoVO> infoVO = deviceManageService.queryDeviceInfo(deviceSerialNumber, updateBy);
        if (infoVO.getSuccess()) {
            return Result.ok(infoVO.getData());
        } else {
            return Result.fail(infoVO.getMessage());
        }
    }

    @PostMapping("/init")
    @ApiOperation("仪器初始化接口")
    public Result<Boolean> deviceInit(@RequestBody DeviceInitDTO initDTO){
        R<Boolean> init = deviceManageService.init(initDTO);
        if (init.getSuccess()) {
            return Result.ok(init.getData());
        }else {
            return Result.fail(init.getMessage());
        }
    }

    @GetMapping("/stopInit")
    @ApiOperation("停止初始化")
    public Result<Boolean> stopInit(@RequestParam String deviceSerialNumber, @RequestParam String updateBy) {
        R<Boolean> r = deviceManageService.stopInit(deviceSerialNumber, updateBy);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/changeDeviceName")
    @ApiOperation("更改仪器名称")
    public Result<Boolean> updateDeviceName(@RequestParam String deviceSerialNumber,
                                            @RequestParam String updateBy,
                                            @RequestParam String deviceName){
        Boolean r = deviceInfoService.changeDeviceName(deviceSerialNumber, deviceName, updateBy);
        if (r) {
            return Result.ok(true);
        } else {
            return Result.fail("更新失败");
        }
    }


    @PostMapping("/start")
    @ApiOperation("仪器开机接口")
    public Result<Boolean> deviceStart(@RequestBody DeviceStartDTO startDTO){
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
    public Result<List<DeviceMonitorVO>> deviceMonitor(@RequestBody DeviceQueryDTO queryDTO){
        monitorService.queryStatus(queryDTO);
        return null;
    }

    @GetMapping("/componentMonitor")
    @ApiOperation("查询器件监控信息")
    public Result<DeviceComponentMonitorVO> componentMonitor(@RequestBody DeviceQueryDTO dto){
        monitorService.componentQuery(dto);
        return null;
    }

    @GetMapping("/getDeviceConfig")
    @ApiOperation("查询仪器配置")
    public Result<QueryDeviceConfigVO> getDeviceConfig(@RequestParam String deviceSerialNumber) {
        R<QueryDeviceConfigVO> r = deviceManageService.queryDeviceConfig(deviceSerialNumber);
        if (r.getSuccess()){
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/getBandWidth")
    @ApiOperation("查询光谱带宽")
    public Result<List<Double>> getBandWidth(@RequestParam String deviceSerialNumber){
        List<Double> bandWidth = deviceManageService.getBandWidth(deviceSerialNumber);
        return Result.ok(bandWidth);
    }

    @PostMapping("/deviceConfig")
    @ApiOperation("仪器配置接口")
    public Result<Boolean> deviceConfig(@RequestBody InsConfigPO po ){
        R<Boolean> r = deviceManageService.deviceConfig(po);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }

    }

    @ApiOperation("设置进样器参数配置")
    @PostMapping("/setInjectorConf")
    public Result<Boolean> setInjectorConf(@RequestBody DeviceAutoInjectorPO injectorPO){
        R<Boolean> r = injectorService.setInjectorConf(injectorPO);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("设置进样器托盘参数配置")
    @PostMapping("/setInjectorTray")
    public Result<Boolean> setInjectorTray(@RequestBody List<InjectionTrayParamPO> poList){
        R<Boolean> r = injectorService.setInjectorTray(poList);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/getInjectorConf")
    @ApiOperation("查询进样器参数配置")
    public Result<DeviceAutoInjectorPO> getInjectorConf(@RequestParam String deviceSerialNumber){
        R<DeviceAutoInjectorPO> r = injectorService.queryInjectorConf(deviceSerialNumber);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/checkConnect")
    @ApiOperation("联机")
    public Result<Integer> checkConnect(@RequestParam String deviceSerialNumber){
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
        List<DeviceCalibrationVO> list = deviceManageService.queryCalibrationInfo(deviceSerialNumber);
        return Result.ok(list);

    }

    @PostMapping("/calibration")
    @ApiOperation("仪器校准")
    public Result<Boolean> deviceCalibration(@RequestBody DeviceCalibrationDTO dto){
        R<Boolean> r = deviceManageService.deviceCalibration(dto);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("/usageStatistics")
    @ApiOperation("使用情况统计")
    public Result<List<DeviceUsageStatisticsVO>> usageStatistics(@RequestBody DeviceUsageCountDTO dto){

        monitorService.countUsageInfo(dto);
        return null;

    }

    @PostMapping("/test")
    public void test(){
        deviceManageService.test();
    }




}
