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

package com.persee.workstation.uv_inexus.util.tokenUtil;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

import java.util.Date;

public class JWTUtils {
    private static final String KEY = "px_iNexus";

    //接受用户信息，返回生成的token
    public static String getToken(String userName){

        return JWT.create()
                .withIssuer("auth0")
                .withClaim("userName",userName)
                .withExpiresAt(new Date(System.currentTimeMillis()+1000*60*60*99))//设置有效期
                .sign(Algorithm.HMAC256(KEY));
    }


    /**
     * 签名验证
     */
    public static boolean verify(String token){

        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(KEY)).withIssuer("auth0").build();
            DecodedJWT jwt = verifier.verify(token);
//            System.out.println("认证通过：");
//            System.out.println("issuer: " + jwt.getIssuer());
//            System.out.println("username: " + jwt.getClaim("userName").asString());
//            System.out.println("过期时间：      " + jwt.getExpiresAt());
            // 存储到UserContext（供日志切面使用）
            String userName = jwt.getClaim("userName").asString();
            LoginUserInfoVO userVO = new LoginUserInfoVO();
            userVO.setUsername(userName); // 设置用户名
            UserContext.setCurrentUser(userVO); // 存入ThreadLocal
            
            return true;
        } catch (Exception e) {
            // 验证失败时清除可能的残留信息
            UserContext.clear();
            return false;
        }

    }
}
