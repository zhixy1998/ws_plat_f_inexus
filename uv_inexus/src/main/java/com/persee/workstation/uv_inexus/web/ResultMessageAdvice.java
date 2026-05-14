package com.persee.workstation.uv_inexus.web;

import com.persee.workstation.uv_inexus.i18n.LocaleContext;
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

@RestControllerAdvice(basePackages = "com.persee.workstation.uv_inexus")
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

        // 从 LocaleContext 获取当前用户的语言设置
        String localeStr = LocaleContext.getLocale();
        Locale locale = parseLocale(localeStr);

        if (body instanceof Result) {
            Result<?> result = (Result<?>) body;
            String messageCode = result.getMessageCode();
            Object[] obj = result.getObj();

            if (messageCode != null && !messageCode.trim().isEmpty()) {
                // 使用当前用户的语言进行翻译
                String yamlMessage = yamlMessageSource.getMessage(messageCode, localeStr, obj, null);
                if (!messageCode.equals(yamlMessage)) {
                    result.setMessage(yamlMessage);
                    return result;
                }
                System.out.println("DEBUG: Message code " + messageCode + " not found in YAML for locale " + localeStr + ", returned: " + yamlMessage);
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
                // 使用当前用户的语言进行翻译
                String yamlMessage = yamlMessageSource.getMessage(messageCode, localeStr, obj, null);
                if (!messageCode.equals(yamlMessage)) {
                    r.setMessage(yamlMessage);
                    return r;
                }
                System.out.println("DEBUG: Message code " + messageCode + " not found in YAML for locale " + localeStr + ", returned: " + yamlMessage);
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

    /**
     * 解析语言字符串为 Locale 对象
     * @param localeStr 语言字符串，如 "zh_CN", "en_US", "ja_JP"
     * @return Locale 对象
     */
    private Locale parseLocale(String localeStr) {
        if (localeStr == null || localeStr.isEmpty()) {
            return Locale.SIMPLIFIED_CHINESE;
        }

        try {
            String[] parts = localeStr.split("_");
            if (parts.length == 2) {
                return new Locale(parts[0], parts[1]);
            } else if (parts.length == 3) {
                return new Locale(parts[0], parts[1], parts[2]);
            }
        } catch (Exception e) {
            // 解析失败，使用默认语言
        }

        return Locale.SIMPLIFIED_CHINESE;
    }
}
