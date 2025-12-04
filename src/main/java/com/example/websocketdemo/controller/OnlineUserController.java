//package com.example.websocketdemo.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.messaging.simp.user.SimpUserRegistry;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.messaging.simp.user.SimpUser;
//import org.springframework.messaging.simp.user.SimpUserRegistry;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.Set;
//import java.util.stream.Collectors;
//
//// 新建一个文件：OnlineUserController.java
//@RestController
//@RequestMapping("/api")
//@RequiredArgsConstructor
//public class OnlineUserController {
//
//    private final SimpUserRegistry simpUserRegistry;  // Spring 自带的在线用户注册表
//
//    @GetMapping("/online-users")
//    public Set<String> getOnlineUsers() {
//        return simpUserRegistry.getUsers()
//                .stream()
//                .map(SimpUser::getName)
//                .collect(Collectors.toSet());
//    }
//}