/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.softwareActivation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.softwareActivation.DeviceInfoDTO;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.softwareActivation.SoftwareActivationPO;
import com.persee.workstation.uv_inexus.service.guide.activationKeyService.DecryptService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.softwareActivation.DeviceDiscoveryService;
import com.persee.workstation.uv_inexus.service.guide.softwareActivation.SoftwareActivationService;
import com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/softwareActivation")
@Api("软件激活管理接口文档")
public class SoftwareActivationController {

    @Autowired
    private SoftwareActivationService softwareActivationService;

    @Autowired
    private DecryptService decryptService;
    
    @Autowired
    private DeviceDiscoveryService deviceDiscoveryService;

    @Autowired
    DeviceInfoService deviceInfoService;

    /**
     * 软件激活接口：通过ID查询设备，验证激活码并更新状态
     */
//    @ButtonPermission("softwareActivationsoftwareActivationDevice")
    @OperateLog(module = "激活仪器", description = "执行激活仪器", recordParams = true)
    @PostMapping("/activate")
    @ApiOperation("软件激活接口（通过ID查询设备，解密激活文件并更新状态）")
    public Result<String> activateSoftware(
            @RequestParam Integer id,  // 接收设备记录ID
            @RequestParam("activationFile") org.springframework.web.multipart.MultipartFile activationFile) {  // 接收激活文件
        try {
            System.out.println("111111111111111111111111"+id+activationFile.getOriginalFilename());
            
            if (activationFile.isEmpty()) {
                return Result.fail(400, YamlMessageSource.getMessageStatic("00701006","激活文件不能为空"));
            }
            
            // 1. 通过ID查询设备记录（优先使用ID查询，确保唯一性）
            SoftwareActivationPO activationPO = softwareActivationService.getById(id);
            if (activationPO == null) {
                //"未找到ID为【" + id + "】的设备记录"
                return Result.fail(404, YamlMessageSource.getMessageStatic("00701001",id));
            }
            System.out.println("查询到的设备记录:" + activationPO);

            // 2. 解密激活文件获取激活码
            String decryptedText = decryptService.decryptActivationFile(activationFile);
            System.out.println("解密后的激活码内容:" + decryptedText);

            // 3. 解析解密后的参数
            Map<String, String> params = parseDecryptedParams(decryptedText);
            String codeDeviceNumber = params.get("devN");
            String expirationDateStr = params.get("expT");
            String features = params.get("fea");
            System.out.println("激活码中的设备序列号:" + codeDeviceNumber);
            System.out.println("激活码中的过期时间:" + expirationDateStr);

            // 4. 校验解析结果
            if (codeDeviceNumber == null || expirationDateStr == null) {
                //激活码格式错误，缺少必要参数（deviceNumber或expirationDate）
                return Result.fail(400, YamlMessageSource.getMessageStatic("00701002"));
            }

            // 5. 验证激活码中的序列号与当前设备是否匹配
            String dbDeviceNumber = activationPO.getDeviceNumber();
            if (dbDeviceNumber == null) {
                //数据库记录中设备序列号为空
                return Result.fail(500, YamlMessageSource.getMessageStatic("00701003"));
            }
            if (!dbDeviceNumber.equals(codeDeviceNumber)) {
                // return Result.fail(403, "激活码与设备序列号不匹配（激活码:" + codeDeviceNumber + "，设备:" + dbDeviceNumber + "）");
                //激活码与设备序列号不匹配
                return Result.fail(403, YamlMessageSource.getMessageStatic("00701004"));
            }

            // 6. 验证日期格式
            LocalDate activationExpirationDate;
            try {
                activationExpirationDate = LocalDate.parse(expirationDateStr);
                System.out.println("解析后的过期时间:" + activationExpirationDate);
            } catch (DateTimeParseException e) {
                //"激活码中的日期格式错误（应为yyyy-MM-dd）:" + e.getMessage()
                return Result.fail(400, YamlMessageSource.getMessageStatic("00701005",e.getMessage()));
            }

            // 7. 验证过期时间是否有效
             if (activationExpirationDate.isBefore(LocalDate.now())) {
                 return Result.fail(201, YamlMessageSource.getMessageStatic("0070100D"));
             }

            // 8. 更新设备激活状态
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String fullTime = LocalDateTime.now().format(formatter);
            activationPO.setActivationStatus(1);
            activationPO.setActivationTime(fullTime);  // 记录激活时间
            activationPO.setExpirationDate(expirationDateStr);  // 更新过期时间
            activationPO.setFeatures(features);  // 付费功能
            softwareActivationService.updateById(activationPO);

            deviceInfoService.activeDevice(codeDeviceNumber);
            //激活成功
            return Result.ok(YamlMessageSource.getMessageStatic("0070100C"));

        } catch (RuntimeException e) {
            e.printStackTrace();
            //"激活失败：" + e.getMessage()
            return Result.fail(400, YamlMessageSource.getMessageStatic("00701006",e.getMessage()));
        } catch (Exception e) {
            //"激活过程异常：" + e.getMessage()
            return Result.fail(500, YamlMessageSource.getMessageStatic("00701007",e.getMessage()));
        }
    }

