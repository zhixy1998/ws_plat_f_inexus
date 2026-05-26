package com.persee.workstation.uv_inexus.web;

import com.persee.workstation.uv_inexus.i18n.I18nMessageSource;
import com.persee.workstation.uv_inexus.i18n.LocaleContext;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.persee.workstation.uv_inexus")
public class ResultMessageAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private I18nMessageSource i18nMessageSource;

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

        String locale = LocaleContext.getLocale();

        if (body instanceof Result) {
            Result<?> result = (Result<?>) body;
            translateResult(result, locale);
            return result;
        }

        if (body instanceof R) {
            R<?> r = (R<?>) body;
            translateR(r, locale);
            return r;
        }

        return body;
    }

    private void translateResult(Result<?> result, String locale) {
        String messageCode = result.getMessageCode();
        if (messageCode != null && !messageCode.trim().isEmpty()) {
            String translated = i18nMessageSource.getMessage(messageCode, locale, result.getObj(), null);
            if (translated != null && !messageCode.equals(translated)) {
                result.setMessage(translated);
            }
            return;
        }

        String message = result.getMessage();
        if (message != null && containsChinese(message)) {
            String translated = i18nMessageSource.getMessage(message, locale);
            if (translated != null && !message.equals(translated)) {
                result.setMessage(translated);
            }
        }
    }

    private void translateR(R<?> r, String locale) {
        String messageCode = r.getMessageCode();
        Object[] obj = r.getObj();

        if (messageCode != null && !messageCode.trim().isEmpty()) {
            String translated = i18nMessageSource.getMessage(messageCode, locale, obj, null);
            if (translated != null && !messageCode.equals(translated)) {
                r.setMessage(translated);
            }
            return;
        }

        String message = r.getMessage();
        if (message != null && containsChinese(message)) {
            String translated = i18nMessageSource.getMessage(message, locale);
            if (translated != null && !message.equals(translated)) {
                r.setMessage(translated);
            }
        }
    }

    private boolean containsChinese(String value) {
        if (value == null) return false;
        for (char c : value.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') return true;
        }
        return false;
    }
}
