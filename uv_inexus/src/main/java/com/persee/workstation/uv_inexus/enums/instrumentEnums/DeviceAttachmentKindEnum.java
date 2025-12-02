/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
public enum DeviceAttachmentKindEnum {

    //固定池，八联池，五联池，流动池，积分球
    FIXED_POOL(1, "固定池"),
    EIGHT_POOL(2, "八联池"),
    FIVE_POOL(3, "五联池"),
    FLOW_POOL(4, "流动池"),
    INTEGRAL_BALL(5, "积分球");



    // 获取comId
    private final String name;
    // 获取description
    private final Integer type;

    private static final Map<Integer, String> map = new HashMap<>();

    static {
        for(DeviceAttachmentKindEnum e : DeviceAttachmentKindEnum.values()) {
            map.put(e.type,e.name);
        }
    }

    // 构造函数
    DeviceAttachmentKindEnum(Integer type, String name) {
        this.name = name;
        this.type = type;
    }

    public static String getComId(Integer type) {
        return map.get(type);
    }

    public static Integer getLength(){
        return map.size();
    }
}
