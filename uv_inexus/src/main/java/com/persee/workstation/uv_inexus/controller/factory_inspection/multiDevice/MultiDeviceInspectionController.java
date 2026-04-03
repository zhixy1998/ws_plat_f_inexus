package com.persee.workstation.uv_inexus.controller.factory_inspection.multiDevice;

import com.persee.workstation.uv_inexus.component.EhcacheUtil;
import com.persee.workstation.uv_inexus.config.LogPoint;
import com.persee.workstation.uv_inexus.dto.factory_inspection.BatchConnectDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.DeviceTaskDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateDto;
import com.persee.workstation.uv_inexus.exception.BusinessException;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;
import com.persee.workstation.uv_inexus.service.factory_inspection.TemplateService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import com.persee.workstation.uv_inexus.vo.instrumentVo.FindDeviceVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static com.persee.workstation.uv_inexus.enums.Cache.CacheEnums.DEFAULT_CACHE;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.OutFactoryCheckEnum.OUT_FACTORY_CHECK_LIST;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
/*多仪器连接控制，实现一键查找并连接初始化多台仪器*/
@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/factory-inspection/multi-device")
@Api(tags = "出厂检验 - 多仪器管理")
@Slf4j
public class MultiDeviceInspectionController {
    @Autowired
    DeviceManageService deviceManageService;
    @Autowired
    TemplateService templateService;
    @Autowired
    EhcacheUtil ehcacheUtil;

