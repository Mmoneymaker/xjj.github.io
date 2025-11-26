package com.example.websocketdemo.Utils;

import com.example.websocketdemo.Service.UserCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

//ChannelInterceptor是spring stomp的模块，是spring websocket的更高层次，实现了更多的功能。
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    UserCacheService userCacheService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if(accessor.getCommand().equals(StompCommand.CONNECT)){
            System.out.println("Token:"+message.getHeaders().toString());
            String token = accessor.getFirstNativeHeader("Token");
            String username=accessor.getFirstNativeHeader("username");
            if (token == null || !userCacheService.isValid(token,username)) {
                // 如果 Token 无效，直接抛出异常，连接会被断开
                throw new IllegalArgumentException("无权访问：Token 无效或已过期");
            }
        }
        return ChannelInterceptor.super.preSend(message, channel);
    }
}
