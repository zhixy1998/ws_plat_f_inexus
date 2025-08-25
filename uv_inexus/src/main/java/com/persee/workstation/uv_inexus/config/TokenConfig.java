package com.persee.workstation.uv_inexus.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.persee.workstation.uv_inexus.util.tokenUtil.TokenInterceptor;

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
        excludePath.add("/com/persee/workstation/uv-inexus/experimenttest/sys/spectrumScanning/obtainReportParameters"); //登录
        excludePath.add("/com/persee/workstation/uv-inexus/system/logout"); //登出
        excludePath.add("/static/**");  //静态资源
        excludePath.add("/assets/**");  //静态资源
        excludePath.add("/com/persee/workstation/uv-inexus/experimenttest/sys/reportDataQuery");

        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/com/persee/workstation/uv-inexus/**")
                .excludePathPatterns(excludePath);
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
