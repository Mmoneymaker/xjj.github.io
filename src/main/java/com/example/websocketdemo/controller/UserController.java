package com.example.websocketdemo.controller;

import com.example.websocketdemo.DTO.User;
import com.example.websocketdemo.Service.IUserService;

import com.example.websocketdemo.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @Autowired
    private IUserService userService;

    @PostMapping("/Registry")
    public Result<String> register(@RequestBody User user, HttpSession session) {
               userService.SaveUser(user);
        return Result.success("注册成功");
    }

    @PostMapping("/Login")
    public Result<String> login( @RequestBody User user, HttpSession session, HttpServletResponse response) {
        //login返回的是token，让前端去接收，方便后续websocket下跨域能用
               userService.login(user,response);
        return Result.success("登陆成功");
    }

//    @GetMapping("/TestToken")
//    public Result<String> testToken(User user,HttpServletRequest request) {
//        userService.test(request);
//        return ResponseEntity.ok().build();
//    }
}
