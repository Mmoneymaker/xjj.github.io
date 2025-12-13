package com.example.websocketdemo.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.websocketdemo.DTO.User;
import com.example.websocketdemo.Service.IUserService;

import com.example.websocketdemo.Service.UserCacheService;
import com.example.websocketdemo.Utils.CookieUtils;
import com.example.websocketdemo.Utils.TokenUtils;
import com.example.websocketdemo.Utils.UserValidator;
import com.example.websocketdemo.common.Result;
import com.example.websocketdemo.exception.BusinessException;
import com.example.websocketdemo.mapper.RegistryMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import cn.hutool.core.lang.UUID;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class IUserServiceImpl extends ServiceImpl<RegistryMapper,User> implements IUserService {

    @Autowired
    private UserCacheService userCacheService;
    @Autowired
    private TokenUtils tokenUtils;
    //一致性策略采用cache-aside方法 写的时候先写进数据库，再删除缓存 读的时候先读缓存，读不到再读数据库，再把读到的写进缓存。
    @Override
    public void SaveUser(User user) {
        // ✅ 必须保留：参数校验失败时抛出业务异常
        if (UserValidator.isInvalid(user)) {
            throw new BusinessException(400, "用户参数格式错误");
        }

        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));

        // ✅ 直接调用，让可能的异常自然抛出
        save(user);  // 可能抛出DuplicateKeyException（用户已存在）
        userCacheService.delete(user.getUsername());  // 可能抛出缓存异常

        log.info("用户注册成功: {}", user.getUsername());
        // 成功就正常结束，不返回任何东西
    }

    @Override
    public void login(User user, HttpServletResponse response) {
        validateLoginParams(user);

        // 核心逻辑：查询数据库验证用户
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", user.getUsername());
        User loginUser = getOne(wrapper);

        if (loginUser == null) {
            throw new BusinessException(401, "用户名不存在");
        }

        if (!BCrypt.checkpw(user.getPassword(), loginUser.getPassword())) {
            throw new BusinessException(401, "密码错误");
        }

        // 登录成功，更新缓存
        userCacheService.saveUser(loginUser);

        // 生成token和设置Cookie
        processSuccessfulLogin(loginUser, response);
    }

    private void processSuccessfulLogin(User loginUser, HttpServletResponse response) {
        String token = tokenUtils.generateJwtToken(loginUser.getUsername());
        log.info("用户登录成功: {}, token: {}", loginUser.getUsername(), token);

        Cookie cookie = CookieUtils.CreateUserCookie(token, loginUser.getUsername());
        response.addCookie(cookie);
        //30分钟
        userCacheService.saveJwtTokenWithExpire(token, loginUser,1800);
    }
//    @Override
//    public void test(HttpServletRequest request){
//        String token=CookieUtils.GetCookie(request);
//        System.out.println("从客户端传来的autoken:"+token);
//    };


    private void validateLoginParams(User user) {
        if (user == null) {
            throw new BusinessException(400, "用户信息不能为空");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new BusinessException(400, "密码不能为空");
        }
    }
}
