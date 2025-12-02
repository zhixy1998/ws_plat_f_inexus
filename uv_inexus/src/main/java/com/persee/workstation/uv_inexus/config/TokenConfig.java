/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
        excludePath.add("/com/persee/workstation/uv-inexus/systemManage/changeLanguage");
        excludePath.add("/com/persee/workstation/uv-inexus/systemManage/queryLanguage");
        excludePath.add("/com/persee/workstation/uv-inexus/systemManage/queryLoginSwitch");
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
