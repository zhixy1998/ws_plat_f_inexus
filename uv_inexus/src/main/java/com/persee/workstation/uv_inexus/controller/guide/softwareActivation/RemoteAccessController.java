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
package com.persee.workstation.uv_inexus.controller.guide.softwareActivation;

import com.persee.workstation.uv_inexus.dto.softwareActivation.EncryptedConfigDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.guide.softwareActivation.RemoteAccessService;
import com.persee.workstation.uv_inexus.service.guide.softwareActivation.impl.LicenseServiceImpl;
import com.persee.workstation.uv_inexus.util.HardwareUtil;
import com.persee.workstation.uv_inexus.util.LicenseValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/remoteAccess")
@Api("远程访问")
public class RemoteAccessController {
    @Autowired
    private RemoteAccessService remoteAccessService;
    @Autowired
    private LicenseServiceImpl licenseService;
    @GetMapping("/hardware-fingerprint")
    @ApiOperation("获取硬件指纹")
    public ResponseEntity<String> getHardwareFingerprint() {
        try {
            String fingerprint = HardwareUtil.getHardwareFingerprint();
            return ResponseEntity.ok(fingerprint);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("获取硬件指纹失败: " + e.getMessage());
        }
    }
    // 新增一个调用Service中无数据库验证方法的端点
    @PostMapping("/validate-file")
    @ApiOperation("验证许可证文件")
    public Result<LicenseValidator.ValidationResult> validateLicenseFile(
            @RequestParam("file") MultipartFile file) {
        return remoteAccessService.validateLicenseFile(file);
    }

    @PostMapping("/getConfigList")
    @ApiOperation("获取配置列表")
    public Result<EncryptedConfigDto> getConfigList() {
        return remoteAccessService.getConfigList();
    }

    @PostMapping("/saveConfig")
    @ApiOperation("保存配置")
    public Result<Boolean> saveConfig(@RequestBody EncryptedConfigDto configDto) {
        return remoteAccessService.saveConfig(configDto);
    }


}
