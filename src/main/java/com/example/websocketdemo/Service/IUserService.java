package com.example.websocketdemo.Service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.example.websocketdemo.DTO.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;


public  interface IUserService extends IService<User> {

    public ResponseEntity<String> SaveUser(User user, HttpSession session);

    public ResponseEntity<String> login (User user, HttpSession session, HttpServletResponse response);

    public void test(HttpServletRequest request);
}
