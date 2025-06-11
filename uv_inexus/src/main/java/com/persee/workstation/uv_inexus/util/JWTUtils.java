package com.persee.workstation.uv_inexus.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;
import java.util.Map;

public class JWTUtils {
    private static final String KEY = "yzx_zxx";

    //接受用户信息，返回生成的token
    public static String getToken(Map<String, Object> claims){
        return JWT.create()
                .withClaim("claims",claims)
                .withExpiresAt(new Date(System.currentTimeMillis()+1000*60*60*12))//设置有效期
                .sign(Algorithm.HMAC256(KEY));
    }

    //接收并验证token，并返回token所携带的信息
    public static Map<String,Object> parseToken(String token){
        return JWT.require(Algorithm.HMAC256(KEY))
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();

    }
}
