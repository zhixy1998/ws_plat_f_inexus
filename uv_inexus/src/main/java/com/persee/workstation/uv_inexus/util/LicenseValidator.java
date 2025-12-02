package com.persee.workstation.uv_inexus.util;

import javax.crypto.SecretKey;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Properties;

public class LicenseValidator {
    
    private String publicKeyStr = null;
    private String privateKeyStr = null;
    
    // 临时存储生成的密钥对，避免配置文件问题
    private static java.security.KeyPair TEMP_KEY_PAIR = null;
    private static String TEMP_PRIVATE_KEY_STR = null;
    private static String TEMP_PUBLIC_KEY_STR = null;
    
    static {
        try {
            // 初始化时生成临时密钥对
            System.out.println("LicenseValidator: 开始生成临时密钥对...");
            TEMP_KEY_PAIR = CryptoUtil.generateRSAKeyPair();
            TEMP_PRIVATE_KEY_STR = CryptoUtil.privateKeyToString(TEMP_KEY_PAIR.getPrivate());
            TEMP_PUBLIC_KEY_STR = CryptoUtil.publicKeyToString(TEMP_KEY_PAIR.getPublic());
            System.out.println(TEMP_PUBLIC_KEY_STR);
            System.out.println(TEMP_PRIVATE_KEY_STR);
            System.out.println("LicenseValidator: 临时密钥对生成成功");
        } catch (Exception e) {
            System.err.println("LicenseValidator: 生成临时密钥对失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getPrivateKeyStr() {
        // 如果外部设置了私钥，则使用外部设置的私钥
        if (privateKeyStr != null && !privateKeyStr.isEmpty() && !privateKeyStr.equals("\"\"")) {
            return privateKeyStr;
        }
        // 如果临时密钥对不存在，则生成新的
        if (TEMP_KEY_PAIR == null) {
            try {
                System.out.println("LicenseValidator: 重新生成临时密钥对...");
                TEMP_KEY_PAIR = CryptoUtil.generateRSAKeyPair();
                TEMP_PRIVATE_KEY_STR = CryptoUtil.privateKeyToString(TEMP_KEY_PAIR.getPrivate());
                TEMP_PUBLIC_KEY_STR = CryptoUtil.publicKeyToString(TEMP_KEY_PAIR.getPublic());
                System.out.println("LicenseValidator: 临时密钥对重新生成成功");
            } catch (Exception e) {
                System.err.println("LicenseValidator: 重新生成临时密钥对失败: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        return TEMP_PRIVATE_KEY_STR;
    }
    
    public String getPublicKeyStr() {
        // 如果外部设置了公钥，则使用外部设置的公钥
        if (publicKeyStr != null && !publicKeyStr.isEmpty() && !publicKeyStr.equals("\"\"")) {
            return publicKeyStr;
        }
        // 如果临时密钥对不存在，则生成新的
        if (TEMP_KEY_PAIR == null) {
            try {
                System.out.println("LicenseValidator: 重新生成临时密钥对...");
                TEMP_KEY_PAIR = CryptoUtil.generateRSAKeyPair();
                TEMP_PRIVATE_KEY_STR = CryptoUtil.privateKeyToString(TEMP_KEY_PAIR.getPrivate());
                TEMP_PUBLIC_KEY_STR = CryptoUtil.publicKeyToString(TEMP_KEY_PAIR.getPublic());
                System.out.println("LicenseValidator: 临时密钥对重新生成成功");
            } catch (Exception e) {
                System.err.println("LicenseValidator: 重新生成临时密钥对失败: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        return TEMP_PUBLIC_KEY_STR;
    }

    /**
     * 设置公钥字符串
     * @param publicKey 公钥字符串
     */
    public void setPublicKey(String publicKey) {
        this.publicKeyStr = publicKey;
    }
    
    /**
     * 设置私钥字符串
     * @param privateKey 私钥字符串
     */
    public void setPrivateKey(String privateKey) {
        this.privateKeyStr = privateKey;
    }

    /**
     * 验证许可证文件内容（不依赖数据库）
     * @param licenseContent 许可证文件内容
     * @param hardwareFingerprint 硬件指纹
     * @return 验证结果
     */
    public ValidationResult validateLicenseFileContent(String licenseContent, String hardwareFingerprint) {
        try {
            System.out.println("开始验证许可证文件内容");
            System.out.println("文件内容: " + licenseContent);
            System.out.println("硬件指纹: " + hardwareFingerprint);
            
            // 解析属性
            Properties props = new Properties();
            props.load(new StringReader(licenseContent));
            
            // 检查是否是加密的许可证文件
            String encryptedData = props.getProperty("encrypted.data");
            String encryptedKey = props.getProperty("encrypted.key");
            
            if (encryptedData != null && encryptedKey != null && !encryptedData.isEmpty() && !encryptedKey.isEmpty()) {
                System.out.println("检测到加密的许可证文件");
                System.out.println("加密数据: " + encryptedData);
                System.out.println("加密密钥: " + encryptedKey);
                try {
                    // 解密加密的许可证文件
                    if (getPrivateKeyStr() == null || getPrivateKeyStr().isEmpty()) {
                        System.out.println("缺少私钥，无法解密许可证文件");
                        return new ValidationResult(false, "缺少私钥，无法解密许可证文件");
                    }
                    
                    System.out.println("使用的私钥: " + getPrivateKeyStr());
                    PrivateKey privateKey = CryptoUtil.stringToPrivateKey(getPrivateKeyStr());
                    String decryptedAESKeyStr = CryptoUtil.decryptRSA(encryptedKey, privateKey);
                    SecretKey aesKey = CryptoUtil.stringToSecretKey(decryptedAESKeyStr);
                    String decryptedContent = CryptoUtil.decryptAES(encryptedData, aesKey);
                    System.out.println("解密后的内容: " + decryptedContent);
                    
                    // 重新解析解密后的内容
                    props.clear();
                    props.load(new StringReader(decryptedContent));
                } catch (Exception e) {
                    // 如果解密失败，假设文件未加密，继续使用原始内容
                    System.err.println("解密许可证文件失败，假设文件未加密: " + e.getMessage());
                    e.printStackTrace();
                    // 重新加载原始内容
                    props.load(new StringReader(licenseContent));
                }
            } else {
                System.out.println("许可证文件未加密");
            }

            // 获取许可证信息
            String licenseKey = props.getProperty("licence.key");
            String signature = props.getProperty("licence.signature");
            String productInfo = props.getProperty("licence.product.info");
            String customerInfo = props.getProperty("licence.customer.info");
            String hardwareFingerprintStored = props.getProperty("licence.hardware.fingerprint");

            System.out.println("许可证信息:");
            System.out.println("- 许可证密钥: " + licenseKey);
            System.out.println("- 签名: " + signature);
            System.out.println("- 产品信息: " + productInfo);
            System.out.println("- 客户信息: " + customerInfo);
            System.out.println("- 存储的硬件指纹: " + hardwareFingerprintStored);

            // 获取过期时间
            String expiresAtStr = props.getProperty("licence.expires.at");
            System.out.println("- 过期时间字符串: " + expiresAtStr);
            if (expiresAtStr == null) {
                return new ValidationResult(false, "缺少过期时间");
            }
            
            // 处理可能被转义的冒号
            String unescapedExpiresAtStr = expiresAtStr.replace("\\:", ":");
            LocalDateTime expiresAt = LocalDateTime.parse(unescapedExpiresAtStr);

            // 获取最大IP数（如果有）
            String maxIpCountStr = props.getProperty("licence.max.ip.count");
            System.out.println("- 最大IP数字符串: " + maxIpCountStr);
            Integer maxIpCount = null;
            if (maxIpCountStr != null && !maxIpCountStr.isEmpty()) {
                maxIpCount = Integer.parseInt(maxIpCountStr);
                System.out.println("- 最大IP数: " + maxIpCount);
            }

            // 验证硬件指纹
            if (hardwareFingerprintStored == null || !hardwareFingerprintStored.equals(hardwareFingerprint)) {
                System.out.println("硬件指纹不匹配!");
                System.out.println("- 存储的指纹: " + hardwareFingerprintStored);
                System.out.println("- 提供的指纹: " + hardwareFingerprint);
                return new ValidationResult(false, "硬件指纹不匹配");
            }

            // 验证许可证
            System.out.println("开始验证许可证...");
            return validateLicense(licenseKey, hardwareFingerprint, signature, expiresAt, productInfo, customerInfo, maxIpCount);
        } catch (DateTimeParseException e) {
            System.err.println("日期格式错误: " + e.getMessage());
            e.printStackTrace();
            return new ValidationResult(false, "日期格式错误: " + e.getMessage());
        } catch (NullPointerException e) {
            System.err.println("许可证文件缺少必要字段: " + e.getMessage());
            e.printStackTrace();
            return new ValidationResult(false, "许可证文件缺少必要字段: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("验证过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return new ValidationResult(false, "验证过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 验证许可证是否有效（不依赖数据库）
     * @param licenseKey 许可证密钥
     * @param hardwareFingerprint 硬件指纹
     * @param signature 数字签名
     * @param expiresAt 过期时间
     * @param productInfo 产品信息
     * @param customerInfo 客户信息
     * @param maxIpCount 最大IP数
     * @return 验证结果
     */
    public ValidationResult validateLicense(String licenseKey, String hardwareFingerprint, String signature, 
                                         LocalDateTime expiresAt, String productInfo, String customerInfo, Integer maxIpCount) {
        // 检查必要参数
        if (licenseKey == null || licenseKey.isEmpty()) {
            return new ValidationResult(false, "许可证密钥不能为空");
        }
        
        if (hardwareFingerprint == null || hardwareFingerprint.isEmpty()) {
            return new ValidationResult(false, "硬件指纹不能为空");
        }
        
        // 注意：签名可以为空，这种情况下我们返回验证失败而不是抛出异常
        if (signature == null || signature.isEmpty()) {
            return new ValidationResult(false, "数字签名不能为空，许可证可能未正确生成");
        }
        
        if (expiresAt == null) {
            return new ValidationResult(false, "过期时间不能为空");
        }
        
        // 检查许可证是否过期
        if (expiresAt.isBefore(LocalDateTime.now())) {
            return new ValidationResult(false, "许可证已过期");
        }
        
        // 验证数字签名
        try {
            // 构造用于签名的数据，必须与生成时完全一致
            String licenseData = createLicenseData(licenseKey, hardwareFingerprint, expiresAt, productInfo, customerInfo, maxIpCount);
            
            System.out.println("验证数据: " + licenseData);
            System.out.println("签名: " + signature);
            System.out.println("公钥: " + getPublicKeyStr());
            
            // 检查公钥是否设置
            if (getPublicKeyStr() == null || getPublicKeyStr().isEmpty()) {
                return new ValidationResult(false, "缺少公钥，无法验证签名");
            }
            
            // 获取公钥
            PublicKey publicKey = CryptoUtil.stringToPublicKey(getPublicKeyStr());
            
            boolean isValid = CryptoUtil.verifySignature(licenseData, signature, publicKey);
            System.out.println("签名验证结果: " + isValid);
            
            if (!isValid) {
                return new ValidationResult(false, "数字签名验证失败");
            }
        } catch (Exception e) {
            System.err.println("数字签名验证异常: " + e.getMessage());
            e.printStackTrace();
            return new ValidationResult(false, "数字签名验证异常: " + e.getMessage());
        }
        
        // 验证成功
        return new ValidationResult(true, "许可证有效");
    }
    
    /**
     * 创建用于签名的许可证数据，避免特殊字符问题
     * @param licenseKey 许可证密钥
     * @param hardwareFingerprint 硬件指纹
     * @param expiresAt 过期时间
     * @param productInfo 产品信息
     * @param customerInfo 客户信息
     * @param maxIpCount 最大IP数
     * @return 用于签名的数据
     */
    private String createLicenseData(String licenseKey, String hardwareFingerprint, LocalDateTime expiresAt, 
                                   String productInfo, String customerInfo, Integer maxIpCount) {
        // 使用分隔符连接各部分数据，避免特殊字符干扰
        StringBuilder sb = new StringBuilder();
        sb.append(licenseKey).append("|")
          .append(hardwareFingerprint).append("|")
          .append(expiresAt.toString()).append("|")
          .append(productInfo).append("|")
          .append(customerInfo);
          
        if (maxIpCount != null) {
            sb.append("|").append(maxIpCount);
        }
        
        return sb.toString();
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}