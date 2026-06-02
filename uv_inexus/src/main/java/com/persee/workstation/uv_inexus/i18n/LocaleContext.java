package com.persee.workstation.uv_inexus.i18n;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocaleContext {

    private static final InheritableThreadLocal<String> LOCALE_HOLDER = new InheritableThreadLocal<>();

    private static final String DEFAULT_LOCALE = "zh-CN";

    public static void setLocale(String locale) {
        if (locale == null || locale.isEmpty()) {
            locale = DEFAULT_LOCALE;
        }
        String normalized = locale.replace('_', '-');
        LOCALE_HOLDER.set(normalized);
    }

    public static String getLocale() {
        String locale = LOCALE_HOLDER.get();
        return locale != null ? locale : DEFAULT_LOCALE;
    }

    public static void clear() {
        LOCALE_HOLDER.remove();
    }

    public static <T> T executeWithLocale(String locale, java.util.concurrent.Callable<T> task) throws Exception {
        String previous = LOCALE_HOLDER.get();
        try {
            setLocale(locale);
            return task.call();
        } finally {
            restoreLocale(previous);
        }
    }

    public static void executeWithLocale(String locale, Runnable task) {
        String previous = LOCALE_HOLDER.get();
        try {
            setLocale(locale);
            task.run();
        } finally {
            restoreLocale(previous);
        }
    }

    private static void restoreLocale(String previous) {
        if (previous != null) {
            LOCALE_HOLDER.set(previous);
        } else {
            LOCALE_HOLDER.remove();
        }
    }
}