    /*
     * 查找设备
     * */
    @GetMapping("/searchDevice")
    @ApiOperation(value = "新驱动-搜索仪器接口", notes = "连接方式(connectType)：0-串口，1-网口，2-Wifi")
    @LogPoint(value = "搜索仪器", module = "设备管理", recordParams = true, recordResult = true)
    public Result<List<SearchInfo>> searchDevice(@RequestParam(required = false) Integer connectType) {
        R<List<SearchInfo>> r = deviceManageService.searchDevice(connectType);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        } else {
            return Result.ok(r.getData());
        }
    }

    @PostMapping("/batch-connect")
    @ApiOperation("批量连接多台仪器")
    public Result<List<FindDeviceVO>> batchConnectDevices(
            @RequestBody BatchConnectDto batchConnectDto) throws InterruptedException {

        List<SearchInfo> searchInfos = batchConnectDto.getSearchInfos();
        String updateBy = batchConnectDto.getUpdateBy();
        String templateCode = batchConnectDto.getTemplateCode();
        if (searchInfos == null || searchInfos.isEmpty()) {
            return Result.fail("设备列表不能为空");
        }

        // 统一设置用户信息
        for (SearchInfo info : searchInfos) {
            info.setUpdateBy(updateBy);
        }

        R result = deviceManageService.connect(searchInfos, updateBy, templateCode);

        if (result.getSuccess()) {
            List<FindDeviceVO> connectedDevices = new ArrayList<>();
            List<R<FindDeviceVO>> deviceResults = (List<R<FindDeviceVO>>) result.getData();

            if (deviceResults != null) {
                for (R<FindDeviceVO> deviceResult : deviceResults) {
                    if (deviceResult.getSuccess() && deviceResult.getData() != null) {
                        connectedDevices.add(deviceResult.getData());
                    }
                }
            }

            if (connectedDevices.isEmpty()) {
                return Result.fail("所有设备连接失败");
            }

            return Result.ok(connectedDevices);
        } else {
            return Result.fail(result.getMessage());
        }
    }


    /**
     * 初始化仪器并创建任务
     *
     * @param
     * @param updateBy
     * @return
     * @throws InterruptedException
     */
    @GetMapping("/init")
    @ApiOperation("初始化仪器并创建任务")
    public Result<DeviceTaskDto> initDevice(@RequestParam String deviceSerialNumber, @RequestParam String templateCode, @RequestParam String updateBy) throws InterruptedException, BusinessException {
        return deviceManageService.createTaskAfterInstrumentInit(deviceSerialNumber, updateBy, templateCode);
    }

    /**
     * 批量断开连接
     *
     * @param deviceSerialNumbers
     * @param updateBy
     * @return
     * @throws InterruptedException
     */
    @GetMapping("/disconnect")
    @ApiOperation("批量断开连接")
    public Result<Boolean> disconnectDevice(@RequestParam List<String> deviceSerialNumbers, @RequestParam String updateBy) throws InterruptedException {
        if (CollectionUtils.isNotEmpty(deviceSerialNumbers)) {
            boolean allSuccess = true;
            String failedDevice = null;

            for (String serialNumber : deviceSerialNumbers) {
                try {
                    R<Boolean> r = deviceManageService.disconnectDevice(serialNumber, updateBy);
                    if (!r.getSuccess()) {
                        allSuccess = false;
                        failedDevice = serialNumber;
                        log.error("断开设备失败：deviceSerialNumber={}, reason={}", serialNumber, r.getMessage());
                    } else {
                        // 断开成功后，从缓存中移除该仪器的信息
                        removeFromCache(serialNumber);
                    }
                } catch (Exception e) {
                    allSuccess = false;
                    failedDevice = serialNumber;
                    log.error("断开设备异常：deviceSerialNumber={}, error={}", serialNumber, e.getMessage(), e);
                }
            }

            if (allSuccess) {
                return Result.ok(true);
            } else {
                return Result.fail("部分设备断开失败：" + failedDevice);
            }
        }
        return Result.fail(false);
    }

    /**
     * 从缓存中移除指定仪器的信息
     * @param deviceSerialNumber 仪器序列号
     */
    private void removeFromCache(String deviceSerialNumber) {
        try {
            // 获取缓存中的列表
            Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(),
                    OUT_FACTORY_CHECK_LIST.getCode(), String.class, Object.class);

            if (cachedObject instanceof List) {
                @SuppressWarnings("unchecked")
                List<DeviceTaskDto> dtoList = (List<DeviceTaskDto>) cachedObject;

                if (CollectionUtils.isNotEmpty(dtoList)) {
                    // 找到并移除包含该设备序列号的项
                    for (int i = 0; i < dtoList.size(); i++) {
                        DeviceTaskDto item = dtoList.get(i);
                        if (item != null && deviceSerialNumber.equals(item.getDeviceSerialNumber())) {
                            dtoList.remove(i);
                            log.info("已从缓存中移除设备：deviceSerialNumber={}", deviceSerialNumber);
                            break;
                        }
                    }

                    // 更新缓存
                    ehcacheUtil.put(DEFAULT_CACHE.getCacheName(),
                            OUT_FACTORY_CHECK_LIST.getCode(), dtoList, String.class, Object.class);
                }
            }
        } catch (Exception e) {
            log.error("从缓存中移除设备信息失败：deviceSerialNumber={}", deviceSerialNumber, e);
        }
    }

    /**
     * 列出所有模板
     */
    @GetMapping("/listTemplate")
    @ApiOperation("列出所有模板")
    public Result<List<TemplateDto>> listTemplate() {
        List<ExcelTemplate> list = templateService.list();
        ArrayList<TemplateDto> dtos = new ArrayList<>();
        for (ExcelTemplate template : list) {
            TemplateDto dto = new TemplateDto();
            dto.setId(template.getId());
            dto.setTemplateCode(template.getTemplateCode());
            dto.setTemplateName(template.getTemplateName());
            dtos.add(dto);
        }
        return Result.ok(dtos);
    }

    /**
     * 列出所有已连接的仪器和任务
     */
    @GetMapping("/listConnectedDevice")
    @ApiOperation("列出所有已连接的仪器和任务")
    public Result<List<DeviceTaskDto>> listConnectedDevice() {
        Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(), OUT_FACTORY_CHECK_LIST.getCode(), String.class, Object.class);

        List<DeviceTaskDto> dtoList;
        if (cachedObject instanceof List) {
            @SuppressWarnings("unchecked")
            List<DeviceTaskDto> checkedList = (List<DeviceTaskDto>) cachedObject;
            dtoList = checkedList;
        } else {
            dtoList = new ArrayList<DeviceTaskDto>();
        }
        return Result.ok(dtoList);
    }

}
