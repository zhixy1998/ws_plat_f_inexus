package com.persee.workstation.uv_inexus.utils.AESencryption;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("customSm4Encryptor") // Bean 名称必须自定义
public class SM4StringEncryptor implements StringEncryptor {
    @Value("${sm4.key}") // 直接从 application.yml 读取密钥
    private String sm4Key;

    @Override
    public String encrypt(String plainText) {
        try {
            return SM4Util.encryptECB(plainText, sm4Key);
        } catch (Exception e) {
            throw new RuntimeException("SM4 加密失败", e);
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        try {
            return SM4Util.decryptECB(encryptedText, sm4Key);
        } catch (Exception e) {
            throw new RuntimeException("SM4 解密失败", e);
        }
    }
}

