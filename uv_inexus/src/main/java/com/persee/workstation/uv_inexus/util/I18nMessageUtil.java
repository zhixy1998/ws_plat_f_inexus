package com.persee.workstation.uv_inexus.util;

import com.persee.workstation.uv_inexus.i18n.I18nMessageSource;
import com.persee.workstation.uv_inexus.i18n.LocaleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class I18nMessageUtil {

    @Autowired
    private I18nMessageSource i18nMessageSource;

    public String getMessage(String code, Object... args) {
        String locale = LocaleContext.getLocale();
        return i18nMessageSource.getMessage(code, locale, args);
    }

    public String getMessage(String code, String locale, Object... args) {
        return i18nMessageSource.getMessage(code, locale, args);
    }

    public String getMessageWithDefault(String code, String defaultMessage, Object... args) {
        return i18nMessageSource.getMessageWithDefault(code, defaultMessage, args);
    }
}

