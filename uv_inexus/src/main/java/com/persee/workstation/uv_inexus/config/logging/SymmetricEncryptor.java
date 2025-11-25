package com.persee.workstation.uv_inexus.config.logging;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class SymmetricEncryptor {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String KEY = "123";

    public static String encrypt(String content) throws Exception {
        SecretKeySpec secretKey = generateKey(KEY);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private static SecretKeySpec generateKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        // AES key must be 16, 24, or 32 bytes long.
        byte[] paddedKeyBytes = Arrays.copyOf(keyBytes, 16);
        return new SecretKeySpec(paddedKeyBytes, "AES");
    }
}
