package com.persee.workstation.uv_inexus.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ColorUtil {
    private static final List<String> COLOR_PALETTE = new ArrayList<>();
    private static final AtomicInteger colorIndex = new AtomicInteger(0);

    static {
        // 24种固定颜色，按照红、绿、蓝三色深浅渐变
        // 红色系 (深红 → 中红 → 暗红)
        COLOR_PALETTE.add("#8B0000"); // 暗红
        COLOR_PALETTE.add("#B22222"); // 火砖红
        COLOR_PALETTE.add("#DC143C"); // 深红
        COLOR_PALETTE.add("#CD5C5C"); // 印度红
        COLOR_PALETTE.add("#F08080"); // 浅珊瑚红
        COLOR_PALETTE.add("#A52A2A"); // 棕色（红褐）

        // 绿色系 (深绿 → 翠绿 → 暗绿)
        COLOR_PALETTE.add("#006400"); // 深绿
        COLOR_PALETTE.add("#228B22"); // 森林绿
        COLOR_PALETTE.add("#2E8B57"); // 海洋绿
        COLOR_PALETTE.add("#3CB371"); // 中等海洋绿
        COLOR_PALETTE.add("#556B2F"); // 暗橄榄绿
        COLOR_PALETTE.add("#8FBC8F"); // 暗海洋绿

        // 蓝色系 (深蓝 → 中蓝 → 暗蓝)
        COLOR_PALETTE.add("#00008B"); // 暗蓝
        COLOR_PALETTE.add("#0000CD"); // 中蓝
        COLOR_PALETTE.add("#191970"); // 午夜蓝
        COLOR_PALETTE.add("#4169E1"); // 皇家蓝
        COLOR_PALETTE.add("#4682B4"); // 钢蓝
        COLOR_PALETTE.add("#5F9EA0"); // 军校蓝

        // 紫红/橙/棕等深色系（补充）
        COLOR_PALETTE.add("#800080"); // 紫色
        COLOR_PALETTE.add("#8B008B"); // 暗洋红
        COLOR_PALETTE.add("#9932CC"); // 暗兰紫
        COLOR_PALETTE.add("#8B4513"); // 鞍棕
        COLOR_PALETTE.add("#D2691E"); // 巧克力色
        COLOR_PALETTE.add("#B8860B"); // 暗金菊色
    }

    /**
     * 获取随机颜色（从24种固定颜色中随机选择）
     */
    public static String getRandomColor() {
        int index = (int)(Math.random() * COLOR_PALETTE.size());
        return COLOR_PALETTE.get(index);
    }

    /**
     * 按顺序获取颜色（循环使用24种颜色）
     * 适合需要区分多个数据系列的场景
     */
    public static String getNextColor() {
        int index = colorIndex.getAndIncrement() % COLOR_PALETTE.size();
        return COLOR_PALETTE.get(index);
    }

    /**
     * 根据索引获取固定颜色
     */
    public static String getColorByIndex(int index) {
        return COLOR_PALETTE.get(index % COLOR_PALETTE.size());
    }

    /**
     * 获取整个调色板
     */
    public static List<String> getAllColors() {
        return new ArrayList<>(COLOR_PALETTE);
    }

    /**
     * 重置颜色索引（按需使用）
     */
    public static void resetIndex() {
        colorIndex.set(0);
    }
}