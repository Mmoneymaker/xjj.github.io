package com.example.websocketdemo.config;

import com.example.websocketdemo.Service.UserStatusBroadcastService;
import com.example.websocketdemo.model.UserStatusEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class RedisPubSubConfig {

    // 频道名称
    public static final String USER_STATUS_CHANNEL = "chat:user:status";

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private UserStatusBroadcastService userStatusBroadcastService;

    // 消息监听器
    @Bean
    public MessageListenerAdapter messageListenerAdapter() {
        return new MessageListenerAdapter(new MessageListener() {
            private final ObjectMapper objectMapper = new ObjectMapper();

            @Override
            public void onMessage(Message message, byte[] pattern) {
                String channel = new String(message.getChannel());
                String body = new String(message.getBody());

                log.info("收到Redis消息: 频道={}, 内容={}", channel, body);

                try {
                    // 解析消息
                    @SuppressWarnings("unchecked")
                    Map<String, Object> msgMap = objectMapper.readValue(body, Map.class);
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
                    log.error("解析Redis消息失败: {}", body, e);
                } catch (Exception e) {
                    log.error("处理Redis消息失败", e);
                }
            }
        });
    }

    // Redis消息监听容器
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic(USER_STATUS_CHANNEL));
        return container;
    }
}