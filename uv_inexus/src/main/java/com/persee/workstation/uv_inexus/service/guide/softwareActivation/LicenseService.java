package com.persee.workstation.uv_inexus.service.guide.softwareActivation;
import java.util.Map;

public interface LicenseService {
    String getPrivateKeyStr();

    String getPublicKeyStr();
    Map<String, String> parseLicense(String licenseContent);

}