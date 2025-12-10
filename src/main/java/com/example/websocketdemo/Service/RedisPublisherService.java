package com.example.websocketdemo.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class RedisPublisherService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发布用户状态变化消息到 Redis
     */
    public void publishUserStatusChange(String username, String event, String instanceId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "USER_STATUS_CHANGE");
            message.put("username", username);
            message.put("event", event); // "ONLINE" or "OFFLINE"
            message.put("instanceId", instanceId);
            message.put("timestamp", System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend("chat:user:status", jsonMessage);

            log.info("发布用户状态变化: 用户={}, 事件={}, 实例={}", username, event, instanceId);
        } catch (Exception e) {
            log.error("发布用户状态变化失败", e);
        }
    }

    /**
     * 发布刷新在线列表消息
     */
    public void publishRefreshOnlineList(String reason) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "USER_STATUS_CHANGE");
            message.put("event", "REFRESH");
            message.put("reason", reason);
            message.put("timestamp", System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend("chat:user:status", jsonMessage);

            log.info("发布刷新在线列表消息: 原因={}", reason);
        } catch (Exception e) {
            log.error("发布刷新消息失败", e);
        }
    }
}