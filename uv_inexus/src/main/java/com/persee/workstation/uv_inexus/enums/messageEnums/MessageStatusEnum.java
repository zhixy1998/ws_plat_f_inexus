/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.enums.messageEnums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum MessageStatusEnum {

    /**
     * 消息状态 1，未读，2，已读
     */
    STATUS_UNREAD(1,"未读"),
    STATUS_READ(2,"已读"),

    OTHER(999, "其他,参与使用，仅作枚举类end标志");


    private final Integer code;

    private final String description;

    private static final Map<Integer, MessageStatusEnum> map = new HashMap<>();

    static {
        for(MessageStatusEnum e : MessageStatusEnum.values()) {
            map.put(e.code,e);
        }
    }

    MessageStatusEnum(Integer code, String description){
        this.code = code;
        this.description = description;
    }


    public static String getDescription(Integer type) {
        if(map.containsKey(type)) {

            return map.get(type).getDescription();
        }
        throw new IllegalArgumentException("Invalid key: " + type);
    }

    public static Integer getLength(){
        return map.size();
    }
}
