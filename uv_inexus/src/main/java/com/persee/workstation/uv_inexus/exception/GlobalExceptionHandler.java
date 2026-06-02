/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.exception;

import com.persee.workstation.uv_inexus.model.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<String>> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        // 检查是否为 SSE 请求
        if (isSseRequest(request)) {
            return handleSseError(ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleGeneralException(
            Exception ex, 
            HttpServletRequest request) {
        // 检查是否为 SSE 请求
        if (isSseRequest(request)) {
            return handleSseError(ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();
        
        // 检查是否为 SSE 请求
        if (isSseRequest(request)) {
            return handleSseError(errorMessage);
        }
        
        Result<String> fail = Result.fail(errorMessage);
        return  ResponseEntity.status(HttpStatus.OK).body(fail);
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<String>> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {
        String errorCode = ex.getErrorCode();
        Object[] args = ex.getArgs();

        Result<String> result;
        if (errorCode != null && !errorCode.isEmpty()) {
            result = Result.failCode(errorCode, args);
        } else {
            result = Result.fail(ex.getErrorMessage());
        }

        if (isSseRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(result);
        }
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
    
    /**
     * 检查是否为 SSE 请求
     */
    private boolean isSseRequest(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return acceptHeader != null && acceptHeader.contains("text/event-stream");
    }
    
    /**
     * 处理 SSE 错误响应
     */
    private ResponseEntity<Result<String>> handleSseError(String message) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.TEXT_PLAIN)
                .body(Result.fail(message));
    }
}
