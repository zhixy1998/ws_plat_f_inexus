/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum DeviceConnectTypeEnum {

    //0-串口，1-网口，2-WIFI
    SERIAL(0, "串口"),
    ETHERNET(1, "网口"),
    WIFI(2, "Wifi");

    // 连接类型
    private final Integer connectType;
    // 描述
    private final String desc;


    private static final Map<Integer, DeviceConnectTypeEnum> enums = new HashMap<>();

    static {
        for(DeviceConnectTypeEnum e : DeviceConnectTypeEnum.values()) {
            enums.put(e.connectType, e);
        }
    }

    // 构造函数
    DeviceConnectTypeEnum(Integer connectType, String desc) {
        this.connectType = connectType;
        this.desc = desc;
    }

    public static DeviceConnectTypeEnum getEnum(Integer type) {
        return enums.get(type);
    }

    public static List<DeviceConnectTypeEnum> getAll(){
        return Arrays.asList(values());
    }
    
}
