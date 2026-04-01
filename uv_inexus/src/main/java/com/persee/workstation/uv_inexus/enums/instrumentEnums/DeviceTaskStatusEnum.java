package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import lombok.Getter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public enum DeviceTaskStatusEnum {
    //TMeasureTask
    NOT_DELETED("0", "未删除"),
    DELETED("1", "已删除"),
    COMPLETED("2", "已结束");

    private final String code;
    private final String desc;

    private static final Map<String, DeviceTaskStatusEnum> map = new HashMap<>();

    static {
        for (DeviceTaskStatusEnum status : DeviceTaskStatusEnum.values()) {
            map.put(status.code, status);
        }
    }

    DeviceTaskStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }


    public static String getDesc(String code) {
        return Optional.ofNullable(map.get(code))
                .map(DeviceTaskStatusEnum::getDesc)
                .orElse("未知状态");
    }



}