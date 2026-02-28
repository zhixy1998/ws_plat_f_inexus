package com.persee.workstation.uv_inexus.util;

import java.util.Random;

public class ColorUtil {
    private static final Random random = new Random();
    
    /**
     * 生成随机颜色（返回十六进制格式）
     */
    public static String getRandomColor() {
        // 生成随机RGB值
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        
        // 转换为十六进制格式 #RRGGBB
        return String.format("#%02X%02X%02X", r, g, b);
    }
    
    /**
     * 生成随机颜色，排除太亮或太暗的颜色
     */
    public static String getRandomColorAvoidExtremes() {
        int r, g, b;
        do {
            r = random.nextInt(256);
            g = random.nextInt(256);
            b = random.nextInt(256);
            // 计算亮度，避免太亮（接近白色）或太暗（接近黑色）
        } while (isTooLight(r, g, b) || isTooDark(r, g, b));
        
        return String.format("#%02X%02X%02X", r, g, b);
    }
    
    private static boolean isTooLight(int r, int g, int b) {
        // RGB平均值 > 200 视为太亮
        return (r + g + b) / 3 > 200;
    }
    
    private static boolean isTooDark(int r, int g, int b) {
        // RGB平均值 < 50 视为太暗
        return (r + g + b) / 3 < 50;
    }
}