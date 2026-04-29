package com.persee.workstation.uv_inexus.enums.factoryInspection;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Getter
@AllArgsConstructor
public enum ReminderTypeEnum {

    //期间核查提醒
    PERIODIC_INSPECTION(1, "期间核查提醒"),
    //到期提醒(标准物使用)
    EXPIRY_REMINDER(2, "标准物到期提醒");

    private final Integer code;
    private final String description;
}