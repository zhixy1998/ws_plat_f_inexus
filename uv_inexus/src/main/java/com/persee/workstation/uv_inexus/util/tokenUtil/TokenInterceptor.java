package com.persee.workstation.uv_inexus.util.tokenUtil;


import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class TokenInterceptor implements HandlerInterceptor{
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,Object handler)throws Exception {

        if (request.getMethod().equals("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        response.setCharacterEncoding("utf-8");

        String token = request.getHeader("Token");
        System.out.println("验证token");
        if (token != null) {
            boolean result = JWTUtils.verify(token);//验证token
            if (result) {
                System.out.println("token验证通过");
                return true;
            }
        }
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        try {
            JSONObject json = new JSONObject();
            json.put("success", "false");
            json.put("message", "验证失败，请重新登录");
            json.put("code", ResultCodeEnum.TOKEN_ERROR.getCode());
            response.getWriter().append(json.toString());
            System.out.println("认证失败，未通过拦截器");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500);
            return false;
        }
        return false;
    }
}
