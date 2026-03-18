package com.persee.workstation.uv_inexus.controller.factory_inspection.multiDevice;

import com.persee.workstation.uv_inexus.component.EhcacheUtil;
import com.persee.workstation.uv_inexus.config.LogPoint;
import com.persee.workstation.uv_inexus.dto.factory_inspection.BatchConnectDto;
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
import java.util.HashMap;
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

    //批量连接并初始化设备
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

        R<FindDeviceVO> result = deviceManageService.connect(searchInfos, updateBy, templateCode);

        if (result.getSuccess()) {
            List<FindDeviceVO> connectedDevices = new ArrayList<>();
            if (result.getData() != null) {
                connectedDevices.add(result.getData());
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
    @PostMapping("/init")
    @ApiOperation("初始化仪器并创建任务")
    public Result<HashMap<String, String>> initDevice(@RequestParam String deviceSerialNumber, @RequestParam String templateCode, @RequestParam String updateBy) throws InterruptedException, BusinessException {
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
            for (String serialNumber : deviceSerialNumbers) {
                R<Boolean> r = deviceManageService.disconnectDevice(serialNumber, updateBy);
                if (r.getSuccess()) {
                    return Result.ok(true);
                } else {
                    return Result.fail("断开失败");
                }
            }
        }
        return Result.fail(false);
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
    public Result<List<HashMap<String, String>>> listConnectedDevice() {
        Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(), OUT_FACTORY_CHECK_LIST.getCode(), String.class, Object.class);

        List<HashMap<String, String>> dtoList;
        if (cachedObject instanceof List) {
            @SuppressWarnings("unchecked")
            List<HashMap<String, String>> checkedList = (List<HashMap<String, String>>) cachedObject;
            dtoList = checkedList;
        } else {
            dtoList = new ArrayList<HashMap<String, String>>();
        }
        return Result.ok(dtoList);
    }

}
