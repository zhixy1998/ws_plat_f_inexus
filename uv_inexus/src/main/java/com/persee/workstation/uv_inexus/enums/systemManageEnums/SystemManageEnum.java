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
