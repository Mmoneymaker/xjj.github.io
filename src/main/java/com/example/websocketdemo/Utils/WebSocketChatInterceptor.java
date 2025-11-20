package com.example.websocketdemo.Utils;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
//ChannelInterceptor是spring stomp的模块，是spring websocket的更高层次，实现了更多的功能。
public class WebSocketChatInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if(accessor.getCommand().equals(StompCommand.SEND)){
            System.out.println("Token:"+message.getHeaders().toString());

            throw new IllegalArgumentException("无效 token，连接拒绝");
        }
        return ChannelInterceptor.super.preSend(message, channel);
    }
}
