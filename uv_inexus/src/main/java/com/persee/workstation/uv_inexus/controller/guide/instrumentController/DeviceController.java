package com.persee.workstation.uv_inexus.controller.guide.instrumentController;


import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceAutoInjectorPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InjectionTrayParamPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InsConfigPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceAutoInjectorService;
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
    private DeviceMonitorService monitorService;

    @Autowired
    private DeviceAutoInjectorService injectorService;

    @GetMapping("/findDevice")
    @ApiOperation("查找仪器的接口")
    public Result<List<FindDeviceVO>> findDevice() throws Exception {
        List<FindDeviceVO> device = deviceManageService.newFindDevice();
        if (device == null) {
            return Result.fail("查找设备异常");
        } else {
            return Result.ok(device);
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
    public Result<List<Double>> getBandWidth(){
        List<Double> bandWidth = deviceManageService.getBandWidth();
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
    public Result<List<DeviceCalibrationVO>> getCalibrationInfo(@RequestParam String deviceSerialNumber) {
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
