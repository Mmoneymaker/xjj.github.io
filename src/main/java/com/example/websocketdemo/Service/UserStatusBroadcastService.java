package com.example.websocketdemo.Service;

import com.example.websocketdemo.model.UserStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserStatusBroadcastService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String ONLINE_USERS_KEY = "chat:online:users";
    private static final String USER_INFO_PREFIX = "chat:user:";

    /**
     * 用户上线
     */
    public void userOnline(String username) {
        try {
            // 1. 添加到在线集合
            redisTemplate.opsForSet().add(ONLINE_USERS_KEY, username);

            // 2. 存储用户信息（30分钟过期）
            String userKey = USER_INFO_PREFIX + username;
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("onlineTime", String.valueOf(System.currentTimeMillis()));
            redisTemplate.opsForHash().putAll(userKey, userInfo);
            redisTemplate.expire(userKey, 30, TimeUnit.MINUTES);

            log.info("用户上线: {}", username);
        } catch (Exception e) {
            log.error("用户上线失败: {}", username, e);
        }
    }

    /**
     * 用户下线
     */
    public void userOffline(String username) {
        try {
            // 1. 从在线集合移除
            redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, username);

            // 2. 删除用户信息
            String userKey = USER_INFO_PREFIX + username;
            redisTemplate.delete(userKey);

            log.info("用户下线: {}", username);
        } catch (Exception e) {
            log.error("用户下线失败: {}", username, e);
        }
    }

    /**
     * 获取所有在线用户
     */
    public List<UserStatusEvent> getOnlineUsers() {
        try {
            Set<String> usernames = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);

            if (usernames == null || usernames.isEmpty()) {
                return Collections.emptyList();
            }

            List<UserStatusEvent> users = new ArrayList<>();
            for (String username : usernames) {
                UserStatusEvent event = new UserStatusEvent();
                event.setUsername(username);
                event.setStatus(UserStatusEvent.StatusType.ONLINE);
                event.setTimestamp(System.currentTimeMillis());
                users.add(event);
            }

            return users;
        } catch (Exception e) {
            log.error("获取在线用户失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        try {
            Long count = redisTemplate.opsForSet().size(ONLINE_USERS_KEY);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取在线用户数失败", e);
            return 0;
        }
    }
}