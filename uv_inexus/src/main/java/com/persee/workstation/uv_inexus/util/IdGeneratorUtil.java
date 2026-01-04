/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
     * 生成短格式ID（6位）
     * @return 6位字符串形式的主键ID
     */
    public static String generateShortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
