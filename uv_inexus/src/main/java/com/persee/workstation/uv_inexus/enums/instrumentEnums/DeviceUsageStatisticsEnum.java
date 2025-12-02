/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum DeviceUsageStatisticsEnum {

    DEVICE_USAGE_STATISTICS("1","仪器使用率统计"),
    USER_USAGE_COUNT("2", "用户使用情况统计"),
    TEST_TYPE_COUNT("3","检测类型统计"),
    TEST_PROJECT_COUNT("4","检测项目分项统计");

    private final String code;

    private final String name;

    private static final Map<String, String> map = new HashMap<>();

    static {
        for(DeviceUsageStatisticsEnum e : DeviceUsageStatisticsEnum.values()) {
            map.put(e.code,e.name);
        }
    }

    private DeviceUsageStatisticsEnum(String code, String name){
        this.code = code;
        this.name = name;
    }


    public static String getName(String code) {
        if(map.containsKey(code)) {
            return map.get(code);
        }
        throw new IllegalArgumentException("Invalid key: " + code);
    }
}
