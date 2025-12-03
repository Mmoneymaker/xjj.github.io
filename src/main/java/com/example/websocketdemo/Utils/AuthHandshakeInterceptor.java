package com.example.websocketdemo.Utils;

import com.example.websocketdemo.Service.TokenValidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    TokenValidateService tokenValidateService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

            // 前端要把 token 放在 URL 里: ws://localhost:8080/ws?token=xxxxx
            String token = servletRequest.getServletRequest().getParameter("token");
            System.out.println("token=="+token);
            if (token == null || token.isEmpty()) {
                System.out.println("握手失败：未携带Token");
                return false; // 直接拒绝连接，前端收到 HTTP 404 或 401
            }

            // 校验 Token
            String username = tokenValidateService.validateTokenAndGetUsername(token);
            if (username == null) {
                System.out.println("握手失败：Token无效");
                return false; // 拒绝连接
            }

            // ★★★ 认证成功，把用户信息存进 Session ★★★
            // 这里的 attributes 就是之后 ChannelInterceptor 里的 accessor.getSessionAttributes()
            attributes.put("user", username);
            attributes.put("authenticated", true);
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}