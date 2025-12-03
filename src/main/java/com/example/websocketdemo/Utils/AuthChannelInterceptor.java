package com.example.websocketdemo.Utils;

import com.example.websocketdemo.Service.TokenValidateService;
import com.example.websocketdemo.Service.UserCacheService;
import com.example.websocketdemo.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
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
    @Autowired
    TokenValidateService tokenValidateService;
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        //CONNECT帧存信息进session
        if(accessor.getCommand().equals(StompCommand.CONNECT)) {
            String token = accessor.getFirstNativeHeader("Token");
            log.info("WebSocket连接建立，心跳配置: {}", accessor.getHeartbeat());
            log.info("前端Websocket传来的Token:"+token);
            String username=accessor.getFirstNativeHeader("username");
            log.info("前端Websocket传来的name:"+username);
            String CachedUsername=tokenValidateService.validateTokenAndGetUsername(token);
            //token方案可能需要增强
                if(CachedUsername==null){
                    throw new MessagingException("Invalid Token");
                }
            //认证成功
            accessor.getSessionAttributes().put("username",username);
            accessor.getSessionAttributes().put("authenticated",true);
        }

        if(requiresAuthentication(accessor.getCommand())){
             Boolean Authenticated= (Boolean) accessor.getSessionAttributes().get("authenticated");
             if(Authenticated==null||!Authenticated){
                 throw new MessagingException("未认证，请先建立连接");
             }
        }

        if (accessor.getCommand() == StompCommand.DISCONNECT) {
            log.info("WebSocket连接断开: {}", accessor.getSessionId());
        }

        return ChannelInterceptor.super.preSend(message, channel);
    }

    private boolean requiresAuthentication(StompCommand command) {
        return (
                        StompCommand.SUBSCRIBE.equals(command) ||
                        StompCommand.SEND.equals(command) ||
                        StompCommand.MESSAGE.equals(command)
        );
    }

    private String getTokenFromMessage(StompHeaderAccessor accessor) {
        // 从 header 中获取 token
        String token = accessor.getFirstNativeHeader("Token");
        if (token != null) {
            return token;
        }
        return null;
    }
}
