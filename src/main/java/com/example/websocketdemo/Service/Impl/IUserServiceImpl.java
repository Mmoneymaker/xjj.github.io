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
import org.springframework.security.crypto.bcrypt.BCrypt;
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
                String encodedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
                user.setPassword(encodedPassword);
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
    public ResponseEntity<String> login(User user, HttpSession session, HttpServletResponse response) {
        boolean loginSuccess = false;
        User loginUser = null;

        // 1. 验证逻辑
        if ( userCacheService.exists(user.getUsername())&&
                BCrypt.checkpw(user.getPassword(), userCacheService.getPassword(user.getUsername()))
        ) {
            loginSuccess = true;
            loginUser = user;
        } else {
            // 数据库查询
            try {
                QueryWrapper<User> wrapper = new QueryWrapper<>();
                User ur = getOne(wrapper.eq("username", user.getUsername()));
                if (ur != null && BCrypt.checkpw(user.getPassword(), ur.getPassword())) {
                    loginSuccess = true;
                    loginUser = ur;
                    // 更新缓存
                    userCacheService.saveUser(ur);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // 2. 登录成功处理
        if (loginSuccess && loginUser != null) {
            String token = CookieUtils.GenerateToken();
            System.out.println("后端生成的token:" + token);

            // 只通过Cookie返回，不通过响应体
            Cookie cookie = CookieUtils.CreateCookie(token);
            response.addCookie(cookie);

            userCacheService.saveToken(token, loginUser);

            return ResponseEntity.ok("登录成功"); // 不返回token
        }

        return ResponseEntity.status(401).body("用户名或密码错误");
    }
    @Override
    public void test(HttpServletRequest request){
        String token=CookieUtils.GetCookie(request);
        System.out.println("从客户端传来的autoken:"+token);
    };
}
