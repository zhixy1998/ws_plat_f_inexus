package com.persee.workstation.uv_inexus.i18n;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class OutputTranslator {

    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");
    /** 型号词条 key 的判定模式：字母前缀+数字+可选字母后缀，如 T600A、F700、A500（见 yaml 仪器型号映射区块） */
    private static final Pattern MODEL_KEY_PATTERN = Pattern.compile("^[A-Z]{1,3}\\d+[A-Z]{0,3}$");
    private static final Set<String> SKIP_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "taskId", "sessionId", "type", "action", "command",
            "code", "id", "itemId", "deviceSerialNumber", "templateCode",
            "params", "method", "result", "beforeData", "afterData",
            // 测量类型字段：值为中文（光度测量/定量测定/光谱扫描/时间扫描），前端以中文作为匹配 key，不能翻译
            "measureType", "methodType", "measurementType", "measureMode",
            // 统计接口按测量类型分组的结果 map：中文 key 是前端匹配 key，不能翻译
            "taskTypeCounts"
    )));

    @Autowired
    private I18nMessageSource messageSource;

    @Autowired
    private LocaleProvider localeProvider;

    /** 缓存排序后的字典条目，避免每次翻译都重新排序 */
    private volatile List<Map.Entry<String, Map<String, String>>> sortedDictEntries;
    private volatile int cachedDictSize = -1;

    /** 缓存型号改写映射（[中文版型号, 当前语言型号]），按语言分组，字典变化时自动重建 */
    private volatile Map<String, List<String[]>> deviceModelRenames = new ConcurrentHashMap<>();
    private volatile int cachedModelRenameSize = -1;

    public JSONObject translate(JSONObject json) {
        if (json == null) {
            return null;
        }
        String locale = localeProvider.getSystemLocale();
        if (isDefaultLocale(locale)) {
            return json;
        }
        return translateFastJsonObject(json, locale);
    }

    public JSONArray translate(JSONArray array) {
        if (array == null) {
            return null;
        }
        String locale = localeProvider.getSystemLocale();
        if (isDefaultLocale(locale)) {
            return array;
        }
        return translateFastJsonArray(array, locale);
    }

    public Map<String, Object> translate(Map<String, Object> map) {
        return translate(map, Collections.emptySet());
    }

    public Map<String, Object> translate(Map<String, Object> map, Set<String> dynamicSkipKeys) {
        if (map == null) {
            return null;
        }
        String locale = localeProvider.getSystemLocale();
        if (isDefaultLocale(locale)) {
            return map;
        }
        return translateMap(map, locale, dynamicSkipKeys);
    }

    public List<Object> translate(List<Object> list) {
        return translate(list, Collections.emptySet());
    }

    public List<Object> translate(List<Object> list, Set<String> dynamicSkipKeys) {
        if (list == null) {
            return null;
        }
        String locale = localeProvider.getSystemLocale();
        if (isDefaultLocale(locale)) {
            return list;
        }
        return translateList(list, locale, dynamicSkipKeys);
    }

    public String translateString(String text) {
        if (text == null || !containsChinese(text)) {
            return text;
        }
        String locale = localeProvider.getSystemLocale();
        if (isDefaultLocale(locale)) {
            return text;
        }
        return translateText(text, locale);
    }

    /**
     * 统一的文本翻译方法：先精确匹配，再剥离非中文前缀匹配，最后做子串替换。
     * 所有内部翻译字符串的地方都应调用此方法。
     */
    private String translateText(String text, String locale) {
        // 1. 精确匹配
        String translated = messageSource.getMessage(text, locale);
        if (!text.equals(translated)) {
            return translated;
        }
        // 2. 前缀剥离：处理"型号前缀+仪器名称"的拼接文案（如 T600A紫外可见分光光度计、T700固体样品池），
        //    剥离开头非中文前缀后对剩余部分精确匹配；前缀若配置了型号映射（如 T600A→TU600A）则一并改写，
        //    未配置时前缀原样保留
        int cjkIndex = indexOfChinese(text);
        if (cjkIndex > 0) {
            String prefix = text.substring(0, cjkIndex);
            String rest = text.substring(cjkIndex);
            String restTranslated = messageSource.getMessage(rest, locale);
            if (!rest.equals(restTranslated)) {
                String prefixTranslated = messageSource.getMessage(prefix, locale);
                return prefixTranslated + " " + restTranslated;
            }
        }
        // 3. 子串替换：按key长度降序排列，优先匹配更长的中文片段
        String result = text;
        for (Map.Entry<String, Map<String, String>> entry : getSortedDictEntries()) {
            String key = entry.getKey();
            if (!containsChinese(key) || key.length() < 2) {
                continue;
            }
            if (!result.contains(key)) {
                continue;
            }
            Map<String, String> translations = entry.getValue();
            String translatedVal = translations.get(locale);
            if (translatedVal == null) {
                translatedVal = translations.get("zh-CN");
            }
            if (translatedVal != null && !key.equals(translatedVal)) {
                result = result.replace(key, translatedVal);
            }
        }
        // 4. 型号改写：将 T600A 等原型号按词边界替换为当前语言的型号写法（如 TU600A）
        return renameDeviceModels(result, locale);
    }

    /**
     * 将文本中的原型号（T600A 等）替换为当前语言的型号写法（如 TU600A）。
     * 型号映射来自字典中 key 形如 T600A 的条目（见 i18n-ui-texts.yaml 仪器型号映射区块）。
     */
    private String renameDeviceModels(String text, String locale) {
        List<String[]> renames = getDeviceModelRenames(locale);
        if (renames.isEmpty()) {
            return text;
        }
        String result = text;
        for (String[] rename : renames) {
            if (result.contains(rename[0])) {
                result = result.replaceAll("\\b" + Pattern.quote(rename[0]) + "\\b",
                        Matcher.quoteReplacement(rename[1]));
            }
        }
        return result;
    }

    /**
     * 获取当前语言的型号改写映射 [原型号, 国际版型号]，按型号长度降序排列（带缓存，字典变化时自动重建）。
     */
    private List<String[]> getDeviceModelRenames(String locale) {
        Map<String, Map<String, String>> all = messageSource.getAllMessages();
        if (cachedModelRenameSize != all.size()) {
            deviceModelRenames = new ConcurrentHashMap<>();
            cachedModelRenameSize = all.size();
        }
        List<String[]> cached = deviceModelRenames.get(locale);
        if (cached != null) {
            return cached;
        }
        List<String[]> renames = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : all.entrySet()) {
            String key = entry.getKey();
            if (!MODEL_KEY_PATTERN.matcher(key).matches()) {
                continue;
            }
            // 与 resolvePattern 的语言回退规则一致：当前语言缺失时回退 zh-CN
            String value = entry.getValue().get(locale);
            if (value == null) {
                value = entry.getValue().get("zh-CN");
            }
            if (value != null && !value.equals(key)) {
                renames.add(new String[]{key, value});
            }
        }
        renames.sort((a, b) -> Integer.compare(b[0].length(), a[0].length()));
        cached = Collections.unmodifiableList(renames);
        deviceModelRenames.put(locale, cached);
        return cached;
    }

    /**
     * 获取按长度降序排列的字典条目（带缓存）。
     * 当字典大小变化时自动重建缓存。
     */
    private List<Map.Entry<String, Map<String, String>>> getSortedDictEntries() {
        Map<String, Map<String, String>> all = messageSource.getAllMessages();
        if (sortedDictEntries == null || cachedDictSize != all.size()) {
            List<Map.Entry<String, Map<String, String>>> entries = new ArrayList<>(all.entrySet());
            entries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
            sortedDictEntries = Collections.unmodifiableList(entries);
            cachedDictSize = all.size();
        }
        return sortedDictEntries;
    }

    private Map<String, Object> translateMap(Map<String, Object> map, String locale, Set<String> dynamicSkipKeys) {
        Map<String, Object> result = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            result.put(key, translateJavaValue(key, value, locale, dynamicSkipKeys));
        }
        return result;
    }

    private Object translateJavaValue(String key, Object value, String locale, Set<String> dynamicSkipKeys) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String str = (String) value;
            if (shouldTranslate(key, str, dynamicSkipKeys)) {
                return translateText(str, locale);
            }
            return str;
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> childMap = (Map<String, Object>) value;
            return translateMap(childMap, locale, dynamicSkipKeys);
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> childList = (List<Object>) value;
            return translateList(childList, locale, dynamicSkipKeys);
        }
        return value;
    }

    private List<Object> translateList(List<Object> list, String locale, Set<String> dynamicSkipKeys) {
        List<Object> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> childMap = (Map<String, Object>) item;
                result.add(translateMap(childMap, locale, dynamicSkipKeys));
            } else if (item instanceof String) {
                String str = (String) item;
                if (containsChinese(str)) {
                    result.add(translateText(str, locale));
                } else {
                    result.add(str);
                }
            } else if (item instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> childList = (List<Object>) item;
                result.add(translateList(childList, locale, dynamicSkipKeys));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    private JSONObject translateFastJsonObject(JSONObject json, String locale) {
        JSONObject result = new JSONObject();
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            result.put(key, translateFastJsonValue(key, value, locale));
        }
        return result;
    }

    private Object translateFastJsonValue(String key, Object value, String locale) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String str = (String) value;
            if (shouldTranslate(key, str)) {
                return translateText(str, locale);
            }
            return str;
        }
        if (value instanceof JSONObject) {
            return translateFastJsonObject((JSONObject) value, locale);
        }
        if (value instanceof JSONArray) {
            return translateFastJsonArray((JSONArray) value, locale);
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            JSONObject jsonObj = new JSONObject(map);
            return translateFastJsonObject(jsonObj, locale);
        }
        if (value instanceof List) {
            JSONArray jsonArr = new JSONArray((List<?>) value);
            return translateFastJsonArray(jsonArr, locale);
        }
        if (isComplexObject(value)) {
            try {
                Object converted = com.alibaba.fastjson2.JSON.toJSON(value);
                if (converted instanceof JSONObject) {
                    return translateFastJsonObject((JSONObject) converted, locale);
                }
                if (converted instanceof JSONArray) {
                    return translateFastJsonArray((JSONArray) converted, locale);
                }
            } catch (Exception e) {
                log.debug("对象转换跳过: {}", e.getMessage());
            }
        }
        return value;
    }

    private boolean isComplexObject(Object value) {
        return !(value instanceof Number || value instanceof Boolean || value instanceof Date
                || value instanceof java.time.temporal.Temporal || value.getClass().isPrimitive());
    }

    private JSONArray translateFastJsonArray(JSONArray array, String locale) {
        JSONArray result = new JSONArray(array.size());
        for (int i = 0; i < array.size(); i++) {
            Object item = array.get(i);
            if (item instanceof JSONObject) {
                result.add(translateFastJsonObject((JSONObject) item, locale));
            } else if (item instanceof String) {
                String str = (String) item;
                if (containsChinese(str)) {
                    result.add(translateText(str, locale));
                } else {
                    result.add(str);
                }
            } else if (item instanceof JSONArray) {
                result.add(translateFastJsonArray((JSONArray) item, locale));
            } else if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) item;
                result.add(translateFastJsonObject(new JSONObject(map), locale));
            } else if (item instanceof List) {
                result.add(translateFastJsonArray(new JSONArray((List<?>) item), locale));
            } else if (item != null && isComplexObject(item)) {
                try {
                    Object converted = com.alibaba.fastjson2.JSON.toJSON(item);
                    if (converted instanceof JSONObject) {
                        result.add(translateFastJsonObject((JSONObject) converted, locale));
                    } else if (converted instanceof JSONArray) {
                        result.add(translateFastJsonArray((JSONArray) converted, locale));
                    } else {
                        result.add(item);
                    }
                } catch (Exception e) {
                    result.add(item);
                }
            } else {
                result.add(item);
            }
        }
        return result;
    }

    private boolean shouldTranslate(String key, String value) {
        return shouldTranslate(key, value, Collections.emptySet());
    }

    private boolean shouldTranslate(String key, String value, Set<String> dynamicSkipKeys) {
        if (SKIP_KEYS.contains(key) || (dynamicSkipKeys != null && dynamicSkipKeys.contains(key))) {
            return false;
        }
        return containsChinese(value);
    }

    private boolean containsChinese(String value) {
        return value != null && CJK_PATTERN.matcher(value).find();
    }

    /**
     * 返回首个中文字符的下标，无中文时返回 -1。
     */
    private int indexOfChinese(String value) {
        if (value == null) {
            return -1;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '\u4e00' && c <= '\u9fff') {
                return i;
            }
        }
        return -1;
    }

    private boolean isDefaultLocale(String locale) {
        return locale == null || "zh-CN".equals(locale) || "zh_CN".equals(locale);
    }
}
