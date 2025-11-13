/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.model;

import lombok.Data;

/**
 * 全局统一返回结果类
 */
@Data
public class Result<T> {

    //返回码
    private Integer code;

    //返回消息
    private String message;

    // 消息码（来自 YAML，例如 '00104004'）
    private String messageCode;

    // 占位符参数（用于 MessageFormat 格式化）
    private Object[] obj;

    //返回数据
    private T data;

    public Result(){}

    // 返回数据
    protected static <T> Result<T> build(T data) {
        Result<T> result = new Result<T>();
        if (data != null)
            result.setData(data);
        return result;
    }

    private static <T> Result<T> build(Integer code, String message) {
        Result<T> result = new Result<T>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    // 通过消息码与参数构建
    private static <T> Result<T> build(Integer code, String messageCode, Object... obj) {
        Result<T> result = new Result<T>();
        result.setCode(code);
        result.setMessageCode(messageCode);
        result.setObj(obj);
        // 消息的解析与格式化完全交由 ResultMessageAdvice 处理
        result.setMessage(messageCode);
        return result;
    }

    public static <T> Result<T> build(T body, Integer code, String message) {
        Result<T> result = build(body);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> build(T body, ResultCodeEnum resultCodeEnum) {
        Result<T> result = build(body);
        result.setCode(resultCodeEnum.getCode());
        result.setMessage(resultCodeEnum.getMessage());
        return result;
    }

    public static<T> Result<T> ok(){
        return Result.ok(null);
    }

    /**
     * 操作成功
     * @param data  baseCategory1List
     * @param <T>
     * @return
     */
    public static<T> Result<T> ok(T data){
        Result<T> result = build(data);
        return build(data, ResultCodeEnum.SUCCESS);
    }

    // 避免与 ok(T data) 发生重载冲突的便捷别名（直接通过 YAML 解析构建）
    public static<T> Result<T> okCode(String messageCode, Object... obj){
        return build(ResultCodeEnum.SUCCESS.getCode(), messageCode, obj);
    }

    public static<T> Result<T> okCode(T data, String messageCode, Object... obj){
        Result<T> result = build(data);
        result.setCode(ResultCodeEnum.SUCCESS.getCode());
        result.setMessageCode(messageCode);
        result.setObj(obj);
        // 消息的解析与格式化完全交由 ResultMessageAdvice 处理
        result.setMessage(messageCode);
        return result;
    }

    public static<T> Result<T> fail(){
        return Result.fail(null);
    }

    public static<T> Result<T> fail(String message){
        return Result.fail(ResultCodeEnum.FAIL.getCode(), message);
    }

    // 操作失败（通过消息码与参数）
    public static<T> Result<T> fail(String messageCode, Object... obj){
        return build(ResultCodeEnum.FAIL.getCode(), messageCode, obj);
    }

    // 失败便捷别名，统一调用风格
    public static<T> Result<T> failCode(String messageCode, Object... obj){
        return fail(messageCode, obj);
    }

    /**
     * 操作失败
     * @param data
     * @param <T>
     * @return
     */
    public static<T> Result<T> fail(T data){
        Result<T> result = build(data);
        return build(data, ResultCodeEnum.FAIL);
    }

    public static<T> Result<T> fail(Integer code, String message) {
        Result<T> result = build(code,message);
        return result;
    }



    public Result<T> message(String msg){
        this.setMessage(msg);
        return this;
    }

    public Result<T> code(Integer code){
        this.setCode(code);
        return this;
    }

    public Result<T> messageCode(String messageCode){
        this.setMessageCode(messageCode);
        return this;
    }

    public Result<T> obj(Object... obj){
        this.setObj(obj);
        return this;
    }
}
