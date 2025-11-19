package com.example.websocketdemo.DTO;

import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("tb_user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;

    public User(String password, String username) {
        this.password = password;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
