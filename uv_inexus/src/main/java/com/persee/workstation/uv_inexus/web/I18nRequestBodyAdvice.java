package com.persee.workstation.uv_inexus.web;

import com.persee.workstation.uv_inexus.i18n.InputTranslator;
import com.persee.workstation.uv_inexus.i18n.LocaleProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ControllerAdvice(basePackages = "com.persee.workstation.uv_inexus")
@Slf4j
public class I18nRequestBodyAdvice implements RequestBodyAdvice {

    @Autowired
    private InputTranslator inputTranslator;

    @Autowired
    private LocaleProvider localeProvider;

    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        String locale = localeProvider.getSystemLocale();
        if (locale == null || "zh-CN".equals(locale) || "zh_CN".equals(locale)) {
            return body;
        }
        translateObject(body, new HashSet<>());
        return body;
    }

    @Override
    /**
     * 处理请求体为空的情况
     * @param body 原始请求体，在此方法中为null
     * @param inputMessage HTTP输入消息，包含请求头和请求体
     * @param parameter 方法参数信息，用于获取参数的详细信息
     * @param targetType 目标类型，表示需要转换的数据类型
     * @param converterType 消息转换器类型，用于处理HTTP消息的转换
     * @return 返回处理后的请求体，这里直接返回原始body（null）
     */
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body; // 直接返回原始请求体，不做任何处理
    }

/**
 * 递归翻译对象，处理对象中的各个字段
 * @param obj 要翻译的对象
 * @param visited 已访问对象的集合，用于防止循环引用导致的无限递归
 */
    private void translateObject(Object obj, Set<Integer> visited) {
    // 如果对象为null，直接返回
        if (obj == null) {
            return;
        }
    // 获取对象的身份哈希码，用于唯一标识对象
        int identity = System.identityHashCode(obj);
    // 如果对象已经被访问过，直接返回，防止循环引用
        if (visited.contains(identity)) {
            return;
        }
    // 将当前对象标记为已访问
        visited.add(identity);

    // 如果对象是CharSequence、Number或Boolean类型，直接返回，这些类型不需要处理
        if (obj instanceof CharSequence || obj instanceof Number || obj instanceof Boolean) {
            return;
        }

    // 如果对象是String类型，直接返回，String类型不需要处理
        if (obj instanceof String) {
            return;
        }

    // 如果对象是Map类型，递归处理Map中的键值对
        if (obj instanceof Map) {
            translateMap((Map<?, ?>) obj, visited);
            return;
        }

    // 如果对象是Collection类型，递归处理Collection中的元素
        if (obj instanceof Collection) {
            translateCollection((Collection<?>) obj, visited);
            return;
        }

    // 如果对象是数组类型，递归处理数组中的元素
        if (obj.getClass().isArray()) {
            translateArray((Object[]) obj, visited);
            return;
        }

    // 获取对象的类名
        String className = obj.getClass().getName();
    // 如果对象是Java标准库或Sun公司的类，直接返回，这些类不需要处理
        if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("sun.")) {
            return;
        }

    // 处理对象中的字段
        translateFields(obj, visited);
    }

/**
 * 翻译对象中的字段值
 * @param obj 需要翻译的对象
 * @param visited 已访问对象的集合，用于防止循环引用导致的无限递归
 */
    private void translateFields(Object obj, Set<Integer> visited) {
    // 从缓存中获取或计算对象类的可编辑字段列表
        List<Field> fields = FIELD_CACHE.computeIfAbsent(obj.getClass(), this::getEditableFields);
    // 遍历所有字段
        for (Field field : fields) {
            try {
            // 获取字段的值
                Object value = field.get(obj);
            // 如果值为null，跳过该字段
                if (value == null) {
                    continue;
                }
            // 如果字段值是字符串类型
                if (value instanceof String) {
                    String str = (String) value;
                // 将字符串翻译成中文
                    String translated = inputTranslator.toChinese(str);
                // 如果翻译结果不为null且与原字符串不同，则更新字段值
                    if (translated != null && !translated.equals(str)) {
                        field.set(obj, translated);
                    }
                } else {
                // 如果字段值不是字符串，递归翻译该对象
                    translateObject(value, visited);
                }
            } catch (Exception e) {
            // 记录调试日志，说明跳过翻译的字段
                log.debug("字段翻译跳过: {}.{}", obj.getClass().getSimpleName(), field.getName());
            }
        }
    }

    private void translateMap(Map<?, ?> map, Set<Integer> visited) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                String translated = inputTranslator.toChinese((String) value);
                if (translated != null && !translated.equals(value) && map instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> mutableMap = (Map<Object, Object>) map;
                    mutableMap.put(entry.getKey(), translated);
                }
            } else {
                translateObject(value, visited);
            }
        }
    }

    private void translateCollection(Collection<?> collection, Set<Integer> visited) {
        for (Object item : collection) {
            translateObject(item, visited);
        }
    }

    private void translateArray(Object[] array, Set<Integer> visited) {
        for (Object item : array) {
            translateObject(item, visited);
        }
    }

    private List<Field> getEditableFields(Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
                    continue;
                }
                field.setAccessible(true);
                result.add(field);
            }
            current = current.getSuperclass();
        }
        return result;
    }
}
