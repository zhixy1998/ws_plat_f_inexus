package com.persee.workstation.uv_inexus.config.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * 一个用于在线程内部传递 @LogPoint 额外加密数据的上下文持有者。
 * <p>
 * 该工具类使用 {@link ThreadLocal} 来确保在一个线程的执行过程中（例如，从业务方法到AOP切面），
 * 可以安全地传递和获取数据，而不会受到其他线程的干扰。
 * <p>
 * <b>使用方法:</b>
 * <ol>
 *   <li>在任何被 {@code @LogPoint} 注解的方法内部，当你需要记录一些动态的、想要加密的数据时，调用 {@link #put(String, Object)}。</li>
 *   <li>{@code LogPointAspect} 切面会自动在方法执行后从此上下文中提取所有数据，并将其合并到主加密日志中。</li>
 *   <li>切面会确保在每次操作后调用 {@link #clear()} 方法，以防止内存泄漏。</li>
 * </ol>
 */
public final class LogPointContextHolder {

    private static final ThreadLocal<Map<String, Object>> CONTEXT = new ThreadLocal<>();

    private LogPointContextHolder() {
        // 私有构造函数，防止实例化
    }

    /**
     * 向当前线程的日志上下文中存入一个键值对。
     * <p>
     * 如果多次使用相同的key，后存入的值会覆盖之前的值。
     *
     * @param key   数据的键
     * @param value 要存储的数据对象
     */
    public static void put(String key, Object value) {
        if (CONTEXT.get() == null) {
            CONTEXT.set(new HashMap<>());
        }
        CONTEXT.get().put(key, value);
    }

    /**
     * 获取当前线程日志上下文中的所有数据。
     * <p>
     * 此方法主要由AOP切面在内部使用。
     *
     * @return 一个包含所有上下文数据的Map，如果没有数据则可能返回null。
     */
    public static Map<String, Object> getContext() {
        return CONTEXT.get();
    }

    /**
     * 清除当前线程的所有日志上下文数据。
     * <p>
     * 此方法对于防止内存泄漏至关重要，通常在AOP切面的 finally 块中调用。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
