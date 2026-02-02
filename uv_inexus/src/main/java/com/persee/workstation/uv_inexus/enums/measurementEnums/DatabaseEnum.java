package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据库枚举
 * 定义数据库类型或模式的枚举
 */
@Getter
@RequiredArgsConstructor
public enum DatabaseEnum {
    /**
     * 向导模式
     */
    GUIDE("guide", "向导"),
    
    /**
     * 经典模式
     */
    CONCISE("concise", "经典");
    
    /**
     * 枚举编码
     */
    private final String code;
    
    /**
     * 枚举描述
     */
    private final String description;
    
    /**
     * 根据编码获取枚举实例
     * @param code 编码
     * @return 对应的枚举实例，未找到返回null
     */
    public static DatabaseEnum getByCode(String code) {
        for (DatabaseEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
    
    /**
     * 根据描述获取枚举实例
     * @param description 描述
     * @return 对应的枚举实例，未找到返回null
     */
    public static DatabaseEnum getByDescription(String description) {
        for (DatabaseEnum value : values()) {
            if (value.getDescription().equals(description)) {
                return value;
            }
        }
        return null;
    }
    
    /**
     * 检查编码是否存在
     * @param code 编码
     * @return 是否存在
     */
    public static boolean containsCode(String code) {
        return getByCode(code) != null;
    }
    
    /**
     * 检查描述是否存在
     * @param description 描述
     * @return 是否存在
     */
    public static boolean containsDescription(String description) {
        return getByDescription(description) != null;
    }
    
    @Override
    public String toString() {
        return "DatabaseEnum{" +
                "code='" + code + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}