package com.persee.workstation.uv_inexus.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class I18nMessageSource {

    private static final String DICT_FILE = "i18n/i18n-dictionary.yaml";
    private static final String UI_TEXT_FILE = "i18n/i18n-ui-texts.yaml";
    private static final String DEFAULT_LOCALE = "zh-CN";
    private static final Set<String> SUPPORTED_LOCALES = new LinkedHashSet<>(Arrays.asList(
            "zh-CN", "en-US", "ru-RU"
    ));

    private final Map<String, Map<String, String>> dictionary = new ConcurrentHashMap<>();

    @Autowired
    private LocaleProvider localeProvider;

    @PostConstruct
    public void init() {
        loadDictionary();
        loadFile(UI_TEXT_FILE);
    }

    @SuppressWarnings("unchecked")
    private void loadDictionary() {
        loadFile(DICT_FILE);
    }

    @SuppressWarnings("unchecked")
    private void loadFile(String filePath) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                log.warn("国际化文件不存在: {}", filePath);
                return;
            }

            Yaml yaml = new Yaml();
            Object rootObj = yaml.load(inputStream);

            if (!(rootObj instanceof Map)) {
                log.error("国际化文件格式错误: {} - 根节点不是 Map", filePath);
                return;
            }

            Map<String, Object> root = (Map<String, Object>) rootObj;
            Object messagesObj = root.get("messages");

            if (!(messagesObj instanceof Map)) {
                log.error("国际化文件格式错误: {} - 'messages' 节点不是 Map", filePath);
                return;
            }

            Map<String, Object> messages = (Map<String, Object>) messagesObj;

            for (Map.Entry<String, Object> entry : messages.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Map) {
                    Map<String, String> translations = new HashMap<>();
                    Map<String, Object> langMap = (Map<String, Object>) value;

                    for (Map.Entry<String, Object> langEntry : langMap.entrySet()) {
                        if ("ja-JP".equals(langEntry.getKey())) continue;
                        translations.put(langEntry.getKey(), String.valueOf(langEntry.getValue()));
                    }

                    dictionary.put(key, translations);
                }
            }

            log.info("国际化文件加载成功: {}，当前共 {} 条消息", filePath, dictionary.size());
        } catch (Exception e) {
            log.error("加载国际化文件失败: {}", filePath, e);
        }
    }

    public String getMessage(String code, String locale, Object[] args, String defaultMessage) {
        String normalizedLocale = normalizeLocale(locale);

        Map<String, String> translations = dictionary.get(code);
        if (translations == null) {
            log.debug("未找到消息码: {} (locale: {})", code, normalizedLocale);
            return defaultMessage != null ? defaultMessage : code;
        }

        String pattern = resolvePattern(translations, normalizedLocale);
        if (pattern == null) {
            return defaultMessage != null ? defaultMessage : code;
        }

        if (args == null || args.length == 0) {
            return pattern;
        }

        try {
            return new MessageFormat(pattern, parseJavaLocale(normalizedLocale)).format(args);
        } catch (Exception e) {
            log.warn("消息格式化失败: code={}, pattern={}", code, pattern, e);
            return pattern;
        }
    }

    public String getMessage(String code, Object[] args, String defaultMessage) {
        String locale = localeProvider.getSystemLocale();
        return getMessage(code, locale, args, defaultMessage);
    }

    public String getMessage(String code, String locale, Object... args) {
        return getMessage(code, locale, args, null);
    }

    public String getMessage(String code, Object... args) {
        return getMessage(code, args, null);
    }

    public String getMessageWithDefault(String code, String defaultMessage, Object... args) {
        return getMessage(code, args, defaultMessage);
    }

    public Map<String, Map<String, String>> getAllMessages() {
        return new HashMap<>(dictionary);
    }

    public boolean hasMessage(String code) {
        return dictionary.containsKey(code);
    }

    private String resolvePattern(Map<String, String> translations, String locale) {
        String pattern = translations.get(locale);
        if (pattern != null) {
            return pattern;
        }

        pattern = translations.get(DEFAULT_LOCALE);
        if (pattern != null) {
            return pattern;
        }

        for (String supportedLocale : SUPPORTED_LOCALES) {
            pattern = translations.get(supportedLocale);
            if (pattern != null) {
                return pattern;
            }
        }

        return translations.values().stream().findFirst().orElse(null);
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.isEmpty()) {
            return DEFAULT_LOCALE;
        }
        String normalized = locale.replace('_', '-');
        if (SUPPORTED_LOCALES.contains(normalized)) {
            return normalized;
        }
        String langOnly = normalized.split("-")[0];
        for (String supported : SUPPORTED_LOCALES) {
            if (supported.startsWith(langOnly)) {
                return supported;
            }
        }
        return DEFAULT_LOCALE;
    }

    private Locale parseJavaLocale(String localeStr) {
        if (localeStr == null || localeStr.isEmpty()) {
            return Locale.CHINA;
        }
        String[] parts = localeStr.replace('-', '_').split("_");
        if (parts.length >= 2) {
            return new Locale(parts[0], parts[1]);
        }
        return new Locale(parts[0]);
    }
}
