/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.tokenUtil;


import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class TokenInterceptor implements HandlerInterceptor{

    @Autowired
    SysUserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,Object handler)throws Exception {

        if (request.getMethod().equals("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        response.setCharacterEncoding("utf-8");

        // String token = request.getHeader("Token");
        // if (token != null) {
        //     boolean result = JWTUtils.verify(token);
        //     if (result) {
        //         return true;
        //     }
        // }

        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token == null || token.trim().isEmpty()) {
            token = request.getHeader("Token");
        }
        if (token != null && !token.trim().isEmpty()) {
            boolean result = JWTUtils.verify(token);
            if (result) {
                LoginUserInfoVO currentUser = UserContext.getCurrentUser();
                String username = currentUser.getUsername();
                SysUserPO sysUserPO = userMapper.selectByUsername(username);
                if (sysUserPO == null) {
                    JSONObject json = new JSONObject();
                    json.put("success", "false");
                    json.put("message", "当前账户已禁用！");
                    json.put("code", ResultCodeEnum.TOKEN_ERROR.getCode());
                    response.getWriter().append(json.toString());
                    return false;

                }
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

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
