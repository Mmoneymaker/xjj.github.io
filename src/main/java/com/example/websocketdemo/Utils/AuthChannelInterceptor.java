package com.example.websocketdemo.Utils;

import com.example.websocketdemo.Service.UserCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;



//ChannelInterceptor是spring stomp的模块，是spring websocket的更高层次，实现了更多的功能。
@Component
@Slf4j
public class AuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    UserCacheService userCacheService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if(accessor.getCommand().equals(StompCommand.CONNECT)){
            String token = accessor.getFirstNativeHeader("Token");
            log.info("WebSocket连接建立，心跳配置: {}", accessor.getHeartbeat());
            System.out.println("前端Websocket传来的Token:"+token);
            String username=accessor.getFirstNativeHeader("username");
            System.out.println("前端Websocket传来的name:"+username);
            boolean result=(userCacheService.isValid(token,username));
            System.out.println("是否校验成功"+result);
            if (token == null || !userCacheService.isValid(token,username)) {
                // 如果 Token 无效，直接抛出异常，连接会被断开
                throw new IllegalArgumentException("无权访问：Token 无效或已过期");
            }
        }
        if (accessor.getCommand() == StompCommand.DISCONNECT) {
            log.info("WebSocket连接断开: {}", accessor.getSessionId());
        }
        return ChannelInterceptor.super.preSend(message, channel);
    }
}
