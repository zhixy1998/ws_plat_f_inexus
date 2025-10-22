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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum DeviceInitPreCommandEnum {

    InitSampleCell(0,  Arrays.asList("InitSampleCell", "SetCellPos"), "附件初始化", "");

    private final Integer num;

    // 获取comId
    private final List<String> comIdList;
    // 获取description
    private final String comValue;

    private final String description;

    private static final Map<Integer, DeviceInitPreCommandEnum> map = new HashMap<>();

    static {
        for(DeviceInitPreCommandEnum e : DeviceInitPreCommandEnum.values()) {
            map.put(e.num,e);
        }
    }

    // 构造函数
    DeviceInitPreCommandEnum(Integer num, List<String> comIdList, String description, String comValue) {
        this.num = num;
        this.comIdList = comIdList;
        this.description = description;
        this.comValue = comValue;
    }

    public static DeviceInitPreCommandEnum getEnum(Integer num) {
        return map.get(num);
    }

    public static Integer getLength(){
        return map.size();
    }
}
