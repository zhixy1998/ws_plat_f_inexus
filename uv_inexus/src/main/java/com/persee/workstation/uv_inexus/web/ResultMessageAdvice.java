package com.persee.workstation.uv_inexus.web;

import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Locale;

@RestControllerAdvice(basePackages = "com.persee.workstation.uv_inexus.controller")
public class ResultMessageAdvice implements ResponseBodyAdvice<Result<?>> {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private YamlMessageSource yamlMessageSource;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getParameterType().isAssignableFrom(Result.class);
    }

    @Override
    public Result<?> beforeBodyWrite(Result<?> body, MethodParameter returnType, MediaType selectedContentType,
                                     Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                     ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }

        String messageCode = body.getMessageCode();
        Object[] obj = body.getObj();
        // 默认使用简体中文
        Locale locale = Locale.SIMPLIFIED_CHINESE;

        // 优先使用 messageCode 和 YamlMessageSource 解析
        if (messageCode != null && !messageCode.trim().isEmpty()) {
            String yamlMessage = yamlMessageSource.getMessage(messageCode, obj, null);
            // 如果 YAML 解析成功（即返回的不是原始代码），则更新消息
            if (!messageCode.equals(yamlMessage)) {
                body.setMessage(yamlMessage);
                return body;
            }
            // 调试信息：如果消息代码匹配但返回了原始代码，说明YAML中未找到该消息
            System.out.println("DEBUG: Message code " + messageCode + " not found in YAML, returned: " + yamlMessage);
        }

        // 如果 YAML 解析失败或 messageCode 为空，尝试使用 Spring 的 MessageSource
        // 这保留了对 .properties 文件的兼容性
        try {
            String message = messageSource.getMessage(body.getMessage(), obj, locale);
            body.setMessage(message);
        } catch (Exception e) {
            // 忽略异常，保留原始消息
        }

        return body;
    }
}