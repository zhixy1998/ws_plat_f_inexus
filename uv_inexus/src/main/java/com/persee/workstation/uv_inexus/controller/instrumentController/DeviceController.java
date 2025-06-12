package com.persee.workstation.uv_inexus.controller.instrumentController;


import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceMonitorService;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import com.persee.workstation.uv_inexus.vo.systemConfigVo.FindDeviceVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/dev/ins")
@ApiModel("仪器管理接口")
public class DeviceController {

    @Autowired
    private DeviceManageService deviceManageService;

    @Autowired
    private DeviceMonitorService monitorService;

    @GetMapping("/findDevice")
    @ApiOperation("查找仪器的接口")
    public Result<List<FindDeviceVO>> findDevice(){
        List<FindDeviceVO> device = deviceManageService.findDevice();
        return Result.ok(device);
    }

    @GetMapping("/query")
    @ApiOperation("仪器管理查询接口")
    public Result<DeviceInfoVO> query(@RequestParam String deviceSerialNumber, @RequestParam String updateBy){
        DeviceInfoVO infoVO = deviceManageService.queryDeviceInfo(deviceSerialNumber, updateBy);

        return Result.ok(infoVO);
    }

    @PostMapping("/init")
    @ApiOperation("仪器初始化接口")
    public Result<Boolean> deviceInit(@RequestBody DeviceInitDTO initDTO){
        Boolean init = deviceManageService.init(initDTO);
        return Result.ok(init);
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

    @PostMapping("/deviceConfig")
    @ApiOperation("仪器配置接口")
    public Result<Boolean> deviceConfig(@RequestBody DeviceLocalConfigDTO configDTO){
        Boolean b = deviceManageService.deviceConfig(configDTO);
        return Result.ok(b);
    }

    @GetMapping("/usageStatistics")
    @ApiOperation("使用情况统计")
    public Result<List<DeviceUsageStatisticsVO>> usageStatistics(@RequestBody DeviceUsageCountDTO dto){

        monitorService.countUsageInfo(dto);
        return null;

    }




}
