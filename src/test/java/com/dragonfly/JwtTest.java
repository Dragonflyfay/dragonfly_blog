package com.dragonfly;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.lang.invoke.VarHandle;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/6 17:10
 */
public class JwtTest {
    @Test
    public void testGen(){
        Map <String,Object> claims=new HashMap<>();
        claims.put("id",1);
        claims.put("username","小蜻蜓");
        //生成JWT令牌
        //
        String token= JWT.create()
                .withClaim("user",claims)//添加载荷
                .withExpiresAt(new Date(System.currentTimeMillis()+1000*60*60*12))//添加过期时间，12小时
                .sign(Algorithm.HMAC256("dragonfly"));//指定算法，配置密钥
        System.out.println(token);
    }
    @Test

    public void testParse(){
        //定义字符串，模拟用户传递过来的token
        String token="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                ".eyJ1c2VyIjp7ImlkIjoxLCJ1c2VybmFtZSI6IuWwj-icu-ickyJ9LCJleHAiOjE3NzcxNTI0NTR9" +
                ".FL1NEzT8sSQNMaoySOAXxcwYeJj-HNamFqwmLxZJewQ";
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256("dragonfly")).build();
        DecodedJWT decodeJWT = jwtVerifier.verify(token);//验证token，生成一个解析后的JWT对象
        Map<String, Claim> claims = decodeJWT.getClaims();//获取解析后所有的载荷
        System.out.println(claims.get("user"));

        //如果篡改
    }

}
