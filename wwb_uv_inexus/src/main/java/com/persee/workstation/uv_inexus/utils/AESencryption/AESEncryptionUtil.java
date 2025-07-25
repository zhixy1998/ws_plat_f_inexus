package com.persee.workstation.uv_inexus.utils.AESencryption;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AESEncryptionUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int KEY_SIZE = 256; // 使用AES-256

    // 生成 AES 密钥 (返回 Base64 编码的字符串)
    public static String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    // 加密字节数组
    public static byte[] encrypt(byte[] data, String base64Key) throws Exception {
        if (base64Key == null || data == null) {
            throw new IllegalArgumentException("Key and data cannot be null");
        }
        SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(base64Key), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    // 解密字节数组
    public static byte[] decrypt(byte[] encryptedData, String base64Key) throws Exception {
        if (base64Key == null || encryptedData == null) {
            throw new IllegalArgumentException("Key and encryptedData cannot be null");
        }
        SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(base64Key), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(encryptedData);
    }

    // 加密并返回Base64字符串
    public static String encryptToBase64(String base64Key, byte[] data) throws Exception {
        byte[] encrypted = encrypt(data, base64Key);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // 从Base64字符串解密
    public static byte[] decryptFromBase64(String base64Key, String base64EncryptedData) throws Exception {
        byte[] encryptedData = Base64.getDecoder().decode(base64EncryptedData);
        return decrypt(encryptedData, base64Key);
    }
}