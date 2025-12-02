package com.persee.workstation.uv_inexus.service.guide.softwareActivation;

import com.persee.workstation.uv_inexus.entity.License;
import com.persee.workstation.uv_inexus.dto.softwareActivation.LicenseValidationResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

public interface LicenseService {
    String getPrivateKeyStr();

    String getPublicKeyStr();
    Map<String, String> parseLicense(String licenseContent);

}