/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.service.guide.activationKeyService;

import org.springframework.stereotype.Service;

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

}