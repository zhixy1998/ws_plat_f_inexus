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

package com.persee.workstation.uv_inexus.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 主键ID生成工具类
 */
@Component
public class IdGeneratorUtil {

    /**
     * 生成UUID主键（去除横线）
     * @return 32位字符串形式的主键ID
     */
    public static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成带前缀的主键ID
     * @param prefix 前缀
     * @return 带前缀的主键ID
     */
    public static String generateIdWithPrefix(String prefix) {
        return prefix + generateId();
    }

    /**
     * 生成短格式ID（15位）
     * @return 15位字符串形式的主键ID
     */
    public static String generateShortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    }
}
