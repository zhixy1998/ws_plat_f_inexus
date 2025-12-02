package com.persee.workstation.uv_inexus.dto.softwareActivation;

import lombok.Data;

@Data
public class EncryptedIpsDto {
    private String isEnabled;
    private String encryptedIps;
}
