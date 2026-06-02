package com.persee.workstation.uv_inexus.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class InputTranslator {

    @Autowired
    private I18nMessageSource messageSource;

    @Autowired
    private LocaleProvider localeProvider;

    private final Map<String, Map<String, String>> reverseIndex = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        buildReverseIndex();
    }

    public void rebuildIndex() {
        buildReverseIndex();
    }

    public String toChinese(String foreignText) {
        if (foreignText == null || foreignText.isEmpty()) {
            return foreignText;
        }
        if (containsChinese(foreignText)) {
            return foreignText;
        }
        String locale = localeProvider.getSystemLocale();
        if ("zh-CN".equals(locale) || "zh_CN".equals(locale) || locale == null) {
            return foreignText;
        }
        Map<String, String> localeIndex = reverseIndex.get(locale);
        if (localeIndex == null) {
            return foreignText;
        }
        String chinese = localeIndex.get(foreignText);
        return chinese != null ? chinese : foreignText;
    }

    private void buildReverseIndex() {
        Map<String, Map<String, String>> allMessages = messageSource.getAllMessages();
        Set<String> supportedLocales = new HashSet<>(Arrays.asList("en-US", "ru-RU"));

        for (String locale : supportedLocales) {
            Map<String, String> index = new HashMap<>();
            for (Map.Entry<String, Map<String, String>> entry : allMessages.entrySet()) {
                String chineseKey = entry.getKey();
                Map<String, String> translations = entry.getValue();

                if (!translations.containsKey("zh-CN")) {
                    continue;
                }
                String chineseText = translations.get("zh-CN");

                String foreignTranslation = translations.get(locale);
                if (foreignTranslation != null && !foreignTranslation.equals(chineseText)) {
                    index.put(foreignTranslation, chineseKey);
                }
            }
            reverseIndex.put(locale, index);
            log.info("InputTranslator 反向索引构建完成: locale={}, 索引条数={}", locale, index.size());
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
