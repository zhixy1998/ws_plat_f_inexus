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
    public static<T> Result<T> warn(){
        return Result.warn(null);
    }

    public static<T> Result<T> warn(T data){
        Result<T> result = build(data);
        return build(data, ResultCodeEnum.WARN);
    }

    // 警告（通过消息码与参数）
    public static<T> Result<T> warn(String messageCode, Object... obj){
        return build(ResultCodeEnum.WARN.getCode(), messageCode, obj);
    }

    // 警告便捷别名，统一调用风格
    public static<T> Result<T> warnCode(String messageCode, Object... obj){
        return warn(messageCode, obj);
    }

    public static<T> Result<T> warn(String message){
        return Result.warn(ResultCodeEnum.WARN.getCode(), message);
    }

    public static<T> Result<T> warn(Integer code, String message) {
        Result<T> result = build(code, message);
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


    /**
     * 参数校验失败（带数据）
     * @param data 返回数据
     * @param <T>
     * @return
     */
    public static<T> Result<T> checkFail(T data){
        Result<T> result = build(data);
        return build(data, ResultCodeEnum.ARGUMENT_VALID_ERROR);
    }

    /**
     * 参数校验失败（不带数据）
     * @param <T>
     * @return
     */
    public static<T> Result<T> checkFail(){
        return checkFail(null);
    }

    /**
     * 参数校验失败（通过消息码与参数）
     * @param messageCode 消息码
     * @param obj 占位符参数
     * @param <T>
     * @return
     */
    public static<T> Result<T> checkFail(String messageCode, Object... obj){
        return build(ResultCodeEnum.ARGUMENT_VALID_ERROR.getCode(), messageCode, obj);
    }

    /**
     * 参数校验失败（通过状态码和消息）
     * @param code 状态码
     * @param message 消息
     * @param <T>
     * @return
     */
    public static<T> Result<T> checkFail(Integer code, String message) {
        Result<T> result = build(code, message);
        return result;
    }

    /**
     * 参数校验失败（通过消息）
     * @param message 消息
     * @param <T>
     * @return
     */
    public static<T> Result<T> checkFail(String message){
        return checkFail(ResultCodeEnum.ARGUMENT_VALID_ERROR.getCode(), message);
    }

    /**
     * 参数校验失败便捷别名，统一调用风格
     * @param messageCode 消息码
     * @param obj 占位符参数
     * @param <T>
     * @return
     */
    public static<T> Result<T> checkFailCode(String messageCode, Object... obj){
        return checkFail(messageCode, obj);
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
