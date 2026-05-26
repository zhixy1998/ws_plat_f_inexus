package com.persee.workstation.uv_inexus.config;

import com.persee.workstation.uv_inexus.i18n.LocaleContext;
import com.persee.workstation.uv_inexus.i18n.LocaleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LocaleInterceptor implements HandlerInterceptor {

    @Autowired
    private LocaleProvider localeProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String locale = localeProvider.resolveFromRequest(request);
        LocaleContext.setLocale(locale);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LocaleContext.clear();
    }
}
