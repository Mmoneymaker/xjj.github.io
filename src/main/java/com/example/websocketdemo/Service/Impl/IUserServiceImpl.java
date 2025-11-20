package com.example.websocketdemo.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.websocketdemo.DTO.User;
import com.example.websocketdemo.Service.IUserService;

import com.example.websocketdemo.Service.UserCacheService;
import com.example.websocketdemo.Utils.CookieUtils;
import com.example.websocketdemo.Utils.UserValidator;
import com.example.websocketdemo.mapper.RegistryMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import cn.hutool.core.lang.UUID;

@Service
public class IUserServiceImpl extends ServiceImpl<RegistryMapper,User> implements IUserService {

    @Autowired
    private UserCacheService userCacheService;

    //一致性策略采用cache-aside方法 写的时候先写进数据库，再删除缓存 读的时候先读缓存，读不到再读数据库，再把读到的写进缓存。
    @Override
    public ResponseEntity<String> SaveUser(User user, HttpSession session) {
        if (UserValidator.isInvalid(user)) {
            return ResponseEntity.status(405).build();
        } else {
            try {
                //这里是否可以先存到redis中缓存下来，后续再去存到数据库中
                //cache-aside机制，写的时候先写进数据库，再删除redis缓存
                save(user);
                userCacheService.delete(user.getUsername());
            } catch (Exception e) {
                if (e instanceof DuplicateKeyException) {
                    System.out.println("用户已经注册过了");//测试用，之后删掉
                    return ResponseEntity.status(403).body("用户已经注册过");
                }
            }
        }
        return ResponseEntity.ok("注册成功");
    }

    @Override
    public ResponseEntity<String>   login(User user, HttpSession session, HttpServletResponse response) {
        if(
                user.getPassword().equals(userCacheService.getPassword(user.getUsername()))
                && userCacheService.exists(user.getUsername())
        ){
            //通过cookie把token存在客户端，客户端每次自动把token放在请求头中,服务端则通过redis缓存
            String token= CookieUtils.CreateToken();
            Cookie cookie=CookieUtils.CreateCookie(token);
            response.addCookie(cookie);
            //这里保存token相关的信息，必须要用hash，hash的key和value映射为实际的DTO对象
            /*stringRedisTemplate.opsForHash().put("user:token:"+token,
                    user.getUsername(),
                    user.getPassword());*/
            userCacheService.saveToken(token,user);
            return ResponseEntity.ok(token);
        }
        //如果没在缓存中查到，就去mysql库里面查，然后再更新到redis中.
        else{
            QueryWrapper<User> wrapper = new QueryWrapper<>();
                   User ur=getOne(wrapper.eq("username",user.getUsername()));
             //      stringRedisTemplate.opsForHash().put("user:",ur.getUsername(),ur.getPassword());
                   userCacheService.saveUser(user);
        }
        return ResponseEntity.status(404).body(null);
    }

    @Override
    public void test(HttpServletRequest request){
        String token=CookieUtils.GetCookie(request);
        System.out.println("从客户端传来的autoken:"+token);
    };
}
