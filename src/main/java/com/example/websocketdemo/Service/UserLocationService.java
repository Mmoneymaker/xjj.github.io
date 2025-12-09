package com.example.websocketdemo.Service;


import com.example.websocketdemo.manager.ServerInstanceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
/* 用户关系映射
* 1.为每个用户存一个key，其中的value为该用户所在的案例的地址
* 该地址从ServerInstanceManager获得*/
public class UserLocationService {
    //去跟用户的名字进行拼接 //要注意这个Service应该是在发送的那一层被调用，不需要在拦截器中注册key
    private final String USER_LOCATION_KEY="chat:location:%s";

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ServerInstanceManager instanceManager;

    // 用户位置TTL时间（1小时），防止异常断开时位置信息永久残留
    @Value("${websocket.user.location.ttl:3600}")
    private long userLocationTtl;

    /**
     * 注册用户位置到当前实例
     * 在WebSocket连接建立后自动调用
     */
    public void registerUserLocation(String username) {
        String userKey = String.format(USER_LOCATION_KEY, username);
        String userLocation = instanceManager.getInstanceId();

        // 设置TTL，防止异常情况下位置信息永久残留
        redisTemplate.opsForValue().set(userKey, userLocation, userLocationTtl, TimeUnit.SECONDS);

        log.info("注册用户位置 - 用户: {}, 实例: {}, TTL: {}秒", username, userLocation, userLocationTtl);
    }

    /**
     * 获取用户所在的服务器实例ID
     * @param username 用户名
     * @return 实例ID，如果用户不存在则返回null
     */
    public String getUserLocation(String username) {
        String userKey = String.format(USER_LOCATION_KEY, username);
        String location = redisTemplate.opsForValue().get(userKey);

        log.debug("查询用户位置 - 用户: {}, 位置: {}", username, location);
        return location;
    }

    /**
     * 手动移除用户位置信息
     * 在WebSocket正常断开时自动调用
     */
    public void removeUserLocation(String username) {
        String userKey = String.format(USER_LOCATION_KEY, username);
        Boolean deleted = redisTemplate.delete(userKey);

        log.info("移除用户位置 - 用户: {}, 删除结果: {}", username, deleted);
    }

    /**
     * 检查用户是否在当前实例
     * @param username 用户名
     * @return true-在当前实例，false-不在或不存在
     */
    public boolean isUserOnCurrentInstance(String username) {
        String userLocation = getUserLocation(username);
        String currentInstance = instanceManager.getInstanceId();
        return currentInstance.equals(userLocation);
    }
}
