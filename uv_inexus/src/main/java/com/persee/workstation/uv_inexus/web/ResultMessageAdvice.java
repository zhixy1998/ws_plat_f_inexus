package com.persee.workstation.uv_inexus.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.i18n.I18nMessageSource;
import com.persee.workstation.uv_inexus.i18n.LocaleProvider;
import com.persee.workstation.uv_inexus.i18n.NoI18n;
import com.persee.workstation.uv_inexus.i18n.OutputTranslator;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.util.*;

@Slf4j
@RestControllerAdvice(basePackages = "com.persee.workstation.uv_inexus")
public class ResultMessageAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private I18nMessageSource i18nMessageSource;

    @Autowired
    private LocaleProvider localeProvider;

    @Autowired
    private OutputTranslator outputTranslator;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> type = returnType.getParameterType();
        boolean supported = Result.class.isAssignableFrom(type) || R.class.isAssignableFrom(type);
        if (supported) {
            log.info("[i18n] supports 匹配成功: type={}, method={}", type.getSimpleName(), returnType.getMethod().getName());
        }
        return supported;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }

        String locale = localeProvider.getSystemLocale();
        boolean needTranslate = !"zh-CN".equals(locale) && !"zh_CN".equals(locale) && locale != null;
        log.info("[i18n] ResultMessageAdvice 触发: locale={}, needTranslate={}, path={}", locale, needTranslate, request.getURI().getPath());

        if (body instanceof Result) {
            Result<?> result = (Result<?>) body;
            translateMessage(result, locale);
            if (needTranslate && result.getData() != null) {
                Object translatedData = translateData(result.getData());
                Result rawResult = result;
                rawResult.setData(translatedData);
                log.info("[i18n] 最终返回 data 类型: {}", rawResult.getData().getClass().getSimpleName());
            }
            return result;
        }

        if (body instanceof R) {
            R<?> r = (R<?>) body;
            translateMessageR(r, locale);
            if (needTranslate && r.getData() != null) {
                Object translatedData = translateData(r.getData());
                R rawR = r;
                rawR.setData(translatedData);
            }
            return r;
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    private Object translateData(Object data) {
        if (data instanceof CharSequence || data instanceof Number || data instanceof Boolean || data instanceof Date) {
            return data;
        }
        Set<String> noI18nFields = collectNoI18nFields(data);
        try {
            Object converted = objectMapper.convertValue(data, Object.class);
            log.info("[i18n] translateData: dataType={}, convertedType={}, noI18nFields={}", 
                    data.getClass().getSimpleName(), converted.getClass().getSimpleName(), noI18nFields);
            if (converted instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) converted;
                log.info("[i18n] translateData map内容: {}", map);
                Map<String, Object> result = outputTranslator.translate(map, noI18nFields);
                log.info("[i18n] translateData 翻译后: {}", result);
                return result;
            } else if (converted instanceof List) {
                return outputTranslator.translate((List<Object>) converted, noI18nFields);
            }
            log.info("[i18n] translateData 未匹配Map/List, 原样返回");
            return data;
        } catch (Exception e) {
            log.warn("[i18n] data 翻译异常: {}", e.getMessage());
            return data;
        }
    }

    private Set<String> collectNoI18nFields(Object data) {
        if (data == null) {
            return Collections.emptySet();
        }
        Set<String> fieldNames = new HashSet<>();
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            if (!list.isEmpty()) {
                collectFromClassHierarchy(list.get(0).getClass(), fieldNames);
            }
        } else {
            collectFromClassHierarchy(data.getClass(), fieldNames);
        }
        return fieldNames;
    }

    private void collectFromClassHierarchy(Class<?> clazz, Set<String> fieldNames) {
        Set<Class<?>> visited = new HashSet<>();
        doCollectFromClassHierarchy(clazz, fieldNames, visited);
    }

    private void doCollectFromClassHierarchy(Class<?> clazz, Set<String> fieldNames, Set<Class<?>> visited) {
        if (clazz == null || clazz == Object.class || !visited.add(clazz)) {
            return;
        }
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(NoI18n.class)) {
                fieldNames.add(field.getName());
            }
            // 递归检查嵌套对象的类型
            Class<?> fieldType = field.getType();
            if (!fieldType.isPrimitive() && !fieldType.isArray()
                    && !CharSequence.class.isAssignableFrom(fieldType)
                    && !Number.class.isAssignableFrom(fieldType)
                    && !Boolean.class.equals(fieldType)
                    && !Date.class.isAssignableFrom(fieldType)
                    && !Collection.class.isAssignableFrom(fieldType)
                    && !Map.class.isAssignableFrom(fieldType)) {
                doCollectFromClassHierarchy(fieldType, fieldNames, visited);
            }
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            doCollectFromClassHierarchy(iface, fieldNames, visited);
        }
        doCollectFromClassHierarchy(clazz.getSuperclass(), fieldNames, visited);
    }

    private void translateMessage(Result<?> result, String locale) {
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

    private void translateMessageR(R<?> r, String locale) {
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
