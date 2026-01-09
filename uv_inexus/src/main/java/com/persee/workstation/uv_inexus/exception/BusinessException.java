package com.persee.workstation.uv_inexus.exception;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import lombok.Getter;
import lombok.Setter;

/**
 * 自定义业务异常类
 */
@Setter
@Getter
public class BusinessException extends Exception {

    private static final long serialVersionUID = 1L;

    // 错误码
    private String errorCode;

    // 错误信息
    private String errorMessage;

    public BusinessException() {
        super();
    }

    public BusinessException(String message) {
        super(message);
        this.errorMessage = message;
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorMessage = message;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorMessage = message;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorMessage = message;
    }

}

