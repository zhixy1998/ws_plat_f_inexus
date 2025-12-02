package com.persee.workstation.uv_inexus.dto.softwareActivation;

// 2. License数据模型 (LicenseData.java)
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;
@Data
public class LicenseData {
    private String hardwareFingerprint;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private String licenseType;
    private Map<String, String> extraParams;
}

