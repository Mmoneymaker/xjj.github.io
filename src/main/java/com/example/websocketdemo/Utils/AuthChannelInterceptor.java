package com.example.websocketdemo.Utils;

import com.example.websocketdemo.Service.TokenValidateService;
import com.example.websocketdemo.Service.UserCacheService;
import com.example.websocketdemo.exception.BusinessException;
import com.example.websocketdemo.model.StompPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;



//ChannelInterceptor是spring stomp的模块，是spring websocket的更高层次，实现了更多的功能。
@Component
@Slf4j
public class AuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    UserCacheService userCacheService; // 确保这里没爆红，如果有，检查包导入

    @Autowired
    TokenValidateService tokenValidateService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 1. 加上 try-catch，否则报错你看不到
        try {
            // 【关键修改1】不要用 wrap，要用 getAccessor
            // wrap 出来的对象可能是只读的，或者修改后无法回写到 message 中
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
//StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            // 防御性编程：accessor 可能是 null
            if (accessor == null) {
                return message;
            }

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String token = accessor.getFirstNativeHeader("Token");
                log.info("WebSocket连接建立，Token: {}", token);

                // 校验 Token
                if (token == null || token.trim().isEmpty()) {
                    log.warn("Token为空，拒绝连接");
                    return null;
                }

                String cachedUsername = tokenValidateService.validateTokenAndGetUsername(token);
                log.info("Token校验通过，用户: {}", cachedUsername);

                if (cachedUsername != null) {
                    // 【关键修改2】确保 StompPrincipal 类存在且实现了 Principal 接口
                    StompPrincipal principal = new StompPrincipal(cachedUsername);
                    accessor.setUser(principal);
                    log.info("Principal 设置成功: {}", principal.getName());
                } else {
                    log.warn("用户名解析为空，拒绝连接");
                    return null;
                }
            }
        } catch (Exception e) {
            // 【关键修改3】打印真正的错误堆栈
            log.error("WebSocket拦截器发生严重错误!!!", e);
            return null; // 发生异常时断开连接
        }
        return message;
    }
}
