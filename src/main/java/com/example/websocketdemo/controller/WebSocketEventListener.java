package com.example.websocketdemo.controller;

import com.example.websocketdemo.Service.UserStatusBroadcastService;
import com.example.websocketdemo.Service.UserLocationService;
import com.example.websocketdemo.model.UserStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WebSocketEventListener {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private UserStatusBroadcastService userStatusBroadcastService;

    @Autowired
    private UserLocationService userLocationService;

    // 使用 @Qualifier 指定使用哪个 TaskScheduler
    @Autowired
    @Qualifier("messageBrokerTaskScheduler") // 或者 "heartBeatScheduler"
    private TaskScheduler taskScheduler;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();

        if (user != null) {
            String username = user.getName();
            log.info("用户连接: {}", username);

            // 立即更新Redis（用户上线）
            userStatusBroadcastService.userOnline(username);

            // 🎯 新增：注册用户位置到当前实例
            userLocationService.registerUserLocation(username);
            log.info("已注册用户 {} 的位置信息", username);

            // ✅ 关键：延迟500ms后广播，确保前端已订阅
            taskScheduler.schedule(() -> {
                broadcastOnlineList();
            }, Instant.now().plusMillis(500));
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();

        if (principal != null) {
            String username = principal.getName();
            log.info("用户断开: {}", username);

            // 用户下线
            userStatusBroadcastService.userOffline(username);

            // 🎯 新增：清理用户位置信息
            userLocationService.removeUserLocation(username);
            log.info("已清理用户 {} 的位置信息", username);

            // 立即广播最新列表
            broadcastOnlineList();
        }
    }

    /**
     * 监听订阅事件 - 当用户订阅 /topic/online-status 时触发
     * ✅ 新增：确保用户一订阅就收到当前列表
     */
    @EventListener
    public void handleSubscriptionEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headers.getDestination();

        // 检查是否是订阅在线状态频道
        if ("/topic/online-status".equals(destination)) {
            Principal principal = headers.getUser();
            if (principal != null) {
                String username = principal.getName();
                log.debug("用户 {} 订阅了在线状态频道", username);

                // ✅ 立即发送当前在线列表给这个用户
                taskScheduler.schedule(() -> {
                    sendOnlineListToUser(username);
                }, Instant.now().plusMillis(300));
            }
        }
    }

    /**
     * 发送在线列表给指定用户（私有消息）
     */
    private void sendOnlineListToUser(String username) {
        try {
            List<UserStatusEvent> onlineUsers = userStatusBroadcastService.getOnlineUsers();

            Map<String, Object> message = new HashMap<>();
            message.put("type", "ONLINE_LIST");
            message.put("users", onlineUsers);
            message.put("timestamp", System.currentTimeMillis());

            // 发送给特定用户
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/online-list",
                    message
            );

            log.debug("向用户 {} 发送在线列表，共 {} 人", username, onlineUsers.size());
        } catch (Exception e) {
            log.error("发送在线列表给用户失败: {}", username, e);
        }
    }

    /**
     * 广播当前在线用户列表给所有人
     */
    private void broadcastOnlineList() {
        try {
            List<UserStatusEvent> onlineUsers = userStatusBroadcastService.getOnlineUsers();

            Map<String, Object> message = new HashMap<>();
            message.put("type", "ONLINE_LIST");
            message.put("users", onlineUsers);
            message.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/online-status", message);

            log.debug("广播在线列表，共 {} 人在线", onlineUsers.size());
        } catch (Exception e) {
            log.error("广播在线列表失败", e);
        }
    }
}