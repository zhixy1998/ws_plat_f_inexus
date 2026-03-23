/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.activationKeyService;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.persee.workstation.uv_inexus.utils.AESencryption.AESEncryptionUtil;

import workstation.utils.AESUtils;

@Service
public class DecryptService {

    // 解密激活文件方法（直接使用activation_key依赖）
    public String decryptActivationFile(MultipartFile activationFile) {
        try {
            if (activationFile == null || activationFile.isEmpty()) {
                throw new RuntimeException("激活文件不能为空");
            }

            // 读取文件内容
            byte[] fileContent = activationFile.getBytes();
            if (fileContent == null || fileContent.length == 0) {
                throw new RuntimeException("文件内容为空");
            }

            // 使用activation_key依赖中的AESEncryptionUtil解密文件
            String aesKeyBase64 = "wUnjmObQly0V3M3hsUd33RjW5pp2GVmFVMihl+wiLTU=";
            byte[] decryptedContent = AESEncryptionUtil.decrypt(fileContent, aesKeyBase64);
            String encryptedKey = new String(decryptedContent, StandardCharsets.UTF_8);

            if (encryptedKey == null || encryptedKey.trim().isEmpty()) {
                throw new RuntimeException("解密后的激活码为空");
            }

            // 使用activation_key依赖中的AESUtils解密激活码
            String plainText = AESUtils.decrypt(encryptedKey);

            System.out.println("解密成功：" + plainText);
            return plainText;
        } catch (Exception e) {
            throw new RuntimeException("激活文件解密失败：" + e.getMessage(), e);
        }
    }

}