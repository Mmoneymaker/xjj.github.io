package com.example.websocketdemo.Utils;


import com.example.websocketdemo.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Date;

// TokenUtils.java
@Component
public class TokenUtils {

    private static final String SECRET_KEY = "your-secret-key";
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24小时

    public String generateJwtToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // 验证 JWT 的签名和过期时间
    public boolean validateJwtTokenFormat(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new BusinessException(401, "Token已过期");
        } catch (Exception e) {
            throw new BusinessException(401, "无效的Token");
        }
    }

    //从JWT提取用户名
    public String getUsernameFromJwtToken(String token) {
       try {Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }   catch (Exception e) {
       throw new BusinessException(401,"Token解析失败");
       }
    }

    // 从 HttpServletRequest 中获取用户名
    public static String getUsernameFromToken(HttpServletRequest request) {
        // 1. 先从 Authorization header 获取
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            TokenUtils tokenUtils = new TokenUtils();
            return tokenUtils.getUsernameFromJwtToken(token);
        }
        return null;
    }
}
