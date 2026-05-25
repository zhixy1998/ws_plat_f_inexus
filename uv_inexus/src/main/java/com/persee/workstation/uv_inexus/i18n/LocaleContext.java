package com.persee.workstation.uv_inexus.i18n;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocaleContext {

    private static final ThreadLocal<String> LOCALE_HOLDER = new ThreadLocal<>();

    private static final String DEFAULT_LOCALE = "zh-CN";

    public static void setLocale(String locale) {
        if (locale == null || locale.isEmpty()) {
            locale = DEFAULT_LOCALE;
        }
        LOCALE_HOLDER.set(locale);
        log.debug("设置当前线程语言: {}", locale);
    }

    public static String getLocale() {
        String locale = LOCALE_HOLDER.get();
        return locale != null ? locale : DEFAULT_LOCALE;
    }

    public static void clear() {
        LOCALE_HOLDER.remove();
        log.debug("清除当前线程语言上下文");
    }

    public static <T> T executeWithLocale(String locale, java.util.concurrent.Callable<T> task) throws Exception {
        try {
            setLocale(locale);
            return task.call();
        } finally {
            clear();
        }
    }

    public static void executeWithLocale(String locale, Runnable task) {
        try {
            setLocale(locale);
            task.run();
        } finally {
            clear();
        }
    }
}
