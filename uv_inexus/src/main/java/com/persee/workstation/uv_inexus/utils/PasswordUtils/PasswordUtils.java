package com.persee.workstation.uv_inexus.utils.PasswordUtils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 使用 Spring 官方的 BCrypt 加密工具（不依赖完整 Security 框架）
 */
public class PasswordUtils {

    // 单例加密器实例
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 加密密码
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码（明文 vs 加密后密码）
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}