    /**
     * 解析解密后的字符串为键值对（如"deviceNumber=123&expirationDate=2025-09-09"）
     */
    private Map<String, String> parseDecryptedParams(String decryptedText) {
        Map<String, String> params = new HashMap<>();
        if (decryptedText == null || decryptedText.trim().isEmpty()) {
            return params;
        }
        String[] pairs = decryptedText.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);  // 按第一个"="分割
            if (keyValue.length == 2) {
                params.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return params;
    }

    /**
     * 分页查询（支持条件筛选）
     */
    @GetMapping("/")
    @ApiOperation("软件激活列表分页查询")
    public Result<PageView<SoftwareActivationPO>> findPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam Map<String, String> params) {
        
        // 过滤合法查询字段
        List<String> allowedFields = Arrays.asList("deviceNumber", "deviceIp", "type", "activationStatus");
        
        Map<String, String> filteredParams = params.entrySet().stream()
                .filter(entry -> allowedFields.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        Page<SoftwareActivationPO> page = new Page<>(pageNum, pageSize);
        IPage<SoftwareActivationPO> resultPage = softwareActivationService.findPage(page, filteredParams);
        resultPage.getRecords().forEach(po -> {
            // 判断features是否为null或空字符串（trim处理避免全空格的情况）
            if (po.getFeatures() == null || po.getFeatures().trim().isEmpty()) {
                po.setFeatures("/");
            }
        });
        return Result.ok(new PageView<>(resultPage));
    }

    /**
     * 根据ID查询单条记录
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询软件激活详情")
    public Result<SoftwareActivationPO> findById(@PathVariable Integer id) {
        SoftwareActivationPO activation = softwareActivationService.getById(id);
        //"未找到ID为" + id + "的激活记录"
        return activation != null ? Result.ok(activation) : Result.fail(404, YamlMessageSource.getMessageStatic("00701008",id));
    }

    /**
     * 新增激活记录
     */
    @PostMapping
    @ApiOperation("新增软件激活记录")
    public Result<Void> insert(@RequestBody SoftwareActivationPO activationPO) {
        softwareActivationService.save(activationPO);
        return Result.ok();
    }

    /**
     * 更新激活记录
     */
    @PutMapping
    @ApiOperation("更新软件激活记录")
    public Result<Void> update(@RequestBody SoftwareActivationPO activationPO) {
        if (activationPO.getId() == null) {
            //ID不能为空
            return Result.fail(400, YamlMessageSource.getMessageStatic("00701009"));
        }
        softwareActivationService.updateById(activationPO);
        return Result.ok();
    }

    /**
     * 根据ID删除
     */
    @DeleteMapping("/{id}")
//    @ButtonPermission("softwareActivationDeleteInstrument")
    @OperateLog(module = "删除仪器", description = "执行删除仪器操作", recordParams = true)
    @ApiOperation("根据ID删除软件激活记录")
    public Result<Void> deleteById(@PathVariable Integer id) {
        softwareActivationService.removeById(id);
        return Result.ok();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
//    @ButtonPermission(value = "revokeActivation",description = "批量删除")
    @ApiOperation("批量删除软件激活记录")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        if (ids.isEmpty()) {
            //删除ID列表不能为空
            return Result.fail(400, YamlMessageSource.getMessageStatic("0070100A"));
        }
        boolean isSuccess = softwareActivationService.removeByIds(ids);
        //批量删除失败
        return isSuccess ? Result.ok() : Result.fail(500, YamlMessageSource.getMessageStatic("0000004C"));
    }

    /**
     * 分页查询新增的设备数据（支持搜索）
     */
    @GetMapping("/addDevice")
    @ApiOperation("自动新增设备并分页查询（保留needAdd参数用于其他用途）")
    public Result<PageView<DeviceInfoDTO>> addAndGetDevices(
            @RequestParam(defaultValue = "false") boolean needAdd, // 保留参数用于其他逻辑
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam Map<String, String> searchParams) {

        // needAdd参数可用于其他业务逻辑（不影响设备自动新增）
        if (needAdd) {
            // log.info("needAdd为true，执行其他关联操作（不影响设备新增）");
            // 此处可添加needAdd相关的其他业务逻辑
        }

        // 执行分页查询（内部会自动处理设备新增/更新，与needAdd无关）
        Page<DeviceInfoDTO> page = new Page<>(pageNum, pageSize);
        IPage<DeviceInfoDTO> resultPage = deviceDiscoveryService.getAddedDevicesPage(page, searchParams);

        return Result.ok(new PageView<>(resultPage));
    }

    /**
     * 重置所有设备的isAddStatus为0
     * 前端关闭弹窗时调用
     */
    @PostMapping("/resetAllAddStatus")
    @ApiOperation("关闭弹窗时重置所有设备状态为历史状态")
    public Result<Boolean> resetAllDeviceAddStatus(@RequestBody Map<String, Object> params) {
        boolean result = deviceDiscoveryService.resetAllDeviceAddStatus(params);
        //设备状态重置失败
        return result ? Result.ok() : Result.fail(500,YamlMessageSource.getMessageStatic("0070100B"));
    }
    
}
