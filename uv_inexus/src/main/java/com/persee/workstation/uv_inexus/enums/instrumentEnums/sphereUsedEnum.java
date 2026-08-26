package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import lombok.Getter;
import java.util.HashMap;
import java.util.Map;
@Getter
public enum sphereUsedEnum {
    RS(0, "RS"),//正常使用
    SR(1, "SR");//交换

    private final Integer code;

    private final String name;

    private static final Map<Integer, String> map = new HashMap<>();

    static {
        for (sphereUsedEnum e : sphereUsedEnum.values()) {
            map.put(e.code, e.name);
        }
    }

    private sphereUsedEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }


    public static String getName(Integer code) {
        if (map.containsKey(code)) {
            return map.get(code);
        }
        throw new IllegalArgumentException("Invalid key: " + code);
    }

    public static Integer getLength() {
        return map.size();
    }
}
