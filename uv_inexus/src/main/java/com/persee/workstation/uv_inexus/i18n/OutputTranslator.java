package com.persee.workstation.uv_inexus.i18n;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
@Slf4j
public class OutputTranslator {

    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");
    private static final Set<String> SKIP_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "taskId", "sessionId", "type", "action", "command",
            "code", "id", "itemId", "deviceSerialNumber", "templateCode",
            "sheetName", "sheetData", "firstRowNum", "lastRowNum",
            "mergedRegions", "rows", "firstRow", "lastRow", "firstCol", "lastCol",
            "value", "row", "col"
    )));

    @Autowired
    private I18nMessageSource messageSource;

    @Autowired
    private LocaleProvider localeProvider;

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
        if (map == null) {
            return null;
        }
        String locale = localeProvider.getSystemLocale();
        if (isDefaultLocale(locale)) {
            return map;
        }
        return translateMap(map, locale);
    }

    public List<Object> translate(List<Object> list) {
        if (list == null) {
            return null;
        }
        String locale = localeProvider.getSystemLocale();
        if (isDefaultLocale(locale)) {
            return list;
        }
        return translateList(list, locale);
    }

    public String translateString(String text) {
        if (text == null || !containsChinese(text)) {
            return text;
        }
        String locale = localeProvider.getSystemLocale();
        if (isDefaultLocale(locale)) {
            return text;
        }
        String translated = messageSource.getMessage(text, locale);
        return text.equals(translated) ? text : translated;
    }

    private Map<String, Object> translateMap(Map<String, Object> map, String locale) {
        Map<String, Object> result = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            result.put(key, translateJavaValue(key, value, locale));
        }
        return result;
    }

    private Object translateJavaValue(String key, Object value, String locale) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String str = (String) value;
            if (shouldTranslate(key, str)) {
                String translated = messageSource.getMessage(str, locale);
                if (!str.equals(translated)) {
                    return translated;
                }
            }
            return str;
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> childMap = (Map<String, Object>) value;
            return translateMap(childMap, locale);
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> childList = (List<Object>) value;
            return translateList(childList, locale);
        }
        return value;
    }

    private List<Object> translateList(List<Object> list, String locale) {
        List<Object> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> childMap = (Map<String, Object>) item;
                result.add(translateMap(childMap, locale));
            } else if (item instanceof String) {
                String str = (String) item;
                if (containsChinese(str)) {
                    String translated = messageSource.getMessage(str, locale);
                    result.add(str.equals(translated) ? str : translated);
                } else {
                    result.add(str);
                }
            } else if (item instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> childList = (List<Object>) item;
                result.add(translateList(childList, locale));
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
                String translated = messageSource.getMessage(str, locale);
                if (!str.equals(translated)) {
                    return translated;
                }
            }
            return str;
        }
        if (value instanceof JSONObject) {
            return translateFastJsonObject((JSONObject) value, locale);
        }
        if (value instanceof JSONArray) {
            return translateFastJsonArray((JSONArray) value, locale);
        }
        return value;
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
                    String translated = messageSource.getMessage(str, locale);
                    result.add(str.equals(translated) ? str : translated);
                } else {
                    result.add(str);
                }
            } else if (item instanceof JSONArray) {
                result.add(translateFastJsonArray((JSONArray) item, locale));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    private boolean shouldTranslate(String key, String value) {
        if (SKIP_KEYS.contains(key)) {
            return false;
        }
        return containsChinese(value);
    }

    private boolean containsChinese(String value) {
        return value != null && CJK_PATTERN.matcher(value).find();
    }

    private boolean isDefaultLocale(String locale) {
        return locale == null || "zh-CN".equals(locale) || "zh_CN".equals(locale);
    }
}
