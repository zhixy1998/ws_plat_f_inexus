package com.persee.workstation.uv_inexus.config.logging;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * 对称加密工具类
 * 使用AES/ECB/PKCS5Padding算法进行加密
 */
public class SymmetricEncryptor {

    // AES加密算法：ECB模式，PKCS5Padding填充
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    // 硬编码的加密密钥
    private static final String KEY = "123";

    /**
     * AES加密方法
     * @param content 待加密的明文内容
     * @return Base64编码的加密结果
     */
    public static String encrypt(String content) throws Exception {
        SecretKeySpec secretKey = generateKey(KEY); // 生成密钥
        Cipher cipher = Cipher.getInstance(ALGORITHM); // 获取加密实例
        cipher.init(Cipher.ENCRYPT_MODE, secretKey); // 初始化加密模式
        byte[] encryptedBytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8)); // 执行加密
        return Base64.getEncoder().encodeToString(encryptedBytes); // Base64编码返回
    }

    /**
     * 生成AES密钥
     * 将字符串密钥填充或截断为16字节长度
     */
    private static SecretKeySpec generateKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        // AES密钥长度必须为16、24或32字节
        byte[] paddedKeyBytes = Arrays.copyOf(keyBytes, 16); // 填充或截断为16字节
        return new SecretKeySpec(paddedKeyBytes, "AES"); // 创建AES密钥规范
    }
}
