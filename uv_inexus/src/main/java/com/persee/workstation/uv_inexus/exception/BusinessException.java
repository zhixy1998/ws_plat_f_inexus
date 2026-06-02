package com.persee.workstation.uv_inexus.exception;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BusinessException extends Exception {

    private static final long serialVersionUID = 1L;

    private String errorCode;

    private String errorMessage;

    private Object[] args;

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

    public static BusinessException withCode(String messageCode, Object... args) {
        BusinessException ex = new BusinessException();
        ex.errorCode = messageCode;
        ex.errorMessage = messageCode;
        ex.args = args;
        return ex;
    }
}

