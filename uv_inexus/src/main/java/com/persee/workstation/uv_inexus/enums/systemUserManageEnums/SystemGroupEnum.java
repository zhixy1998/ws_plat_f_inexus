/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.enums.systemUserManageEnums;

import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceInitInfoEnum;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum SystemGroupEnum {

    DEV_USER("dev-g","开发组"),
    PRODUCTION_LINE_ADMIN("cx-01","产线管理员"),
    PRODUCTION_LINE_INSTALL("cx-02","产线装调员"),
    PRODUCTION_LINE_INSPECTOR("cx-03","产线检验员"),
    REPAIR_USER("re-g","维修人员"),
    CUSTOMER_ADMIN("cus-01", "客户管理组"),
    CUSTOMER_TESTER("cus-02", "实验组"),;


    private final String groupNo;

    private final String name;

    SystemGroupEnum(String groupNo, String name){
        this.groupNo = groupNo;
        this.name = name;
    }
    private static final Map<String, String> map = new HashMap<>();

    static {
        for(SystemGroupEnum e : SystemGroupEnum.values()) {
            map.put(e.groupNo,e.name);
        }
    }


    public static String getName(String groupNo) {
        if(map.containsKey(groupNo)) {
            return map.get(groupNo);
        }
        return "";
    }

}
