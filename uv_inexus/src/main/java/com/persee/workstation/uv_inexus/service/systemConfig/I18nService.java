package com.persee.workstation.uv_inexus.service.systemConfig;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class I18nService {

    private static final String DICT_FILE = "i18n/i18n-dictionary.yaml";

    // 缓存：key -> {locale -> text}
    private Map<String, Map<String, String>> dictionary = new ConcurrentHashMap<>();

    // 默认语言
    private static final String DEFAULT_LOCALE = "en-US";

    @PostConstruct
    public void init() {
        loadDictionary();
    }

    /**
     * 加载字典文件
     */
    private void loadDictionary() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(DICT_FILE)) {
            if (inputStream == null) {
                log.error("国际化字典文件不存在: {}", DICT_FILE);
                return;
            }

            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(inputStream);

            if (data != null && data.containsKey("messages")) {
                Map<String, Object> messages = (Map<String, Object>) data.get("messages");

                for (Map.Entry<String, Object> entry : messages.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof Map) {
                        Map<String, String> translations = new HashMap<>();
                        Map<String, Object> langMap = (Map<String, Object>) value;

                        for (Map.Entry<String, Object> langEntry : langMap.entrySet()) {
                            translations.put(langEntry.getKey(), String.valueOf(langEntry.getValue()));
                        }

                        dictionary.put(key, translations);
                    }
                }

                log.info("国际化字典加载成功，共 {} 条消息", dictionary.size());
            }
        } catch (Exception e) {
            log.error("加载国际化字典失败", e);
        }
    }

    /**
     * 获取国际化文本
     * @param key 键
     * @param locale 语言（zh-CN / en-US / ja-JP）
     * @param args 占位符参数
     */
    public String get(String key, String locale, Object... args) {
        if (locale == null || locale.isEmpty()) {
            locale = DEFAULT_LOCALE;
        }

        Map<String, String> translations = dictionary.get(key);
        if (translations == null) {
            return key;
        }

        String text = translations.get(locale);
        if (text == null) {
            text = translations.get(DEFAULT_LOCALE);
        }
        if (text == null) {
            return key;
        }

        // 处理占位符
        if (args != null && args.length > 0) {
            try {
                return new MessageFormat(text, Locale.CHINA).format(args);
            } catch (Exception e) {
                log.warn("消息格式化失败: key={}, text={}", key, text);
                return text;
            }
        }

        return text;
    }

    /**
     * 获取所有消息（用于前端初始化）
     */
    public Map<String, Map<String, String>> getAllMessages() {
        return new HashMap<>(dictionary);
    }
}
