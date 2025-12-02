/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.utils.AESencryption;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HmacMD5Util {
    private static final String KEY = "3cdb0c5c984e85e9dfb259425fc00a8c";

    /**
     * HmacMD5 计算
     */
    public static String hmacMD5(String data) {
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            SecretKeySpec secretKeySpec = new SecretKeySpec(KEY.getBytes(), "HmacMD5");
            mac.init(secretKeySpec);
            byte[] result = mac.doFinal(data.getBytes());
            return bytesToHex(result);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HmacMD5计算失败", e);
        }
    }

    /**
     * 生成指定长度的随机密钥
     */
    public static String generateRandomKey(int length) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < length; i++) {
            // 生成0-9的随机数字
            int randomNum = (int) (Math.random() * 10);
            key.append(randomNum);
        }
        return key.toString();
    }

//    public static void main(String[] args) {
//        System.out.println(hmacMD5("123456", generateRandomKey(16)));
//    }
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(hmacMD5("123456"));
    }
}
