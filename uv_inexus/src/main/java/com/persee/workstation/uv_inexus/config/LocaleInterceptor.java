package com.persee.workstation.uv_inexus.config;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemManageDAO;
import com.persee.workstation.uv_inexus.i18n.LocaleContext;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemManagePO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 语言拦截器 - 从数据库用户配置中读取语言
 */
@Component
public class LocaleInterceptor implements HandlerInterceptor {

    @Autowired
    SystemManageDAO systemManageDao;
    @Value("${sys-manage-default.defaultId}")
    String defaultId;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String locale = null;

        // 1. 获取当前登录用户的 ID (根据你的 TokenUtil 或 Session 获取实际 userId)
        // 假设你有一个工具类可以获取当前用户 ID，如果没有，请替换为你项目里的实际获取方式

        // 2. 直接查库获取用户信息（包含 language 字段）
        SystemManagePO user = systemManageDao.selectById(defaultId);
        if (user != null && StringUtils.isNotBlank(user.getLanguage())) {
            locale = user.getLanguage();
            // 在这里直接做转换：将数据库的代码转为前端友好的名称
            locale = convertToName(locale);
        }

//        // 3. 如果数据库没配，再看请求头（兼容未登录或旧数据）
//        if (StringUtils.isBlank(locale)) {
//            String headerLocale = request.getHeader("Accept-Language");
//            if (StringUtils.isNotBlank(headerLocale)) {
//                locale = headerLocale.split(",")[0].replace('-', '_');
//            }
//        }

        // 4. 兜底默认中文
        if (StringUtils.isBlank(locale)) {
            locale = "zh_CN";
        }

        LocaleContext.setLocale(locale);
        return true;
    }

    /**
     * 简易转换方法：en_US -> 英文
     */
    private String convertToName(String code) {
        if ("English".equals(code)) return "en_US";
        if ("日语".equals(code)) return "ja_JP";
        return "zh_CN"; // 默认返回中文
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LocaleContext.clear();
    }
}
