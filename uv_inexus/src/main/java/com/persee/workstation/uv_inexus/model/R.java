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

    // 通过消息码与参数构建（消息解析交由 ResponseBodyAdvice 处理）
    private static <T> R<T> build(Boolean success, String messageCode, Object... obj) {
        R<T> R = new R<T>();
        R.setSuccess(success);
        R.setMessageCode(messageCode);
        R.setObj(obj);
        // 暂存 messageCode 到 message，实际解析在响应拦截器中完成
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
     * @param data  baseCategory1List
     * @param <T>
     * @return
     */
    public static<T> R<T> ok(T data){
        R<T> R = build(data);
        return build(data, true);
    }

    // 操作成功（通过消息码与占位参数）- 自动识别消息码
    public static<T> R<T> ok(String messageCode, Object... obj){
        if (isMessageCode(messageCode)) {
            return build(true, messageCode, obj);
        } else {
            return build(true, messageCode);
        }
    }

    // 操作成功（通过消息码与占位参数）
    public static<T> R<T> okCode(String messageCode, Object... obj){
        return build(true, messageCode, obj);
    }

    // 操作成功（携带数据 + 消息码与占位参数）
    public static<T> R<T> okCode(T data, String messageCode, Object... obj){
        R<T> R = build(data);
        R.setSuccess(true);
        R.setMessageCode(messageCode);
        R.setObj(obj);
        R.setMessage(messageCode);
        return R;
    }

    public static<T> R<T> fail(){
        return R.fail(null);
    }

    /**
     * 操作失败
     * @param data
     * @param <T>
     * @return
     */
    public static<T> R<T> fail(T data){
        R<T> R = build(data);
        return build(data, false);
    }

    public static<T> R<T> fail(String message) {
        // 判断是否是消息码（8位十六进制）
        if (isMessageCode(message)) {
            return build(false, message, new Object[0]);  // 显式传递空数组，触发正确的重载
        } else {
            R<T> R = build(false,message);
            return R;
        }
    }
// ... existing code ...



    // 操作失败（通过消息码与占位参数）- 自动识别消息码
    public static<T> R<T> fail(String messageCode, Object... obj){
        // 判断是否是消息码（8位十六进制）
        if (isMessageCode(messageCode)) {
            return build(false, messageCode, obj);
        } else {
            // 如果不是消息码，直接返回原文
            return build(false, messageCode);
        }
    }

    // 失败别名，统一调用风格
    public static<T> R<T> failCode(String messageCode, Object... obj){
        return fail(messageCode, obj);
    }

    public static<T> R<T> fail(Boolean success, String message) {
        R<T> R = build(success,message);
        return R;
    }



    public R<T> message(String msg){
        // 判断是否是消息码（8位十六进制）
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
