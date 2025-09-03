package com.persee.workstation.uv_inexus.util.tokenUtil;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

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
            System.out.println("认证通过：");
            System.out.println("issuer: " + jwt.getIssuer());
            System.out.println("username: " + jwt.getClaim("userName").asString());
            System.out.println("过期时间：      " + jwt.getExpiresAt());
            return true;
        } catch (Exception e){
            return false;
        }

    }
}
