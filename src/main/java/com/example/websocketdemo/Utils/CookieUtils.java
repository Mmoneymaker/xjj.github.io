package com.example.websocketdemo.Utils;

import cn.hutool.core.lang.UUID;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.websocketdemo.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;


@Component
public class CookieUtils {

    /**
     * 创建用户专用的 Cookie（使用用户名作为Cookie名称前缀）
     */
    public static Cookie CreateUserCookie(String token, String username) {
        try {
            // 从 Token 中提取用户名（如果没有，则直接使用传入的 username）
            String extractedUsername = extractUsernameFromToken(token);
            String cookieUsername = extractedUsername != null ? extractedUsername : username;

            // 使用用户名作为Cookie名称前缀，避免冲突
            Cookie cookie = new Cookie("authToken_" + cookieUsername, token);
            cookie.setPath("/"); // 使用根路径，但Cookie名称是唯一的
            cookie.setHttpOnly(false); // httponly会让js无法读取到cookie信息
            cookie.setMaxAge(24 * 60 * 60); // 24小时
            return cookie;
        } catch (Exception e) {
            // 如果解析失败，使用通用Cookie名称
            Cookie cookie = new Cookie("authToken", token);
            cookie.setPath("/");
            cookie.setHttpOnly(false);
            cookie.setMaxAge(24 * 60 * 60);
            return cookie;
        }
    }

    /**
     * 获取当前用户的 Cookie（通过查找用户特定的Cookie名称）
     */
//    public static String GetUserCookie(HttpServletRequest request) {
//        String[] cookies = request.getCookies();
//        if (cookies == null) {
//            return null;
//        }
//
//        // 1. 优先查找用户特定的Cookie（格式：authToken_用户名）
//        for (Cookie cookie : cookies) {
//            String cookieName = cookie.getName();
//            if (cookieName.startsWith("authToken_") && cookieName.length() > 10) {
//                return cookie.getValue(); // 找到用户特定Cookie，直接返回
//            }
//        }
//
//        // 2. 回退到通用Cookie
//        return GetCookie(request);
//    }

    /**
     * 通用的 Cookie 获取方法（根路径）
     */
    public static String GetCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String token = null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("authToken")) {
                token = cookie.getValue();
            }
        }
        return token;
    }

    /**
     * 从 JWT Token 中提取用户名
     */
    private static String extractUsernameFromToken(String token) {
        try {
            // 这里需要解析 JWT Token，但为了简单起见，我们假设 Token 中直接包含用户名
            // 在实际项目中，应该使用 TokenUtils.getUsernameFromJwtToken(token)
            // 暂时返回 null，表示无法从 Token 提取
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String GenerateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
