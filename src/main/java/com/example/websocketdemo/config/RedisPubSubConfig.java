package com.example.websocketdemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Slf4j
@Configuration
public class RedisPubSubConfig {

    // 频道名称
    public static final String USER_STATUS_CHANNEL = "chat:user:status";
    public static final String MESSAGE_CHANNEL = "chat:message:route";

    @Autowired
    private UserStatusHandler userStatusHandler;

    @Autowired
    private MessageRouteHandler messageRouteHandler;

    // 用户状态监听器适配器
    @Bean
    public MessageListenerAdapter userStatusListenerAdapter() {
        MessageListenerAdapter adapter = new MessageListenerAdapter(userStatusHandler, "handle");
        adapter.setDefaultListenerMethod("handle");  // 明确指定方法名
        return adapter;
    }

    // 消息路由监听器适配器
    @Bean
    public MessageListenerAdapter messageRouteListenerAdapter() {
        MessageListenerAdapter adapter = new MessageListenerAdapter(messageRouteHandler, "handle");
        adapter.setDefaultListenerMethod("handle");  // 明确指定方法名
        return adapter;
    }

    // Redis消息监听容器 - 一个容器监听多个频道
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter userStatusListenerAdapter,
            MessageListenerAdapter messageRouteListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 添加用户状态监听器
        container.addMessageListener(
            userStatusListenerAdapter,
            new PatternTopic(USER_STATUS_CHANNEL)
        );

        // 添加消息路由监听器
        container.addMessageListener(
            messageRouteListenerAdapter,
            new PatternTopic(MESSAGE_CHANNEL)
        );

        log.info("Redis Pub/Sub 容器初始化完成，监听频道: {} 和 {}", USER_STATUS_CHANNEL, MESSAGE_CHANNEL);

        return container;
    }
}