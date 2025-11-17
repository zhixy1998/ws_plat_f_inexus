package com.persee.workstation.uv_inexus.web;

import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.R;
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
/*
* 信息码拦截器，暂未使用。建议使用YamlMessageSource的静态方法getMessageStatic返回信息
* */
@RestControllerAdvice(basePackages = "com.persee.workstation.uv_inexus.controller")
public class ResultMessageAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private YamlMessageSource yamlMessageSource;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> type = returnType.getParameterType();
        return Result.class.isAssignableFrom(type) || R.class.isAssignableFrom(type);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }

        // 默认使用简体中文
        Locale locale = Locale.SIMPLIFIED_CHINESE;

        if (body instanceof Result) {
            Result<?> result = (Result<?>) body;
            String messageCode = result.getMessageCode();
            Object[] obj = result.getObj();

            if (messageCode != null && !messageCode.trim().isEmpty()) {
                String yamlMessage = yamlMessageSource.getMessage(messageCode, obj, null);
                if (!messageCode.equals(yamlMessage)) {
                    result.setMessage(yamlMessage);
                    return result;
                }
                System.out.println("DEBUG: Message code " + messageCode + " not found in YAML, returned: " + yamlMessage);
            }

            try {
                String message = messageSource.getMessage(result.getMessage(), obj, locale);
                result.setMessage(message);
            } catch (Exception e) {
                // 忽略异常，保留原始消息
            }
            return result;
        }

        if (body instanceof R) {
            R<?> r = (R<?>) body;
            String messageCode = r.getMessageCode();
            Object[] obj = r.getObj();

            if (messageCode != null && !messageCode.trim().isEmpty()) {
                String yamlMessage = yamlMessageSource.getMessage(messageCode, obj, null);
                if (!messageCode.equals(yamlMessage)) {
                    r.setMessage(yamlMessage);
                    return r;
                }
                System.out.println("DEBUG: Message code " + messageCode + " not found in YAML, returned: " + yamlMessage);
            }

            try {
                String message = messageSource.getMessage(r.getMessage(), obj, locale);
                r.setMessage(message);
            } catch (Exception e) {
                // 忽略异常，保留原始消息
            }
            return r;
        }

        return body;
    }
}