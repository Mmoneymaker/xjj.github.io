package com.example.websocketdemo.Service;

import com.example.websocketdemo.model.UserStatusEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserStatusBroadcastService {
    //这个Service是在event层被调用的,
        //存储用户在线信息Hash结构，并广播到对应地址
    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Autowired
    StringRedisTemplate redisTemplate;

    private static final String ONLINE_USERS_KEY="chat:online:users:";
    private static final String USER_INFO_KEY_PREFIX="chat:user:info:";
    //上线
    public void broadcastUserOnline(String username){
       redisTemplate.opsForSet().add(ONLINE_USERS_KEY,username);
       Map<String,String> userInfo=new HashMap<>();
       String userInfoKey=USER_INFO_KEY_PREFIX+username;
       userInfo.put("username",username);
       userInfo.put("LoginTime",String.valueOf(System.currentTimeMillis()));
       redisTemplate.opsForHash().putAll(userInfoKey,userInfo);
        UserStatusEvent event = new UserStatusEvent(
                username,
                UserStatusEvent.StatusType.ONLINE,
                System.currentTimeMillis(),
                null// 示例头像路径
        );
        messagingTemplate.convertAndSend("/topic/user-status", event);
    }

    public void broadcastUserOffline(String username) {
        // 1. 从Redis移除
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, username);

        // 3. 创建下线事件
        UserStatusEvent event = new UserStatusEvent(
                username,
                UserStatusEvent.StatusType.OFFLINE,
                System.currentTimeMillis(),
                null
        );

        // 4. 广播
        messagingTemplate.convertAndSend("/topic/user-status", event);

        redisTemplate.delete(ONLINE_USERS_KEY+username);
    }

    /**
     * 处理心跳 - 更新用户活跃时间
     */
    public void updateUserHeartbeat(String userId) {
        String userInfoKey = USER_INFO_KEY_PREFIX + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(userInfoKey))) {
            redisTemplate.expire(userInfoKey, 30, TimeUnit.MINUTES); // 续期
        }
    }

    public List<UserStatusEvent> getOnlineUsers() {
           Set<String> Onlineuser= redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
           List<UserStatusEvent> onlineUsers = new ArrayList<>();
        assert Onlineuser != null;
        for(String username:Onlineuser){
              String userInfoKey=USER_INFO_KEY_PREFIX+username;
              String logintime=(String)redisTemplate.opsForHash().get(userInfoKey,"LoginTime");

            assert logintime != null;
            UserStatusEvent event = new UserStatusEvent(
                    username,
                    UserStatusEvent.StatusType.ONLINE,
                    Long.parseLong(logintime),
                    null
            );
            onlineUsers.add(event);
          }

        return onlineUsers;
    }
}
