/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.enums.systemManageEnums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum SystemManageEnum {
    methodPanel("method","方法面板"),
    projectPanel("project","项目面板"),
    taskPanel("task","任务面板");


    private final String name;

    private final String description;

    SystemManageEnum(String name, String description){
        this.description = description;
        this.name = name;
    }
    private static final Map<String, String> map = new HashMap<>();

    static {
        for(SystemManageEnum e : SystemManageEnum.values()) {
            map.put(e.name,e.description);
        }
    }


    public static String getName(String name) {
        if(map.containsKey(name)) {
            return map.get(name);
        }
        return "";
    }
}
