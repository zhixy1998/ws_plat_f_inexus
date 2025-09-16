package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum TaskEnum {
    finished("1","已完成"),
    un_finished("0","未完成");

    private final String status;

    private final String message;

    TaskEnum(String status, String message) {
        this.status = status;
        this.message = message;
    }
    public static TaskEnum getTaskEnum(String status) {
        for (TaskEnum taskEnum : TaskEnum.values()) {
            if (taskEnum.status.equals(status)) {
                return taskEnum;
            }
        }
        return null;
    }
    public static String getTaskStr(String status) {
        return getTaskEnum(status).getMessage();
    }
}
