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

package com.persee.workstation.uv_inexus.enums.messageEnums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xudian
 */

@Getter
public enum MessageTypeEnum {

    /**
     * 消息类型， 1，系统提示，2，仪器消息，3，远程消息，4，未知
     */
    TYPE_SYSTEM_TIP(1,"系统提示"),
    TYPE_INSTRUMENT_MESSAGE(2,"仪器消息"),
    TYPE_REMOTE_MESSAGE(3,"远程消息"),
    TYPE_UNKNOWN(4,"未知"),
    //提醒消息（智能助手使用）
    TYPE_REMIND_MESSAGE(5,"提醒消息"),
    //通知但不提醒
    TYPE_NOTICE_MESSAGE(6,"通知但不提醒"),

    OTHER(999, "其他,参与使用，仅作枚举类end标志");


    private final Integer code;

    private final String description;

    private static final Map<Integer, MessageTypeEnum> map = new HashMap<>();

    static {
        for(MessageTypeEnum e : MessageTypeEnum.values()) {
            map.put(e.code,e);
        }
    }

    MessageTypeEnum(Integer code, String description){
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
