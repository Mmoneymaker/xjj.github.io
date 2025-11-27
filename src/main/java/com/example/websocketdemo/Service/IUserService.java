package com.example.websocketdemo.Service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.example.websocketdemo.DTO.User;
import com.example.websocketdemo.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;


public  interface IUserService extends IService<User> {

    public void SaveUser(User user);

    public void login (User user,  HttpServletResponse response);

//    public void test(HttpServletRequest request);
}
