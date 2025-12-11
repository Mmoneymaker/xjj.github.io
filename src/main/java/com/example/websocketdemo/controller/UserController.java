package com.example.websocketdemo.controller;

import com.example.websocketdemo.DTO.User;
import com.example.websocketdemo.Service.IUserService;

import com.example.websocketdemo.Service.UserStatusBroadcastService;
import com.example.websocketdemo.common.Result;
import com.example.websocketdemo.model.UserStatusEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.dao.DuplicateKeyException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private SimpUserRegistry userRegistry;

    @Autowired
    private IUserService userService;

    @Autowired
    UserStatusBroadcastService userStatusBroadcastService;
//    // 新增这个接口 替换成了websocketEventListener的事件触发形式,并做了分布式处理
//    @GetMapping("/api/online-users")
//    public List<String> getOnlineUsers() {
//        return userRegistry.getUsers().stream()
//                .map(SimpUser::getName)
//                .collect(Collectors.toList());
//    }
    /**
     * 新增：获取在线人数
     */
    @GetMapping("/online-count")
    public Map<String, Object> getOnlineCount() {
        List<UserStatusEvent> events = userStatusBroadcastService.getOnlineUsers();
        return Map.of(
                "count", events.size(),
                "timestamp", System.currentTimeMillis()
        );
    }
    @PostMapping("/Registry")
    public Result<String> register(@RequestBody User user, HttpSession session) {
        try {
            userService.SaveUser(user);
            return Result.success("注册成功");
        } catch (DuplicateKeyException e) {
            return Result.error(403, "用户名已存在");
        }
    }

    @PostMapping("/Login")
    public Result<String> login( @RequestBody User user, HttpSession session, HttpServletResponse response) {
        //login返回的是token，让前端去接收，方便后续websocket下跨域能用
               userService.login(user,response);
        return Result.success("登陆成功");
    }

    /**
     * 获取在线用户列表
     */
    @GetMapping("/online")
    public Result<List<Map<String, Object>>> getOnlineUsers() {
        try {
            // 获取在线用户列表
            List<UserStatusEvent> onlineUsers = userStatusBroadcastService.getOnlineUsers();

            log.info("获取在线用户列表，当前在线人数: {}", onlineUsers.size());

            // 转换为前端需要的格式
            List<Map<String, Object>> userList = onlineUsers.stream()
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("username", user.getUsername());
                    userInfo.put("status", user.getStatus());
                    userInfo.put("timestamp", user.getTimestamp());
                    return userInfo;
                })
                .collect(Collectors.toList());

            return Result.success(userList);
        } catch (Exception e) {
            log.error("获取在线用户失败", e);
            return Result.error(500, "获取在线用户失败: " + e.getMessage());
        }
    }
}
