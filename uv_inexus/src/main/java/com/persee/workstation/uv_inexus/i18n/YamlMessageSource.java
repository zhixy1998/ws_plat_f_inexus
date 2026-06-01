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
* YAML 消息源，支持多语言（zh-CN / en-US / ja-JP）。

 * 加载 classpath 下的：
 * - i18n/messages.yaml
 *
 * YAML 结构示例：
 * messages:
 *   '00000001':
 *     zh-CN: '用户不存在'
 *     en-US: 'User does not exist'
 *     ja-JP: 'ユーザーが存在しません'
 */
@Component
public class YamlMessageSource {

    private static final String DICT_FILE = "i18n/i18n-dictionary.yaml";
    private static final String MSG_FILE_ZH_CN = "i18n/messages_zh_CN.yaml";
    private static final String DEFAULT_LOCALE = "zh-CN";

    // 缓存：key -> {locale -> text}
    private Map<String, Map<String, String>> dictionary = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadDictionary();
        loadMessagesZhCN();
        System.out.println("调试信息：YamlMessageSource 初始化完成，共加载 " + dictionary.size() + " 条消息");
    }

    /**
     * 加载字典文件（多语言）
     */
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

            System.out.println("调试信息：成功从 i18n-dictionary.yaml 加载 " + dictionary.size() + " 条消息");
        } catch (Exception ex) {
            System.out.println("调试信息：加载 YAML 时发生异常：" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * 加载 messages_zh_CN.yaml（扁平结构，key直接对应中文文本）
     * 将其内容以 zh-CN locale 写入 dictionary，覆盖 dictionary 中已有的同 key
     */
    @SuppressWarnings("unchecked")
    private void loadMessagesZhCN() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(MSG_FILE_ZH_CN)) {
            if (inputStream == null) {
                System.out.println("调试信息：未找到 YAML 文件：" + MSG_FILE_ZH_CN);
                return;
            }

            System.out.println("调试信息：找到 YAML 文件并开始加载：" + MSG_FILE_ZH_CN);
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
            int count = 0;

            for (Map.Entry<String, Object> entry : messages.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // 扁平结构：value 是字符串，直接作为 zh-CN 的翻译
                if (value instanceof String) {
                    Map<String, String> translations = dictionary.get(key);
                    if (translations == null) {
                        translations = new HashMap<>();
                        dictionary.put(key, translations);
                    }
                    translations.put("zh-CN", (String) value);
                    count++;
                }
            }

            System.out.println("调试信息：成功从 messages_zh_CN.yaml 加载 " + count + " 条中文消息");
        } catch (Exception ex) {
            System.out.println("调试信息：加载 messages_zh_CN.yaml 时发生异常：" + ex.getMessage());
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
        String locale = normalizeLocale(LocaleContext.getLocale());
        return getMessage(code, locale, args, defaultMessage);
    }

    /**
     * 规范化 locale 字符串，将 zh_CN 转为 zh-CN 等格式
     */
    private String normalizeLocale(String locale) {
        if (locale == null || locale.isEmpty()) {
            return DEFAULT_LOCALE;
        }
        // zh_CN -> zh-CN
        return locale.replace('_', '-');
    }

    /**
     * 根据消息码和语言获取消息
     * @param code 消息码
     * @param locale 语言（zh-CN / en-US / ja-JP）
     * @param args 占位符参数
     * @param defaultMessage 默认消息
     */
    public String getMessage(String code, String locale, Object[] args, String defaultMessage) {
        locale = normalizeLocale(locale);
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
