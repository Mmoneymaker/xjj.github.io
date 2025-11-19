package com.example.websocketdemo.Utils;

import cn.hutool.core.lang.UUID;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;


public class CookieUtils {

    public static Cookie CreateCookie(String token){
        Cookie cookie=new Cookie("authToken",token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);//httponly会让js无法读取到cookie信息
        cookie.setSecure(true);
        cookie.setMaxAge(3600);//cookie存活时间
        return cookie;
    }

    public static String GetCookie(HttpServletRequest request){
        Cookie[] cookies=request.getCookies();
        String token=null;
        for(Cookie cookie:cookies){
            if(cookie.getName().equals("authToken")){
                token=cookie.getValue();
                System.out.println("GetCookie方法中的token:"+token);
            }
        }
        return token;
    }

    public static String CreateToken(){
        String token= UUID.randomUUID().toString(true);
        System.out.println("服务端生成的token:"+token);
        return token;
    }

    public static String GenerateToken(){
        byte[] bytes=new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
