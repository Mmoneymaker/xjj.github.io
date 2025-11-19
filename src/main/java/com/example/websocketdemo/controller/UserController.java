package com.example.websocketdemo.controller;

import com.example.websocketdemo.DTO.User;
import com.example.websocketdemo.Service.IUserService;

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
    public ResponseEntity<String> register(@RequestBody User user, HttpSession session) {

        return userService.SaveUser(user,session);
    }

    @PostMapping("/Login")
    public ResponseEntity<String> login(@RequestBody User user, HttpSession session, HttpServletResponse response) {
        //login返回的是token，让前端去接收，方便后续websocket下跨域能用
        return userService.login(user,session,response);
    }

    @GetMapping("/TestToken")
    public ResponseEntity<String> testToken(User user,HttpServletRequest request) {
        userService.test(request);
        return ResponseEntity.ok().build();
    }
}
