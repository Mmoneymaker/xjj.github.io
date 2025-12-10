package com.example.websocketdemo.config;

import com.example.websocketdemo.Service.UserStatusBroadcastService;
import com.example.websocketdemo.model.UserStatusEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户状态消息处理器
 * 处理用户上线/下线消息
 */
@Slf4j
@Component
public class UserStatusHandler {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private UserStatusBroadcastService userStatusBroadcastService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理用户状态变化消息
     *
     * @param messageBody 消息内容（JSON格式）
     */
    public void handle(String messageBody) {
        try {
            // 解析消息
            @SuppressWarnings("unchecked")
            Map<String, Object> msgMap = objectMapper.readValue(messageBody, Map.class);
            String type = (String) msgMap.get("type");

            if ("USER_STATUS_CHANGE".equals(type)) {
                // 获取最新的在线用户列表
                List<UserStatusEvent> onlineUsers = userStatusBroadcastService.getOnlineUsers();

                // 构建广播消息
                Map<String, Object> broadcastMsg = new HashMap<>();
                broadcastMsg.put("type", "ONLINE_LIST");
                broadcastMsg.put("users", onlineUsers);
                broadcastMsg.put("timestamp", System.currentTimeMillis());

                // 广播给当前实例的所有用户
                messagingTemplate.convertAndSend("/topic/online-status", broadcastMsg);

                log.debug("通过Redis广播更新在线列表，当前在线 {} 人", onlineUsers.size());
            }
        } catch (JsonProcessingException e) {
            log.error("解析用户状态消息失败: {}", messageBody, e);
        } catch (Exception e) {
            log.error("处理用户状态消息失败", e);
        }
    }
}