package com.persee.workstation.uv_inexus.i18n;

/**
 * 语言上下文 - 使用 ThreadLocal 存储当前用户的语言设置
 */
public class LocaleContext {

    private static final ThreadLocal<String> LOCALE_HOLDER = new ThreadLocal<>();

    private static final String DEFAULT_LOCALE = "zh_CN";

    /**
     * 设置当前语言
     */
    public static void setLocale(String locale) {
        if (locale == null || locale.isEmpty()) {
            locale = DEFAULT_LOCALE;
        }
        LOCALE_HOLDER.set(locale);
    }

    /**
     * 获取当前语言
     */
    public static String getLocale() {
        String locale = LOCALE_HOLDER.get();
        return locale != null ? locale : DEFAULT_LOCALE;
    }

    /**
     * 清除当前语言
     */
    public static void clear() {
        LOCALE_HOLDER.remove();
    }
}
