package com.persee.workstation.uv_inexus.model;

import lombok.Data;

@Data
public class R<T> {
    //返回码
    private Boolean success;

    //返回消息
    private String message;

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
        R<T> R = build(false,message);
        return R;
    }

    public static<T> R<T> fail(Boolean success, String message) {
        R<T> R = build(success,message);
        return R;
    }



    public R<T> message(String msg){
        this.setMessage(msg);
        return this;
    }

    public R<T> success(Boolean success){
        this.setSuccess(success);
        return this;
    }
}
