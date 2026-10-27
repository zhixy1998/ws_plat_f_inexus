package com.persee.workstation.uv_inexus.util;

import com.persee.workstation.uv_inexus.i18n.I18nMessageSource;
import com.persee.workstation.uv_inexus.i18n.LocaleProvider;
import com.persee.workstation.uv_inexus.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class I18nMessageUtil {

    @Autowired
    private I18nMessageSource i18nMessageSource;

    @Autowired
    private LocaleProvider localeProvider;

    public String getMessage(String code, Object... args) {
        if (code == null) {
            return null;
        }
        String locale = localeProvider.getSystemLocale();
        return i18nMessageSource.getMessage(code, locale, args);
    }

    public String getMessage(String code, String locale, Object... args) {
        if (code == null) {
            return null;
        }
        return i18nMessageSource.getMessage(code, locale, args);
    }

    /**
     * 将异常转换为面向用户的提示文本：优先按异常携带的国际化消息码翻译；
     * 异常消息为空（如 NPE）时退化为异常类名，避免把 null 当作消息码传入导致 NPE。
     */
    public String getMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String message = throwable.getMessage();
        return message != null ? i18nMessageSource.getMessage(message) : throwable.getClass().getSimpleName();
    }

    /**
     * 将操作结果 R 转换为面向用户的提示文本：message 若是消息码则按占位符参数翻译，
     * 否则原样返回；message 为空时退化为 messageCode；两者皆空返回 null，避免把 null
     * 当作消息码传入导致 NPE（与异常路径同一类隐患）。
     */
    public String getMessage(R<?> result) {
        if (result == null) {
            return null;
        }
        String message = result.getMessage();
        if (message != null) {
            return translate(message, result.getObj());
        }
        String code = result.getMessageCode();
        if (code != null) {
            return translate(code, result.getObj());
        }
        return null;
    }

    private String translate(String code, Object[] args) {
        return args != null && args.length > 0
                ? i18nMessageSource.getMessage(code, args)
                : i18nMessageSource.getMessage(code);
    }

    public String getMessageWithDefault(String code, String defaultMessage, Object... args) {
        if (code == null) {
            return defaultMessage;
        }
        return i18nMessageSource.getMessageWithDefault(code, defaultMessage, args);
    }
}
