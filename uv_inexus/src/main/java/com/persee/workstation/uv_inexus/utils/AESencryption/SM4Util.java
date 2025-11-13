package com.persee.workstation.uv_inexus.utils.AESencryption;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;
import java.util.Base64;

public class SM4Util {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // ECB 模式加密
    public static String encryptECB(String plainText, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(), "SM4"));
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // ECB 模式解密
    public static String decryptECB(String cipherText, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(), "SM4"));
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(decrypted);
    }
    public static void main(String[] args) throws Exception {
        String key = "86C63180C2806ED1"; // 与配置文件中的 sm4.key 一致
        String plainPassword = "px123456";
        String encrypted = SM4Util.encryptECB(plainPassword, key);
        System.out.println(encrypted);
        System.out.println(SM4Util.decryptECB(encrypted, key));
    }
}

