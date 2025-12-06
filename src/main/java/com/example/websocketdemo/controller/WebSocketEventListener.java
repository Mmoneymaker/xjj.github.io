package com.example.websocketdemo.controller;

import com.example.websocketdemo.Service.UserStatusBroadcastService;
import com.example.websocketdemo.model.ChatMessage;
import com.example.websocketdemo.model.UserStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.List;

/**
 * Created by rajeevkumarsingh on 25/07/17.
 */
@Slf4j
@Component
public class WebSocketEventListener {


    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    UserStatusBroadcastService userStatusBroadcastService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
         StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
         Principal user=accessor.getUser();
         String username=user.getName();
        new Thread(() -> {
            try {
                Thread.sleep(500); // 等待前端订阅
                userStatusBroadcastService.broadcastUserOnline(username);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();

        if (principal != null) {
            String username= principal.getName();

            // 广播下线事件
            userStatusBroadcastService.broadcastUserOffline(username);

            log.debug("用户断开: {}", username);
        }
    }

    @EventListener
    public void handleSubscriptionEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headers.getDestination();

        // 检查是否是订阅用户状态频道
        if ("/topic/user-status".equals(destination)) {
            Principal principal = headers.getUser();
            if (principal != null) {
                String username = principal.getName();

                // 向该用户发送当前所有在线用户列表
                new Thread(() -> {
                    try {
                        Thread.sleep(300); // 确保订阅生效
                        sendInitialOnlineList(username);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }

    private void sendInitialOnlineList(String targetUserId) {
        List<UserStatusEvent> onlineUsers = userStatusBroadcastService.getOnlineUsers();

        // 使用用户专属队列发送，避免广播
        for (UserStatusEvent user : onlineUsers) {
            messagingTemplate.convertAndSendToUser(
                    targetUserId,
                    "/queue/initial-online",
                    user
            );
        }

        log.debug("向用户 {} 发送初始在线列表，共 {} 人",
                targetUserId, onlineUsers.size());
    }


}
