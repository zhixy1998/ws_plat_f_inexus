/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.i18n;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YAML 消息源，支持多语言（zh_CN / en_US / ja_JP）。
 *
 * 加载 classpath 下的：
 * - i18n/messages.yaml
 *
 * YAML 结构示例：
 * messages:
 *   '00000001':
 *     zh_CN: '用户不存在'
 *     en_US: 'User does not exist'
 *     ja_JP: 'ユーザーが存在しません'
 */
@Component
@Deprecated
public class YamlMessageSource {

    private static final String DICT_FILE = "i18n/i18n-dictionary.yaml";
    private static final String DEFAULT_LOCALE = "zh_CN";

    // 缓存：key -> {locale -> text}
    private Map<String, Map<String, String>> dictionary = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadDictionary();
        System.out.println("调试信息：YamlMessageSource 初始化完成，共加载 " + dictionary.size() + " 条消息");
    }

    /**
     * 加载字典文件
     */
    // ... existing code ...
    @SuppressWarnings("unchecked")
    private void loadDictionary() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(DICT_FILE)) {
            if (inputStream == null) {
                System.out.println("调试信息：未找到 YAML 文件：" + DICT_FILE);
                return;
            }

            System.out.println("调试信息：找到 YAML 文件并开始加载：" + DICT_FILE);
            Yaml yaml = new Yaml();
            Object rootObj = yaml.load(inputStream);

            if (!(rootObj instanceof Map)) {
                System.out.println("调试信息：YAML 根节点不是 Map 类型");
                return;
            }

            Map<String, Object> root = (Map<String, Object>) rootObj;
            Object messagesObj = root.get("messages");

            if (!(messagesObj instanceof Map)) {
                System.out.println("调试信息：'messages' 节点不是 Map 类型");
                return;
            }

            Map<String, Object> messages = (Map<String, Object>) messagesObj;

            System.out.println("调试信息：开始遍历消息，共 " + messages.size() + " 条");

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

                    // 打印特定消息码的加载情况
                    if ("00608001".equals(key)) {
                        System.out.println("调试信息：成功加载消息码 00608001: " + translations);
                    }
                }
            }

            System.out.println("调试信息：成功从 YAML 加载 " + dictionary.size() + " 条消息");

            // 验证 00608001 是否在字典中
            if (dictionary.containsKey("00608001")) {
                System.out.println("调试信息：✓ 消息码 00608001 已加载到字典中");
            } else {
                System.out.println("调试信息：✗ 消息码 00608001 未在字典中找到！");
            }
        } catch (Exception ex) {
            System.out.println("调试信息：加载 YAML 时发生异常：" + ex.getMessage());
            ex.printStackTrace();
        }
    }
// ... existing code ...


    /**
     * 根据消息码获取消息（自动使用当前线程的语言设置）
     * @param code 消息码
     * @param args 占位符参数
     * @param defaultMessage 默认消息
     */
    public String getMessage(String code, Object[] args, String defaultMessage) {
        String locale = LocaleContext.getLocale();
        return getMessage(code, locale, args, defaultMessage);
    }

    /**
     * 根据消息码和语言获取消息
     * @param code 消息码
     * @param locale 语言（zh_CN / en_US / ja_JP）
     * @param args 占位符参数
     * @param defaultMessage 默认消息
     */
    public String getMessage(String code, String locale, Object[] args, String defaultMessage) {
        if (locale == null || locale.isEmpty()) {
            locale = DEFAULT_LOCALE;
        }

        Map<String, String> translations = dictionary.get(code);
        if (translations == null) {
            return defaultMessage != null ? defaultMessage : code;
        }

        String pattern = translations.get(locale);
        if (pattern == null) {
            pattern = translations.get(DEFAULT_LOCALE);
        }
        if (pattern == null) {
            return defaultMessage != null ? defaultMessage : code;
        }

        if (args == null || args.length == 0) {
            return pattern;
        }

        try {
            MessageFormat mf = new MessageFormat(pattern, Locale.CHINA);
            return mf.format(args);
        } catch (Exception e) {
            return pattern;
        }
    }

    /**
     * 便捷重载：不提供默认消息时，未命中则返回 code
     */
    public String getMessage(String code, Object... args) {
        return getMessage(code, args, null);
    }

    /**
     * 便捷重载：指定语言
     */
    public String getMessage(String code, String locale, Object... args) {
        return getMessage(code, locale, args, null);
    }
}
