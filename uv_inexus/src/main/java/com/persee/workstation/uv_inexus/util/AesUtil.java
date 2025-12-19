package com.persee.workstation.uv_inexus.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加解密工具类
 */
public class AesUtil {

    // 16位 AES 密钥 (实际项目中应配置在安全的地方)
    private static final String KEY = "persee_inexus_mq"; 
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * 加密
     * @param content 待加密内容
     * @return Base64编码的密文
     */
    public static String encrypt(String content) {
        if (content == null) {
            return null;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedBytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt failed", e);
        }
    }

    /**
     * 解密
     * @param encryptedContent Base64编码的密文
     * @return 解密后的原始内容
     */
    public static String decrypt(String encryptedContent) {
        if (encryptedContent == null) {
            return null;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedContent);
            byte[] originalBytes = cipher.doFinal(decodedBytes);
            return new String(originalBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decrypt failed", e);
        }
    }
}
