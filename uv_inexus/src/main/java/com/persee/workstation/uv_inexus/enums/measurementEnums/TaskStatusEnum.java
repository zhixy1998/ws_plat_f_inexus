package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 任务状态枚举
 */
@Getter
public enum TaskStatusEnum {
    
    /** 未测试 */
    UNTESTED("0", "未测"),
    
    /** 已测试/进行中 */
    TESTED("1", "已测"),
    
    /** 已结束/完成 */
    COMPLETED("2", "结束");
    
    private final String code;
    private final String desc;
    
    TaskStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    /**
     * 根据code获取枚举
     */
    public static TaskStatusEnum getByCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 判断是否是有效的状态码
     */
    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }
    
    /**
     * 是否是终态（不能继续操作）
     */
    public boolean isFinalState() {
        return this == COMPLETED;
    }
    
    /**
     * 是否是待测状态
     */
    public boolean isUntested() {
        return this == UNTESTED;
    }
    
    /**
     * 是否已测试
     */
    public boolean isTested() {
        return this == TESTED;
    }
}