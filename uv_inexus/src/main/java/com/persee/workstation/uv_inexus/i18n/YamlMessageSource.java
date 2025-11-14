package com.persee.workstation.uv_inexus.i18n;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 简化版 YAML 消息源（仅使用 zh_CN 包），作为 Spring Bean 管理。
 *
 * 仅加载 classpath 下的：
 * - i18n/messages_zh_CN.yaml
 *
 * YAML 结构示例：
 * locale: zh_CN
 * messages:
 *   '00301017': '成功导入{0}个方法'
 */
@Component

public class YamlMessageSource {

    private static final String PATH_ZH_CN = "i18n/messages_zh_CN.yaml";
    private static final Locale LOCALE_ZH_CN = Locale.SIMPLIFIED_CHINESE;
    private static volatile Map<String, String> STATIC_MESSAGES;

    private Map<String, String> messages;

    @PostConstruct
    public void init() {
        this.messages = loadMessages(PATH_ZH_CN);
        // 调试信息：输出加载的消息数量
        System.out.println("DEBUG: YamlMessageSource initialized with " + (messages != null ? messages.size() : 0) + " messages");

    }

    /**
     * 根据消息码获取消息，并使用 MessageFormat 按 zh_CN 进行占位符格式化。
     * 若未找到消息，返回 defaultMessage（若为 null 则返回 code 本身）。
     */
    public String getMessage(String code, Object[] args, String defaultMessage) {
        String pattern = (messages != null) ? messages.get(code) : null;
        if (pattern == null) {
            return defaultMessage != null ? defaultMessage : code;
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        MessageFormat mf = new MessageFormat(pattern, LOCALE_ZH_CN);
        return mf.format(args);
    }

    /** 便捷重载：不提供默认消息时，未命中则返回 code。 */
    public String getMessage(String code, Object... args) {
        return getMessage(code, args, null);
    }

    public static String getMessageStatic(String code, Object[] args, String defaultMessage) {
        Map<String, String> m = STATIC_MESSAGES;
        if (m == null) {
            synchronized (YamlMessageSource.class) {
                if (STATIC_MESSAGES == null) {
                    STATIC_MESSAGES = loadMessagesStatic(PATH_ZH_CN);
                }
                m = STATIC_MESSAGES;
            }
        }
        String pattern = (m != null) ? m.get(code) : null;
        if (pattern == null) {
            return defaultMessage != null ? defaultMessage : code;
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        MessageFormat mf = new MessageFormat(pattern, LOCALE_ZH_CN);
        return mf.format(args);
    }

    public static String getMessageStatic(String code, Object... args) {
        return getMessageStatic(code, args, null);
    }

    /** 暴露原始消息（不可变），仅来自 zh_CN 包。 */
    public Map<String, String> getMessages() {
        return messages;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> loadMessages(String classpathLocation) {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (is == null) {
                System.out.println("DEBUG: YAML file not found at: " + classpathLocation);
                return Collections.emptyMap();
            }
            System.out.println("DEBUG: YAML file found and loading: " + classpathLocation);
            Yaml yaml = new Yaml();
            Object rootObj = yaml.load(is);
            if (!(rootObj instanceof Map)) {
                System.out.println("DEBUG: YAML root is not a Map");
                return Collections.emptyMap();
            }
            Map<String, Object> root = (Map<String, Object>) rootObj;
            Object messagesObj = root.get("messages");
            if (!(messagesObj instanceof Map)) {
                System.out.println("DEBUG: 'messages' section is not a Map");
                return Collections.emptyMap();
            }
            Map<String, Object> raw = (Map<String, Object>) messagesObj;
            Map<String, String> out = new LinkedHashMap<>(raw.size());
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                String k = String.valueOf(e.getKey());
                String v = String.valueOf(e.getValue());
                out.put(k, v);
            }
            System.out.println("DEBUG: Successfully loaded " + out.size() + " messages from YAML");
            return Collections.unmodifiableMap(out);
        } catch (Exception ex) {
            System.out.println("DEBUG: Exception while loading YAML: " + ex.getMessage());
            ex.printStackTrace();
            // 解析异常时返回空映射，避免影响主流程
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> loadMessagesStatic(String classpathLocation) {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (is == null) {
                return Collections.emptyMap();
            }
            Yaml yaml = new Yaml();
            Object rootObj = yaml.load(is);
            if (!(rootObj instanceof Map)) {
                return Collections.emptyMap();
            }
            Map<String, Object> root = (Map<String, Object>) rootObj;
            Object messagesObj = root.get("messages");
            if (!(messagesObj instanceof Map)) {
                return Collections.emptyMap();
            }
            Map<String, Object> raw = (Map<String, Object>) messagesObj;
            Map<String, String> out = new LinkedHashMap<>(raw.size());
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                String k = String.valueOf(e.getKey());
                String v = String.valueOf(e.getValue());
                out.put(k, v);
            }
            return Collections.unmodifiableMap(out);
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }
}