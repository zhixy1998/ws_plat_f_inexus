/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.enums.systemUserManageEnums;

import lombok.Getter;

@Getter
public enum SystemRoleEnum {

    DEVELOPER("1","开发人员"),
    PRODUCTION_LINE("2","产线人员"),
    REPAIR_PERSON("3","维修人员"),
    CUSTOMER("4","客户人员");

    private final String sid;

    private final String name;

    SystemRoleEnum(String sid, String name){
        this.sid = sid;
        this.name = name;
    }

}
