/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.model;

import lombok.Data;

@Data
public class R<T> {
    //返回码
    private Boolean success;

    //返回消息
    private String message;

    // 消息码（用于统一的消息解析）
    private String messageCode;

    // 占位符参数（用于 MessageFormat 格式化）
    private Object[] obj;

    //返回数据
    private T data;

    public R(){}

    // 返回数据
    protected static <T> R<T> build(T data) {
        R<T> R = new R<T>();
        if (data != null)
            R.setData(data);
        return R;
    }

    private static <T> R<T> build(Boolean success, String message) {
        R<T> R = new R<T>();
        R.setSuccess(success);
        R.setMessage(message);
        return R;
    }

    // 核心构建：通过消息码与参数构建
    private static <T> R<T> buildWithCode(Boolean success, String messageCode, Object... obj) {
        R<T> R = new R<T>();
        R.setSuccess(success);
        R.setMessageCode(messageCode);
        R.setObj(obj);
        R.setMessage(messageCode);
        return R;
    }

    public static <T> R<T> build(T body, Boolean success, String message) {
        R<T> R = build(body);
        R.setSuccess(success);
        R.setMessage(message);
        return R;
    }

    public static <T> R<T> build(T body, Boolean success) {
        R<T> R = build(body);
        R.setSuccess(success);
        return R;
    }

    public static<T> R<T> ok(){
        return R.ok(null);
    }

    /**
     * 操作成功
     */
    public static<T> R<T> ok(T data){
        R<T> R = build(data);
        return build(data, true);
    }

    // 操作成功（通过消息码字符串 - 自动识别）
    public static<T> R<T> ok(String messageCode, Object... obj){
        if (isMessageCode(messageCode)) {
            return buildWithCode(true, messageCode, obj);
        } else {
            return build(true, messageCode);
        }
    }

    public static<T> R<T> okCode(String messageCode, Object... obj){
        return buildWithCode(true, messageCode, obj);
    }

    public static<T> R<T> okCode(T data, String messageCode, Object... obj){
        R<T> R = build(data);
        R.setSuccess(true);
        R.setMessageCode(messageCode);
        R.setObj(obj);
        R.setMessage(messageCode);
        return R;
    }

    public static<T> R<T> warn(){
        return R.warn(null);
    }

    public static<T> R<T> warn(T data){
        R<T> R = build(data);
        return build(data, false);
    }

    // 警告（通过消息码字符串 - 自动识别）
    public static<T> R<T> warn(String messageCode, Object... obj){
        if (isMessageCode(messageCode)) {
            return buildWithCode(false, messageCode, obj);
        } else {
            return build(false, messageCode);
        }
    }

    public static<T> R<T> warnCode(String messageCode, Object... obj){
        return buildWithCode(false, messageCode, obj);
    }

    public static<T> R<T> warn(String message){
        return build(false, message);
    }

    public static<T> R<T> fail(){
        return R.fail(null);
    }

    /**
     * 操作失败
     */
    public static<T> R<T> fail(T data){
        R<T> R = build(data);
        return build(data, false);
    }

    public static<T> R<T> fail(String message) {
        if (isMessageCode(message)) {
            return buildWithCode(false, message);
        } else {
            return build(false, message);
        }
    }

    // 操作失败（通过消息码字符串 - 自动识别）
    public static<T> R<T> fail(String messageCode, Object... obj){
        if (isMessageCode(messageCode)) {
            return buildWithCode(false, messageCode, obj);
        } else {
            return build(false, messageCode);
        }
    }

    public static<T> R<T> failCode(String messageCode, Object... obj){
        return buildWithCode(false, messageCode, obj);
    }

    public static<T> R<T> fail(Boolean success, String message) {
        R<T> R = build(success,message);
        return R;
    }

    public R<T> message(String msg){
        if (isMessageCode(msg)) {
            this.setMessageCode(msg);
            this.setMessage(msg);
        } else {
            this.setMessage(msg);
        }
        return this;
    }

    public R<T> success(Boolean success){
        this.setSuccess(success);
        return this;
    }

    public R<T> messageCode(String messageCode){
        this.setMessageCode(messageCode);
        return this;
    }

    public R<T> obj(Object... obj){
        this.setObj(obj);
        return this;
    }

    /**
     * 判断是否为消息码（8位十六进制）
     */
    private static boolean isMessageCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        return code.matches("^[0-9a-fA-F]{8}$");
    }
}
