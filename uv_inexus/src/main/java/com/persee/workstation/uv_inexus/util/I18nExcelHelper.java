package com.persee.workstation.uv_inexus.util;

import com.persee.workstation.uv_inexus.i18n.LocaleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Excel 导出国际化工具类
 * 用于翻译 Sheet 名称、表头列名等 Excel 中的中文文本
 */
@Component
public class I18nExcelHelper {

    @Autowired
    private I18nMessageUtil i18nMessageUtil;

    @Autowired
    private LocaleProvider localeProvider;

    /**
     * 翻译文本（Sheet名、表头等），如果是中文环境则返回原文
     */
    public String t(String chineseText) {
        return i18nMessageUtil.getMessageWithDefault(chineseText, chineseText);
    }

    /**
     * 批量翻译文本数组
     */
    public String[] t(String[] chineseTexts) {
        String[] result = new String[chineseTexts.length];
        for (int i = 0; i < chineseTexts.length; i++) {
            result[i] = t(chineseTexts[i]);
        }
        return result;
    }

    /**
     * 判断当前是否为中文环境（不需要翻译）
     */
    public boolean isChinese() {
        String locale = localeProvider.getSystemLocale();
        return locale == null || "zh-CN".equals(locale) || "zh_CN".equals(locale);
    }
}
