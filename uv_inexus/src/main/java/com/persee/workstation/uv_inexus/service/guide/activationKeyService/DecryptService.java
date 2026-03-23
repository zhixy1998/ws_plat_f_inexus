/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.activationKeyService;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import workstation.utils.AESUtils; // 项目 B 的工具类

@Service
public class DecryptService {

    // 解密方法
    public String decryptActivationKey(String encryptedKey) {
        try {
            // System.out.println("11111111111111111111111"+encryptedKey);
            // 1. 先移除激活码中的所有 "-" 字符
            String keyWithoutHyphen = encryptedKey.replaceAll("-", "");
            // System.out.println("222222222222222222222"+keyWithoutHyphen);

            // 调用项目 B 的解密方法
            String decryptedContent = AESUtils.decrypt(keyWithoutHyphen);
            System.out.println("解密成功：" + decryptedContent);
            return decryptedContent;
        } catch (Exception e) {
            // 处理解密失败的情况（如密钥不匹配、加密串格式错误等）
            throw new RuntimeException("激活码解密失败：" + e.getMessage(), e);
        }
    }

    // 解密激活文件方法
    public String decryptActivationFile(MultipartFile activationFile) {
        try {
            // 读取文件内容
            byte[] fileBytes = activationFile.getBytes();
            String fileContent = new String(fileBytes, "UTF-8").trim();
            
            if (fileContent.isEmpty()) {
                throw new RuntimeException("激活文件内容为空");
            }
            
            // 提取激活码：查找"激活码："后面的内容
            String encryptedKey = extractActivationKey(fileContent);
            
            if (encryptedKey.isEmpty()) {
                throw new RuntimeException("激活文件中未找到激活码");
            }
            
            // 清理激活码：移除所有非Base64字符（只保留A-Za-z0-9+/=）
            encryptedKey = encryptedKey.replaceAll("[^A-Za-z0-9+/=]", "");
            
            if (encryptedKey.isEmpty()) {
                throw new RuntimeException("激活文件中无有效激活码");
            }

            // 调用解密方法
            return decryptActivationKey(encryptedKey);
        } catch (Exception e) {
            throw new RuntimeException("激活文件解密失败：" + e.getMessage(), e);
        }
    }
    
    // 从文件内容中提取激活码
    private String extractActivationKey(String fileContent) {
        // 查找"激活码："后面的内容
        int startIndex = fileContent.indexOf("激活码：");
        if (startIndex == -1) {
            // 如果找不到"激活码："，尝试查找"激活码:"（英文冒号）
            startIndex = fileContent.indexOf("激活码:");
        }
        
        if (startIndex != -1) {
            // 从激活码标记后开始提取
            startIndex += "激活码：".length();
            // 提取到下一行或文件结束
            int endIndex = fileContent.indexOf("\n", startIndex);
            if (endIndex == -1) {
                endIndex = fileContent.length();
            }
            return fileContent.substring(startIndex, endIndex).trim();
        }
        
        // 如果没有找到激活码标记，尝试整个文件内容
        return fileContent;
    }

}