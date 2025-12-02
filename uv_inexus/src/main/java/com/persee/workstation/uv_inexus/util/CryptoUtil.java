package com.persee.workstation.uv_inexus.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoUtil {
    
    private static final String AES_ALGORITHM = "AES";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    
    /**
     * 生成AES密钥
     * @return AES密钥
     */
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }
    
    /**
     * 使用AES加密数据
     * @param data 待加密数据
     * @param key 密钥
     * @return 加密后的数据(Base64编码)
     */
    public static String encryptAES(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedData);
    }
    
    /**
     * 使用AES解密数据
     * @param encryptedData 加密的数据(Base64编码)
     * @param key 密钥
     * @return 解密后的数据
     */
    public static String decryptAES(String encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedData = cipher.doFinal(decodedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
    
    /**
     * 生成RSA密钥对
     * @return RSA密钥对
     */
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }
    
    /**
     * 使用私钥对数据进行签名
     * @param data 待签名数据
     * @param privateKey 私钥
     * @return 签名(Base64编码)
     */
    public static String sign(String data, PrivateKey privateKey) throws Exception {
        System.out.println("开始签名数据: " + data);
        System.out.println("私钥类型: " + (privateKey != null ? privateKey.getClass().getName() : "null"));
        
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signedData = signature.sign();
        String result = Base64.getEncoder().encodeToString(signedData);
        System.out.println("签名完成，结果: " + result);
        return result;
    }
    
    /**
     * 使用公钥验证签名
     * @param data 原始数据
     * @param signature 签名(Base64编码)
     * @param publicKey 公钥
     * @return 验证结果
     */
    public static boolean verifySignature(String data, String signature, PublicKey publicKey) throws Exception {
        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
        sig.initVerify(publicKey);
        sig.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] decodedSignature = Base64.getDecoder().decode(signature);
        return sig.verify(decodedSignature);
    }
    
    /**
     * 使用RSA公钥加密数据
     * @param data 待加密数据
     * @param publicKey 公钥
     * @return 加密后的数据(Base64编码)
     */
    public static String encryptRSA(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedData);
    }
    
    /**
     * 使用RSA私钥解密数据
     * @param encryptedData 加密的数据(Base64编码)
     * @param privateKey 私钥
     * @return 解密后的数据
     */
    public static String decryptRSA(String encryptedData, PrivateKey privateKey) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("RSA解密失败:");
            System.err.println("- 加密数据: " + encryptedData);
            System.err.println("- 私钥类型: " + (privateKey != null ? privateKey.getClass().getName() : "null"));
            throw e;
        }
    }
    
    /**
     * 将SecretKey转换为字符串
     * @param secretKey SecretKey
     * @return Base64编码的密钥字符串
     */
    public static String secretKeyToString(SecretKey secretKey) {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
    
    /**
     * 从字符串还原SecretKey
     * @param secretKeyStr Base64编码的密钥字符串
     * @return SecretKey对象
     */
    public static SecretKey stringToSecretKey(String secretKeyStr) {
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyStr);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, AES_ALGORITHM);
    }
    
    /**
     * 将公钥转换为字符串
     * @param publicKey 公钥
     * @return Base64编码的公钥字符串
     */
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    /**
     * 从字符串还原公钥
     * @param publicKeyStr Base64编码的公钥字符串
     * @return 公钥对象
     */
    public static PublicKey stringToPublicKey(String publicKeyStr) throws Exception {
        try {
            // 检查是否为空
            if (publicKeyStr == null || publicKeyStr.isEmpty()) {
                throw new IllegalArgumentException("公钥字符串为空");
            }
            
            // 清理公钥字符串，移除可能的空格、换行符和其他非法字符
            String cleanedKey = publicKeyStr.replaceAll("\\s+", "").trim();
            
            // 确保字符串长度是4的倍数（Base64编码要求）
            while (cleanedKey.length() % 4 != 0) {
                cleanedKey += "=";
            }
            
            // 检查是否为空
            if (cleanedKey.isEmpty()) {
                throw new IllegalArgumentException("公钥字符串为空");
            }
            
            byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            return keyFactory.generatePublic(spec);
        } catch (IllegalArgumentException e) {
            System.err.println("公钥字符串格式错误: " + e.getMessage());
            System.err.println("原始公钥字符串长度: " + (publicKeyStr != null ? publicKeyStr.length() : "null"));
            System.err.println("清理后公钥字符串长度: " + (publicKeyStr != null ? publicKeyStr.replaceAll("\\s+", "").trim().length() : "null"));
            if (publicKeyStr != null && publicKeyStr.length() > 100) {
                System.err.println("公钥字符串预览: " + publicKeyStr.substring(0, 100) + "...");
            } else {
                System.err.println("公钥字符串: " + (publicKeyStr != null ? publicKeyStr : "null"));
            }
            throw new Exception("公钥Base64解码失败，请检查配置文件中的公钥是否正确", e);
        } catch (Exception e) {
            System.err.println("公钥解析失败: " + e.getMessage());
            System.err.println("可能的原因:");
            System.err.println("1. 公钥字符串不是有效的X.509格式");
            System.err.println("2. 公钥内容已损坏");
            System.err.println("3. 公钥与私钥不匹配");
            throw new Exception("公钥解析失败，请检查配置文件中的公钥是否正确", e);
        }
    }
    
    /**
     * 将私钥转换为字符串
     * @param privateKey 私钥
     * @return Base64编码的私钥字符串
     */
    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
    
    /**
     * 从字符串还原私钥
     * @param privateKeyStr Base64编码的私钥字符串
     * @return 私钥对象
     */
    public static PrivateKey stringToPrivateKey(String privateKeyStr) throws Exception {
        try {
            // 检查是否为空
            if (privateKeyStr == null || privateKeyStr.isEmpty()) {
                throw new IllegalArgumentException("私钥字符串为空");
            }
            
            // 清理私钥字符串，移除可能的空格、换行符和其他非法字符
            String cleanedKey = privateKeyStr.replaceAll("\\s+", "").trim();
            
            // 确保字符串长度是4的倍数（Base64编码要求）
            while (cleanedKey.length() % 4 != 0) {
                cleanedKey += "=";
            }
            
            // 检查是否为空
            if (cleanedKey.isEmpty()) {
                throw new IllegalArgumentException("私钥字符串为空");
            }
            
            System.out.println("解码私钥，清理后的字符串长度: " + cleanedKey.length());
            byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);
            System.out.println("解码后的字节数组长度: " + keyBytes.length);
            
            // 检查解码后的字节长度是否合理
            if (keyBytes.length < 100) {
                throw new IllegalArgumentException("解码后的私钥数据太短，可能无效");
            }
            
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey result = keyFactory.generatePrivate(spec);
            System.out.println("私钥还原成功");
            return result;
        } catch (IllegalArgumentException e) {
            System.err.println("私钥字符串格式错误: " + e.getMessage());
            System.err.println("原始私钥字符串长度: " + (privateKeyStr != null ? privateKeyStr.length() : "null"));
            System.err.println("清理后私钥字符串长度: " + (privateKeyStr != null ? privateKeyStr.replaceAll("\\s+", "").trim().length() : "null"));
            if (privateKeyStr != null && privateKeyStr.length() > 100) {
                System.err.println("私钥字符串预览: " + privateKeyStr.substring(0, 100) + "...");
            } else {
                System.err.println("私钥字符串: " + (privateKeyStr != null ? privateKeyStr : "null"));
            }
            throw new Exception("私钥Base64解码失败，请检查配置文件中的私钥是否正确", e);
        } catch (Exception e) {
            System.err.println("私钥解析失败: " + e.getMessage());
            System.err.println("可能的原因:");
            System.err.println("1. 私钥字符串不是有效的PKCS#8格式");
            System.err.println("2. 私钥内容已损坏");
            System.err.println("3. 私钥与公钥不匹配");
            e.printStackTrace();
            throw new Exception("私钥解析失败，请检查配置文件中的私钥是否正确", e);
        }
    }
    
    // 测试方法，用于验证修复是否有效
    public static void main(String[] args) {
        try {
            System.out.println("开始测试密钥加载...");
            
            // 测试私钥
            String privateKeyStr = "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";
            
            String publicKeyStr = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu1SU1LfUJNLTkO4CGbY8AQ0Y3pNGxuRualezQUR5Oy7O+J9GyUyV91fYgrpwvcCeWPRVtaAd3k9a9XFJEOyLHdt2fROV+ItcNOO4GQfYOaBdLA5RHSUOuLlNTaxkNNPayN/VXXNzaXmVNCnoFP1KifT/d19iTBR1OfFqnYlUdHNRsSo1NR2l4WWXZZizblpDtIje8Fa91/Tmth8eadoJ41ObitF4da9Ij6vLvQqx4zt0ru15d+RPXEOpd7WWtFzo2Nad4FOC1C97A5Te9R5gtZkLZUpoPeB8V+F/1LiV/XDPbgcLQsxMGK2PYPSej1uJEwNL8F8VcAY7kMbmbyIDAQAB";
            
            System.out.println("清理前私钥长度: " + privateKeyStr.length());
            String cleanedKey = privateKeyStr.replaceAll("\\s+", "").trim();
            System.out.println("清理后私钥长度: " + cleanedKey.length());
            System.out.println("清理后私钥预览: " + cleanedKey.substring(0, 50) + "...");
            
            PrivateKey privateKey = stringToPrivateKey(privateKeyStr);
            System.out.println("私钥加载成功!");
            
            PublicKey publicKey = stringToPublicKey(publicKeyStr);
            System.out.println("公钥加载成功!");
            
            System.out.println("所有密钥加载测试通过!");
        } catch (Exception e) {
            System.err.println("密钥加载测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}