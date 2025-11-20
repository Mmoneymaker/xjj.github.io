package com.example.websocketdemo.Service;


import com.example.websocketdemo.DTO.User;
import com.example.websocketdemo.Utils.RedisKeyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

//Redis操作专门用UserCacheService类进行实现的目的
//1.只有Service注解才能实现redis的自动注入
//2****.可以被IUserServiceImpl注入(方便复用)，为什么方便复用还没理解
//3.可以AOP 假设想对所有Redis缓存操作加：请求日志，性能监控，统一异常处理。只需要在这个类上加上AOP切面即可。
@Service
public class UserCacheService {
    //StringRedisTemplate自动使用StringRedisSerializer进行序列化，如果想变成json格式的需要自己配置为Jackson2Json或者GenericJackson2json
    @Autowired
    private RedisTemplate<String,Object> redis;

    public void saveUser(User user) {
        redis.opsForHash().put(RedisKeyUtils.USER_HASH,
                user.getUsername(),
                user.getPassword());
    }

    public String getPassword(String username) {
        Object pwd = redis.opsForHash().get(RedisKeyUtils.USER_HASH, username);
        return pwd == null ? null : pwd.toString();
    }

    public boolean exists(String username) {
        return redis.opsForHash().hasKey(RedisKeyUtils.USER_HASH, username);
    }

    public void delete(String username) {
        redis.opsForHash().delete(RedisKeyUtils.USER_HASH, username);
    }

    public void saveToken(String token, User user) {
        redis.opsForHash().put(RedisKeyUtils.tokenKey(token),
                user.getUsername(),
                user);
    }
}
