package com.persee.workstation.uv_inexus.config;

import com.persee.workstation.uv_inexus.i18n.LocaleContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 语言拦截器 - 从请求头或参数中读取语言设置
 */
@Component
public class LocaleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 优先从请求头读取
        String locale = request.getHeader("Accept-Language");

        // 如果请求头没有，从参数读取
        if (locale == null || locale.isEmpty()) {
            locale = request.getParameter("locale");
        }

        // 设置到上下文
        if (locale != null && !locale.isEmpty()) {
            LocaleContext.setLocale(locale);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除，避免内存泄漏
        LocaleContext.clear();
    }
}
