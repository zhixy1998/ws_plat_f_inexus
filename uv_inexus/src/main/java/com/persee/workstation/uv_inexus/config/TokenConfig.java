/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.config;

import com.persee.workstation.uv_inexus.util.tokenUtil.TokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class TokenConfig implements WebMvcConfigurer {
    private final TokenInterceptor tokenInterceptor;

    //构造方法
    public TokenConfig(TokenInterceptor tokenInterceptor){
        this.tokenInterceptor = tokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePath = new ArrayList<>();
        excludePath.add("/user_register"); //注册
        excludePath.add("/com/persee/workstation/uv-inexus/system/login"); //登录
        excludePath.add("/com/persee/workstation/uv-inexus/experimenttest/sys/obtainReportParameters");
        excludePath.add("/com/persee/workstation/uv-inexus/experimenttest/sys/reportDataQuery");
        excludePath.add("/com/persee/workstation/uv-inexus/experimenttest/sys/queryReportTableData");
        excludePath.add("/com/persee/workstation/uv-inexus/system/logout"); //登出
        excludePath.add("/static/**");  //静态资源
        excludePath.add("/assets/**");  //静态资源
        excludePath.add("/com/persee/workstation/uv-inexus/experimenttest/sys/concise/obtainReportParameters");
        excludePath.add("/com/persee/workstation/uv-inexus/experimenttest/sys/concise/reportDataQuery");
        excludePath.add("/com/persee/workstation/uv-inexus/experimenttest/sys/concise/queryReportTableData");


        registry.addInterceptor(tokenInterceptor)
                // .excludePathPatterns("/login", "/logout") // 排除不需要验证的接口
                .addPathPatterns("/com/persee/workstation/uv-inexus/**")
                .excludePathPatterns(excludePath);
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
