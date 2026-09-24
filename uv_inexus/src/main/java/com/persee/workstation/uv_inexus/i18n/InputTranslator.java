package com.persee.workstation.uv_inexus.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
@Slf4j
public class InputTranslator {

    /** 型号词条 key 的判定模式：字母前缀+数字+可选字母后缀，如 T600A、F700、A500（见 yaml 仪器型号映射区块） */
    private static final Pattern MODEL_KEY_PATTERN = Pattern.compile("^[A-Z]{1,3}\\d+[A-Z]{0,3}$");

    @Autowired
    private I18nMessageSource messageSource;

    @Autowired
    private LocaleProvider localeProvider;

    private final Map<String, Map<String, String>> reverseIndex = new ConcurrentHashMap<>();

    /** 当前语言的国际版型号 → 原型号（如 en-US 下 TU600A → T600A），key 按长度降序排列，避免 TU600A 误匹配 TU600AS */
    private final Map<String, List<Map.Entry<String, String>>> modelRenamesByLocale = new ConcurrentHashMap<>();

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
        if (chinese != null) {
            return chinese;
        }
        // 组合文案回译：国际版型号 + 已翻译名称（如 "TU600A UV-Vis Spectrophotometer" → T600A紫外可见分光光度计）
        List<Map.Entry<String, String>> renames = modelRenamesByLocale.get(locale);
        if (renames == null || renames.isEmpty()) {
            return foreignText;
        }
        for (Map.Entry<String, String> rename : renames) {
            String intlModel = rename.getKey();
            String chineseModel = rename.getValue();
            if (foreignText.startsWith(intlModel)) {
                String remainder = foreignText.substring(intlModel.length());
                if (remainder.isEmpty()) {
                    return chineseModel;
                }
                remainder = remainder.replaceFirst("^\\s+", "");
                String restChinese = localeIndex.get(remainder);
                if (restChinese != null) {
                    return chineseModel + restChinese;
                }
            }
            // 型号位于末尾的语序（如俄语 "Диаграмма состояния TU600B"）
            if (foreignText.endsWith(intlModel)) {
                String head = foreignText.substring(0, foreignText.length() - intlModel.length());
                head = head.replaceFirst("\\s+$", "");
                String headChinese = localeIndex.get(head);
                if (headChinese != null) {
                    return headChinese + chineseModel;
                }
            }
        }
        return foreignText;
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
            buildModelRenames(locale, allMessages);
        }
    }

    /**
     * 构建当前语言的国际版型号 → 原型号映射（如 en-US 下 TU600A → T600A）。
     * 数据来源：字典中 key 形如 T600A 且当前语言译文与 key 不同的条目。
     */
    private void buildModelRenames(String locale, Map<String, Map<String, String>> allMessages) {
        List<Map.Entry<String, String>> renames = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : allMessages.entrySet()) {
            String key = entry.getKey();
            if (!MODEL_KEY_PATTERN.matcher(key).matches()) {
                continue;
            }
            String intlModel = entry.getValue().get(locale);
            if (intlModel != null && !intlModel.equals(key)) {
                renames.add(new AbstractMap.SimpleEntry<>(intlModel, key));
            }
        }
        renames.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        modelRenamesByLocale.put(locale, Collections.unmodifiableList(renames));
        log.info("InputTranslator 型号改写映射构建完成: locale={}, {} 条", locale, renames.size());
    }

    private boolean containsChinese(String value) {
        if (value == null) return false;
        for (char c : value.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') return true;
        }
        return false;
    }
}
