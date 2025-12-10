package com.example.websocketdemo.config;

import com.example.websocketdemo.config.RedisPubSubConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Redis消息分发器
 * 根据频道将消息分发到不同的处理器
 */
@Slf4j
@Component
public class RedisMessageDispatcher {

    @Autowired
    private UserStatusHandler userStatusHandler;

    @Autowired
    private MessageRouteHandler messageRouteHandler;

    /**
     * 分发消息到对应的处理器
     *
     * @param channel Redis频道名称
     * @param message 消息内容
     */
    public void dispatch(String channel, String message) {
        log.debug("收到Redis消息: 频道={}, 内容={}", channel, message);

        try {
            switch (channel) {
                case RedisPubSubConfig.USER_STATUS_CHANNEL:
                    userStatusHandler.handle(message);
                    break;

                case RedisPubSubConfig.MESSAGE_CHANNEL:
                    messageRouteHandler.handle(message);
                    break;

                default:
                    log.warn("未知的Redis频道: {}", channel);
            }
        } catch (Exception e) {
            log.error("处理Redis消息失败: 频道={}, 消息={}", channel, message, e);
        }
    }
}