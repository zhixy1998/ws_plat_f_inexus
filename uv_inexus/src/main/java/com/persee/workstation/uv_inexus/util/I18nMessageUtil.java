package com.persee.workstation.uv_inexus.util;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import com.persee.workstation.uv_inexus.i18n.LocaleContext;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class I18nMessageUtil {

    @Autowired
    private YamlMessageSource yamlMessageSource;

    public String getMessage(String code, Object... args) {
        String locale = LocaleContext.getLocale();
        return getMessage(code, locale, args);
    }

    public String getMessage(String code, String locale, Object... args) {
        if (locale == null || locale.isEmpty()) {
            locale = "zh-CN";
        }

        String message = yamlMessageSource.getMessage(code, locale, args, null);

        if (code.equals(message)) {
            log.warn("未找到消息码 {} 的翻译，语言: {}", code, locale);
        }

        return message;
    }

    public String getMessageWithDefault(String code, String defaultMessage, Object... args) {
        String locale = LocaleContext.getLocale();
        return yamlMessageSource.getMessage(code, locale, args, defaultMessage);
    }
}

