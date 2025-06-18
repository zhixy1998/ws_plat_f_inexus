package com.persee.workstation.uv_inexus.controller.instrumentController;


import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.instrumentPo.InsConfigPO;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceMonitorService;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import com.persee.workstation.uv_inexus.vo.instrumentVo.FindDeviceVO;
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
        QueryDeviceConfigVO vo = deviceManageService.queryDeviceConfig(deviceSerialNumber);
        return Result.ok(vo);
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
        Boolean b = deviceManageService.deviceConfig(po);
        return Result.ok(b);
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
        Boolean b = deviceManageService.deviceCalibration(dto);
        return Result.ok(b);
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
