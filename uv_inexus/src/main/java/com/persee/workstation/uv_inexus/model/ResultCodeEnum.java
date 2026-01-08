/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.model;

import lombok.Getter;

/**
 * 统一返回结果状态信息类
 *
 */
@Getter
public enum ResultCodeEnum {

    SUCCESS(200,"成功"),
    FAIL(201, "失败"),
    WARN(202, "警告"),
    SERVICE_ERROR(2012, "服务异常"),
    SYS_ERROR(500,"系统异常"),
    DATA_ERROR(204, "数据异常"),
    ILLEGAL_REQUEST(205, "非法请求"),
    REPEAT_SUBMIT(206, "重复提交"),
    ARGUMENT_VALID_ERROR(210, "参数校验异常"),

    LOGIN_AUTH(208, "未登陆"),
    PERMISSION(209, "没有权限"),
    ACCOUNT_ERROR(214, "账号不正确"),
    ACCOUNT_EXIST(215,"账号已存在"),
    PASSWORD_ERROR(216, "密码不正确"),
    LOGIN_MOBLE_ERROR( 217, "账号或密码不正确"),
    ACCOUNT_STOP( 218, "账号已停用"),
    NODE_ERROR( 219, "该节点下有子节点，不可以删除"),

    DEVICE_DISCONNECT( 220, "设备已断开"),  //实时回显用

    TOKEN_ERROR(401, "Token过期或有误")
    ;

    private Integer code;

    private String message;

    private ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}