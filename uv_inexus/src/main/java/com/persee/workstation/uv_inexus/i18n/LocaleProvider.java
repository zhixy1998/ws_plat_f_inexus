package com.persee.workstation.uv_inexus.i18n;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemManageDAO;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemManagePO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class LocaleProvider {

    private static final String DEFAULT_LOCALE = "zh-CN";
    private static final long CACHE_REFRESH_INTERVAL_MS = 30_000;

    @Autowired
    private SystemManageDAO systemManageDAO;

    @Value("${sys-manage-default.defaultId}")
    private String defaultId;

    private final AtomicReference<String> cachedLocale = new AtomicReference<>(DEFAULT_LOCALE);
    private volatile long lastRefreshTime = 0;

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public String getSystemLocale() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime > CACHE_REFRESH_INTERVAL_MS) {
            refreshCache();
        }
        return cachedLocale.get();
    }

    public String getUserLocale(String userId) {
        return getSystemLocale();
    }

    public String resolveFromRequest(HttpServletRequest request) {
        return getSystemLocale();
    }

    @Scheduled(fixedDelay = CACHE_REFRESH_INTERVAL_MS)
    public void refreshCache() {
        try {
            SystemManagePO config = systemManageDAO.selectById(defaultId);
            if (config != null && StringUtils.isNotBlank(config.getLanguage())) {
                cachedLocale.set(normalize(config.getLanguage()));
            }
        } catch (Exception e) {
            log.error("获取系统语言配置失败", e);
        }
        lastRefreshTime = System.currentTimeMillis();
    }

    private String normalize(String locale) {
        if (locale == null || locale.isEmpty()) {
            return DEFAULT_LOCALE;
        }
        return locale.replace('_', '-');
    }
}
